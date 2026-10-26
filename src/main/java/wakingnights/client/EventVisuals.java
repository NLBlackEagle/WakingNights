package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.level.NightTracker;

/**
 * The sky, fog and lightning of a Waking Night. They turn to the night type's colours while the sun sets
 * (dusk to nightfall), the warning of what's coming, and back while it rises. The stars go out with them,
 * only the moon keeps shining. The flash of a strike (sky, fog and ground) is in the lightning's colour.
 * Sky, clouds, stars, moon, lightning and the light outside are hooked in WorldMixin, RenderGlobalMixin,
 * RenderLightningBoltMixin, EntityRendererMixin and WorldProviderMixin.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class EventVisuals {
    //Ticks before dawn the colours start fading back
    private static final float FADE_OUT = 1000.0F;
    //How far a strike pulls the light outside towards the lightning colour
    private static final float LIGHTMAP_FLASH = 0.6F;

    private static boolean active = false;
    private static float[] sky = new float[3];
    private static float[] fog = new float[3];
    private static float[] clouds = new float[3];
    private static float[] lightning = new float[3];
    private static float[] light = {1.0F, 1.0F, 1.0F};
    private static float[] moon = {1.0F, 1.0F, 1.0F};

    public static void set(boolean isActive, int skyColor, int fogColor, int cloudColor, int lightningColor, int lightColor, int moonColor) {
        active = isActive;
        sky = toRgb(skyColor);
        fog = toRgb(fogColor);
        clouds = toRgb(cloudColor);
        lightning = toRgb(lightningColor);
        light = toRgb(lightColor);
        moon = toRgb(moonColor);
    }

    /** 0 = normal night, 1 = fully the night type's colours */
    public static float getProgress(World world, float partialTicks) {
        if(!active || world == null || world.provider.getDimension() != 0) return 0.0F;
        float time = (world.getWorldTime() % 24000L) + partialTicks;
        if(time < NightTracker.DUSK || time >= NightTracker.NIGHT_END) return 0.0F;
        float fadeIn = (time - NightTracker.DUSK) / (float) (NightTracker.NIGHTFALL - NightTracker.DUSK);
        float fadeOut = (NightTracker.NIGHT_END - time) / FADE_OUT;
        return MathHelper.clamp(Math.min(fadeIn, fadeOut), 0.0F, 1.0F);
    }

    public static Vec3d applySky(World world, Vec3d color, float partialTicks) {
        float progress = getProgress(world, partialTicks);
        return lerp(lerp(color, sky, progress), lightning, getFlash(world, partialTicks, progress));
    }

    /**
     * How strongly the sky and fog flash right after a strike, as far as vanilla's white flash does (45%).
     * 0 when there's no Waking Night, then vanilla's flash is left alone.
     */
    private static float getFlash(World world, float partialTicks, float progress) {
        if(progress <= 0.0F || world.getLastLightningBolt() <= 0) return 0.0F;
        return Math.min(1.0F, world.getLastLightningBolt() - partialTicks) * 0.45F;
    }

    /**
     * The light outside, for one lightmap entry (skyLight is vanilla's sky light brightness for it):
     * - tinted by the night type's light colour, so e.g. a bloodmoon drowns everything outside in a faint dark red,
     *   while torches and other block light keep their colour
     * - the flash of a strike: vanilla brightens everything the sky reaches for a moment (turned off during a Waking
     *   Night, see EntityRendererMixin). Instead, that light is pulled towards the lightning colour, so black lightning
     *   makes the world go dark for a moment.
     */
    public static void applyLightmap(World world, float sunBrightness, float skyLight, float[] colors) {
        float progress = getProgress(world, 1.0F);
        if(progress <= 0.0F) return;
        //How much this entry is lit by the sky (0 underground, 1 in the open)
        float exposure = MathHelper.clamp(skyLight / (sunBrightness * 0.95F + 0.05F), 0.0F, 1.0F);

        float tint = exposure * progress;
        for(int i = 0; i < 3; i++) colors[i] *= 1.0F + (light[i] - 1.0F) * tint;

        if(world.getLastLightningBolt() <= 0) return;
        float strength = exposure * LIGHTMAP_FLASH * progress;
        for(int i = 0; i < 3; i++) colors[i] += (lightning[i] - colors[i]) * strength;
    }

    public static Vec3d applyClouds(World world, Vec3d color, float partialTicks) {
        return lerp(color, clouds, getProgress(world, partialTicks));
    }

    /**
     * Called with the moon texture bound, right before vanilla draws the moon. Vanilla adds the moon's light to the sky
     * (its texture is a bright disc on solid black, which adding hides), so it can only brighten. To colour it, first the
     * moon's shape is cut out of the sky (the bright disc darkens what's behind it, the black around it changes nothing),
     * then vanilla's moon is added in the moon colour on top. Black: just the dark disc.
     */
    public static void prepareMoon(World world, float partialTicks) {
        float progress = getProgress(world, partialTicks);
        if(progress <= 0.0F || (moon[0] >= 1.0F && moon[1] >= 1.0F && moon[2] >= 1.0F)) return;
        float visibility = 1.0F - world.getRainStrength(partialTicks);
        float cut = progress * visibility;

        int phase = world.getMoonPhase();
        float minU = (phase % 4) / 4.0F, minV = (phase / 4 % 2) / 2.0F;
        float maxU = (phase % 4 + 1) / 4.0F, maxV = (phase / 4 % 2 + 1) / 2.0F;
        float size = 20.0F;

        GlStateManager.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        GlStateManager.color(cut, cut, cut, 1.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-size, -100.0D, size).tex(maxU, maxV).endVertex();
        buffer.pos(size, -100.0D, size).tex(minU, maxV).endVertex();
        buffer.pos(size, -100.0D, -size).tex(minU, minV).endVertex();
        buffer.pos(-size, -100.0D, -size).tex(maxU, minV).endVertex();
        tessellator.draw();

        //Back to vanilla's blending for its own moon, now in the moon colour
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F + (moon[0] - 1.0F) * progress, 1.0F + (moon[1] - 1.0F) * progress, 1.0F + (moon[2] - 1.0F) * progress, visibility);
    }

    public static float applyStars(World world, float brightness, float partialTicks) {
        return brightness * (1.0F - getProgress(world, partialTicks));
    }

    /** Lightning colour while a Waking Night is going on, or null for vanilla lightning */
    public static float[] getLightningColor() {
        Minecraft mc = Minecraft.getMinecraft();
        return getProgress(mc.world, mc.getRenderPartialTicks()) > 0.0F ? lightning : null;
    }

    @SubscribeEvent
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        World world = event.getEntity().world;
        float partialTicks = (float) event.getRenderPartialTicks();
        float progress = getProgress(world, partialTicks);
        if(progress <= 0.0F) return;
        float flash = getFlash(world, partialTicks, progress);
        float[] color = {event.getRed(), event.getGreen(), event.getBlue()};
        for(int i = 0; i < 3; i++) {
            color[i] += (fog[i] - color[i]) * progress;
            color[i] += (lightning[i] - color[i]) * flash;
        }
        event.setRed(color[0]);
        event.setGreen(color[1]);
        event.setBlue(color[2]);
    }

    private static Vec3d lerp(Vec3d color, float[] target, float progress) {
        if(progress <= 0.0F) return color;
        return new Vec3d(color.x + (target[0] - color.x) * progress,
                color.y + (target[1] - color.y) * progress,
                color.z + (target[2] - color.z) * progress);
    }

    private static float[] toRgb(int color) {
        return new float[]{(color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F};
    }
}
