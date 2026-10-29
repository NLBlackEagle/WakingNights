package wakingnights.event;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.SpecialType;
import wakingnights.level.NightTracker;
import wakingnights.level.WakingNightsWorldData;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * A night type's own spawns (spawns=... in its config entry), once the sun is gone: every spawnInterval seconds a
 * group (spawnGroup) picked from its weighted list spawns around each player, up to spawnCap around them. Out of
 * sight like vanilla at first; spawnInSightAfter lets them also spawn in view after that many seconds, and
 * spawnCloseAtHalf brings them in closer and closer until halfway through the night. The first few times a night
 * happens there are fewer and weaker ones (ramp). spawnSpecialChance of them (all by default) are
 * special (blooded or blighted at random), so they hunt the player.
 * They're marked in their ForgeData, which is saved with them:
 * - "EagleMixinsOverworldAllowed": EagleMixins doesn't kill them for being outside their biomes (SRP mobs in the overworld)
 * - "wakingnights_event": the night type they belong to. They're removed when that night is over, both the loaded ones
 *   and any that load in later (from a chunk that wasn't loaded at the end): they must never outlive their night.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public final class EventSpawner {
    public static final String EVENT_TAG = "wakingnights_event";
    //A Lycanites wave's number (LycanitesHook): once a newer wave started, it's a stray too
    public static final String WAVE_TAG = "wakingnights_wave";
    public static final String OVERWORLD_ALLOWED_TAG = "EagleMixinsOverworldAllowed";

    //Out of sight like vanilla: at least this far, and not where the player can see
    private static final double MIN_DISTANCE = 24.0D;
    private static final double MAX_DISTANCE = 48.0D;
    //From halfway through the night with spawnCloseAtHalf: this close at least
    private static final double CLOSE_DISTANCE = 6.0D;
    private static final int PLACE_ATTEMPTS = 8;
    //A group spawns around one spot, each within this many blocks of it
    private static final int GROUP_SPREAD = 4;
    //Not in lit places such as bases
    private static final int MAX_BLOCK_LIGHT = 7;
    //Within this many degrees of where the player looks counts as in view, unless something blocks it
    private static final double VIEW_ANGLE = 70.0D;

    private static final UUID RAMP_ID = UUID.fromString("6b1f1d52-3c2a-4f0e-9a7e-1b4c2e5d8f06");
    private static final Random RAND = new Random();
    private static final Map<UUID, List<WeakReference<Entity>>> SPAWNED = new HashMap<>();
    //World time each player's next group may spawn, and how many may be around them this night
    private static final Map<UUID, Long> NEXT_GROUP = new HashMap<>();
    private static final Map<UUID, Integer> CAPS = new HashMap<>();

    private EventSpawner() {}

    /** A new Waking Night starts with none */
    public static void reset() {
        SPAWNED.clear();
        NEXT_GROUP.clear();
        CAPS.clear();
    }

    /** Called every tick once the sun is gone during a Waking Night (world: the overworld, whose clock it goes by) */
    public static void tick(World world, WakingNightsWorldData data, List<EntityPlayerMP> players) {
        long now = world.getTotalWorldTime();
        for(EntityPlayerMP player : players) {
            if(player.isSpectator() || player.isCreative()) continue;
            //The night type of the player's dimension
            String typeId = data.getType(player.dimension);
            NightType type = typeId == null ? null : NightType.get(typeId);
            if(type == null || type.getSpawns(player.dimension).isEmpty() || type.spawnIntervalMax <= 0) continue;
            //The first few times this night happens, fewer of them (ramp)
            double ramp = type.getRamp(data.getTimesHappened(type.id));
            UUID id = player.getUniqueID();
            Long next = NEXT_GROUP.get(id);
            if(next != null && now < next) continue;
            NEXT_GROUP.put(id, now + (long) (randomBetween(type.spawnIntervalMin, type.spawnIntervalMax) * 20.0D));
            int cap = CAPS.computeIfAbsent(id, uuid -> scale(type.spawnCapMin + RAND.nextInt(type.spawnCapMax - type.spawnCapMin + 1), ramp));
            int room = cap - countAlive(player);
            if(room <= 0) continue;
            int group = Math.min(room, scale(type.spawnGroupMin + RAND.nextInt(type.spawnGroupMax - type.spawnGroupMin + 1), ramp));
            spawnGroup(player.world, type, player, group, ramp);
        }
    }

    /** A count times the ramp, at least 1 */
    private static int scale(int count, double ramp) {
        return Math.max(1, (int) Math.round(count * ramp));
    }

    private static double randomBetween(double min, double max) {
        return min + RAND.nextDouble() * (max - min);
    }

    private static int countAlive(EntityPlayer player) {
        List<WeakReference<Entity>> list = SPAWNED.get(player.getUniqueID());
        if(list == null) return 0;
        for(Iterator<WeakReference<Entity>> it = list.iterator(); it.hasNext(); ) {
            Entity entity = it.next().get();
            if(entity == null || entity.isDead) it.remove();
        }
        return list.size();
    }

    /** One spot around the player, the whole group around that spot */
    private static void spawnGroup(World world, NightType type, EntityPlayer player, int size, double ramp) {
        Placement placement = getPlacement(world, type);
        BlockPos center = findSpot(world, player, placement);
        if(center == null) return;
        List<WeakReference<Entity>> spawned = SPAWNED.computeIfAbsent(player.getUniqueID(), id -> new ArrayList<>());
        for(int i = 0; i < size; i++) {
            BlockPos near = i == 0 ? center : findGround(world, center.add(RAND.nextInt(GROUP_SPREAD * 2 + 1) - GROUP_SPREAD, 0,
                    RAND.nextInt(GROUP_SPREAD * 2 + 1) - GROUP_SPREAD), 3);
            if(near == null) continue;
            Entity mob = spawn(world, type, near, ramp);
            if(mob != null) spawned.add(new WeakReference<>(mob));
        }
    }

    private static Entity spawn(World world, NightType type, BlockPos pos, double ramp) {
        String id = type.getSpawns(world.provider.getDimension()).pick(RAND, getNightPart(world));
        if(id == null) return null;
        Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(id), world);
        if(!(entity instanceof EntityLiving)) {
            WakingNights.LOGGER.warn("Night type {} spawn {} doesn't exist or isn't a mob", type.id, id);
            return null;
        }
        EntityLiving mob = (EntityLiving) entity;
        mob.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, RAND.nextFloat() * 360.0F, 0.0F);
        if(!world.getCollisionBoxes(mob, mob.getEntityBoundingBox()).isEmpty() || world.containsAnyLiquid(mob.getEntityBoundingBox())) return null;
        mob.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        weaken(mob, ramp);
        //Marked before it joins the world, so EagleMixins already sees it and it's never in the world unmarked
        mob.getEntityData().setBoolean(OVERWORLD_ALLOWED_TAG, true);
        mob.getEntityData().setString(EVENT_TAG, type.id);
        if(RAND.nextInt(100) < type.spawnSpecialChance) {
            CapabilitySpecialEntityHandler.setType(mob, RAND.nextBoolean() ? SpecialType.BLOODED : SpecialType.BLIGHTED);
        }
        world.spawnEntity(mob);
        return mob;
    }

    /** The first few times a night happens, its own spawns are weaker too (ramp): less health and damage, saved with them */
    private static void weaken(EntityLiving mob, double ramp) {
        if(ramp >= 1.0D) return;
        for(IAttribute attribute : new IAttribute[]{SharedMonsterAttributes.MAX_HEALTH, SharedMonsterAttributes.ATTACK_DAMAGE}) {
            IAttributeInstance instance = mob.getEntityAttribute(attribute);
            if(instance != null && instance.getModifier(RAMP_ID) == null) {
                instance.applyModifier(new AttributeModifier(RAMP_ID, "Waking Nights ramp", ramp - 1.0D, 2));
            }
        }
        mob.setHealth(mob.getMaxHealth());
    }

    /** How far the night is, from nightfall (0) to dawn (1) */
    private static double getNightPart(World world) {
        double sinceNightfall = (NightTracker.getTime() % 24000L) - NightTracker.NIGHTFALL;
        return Math.max(0.0D, Math.min(1.0D, sinceNightfall / (NightTracker.NIGHT_END - NightTracker.NIGHTFALL)));
    }

    /** How close to the player and whether in their sight, by how far the night is (spawnInSightAfter, spawnCloseAtHalf) */
    private static Placement getPlacement(World world, NightType type) {
        float sinceNightfall = (NightTracker.getTime() % 24000L) - NightTracker.NIGHTFALL;
        float half = (NightTracker.NIGHT_END - NightTracker.NIGHTFALL) / 2.0F;
        float inSightFrom = type.spawnInSightAfter * 20.0F;
        boolean inSight = type.spawnInSightAfter >= 0 && sinceNightfall >= inSightFrom;
        double min = MIN_DISTANCE;
        if(inSight && type.spawnCloseAtHalf) {
            float progress = half <= inSightFrom ? 1.0F : (sinceNightfall - inSightFrom) / (half - inSightFrom);
            min = MIN_DISTANCE + (CLOSE_DISTANCE - MIN_DISTANCE) * Math.min(1.0F, Math.max(0.0F, progress));
        }
        return new Placement(min, inSight);
    }

    /** A spot on the ground around the player, out of their sight (unless allowed) and not lit */
    private static BlockPos findSpot(World world, EntityPlayer player, Placement placement) {
        for(int attempt = 0; attempt < PLACE_ATTEMPTS; attempt++) {
            double angle = RAND.nextDouble() * Math.PI * 2.0D;
            double distance = placement.minDistance + RAND.nextDouble() * (MAX_DISTANCE - placement.minDistance);
            BlockPos column = new BlockPos(player.posX + Math.cos(angle) * distance, player.posY, player.posZ + Math.sin(angle) * distance);
            BlockPos pos = findGround(world, column, 16);
            if(pos == null) continue;
            if(!placement.inSight && isInSight(world, player, pos)) continue;
            return pos;
        }
        return null;
    }

    /** The highest standing spot in this column within range blocks up or down: solid ground, room above, not lit */
    private static BlockPos findGround(World world, BlockPos column, int range) {
        if(!world.isBlockLoaded(column)) return null;
        for(int dy = range; dy >= -range; dy--) {
            BlockPos pos = column.up(dy);
            if(!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) continue;
            if(!hasRoom(world, pos)) continue;
            if(world.getLightFor(EnumSkyBlock.BLOCK, pos) > MAX_BLOCK_LIGHT) return null;
            return pos;
        }
        return null;
    }

    /** Two blocks a mob can stand in: grass, flowers or snow are fine, liquid isn't (the mob's own size is checked when placed) */
    private static boolean hasRoom(World world, BlockPos pos) {
        for(int i = 0; i < 2; i++) {
            Material material = world.getBlockState(pos.up(i)).getMaterial();
            if(material.blocksMovement() || material.isLiquid()) return false;
        }
        return true;
    }

    /** Where the player looks and nothing blocks the view */
    private static boolean isInSight(World world, EntityPlayer player, BlockPos pos) {
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d target = new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        if(player.getLook(1.0F).dotProduct(target.subtract(eyes).normalize()) < Math.cos(Math.toRadians(VIEW_ANGLE))) return false;
        return world.rayTraceBlocks(eyes, target, false, true, false) == null;
    }

    private static final class Placement {
        final double minDistance;
        final boolean inSight;

        Placement(double minDistance, boolean inSight) {
            this.minDistance = minDistance;
            this.inSight = inSight;
        }
    }

    /**
     * End of a Waking Night (or another one starting): every loaded event spawn that doesn't belong to what's going
     * on now is removed, in a puff of smoke, without drops
     */
    public static void removeStrays(WakingNightsWorldData data) {
        for(WorldServer world : DimensionManager.getWorlds()) {
            for(Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if(entity.isDead || !isStray(entity, data)) continue;
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, entity.posX, entity.posY + entity.height * 0.5D, entity.posZ,
                        12, entity.width * 0.5D, entity.height * 0.4D, entity.width * 0.5D, 0.02D);
                entity.setDead();
            }
        }
    }

    /** An event spawn loading in after its night is over never comes back */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJoinWorld(EntityJoinWorldEvent event) {
        if(!event.getWorld().isRemote && isStray(event.getEntity(), WakingNightsWorldData.get(event.getWorld()))) event.setCanceled(true);
    }

    /** It's an event spawn and its night type isn't the one going on in its dimension, or it's from an older Lycanites wave */
    private static boolean isStray(Entity entity, WakingNightsWorldData data) {
        if(!entity.getEntityData().hasKey(EVENT_TAG)) return false;
        if(entity.getEntityData().hasKey(WAVE_TAG) && entity.getEntityData().getLong(WAVE_TAG) != data.getWave()) return true;
        String type = data.getType(entity.dimension);
        return type == null || !type.equals(entity.getEntityData().getString(EVENT_TAG));
    }
}
