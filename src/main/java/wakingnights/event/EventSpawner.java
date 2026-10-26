package wakingnights.event;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
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
 * A night type's own spawns (spawns=... in its config entry), once the sun is gone: picked from its weighted list
 * and placed around each player like vanilla spawning, out of sight. Every one of them is special (blooded or blighted
 * at random), so it hunts the player.
 * They're marked in their ForgeData, which is saved with them:
 * - "overworld_allowed": EagleMixins doesn't kill them for being outside their biomes (SRP mobs in the overworld)
 * - "wakingnights_event": the night type they belong to. They're removed when that night is over, both the loaded ones
 *   and any that load in later (from a chunk that wasn't loaded at the end): they must never outlive their night.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public final class EventSpawner {
    public static final String EVENT_TAG = "wakingnights_event";
    public static final String OVERWORLD_ALLOWED_TAG = "overworld_allowed";

    //Out of sight like vanilla: at least this far, and not where the player can see
    private static final double MIN_DISTANCE = 24.0D;
    private static final double MAX_DISTANCE = 48.0D;
    private static final int PLACE_ATTEMPTS = 8;
    //Not in lit places such as bases
    private static final int MAX_BLOCK_LIGHT = 7;
    //Within this many degrees of where the player looks counts as in view, unless something blocks it
    private static final double VIEW_ANGLE = 70.0D;

    private static final Random RAND = new Random();
    private static final Map<UUID, List<WeakReference<Entity>>> SPAWNED = new HashMap<>();

    private EventSpawner() {}

    /** A new Waking Night starts with none */
    public static void reset() {
        SPAWNED.clear();
    }

    /** Called every tick once the sun is gone during a Waking Night */
    public static void tick(World world, NightType type, List<EntityPlayerMP> players) {
        if(type.spawns.isEmpty() || type.spawnsPerMinute <= 0) return;
        double chancePerTick = type.spawnsPerMinute / 1200.0D;
        for(EntityPlayerMP player : players) {
            if(player.world != world || player.isSpectator() || player.isCreative() || RAND.nextDouble() >= chancePerTick) continue;
            if(countAlive(player) >= type.spawnCap) continue;
            Entity spawned = spawn(world, type, player);
            if(spawned != null) SPAWNED.computeIfAbsent(player.getUniqueID(), id -> new ArrayList<>()).add(new WeakReference<>(spawned));
        }
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

    private static Entity spawn(World world, NightType type, EntityPlayer player) {
        String id = type.pickSpawn(RAND);
        if(id == null) return null;
        Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(id), world);
        if(!(entity instanceof EntityLiving)) {
            WakingNights.LOGGER.warn("Night type {} spawn {} doesn't exist or isn't a mob", type.id, id);
            return null;
        }
        EntityLiving mob = (EntityLiving) entity;
        BlockPos pos = findSpot(world, player, mob);
        if(pos == null) return null;

        mob.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, RAND.nextFloat() * 360.0F, 0.0F);
        if(!world.getCollisionBoxes(mob, mob.getEntityBoundingBox()).isEmpty() || world.containsAnyLiquid(mob.getEntityBoundingBox())) return null;
        mob.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        //Marked before it joins the world, so EagleMixins already sees it and it's never in the world unmarked
        mob.getEntityData().setBoolean(OVERWORLD_ALLOWED_TAG, true);
        mob.getEntityData().setString(EVENT_TAG, type.id);
        CapabilitySpecialEntityHandler.setType(mob, RAND.nextBoolean() ? SpecialType.BLOODED : SpecialType.BLIGHTED);
        world.spawnEntity(mob);
        return mob;
    }

    /** Solid ground with room above, around the player, out of their sight and not lit */
    private static BlockPos findSpot(World world, EntityPlayer player, EntityLiving mob) {
        int height = (int) Math.ceil(mob.height);
        for(int attempt = 0; attempt < PLACE_ATTEMPTS; attempt++) {
            double angle = RAND.nextDouble() * Math.PI * 2.0D;
            double distance = MIN_DISTANCE + RAND.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            BlockPos column = new BlockPos(player.posX + Math.cos(angle) * distance, player.posY, player.posZ + Math.sin(angle) * distance);
            if(!world.isBlockLoaded(column)) continue;
            for(int dy = 8; dy >= -16; dy--) {
                BlockPos pos = column.up(dy);
                if(!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) continue;
                if(!hasRoom(world, pos, height)) continue;
                if(world.getLightFor(EnumSkyBlock.BLOCK, pos) > MAX_BLOCK_LIGHT || isInSight(world, player, pos, mob.height)) break;
                return pos;
            }
        }
        return null;
    }

    private static boolean hasRoom(World world, BlockPos pos, int height) {
        for(int i = 0; i < height; i++) {
            BlockPos above = pos.up(i);
            if(!world.isAirBlock(above) || world.getBlockState(above).getMaterial() == Material.WATER) return false;
        }
        return true;
    }

    /** Where the player looks and nothing blocks the view */
    private static boolean isInSight(World world, EntityPlayer player, BlockPos pos, float height) {
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d target = new Vec3d(pos.getX() + 0.5D, pos.getY() + height * 0.5D, pos.getZ() + 0.5D);
        if(player.getLook(1.0F).dotProduct(target.subtract(eyes).normalize()) < Math.cos(Math.toRadians(VIEW_ANGLE))) return false;
        return world.rayTraceBlocks(eyes, target, false, true, false) == null;
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

    /** It's an event spawn and its night type isn't the one going on */
    private static boolean isStray(Entity entity, WakingNightsWorldData data) {
        if(!entity.getEntityData().hasKey(EVENT_TAG)) return false;
        String type = data.getCurrentType();
        return type == null || !type.equals(entity.getEntityData().getString(EVENT_TAG));
    }
}
