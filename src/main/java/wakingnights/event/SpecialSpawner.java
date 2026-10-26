package wakingnights.event;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.SpecialType;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.level.NightTracker;
import wakingnights.level.WakingNightsWorldData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * During a Waking Night (once the sun is gone), a small share of the hostile mobs spawning near a player turns
 * blooded or blighted (per the night type) until dawn, with limits per player: alive at once, time between two,
 * and a breather after the player dies.
 * Special mobs hunt the nearest player for as long as the Waking Night lasts (HuntPlayerTask).
 * Catches natural spawns and Bloodmoon's, which both go through Forge's special spawn event (also when another mod
 * cancelled it). Creative and spectator players don't get any.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public final class SpecialSpawner {
    private static final double FOLLOW_RANGE = 64.0D;
    private static final UUID HEALTH_ID = UUID.fromString("6b1f1d52-3c2a-4f0e-9a7e-1b4c2e5d8f01");
    private static final UUID DAMAGE_ID = UUID.fromString("6b1f1d52-3c2a-4f0e-9a7e-1b4c2e5d8f02");
    private static final UUID SPEED_ID = UUID.fromString("6b1f1d52-3c2a-4f0e-9a7e-1b4c2e5d8f03");
    private static final UUID KNOCKBACK_ID = UUID.fromString("6b1f1d52-3c2a-4f0e-9a7e-1b4c2e5d8f04");
    private static final UUID ARMOR_ID = UUID.fromString("6b1f1d52-3c2a-4f0e-9a7e-1b4c2e5d8f05");
    private static final Random RAND = new Random();
    private static final Map<UUID, PlayerTally> TALLIES = new HashMap<>();
    //What happened to the hostile spawns this Waking Night, for /wakingnights status
    private static final Map<String, Integer> OUTCOMES = new HashMap<>();

    private SpecialSpawner() {}

    /** A new Waking Night starts counting from zero */
    public static void reset() {
        TALLIES.clear();
        OUTCOMES.clear();
    }

    /** What happened to the hostile spawns seen this Waking Night, e.g. "turned special: 4, chance: 30" */
    public static String getOutcomes() {
        if(OUTCOMES.isEmpty()) return "no hostile spawns seen yet";
        StringBuilder builder = new StringBuilder();
        OUTCOMES.forEach((outcome, count) -> builder.append(builder.length() > 0 ? ", " : "").append(outcome).append(": ").append(count));
        return builder.toString();
    }

    private static void count(String outcome) {
        OUTCOMES.merge(outcome, 1, Integer::sum);
    }

    /** Why there was no player in range: how far the nearest one was, or none in that dimension at all */
    private static void countOutOfRange(World world, double x, double y, double z) {
        EntityPlayer nearest = world.getClosestPlayer(x, y, z, -1.0D, false);
        if(nearest == null) {
            count("no player in that dimension (" + world.provider.getDimension() + ")");
            return;
        }
        double distance = Math.sqrt(nearest.getDistanceSq(x, y, z));
        double height = Math.abs(nearest.posY - y);
        count(distance <= 128.0D ? "out of range, 64-128 blocks" : "out of range, over 128 blocks");
        if(height > 32.0D) count("of those, over 32 blocks above or below");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        World world = event.getWorld();
        //Cancelling this event only skips the mob's own spawn setup (mods like Infernal Mobs or Champions do that),
        //the mob still spawns, so a cancelled one counts too
        if(world.isRemote) return;
        WakingNightsWorldData data = WakingNightsWorldData.get(world);
        if(!data.isEventActive()) return;
        if(!(mob instanceof IMob)) {
            ResourceLocation id = EntityList.getKey(mob);
            count("not marked hostile (" + (id == null ? mob.getClass().getSimpleName() : id) + ")");
            return;
        }
        if(isBlacklisted(mob)) {
            count("blacklisted");
            return;
        }
        if(CapabilitySpecialEntityHandler.isSpecial(mob)) return;

        if(!NightTracker.isAfterNightfall(world.getWorldTime())) {
            count("before nightfall");
            return;
        }
        NightType type = NightType.get(data.getCurrentType());
        if(type == null || (type.special == null && !type.mixedSpecial)) {
            count("night type makes none");
            return;
        }

        //Where the mob really is: the event's own coordinates aren't always passed in the same way by mods' spawners
        if(mob.getDistanceSq(event.getX(), event.getY(), event.getZ()) > 4.0D) count("event position differs from mob");
        EntityPlayer player = world.getClosestPlayer(mob.posX, mob.posY, mob.posZ, ForgeConfigHandler.server.specialRange, false);
        if(player == null) {
            countOutOfRange(world, mob.posX, mob.posY, mob.posZ);
            return;
        }
        if(player.isSpectator() || player.isCreative()) {
            count("player in creative");
            return;
        }
        PlayerTally tally = TALLIES.computeIfAbsent(player.getUniqueID(), id -> new PlayerTally());
        long now = world.getTotalWorldTime();
        if(!tally.canHaveMore(now)) {
            count("player limit or wait");
            return;
        }
        if(RAND.nextDouble() * 100.0D >= ForgeConfigHandler.server.specialChance) {
            count("chance");
            return;
        }
        count("turned special");

        SpecialType special = type.mixedSpecial ? (RAND.nextBoolean() ? SpecialType.BLOODED : SpecialType.BLIGHTED) : type.special;
        //Not in the world yet: the type goes to clients when they start tracking it, the hunt starts once it joins
        CapabilitySpecialEntityHandler.setType(mob, special);
        tally.add(mob, now);
    }

    /**
     * Special mobs get their hunt, also when they come back from a saved chunk. One that doesn't belong in the night
     * it comes back in (see shouldVanish) doesn't come back at all.
     */
    @SubscribeEvent
    public static void onJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if(event.getWorld().isRemote || !(entity instanceof EntityLiving)) return;
        SpecialType type = CapabilitySpecialEntityHandler.getType(entity);
        if(type == SpecialType.NONE) return;
        if(!isEventSpawn(entity) && shouldVanish(type, WakingNightsWorldData.get(event.getWorld()))) {
            event.setCanceled(true);
            return;
        }
        EntityLiving mob = (EntityLiving) entity;
        IAttributeInstance followRange = mob.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        if(followRange != null && followRange.getBaseValue() < FOLLOW_RANGE) followRange.setBaseValue(FOLLOW_RANGE);
        applyBoosts(mob);
        mob.targetTasks.addTask(0, new HuntPlayerTask(mob));
        mob.tasks.addTask(0, new FreezeWhenWatchedTask(mob));
        if(ForgeConfigHandler.server.specialBreakDoors) {
            //Zombies have their own door breaking, only switched off
            if(mob instanceof EntityZombie) ((EntityZombie) mob).setBreakDoorsAItask(true);
            else if(mob.getNavigator() instanceof PathNavigateGround) {
                ((PathNavigateGround) mob.getNavigator()).setBreakDoors(true);
                mob.tasks.addTask(1, new EntityAIBreakDoor(mob));
            }
        }
    }

    /** Special mobs killed by a player drop their kind's rewards (Blooded / Blighted Drops) */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        EntityLivingBase mob = event.getEntityLiving();
        if(mob.world.isRemote || !(event.getSource().getTrueSource() instanceof EntityPlayer)) return;
        SpecialType type = CapabilitySpecialEntityHandler.getType(mob);
        if(type == SpecialType.NONE) return;

        String[] drops = type == SpecialType.BLOODED ? ForgeConfigHandler.server.bloodedDrops : ForgeConfigHandler.server.blightedDrops;
        for(String entry : drops) {
            String[] parts = entry.trim().split("\\s+");
            if(parts.length != 2) {
                WakingNights.LOGGER.warn("Invalid special mob drop, should be <item> <chance>: {}", entry);
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0]));
            if(item == null) continue;
            try {
                if(RAND.nextDouble() * 100.0D >= Double.parseDouble(parts[1])) continue;
            }
            catch(NumberFormatException e) {
                WakingNights.LOGGER.warn("Invalid special mob drop chance: {}", entry);
                continue;
            }
            event.getDrops().add(new EntityItem(mob.world, mob.posX, mob.posY, mob.posZ, new ItemStack(item)));
        }
    }

    /** Special mobs that hit a player (also with an arrow) give them the night's hit effects */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if(!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote || event.getAmount() <= 0) return;
        Entity attacker = event.getSource().getTrueSource();
        if(!(attacker instanceof EntityLiving) || !CapabilitySpecialEntityHandler.isSpecial(attacker)) return;

        NightType type = NightType.get(WakingNightsWorldData.get(attacker.world).getCurrentType());
        List<HitEffect> effects = type != null && type.hitEffects != null
                ? type.hitEffects
                : HitEffect.parseAll(Arrays.asList(ForgeConfigHandler.server.specialHitEffects));
        for(HitEffect effect : effects) {
            PotionEffect potion = effect.create();
            if(potion != null) event.getEntityLiving().addPotionEffect(potion);
        }
    }

    /**
     * Special mobs are tougher. Attribute modifiers with fixed ids: saved with the mob, so they're only ever added once.
     * A mob that just got its health boost is healed up to it.
     */
    public static void applyBoosts(EntityLiving mob) {
        boolean healthAdded = addModifier(mob, SharedMonsterAttributes.MAX_HEALTH, HEALTH_ID, "Waking Nights health", ForgeConfigHandler.server.specialHealthMultiplier - 1.0D, 1);
        addModifier(mob, SharedMonsterAttributes.ATTACK_DAMAGE, DAMAGE_ID, "Waking Nights damage", ForgeConfigHandler.server.specialDamageMultiplier - 1.0D, 1);
        addModifier(mob, SharedMonsterAttributes.MOVEMENT_SPEED, SPEED_ID, "Waking Nights speed", ForgeConfigHandler.server.specialSpeedMultiplier - 1.0D, 1);
        addModifier(mob, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Waking Nights knockback resistance", ForgeConfigHandler.server.specialKnockbackResistance, 0);
        addModifier(mob, SharedMonsterAttributes.ARMOR, ARMOR_ID, "Waking Nights armor", ForgeConfigHandler.server.specialArmor, 0);
        if(healthAdded) mob.setHealth(mob.getMaxHealth());
    }

    /** Operation 0 adds the amount, 1 adds that share of the base value. False if the mob already had it or lacks the attribute. */
    private static boolean addModifier(EntityLiving mob, IAttribute attribute, UUID id, String name, double amount, int operation) {
        IAttributeInstance instance = mob.getEntityAttribute(attribute);
        if(instance == null || instance.getModifier(id) != null || amount == 0.0D) return false;
        instance.applyModifier(new AttributeModifier(id, name, amount, operation));
        return true;
    }

    /**
     * A Waking Night ended or another started: loaded special mobs that don't belong in what's going on now vanish,
     * in a puff of smoke
     */
    public static void removeStrays(WakingNightsWorldData data) {
        for(WorldServer world : DimensionManager.getWorlds()) {
            for(Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if(!(entity instanceof EntityLiving) || entity.isDead || isEventSpawn(entity)) continue;
                SpecialType type = CapabilitySpecialEntityHandler.getType(entity);
                if(type == SpecialType.NONE || !shouldVanish(type, data)) continue;
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ,
                        12, entity.width * 0.5D, entity.height * 0.4D, entity.width * 0.5D, 0.02D);
                entity.setDead();
            }
        }
    }

    /** A night type's own spawns follow their own rule: they go when their night does (EventSpawner) */
    private static boolean isEventSpawn(Entity entity) {
        return entity.getEntityData().hasKey(EventSpawner.EVENT_TAG);
    }

    /**
     * A special mob only belongs in a Waking Night that makes its kind (a blooded mob in a bloodmoon, not in a blight),
     * unless its kind is kept outside its nights (a night type making it has despawn=false)
     */
    private static boolean shouldVanish(SpecialType type, WakingNightsWorldData data) {
        NightType current = NightType.get(data.getCurrentType());
        if(current != null && current.produces(type)) return false;
        return !NightType.isKept(type);
    }

    private static boolean isBlacklisted(EntityLiving mob) {
        if(!mob.isNonBoss()) return true;
        if(ForgeConfigHandler.server.specialExcludeRanged && mob instanceof IRangedAttackMob) return true;
        ResourceLocation id = EntityList.getKey(mob);
        return id != null && Arrays.asList(ForgeConfigHandler.server.specialBlacklist).contains(id.toString());
    }

    /** A player who died gets a breather before the next special mob comes for them */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if(!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        TALLIES.computeIfAbsent(player.getUniqueID(), id -> new PlayerTally()).blockedUntil =
                player.world.getTotalWorldTime() + ForgeConfigHandler.server.specialDeathGraceSeconds * 20L;
    }

    /** One player's special mobs this Waking Night */
    private static class PlayerTally {
        private final List<WeakReference<EntityLiving>> alive = new ArrayList<>();
        //Long ago, but not Long.MIN_VALUE: now - last would overflow and look like a moment ago
        private long last = -1000000L;
        private long blockedUntil = 0L;

        boolean canHaveMore(long now) {
            for(Iterator<WeakReference<EntityLiving>> it = this.alive.iterator(); it.hasNext(); ) {
                EntityLiving mob = it.next().get();
                if(mob == null || mob.isDead) it.remove();
            }
            return this.alive.size() < ForgeConfigHandler.server.specialMaxAlive
                    && now >= this.blockedUntil
                    && now - this.last >= ForgeConfigHandler.server.specialSecondsBetween * 20L;
        }

        void add(EntityLiving mob, long now) {
            this.alive.add(new WeakReference<>(mob));
            this.last = now;
        }
    }
}
