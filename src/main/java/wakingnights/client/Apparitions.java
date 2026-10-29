package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.event.LycanitesHook;
import wakingnights.event.NightHooks;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.level.NightTracker;
import wakingnights.potion.PotionWakingNight;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * At Waking Night II, very rarely, a ghostly figure stands at the edge of the player's screen at night, in plain view:
 * a see-through mob that keeps watching them as they move, doing nothing else. It's gone the moment they look straight at it.
 * Only that player sees it: it exists on their client alone (never in the world, no AI) and is drawn here.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class Apparitions {
    private static final int LEVEL = 2;
    private static final double MIN_DISTANCE = 8.0D;
    private static final double MAX_DISTANCE = 32.0D;
    //How far to the side of where the player looks it appears, as a share of the way to the edge of the screen
    private static final double MIN_SCREEN_SHARE = 0.6D;
    private static final double MAX_SCREEN_SHARE = 0.85D;
    private static final int SPAWN_ATTEMPTS = 10;
    //Looking within this many degrees of it counts as looking straight at it
    private static final double LOOK_ANGLE = 12.0D;
    private static final double MAX_PLAYER_DISTANCE = 48.0D;
    private static final int FADE_IN = 20;
    private static final int FADE_OUT = 5;
    private static final int LIFETIME = 600;

    private static final float ALPHA = 0.45F;
    private static final float[] COLOR = {0.75F, 0.85F, 1.0F};
    //Lit even in the dark, so it shows from far away (lightmap coordinates, 240 = fully lit)
    private static final float GLOW = 240.0F;
    //How far up and down from the player's height it looks for ground to stand on
    private static final int GROUND_SEARCH_UP = 6;
    private static final int GROUND_SEARCH_DOWN = 8;

    //Apparition Any Mob: tries to find a fitting mob, the biggest it may be, and the ids still worth trying
    private static final int ANY_MOB_ATTEMPTS = 10;
    private static final float MAX_ANY_WIDTH = 3.0F, MAX_ANY_HEIGHT = 4.0F;
    private static List<ResourceLocation> anyMobIds;
    private static final Random RAND = new Random();
    private static EntityLiving ghost;
    private static int age;
    //Ticks since it started fading out, -1 while it isn't
    private static int fading = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.world == null || mc.player == null || mc.isGamePaused()) {
            if(mc.world == null) ghost = null;
            return;
        }

        if(ghost != null) {
            tickGhost(mc.player);
            return;
        }
        if(getLevel(mc.player) != LEVEL || !isNight(mc.world)) return;
        double averageTicks = ForgeConfigHandler.client.apparitionAverageMinutes * 1200.0D;
        if(averageTicks > 0 && RAND.nextDouble() < 1.0D / averageTicks) {
            for(int attempt = 0; attempt < SPAWN_ATTEMPTS && ghost == null; attempt++) spawn(mc.player);
        }
    }

    private static void tickGhost(EntityPlayerSP player) {
        if(ghost.world != player.world) {
            ghost = null;
            return;
        }
        age++;
        watch(ghost, player);
        if(fading >= 0) {
            if(++fading >= FADE_OUT) ghost = null;
            return;
        }
        if(age > LIFETIME || isLookedAt(player) || player.getDistance(ghost) > MAX_PLAYER_DISTANCE) fading = 0;
    }

    private static boolean isLookedAt(EntityPlayerSP player) {
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d toGhost = new Vec3d(ghost.posX, ghost.posY + ghost.height * 0.6D, ghost.posZ).subtract(eyes).normalize();
        return player.getLook(1.0F).dotProduct(toGhost) > Math.cos(Math.toRadians(LOOK_ANGLE));
    }

    /** Debug command: one right away, whatever the level or time. Tells the player if there was no room for it. */
    public static void forceSpawn() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if(player == null) return;
        for(int attempt = 0; attempt < SPAWN_ATTEMPTS && ghost == null; attempt++) spawn(player);
        if(ghost == null) player.sendStatusMessage(new TextComponentString("No room for an apparition nearby"), true);
    }

    private static void spawn(EntityPlayerSP player) {
        Entity entity = ForgeConfigHandler.client.apparitionAnyMob ? createAnyMob(player.world) : createListed(player.world);
        if(entity == null) return;

        //Towards the edge of the screen, to one side of where the player looks, standing on the ground with room for it
        double[] halfFov = getHalfFov();
        double share = MIN_SCREEN_SHARE + RAND.nextDouble() * (MAX_SCREEN_SHARE - MIN_SCREEN_SHARE);
        double angle = halfFov[0] * share * (RAND.nextBoolean() ? 1 : -1);
        double yaw = Math.toRadians(player.rotationYaw + angle);
        double distance = MIN_DISTANCE + RAND.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        BlockPos column = new BlockPos(player.posX - Math.sin(yaw) * distance, player.posY, player.posZ + Math.cos(yaw) * distance);
        BlockPos ground = findGround(player.world, column, (int) Math.ceil(entity.height));
        if(ground == null) return;

        double x = ground.getX() + 0.5D, y = ground.getY(), z = ground.getZ() + 0.5D;
        //On screen vertically too, and nothing in the way
        if(Math.abs(getPitchTo(player, x, y + entity.height * 0.5D, z) - player.rotationPitch) > halfFov[1] * MAX_SCREEN_SHARE) return;
        if(!canSee(player, x, y, z, entity.height)) return;

        EntityLiving living = (EntityLiving) entity;
        living.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        watch(living, player);
        watch(living, player);
        ghost = living;
        age = 0;
        fading = -1;
    }

    /** One of the Apparition Entities */
    @Nullable
    private static Entity createListed(World world) {
        String[] ids = ForgeConfigHandler.client.apparitionEntities;
        if(ids == null || ids.length == 0) return null;
        String id = ids[RAND.nextInt(ids.length)].trim();
        Entity entity = EntityList.createEntityByIDFromName(new ResourceLocation(id), world);
        if(!(entity instanceof EntityLiving)) {
            WakingNights.LOGGER.warn("Apparition entity {} doesn't exist or isn't a mob", id);
            return null;
        }
        return entity;
    }

    /**
     * Any hostile mob any mod registered, vanilla's, parasites, Lycanites creatures...: only drawn, never added to the world, so
     * no mod's rules for it (like EagleMixins killing parasites) apply. Not bosses or anything huge, and not mobs that
     * failed before (some can't be made outside the world)
     */
    @Nullable
    private static Entity createAnyMob(World world) {
        if(anyMobIds == null) anyMobIds = new ArrayList<>(EntityList.getEntityNameList());
        for(int attempt = 0; attempt < ANY_MOB_ATTEMPTS && !anyMobIds.isEmpty(); attempt++) {
            ResourceLocation id = anyMobIds.get(RAND.nextInt(anyMobIds.size()));
            Entity entity;
            try {
                entity = EntityList.createEntityByIDFromName(id, world);
            }
            catch(RuntimeException | LinkageError e) {
                anyMobIds.remove(id);
                continue;
            }
            if(!(entity instanceof EntityLiving) || !isHostile(entity) || !entity.isNonBoss()
                    || entity.width > MAX_ANY_WIDTH || entity.height > MAX_ANY_HEIGHT) {
                //Never one: not asked again
                anyMobIds.remove(id);
                continue;
            }
            return entity;
        }
        return null;
    }

    /** Marked hostile the vanilla way, or a hostile Lycanites creature (those aren't marked) */
    private static boolean isHostile(Entity entity) {
        return entity instanceof IMob || (NightHooks.LYCANITES && LycanitesHook.isHostileCreature(entity));
    }

    /** Half the horizontal and vertical field of view, in degrees, from the FOV setting and the window's shape */
    private static double[] getHalfFov() {
        Minecraft mc = Minecraft.getMinecraft();
        double vertical = mc.gameSettings.fovSetting / 2.0D;
        double aspect = mc.displayHeight > 0 ? (double) mc.displayWidth / mc.displayHeight : 16.0D / 9.0D;
        double horizontal = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(vertical)) * aspect));
        return new double[]{horizontal, vertical};
    }

    /** Pitch the player would need to look at this point (negative is up, like rotationPitch) */
    private static double getPitchTo(EntityPlayerSP player, double x, double y, double z) {
        double dx = x - player.posX, dy = y - (player.posY + player.getEyeHeight()), dz = z - player.posZ;
        return -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    /** Nothing blocks the view from the player's eyes to its head or the middle of its body */
    private static boolean canSee(EntityPlayerSP player, double x, double y, double z, float height) {
        Vec3d eyes = player.getPositionEyes(1.0F);
        return player.world.rayTraceBlocks(eyes, new Vec3d(x, y + height * 0.85D, z), false, true, false) == null
                || player.world.rayTraceBlocks(eyes, new Vec3d(x, y + height * 0.5D, z), false, true, false) == null;
    }

    /** Turns body and head to the player and looks up or down at their eyes. Keeps the previous angles for smooth turning. */
    private static void watch(EntityLiving living, EntityPlayerSP player) {
        double dx = player.posX - living.posX, dz = player.posZ - living.posZ;
        double dy = player.posY + player.getEyeHeight() - (living.posY + living.getEyeHeight());
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        living.prevRotationYaw = living.rotationYaw;
        living.prevRotationYawHead = living.rotationYawHead;
        living.prevRenderYawOffset = living.renderYawOffset;
        living.prevRotationPitch = living.rotationPitch;
        //Keep the new angle within half a turn of the old one, or it spins the long way round while interpolating
        float turned = living.rotationYaw + MathHelper.wrapDegrees(yaw - living.rotationYaw);
        living.rotationYaw = living.rotationYawHead = living.renderYawOffset = turned;
        living.rotationPitch = pitch;
    }

    /** A spot near the given height with solid ground below and room above, or null */
    private static BlockPos findGround(World world, BlockPos column, int height) {
        for(int dy = GROUND_SEARCH_UP; dy >= -GROUND_SEARCH_DOWN; dy--) {
            BlockPos pos = column.up(dy);
            if(!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) continue;
            boolean room = true;
            for(int i = 0; i < height && room; i++) room = world.isAirBlock(pos.up(i));
            if(room) return pos;
        }
        return null;
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if(ghost == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        RenderHelper.enableStandardItemLighting();
        mc.entityRenderer.enableLightmap();
        try {
            mc.getRenderManager().renderEntityStatic(ghost, event.getPartialTicks(), false);
        }
        catch(RuntimeException e) {
            //A mob that can't be drawn outside the world: gone, and never picked again
            if(anyMobIds != null) anyMobIds.remove(EntityList.getKey(ghost));
            ghost = null;
        }
        mc.entityRenderer.disableLightmap();
        RenderHelper.disableStandardItemLighting();
    }

    /** Right before it's drawn (after vanilla resets the colour): see-through, pale and faintly lit */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if(ghost == null || event.getEntity() != ghost) return;
        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        float fade = fading >= 0 ? 1.0F - (fading + partialTicks) / FADE_OUT : Math.min(1.0F, (age + partialTicks) / FADE_IN);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(COLOR[0], COLOR[1], COLOR[2], ALPHA * Math.max(0.0F, fade));
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, GLOW, GLOW);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        if(ghost == null || event.getEntity() != ghost) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    private static int getLevel(EntityPlayerSP player) {
        PotionEffect effect = player.getActivePotionEffect(PotionWakingNight.INSTANCE);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    private static boolean isNight(World world) {
        return world.provider.getDimension() == 0 && NightTracker.isNight(world.getWorldTime());
    }
}
