package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;

import java.io.IOException;

/**
 * When a Waking Night truly starts (nightfall): the night type's banner at the top of the screen, like Lycanites' mob
 * event images, and its start sound all around the player. Banners are textures/gui/events/<night type>.png, 256x256 with
 * the banner in the top half; a night type without one gets the_night_wakes.png.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class EventBanner {
    private static final ResourceLocation FALLBACK = new ResourceLocation(WakingNights.MODID, "textures/gui/events/the_night_wakes.png");
    //Milliseconds: fading in, fully shown until, gone at
    private static final long FADE_IN = 1000, HOLD_UNTIL = 5500, END = 7000;
    //Largest share of the screen width the banner takes
    private static final float MAX_WIDTH_SHARE = 0.7F;

    private static ResourceLocation banner;
    private static long shownAt;

    public static void show(String nightType, String sound) {
        banner = getBanner(nightType);
        shownAt = Minecraft.getSystemTime();
        if(!sound.isEmpty()) {
            Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(sound), SoundCategory.MASTER,
                    1.0F, 1.0F, false, 0, ISound.AttenuationType.NONE, 0.0F, 0.0F, 0.0F));
        }
    }

    private static ResourceLocation getBanner(String nightType) {
        if(nightType.isEmpty()) return FALLBACK;
        ResourceLocation texture = new ResourceLocation(WakingNights.MODID, "textures/gui/events/" + nightType + ".png");
        try {
            Minecraft.getMinecraft().getResourceManager().getResource(texture).close();
            return texture;
        }
        catch(IOException e) {
            return FALLBACK;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if(banner == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        long elapsed = Minecraft.getSystemTime() - shownAt;
        if(elapsed >= END) {
            banner = null;
            return;
        }
        float alpha = elapsed < FADE_IN ? elapsed / (float) FADE_IN
                : elapsed < HOLD_UNTIL ? 1.0F
                : 1.0F - (elapsed - HOLD_UNTIL) / (float) (END - HOLD_UNTIL);
        //Below the alpha cut-off vanilla's rendering would skip it entirely anyway
        if(alpha < 0.02F) return;

        ScaledResolution resolution = event.getResolution();
        int width = Math.min(256, (int) (resolution.getScaledWidth() * MAX_WIDTH_SHARE));
        int height = width / 2;
        int x = (resolution.getScaledWidth() - width) / 2;
        int y = (int) (resolution.getScaledHeight() * 0.06F);

        Minecraft.getMinecraft().getTextureManager().bindTexture(banner);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        //The top half of the 256x256 texture
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 256, 128, width, height, 256, 256);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }
}
