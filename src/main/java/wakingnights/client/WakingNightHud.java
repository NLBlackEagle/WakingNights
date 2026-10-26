package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.potion.PotionWakingNight;

/**
 * Vanilla only shows an effect in the top right of the screen if it shows particles, and the Waking Night icon
 * doesn't (players would be surrounded by swirls all the time). So it's drawn here instead, at the end of the
 * harmful effects row, the same way vanilla draws its icons.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class WakingNightHud {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if(event.getType() != RenderGameOverlayEvent.ElementType.POTION_ICONS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.player == null) return;
        PotionEffect effect = mc.player.getActivePotionEffect(PotionWakingNight.INSTANCE);
        if(effect == null || effect.doesShowParticles()) return;

        //Same slot counting as GuiIngame#renderPotionEffects
        int harmful = 0;
        for(PotionEffect other : mc.player.getActivePotionEffects()) {
            Potion potion = other.getPotion();
            if(potion.shouldRenderHUD(other) && other.doesShowParticles() && !potion.isBeneficial()) harmful++;
        }

        ScaledResolution resolution = event.getResolution();
        int x = resolution.getScaledWidth() - 25 * (harmful + 1);
        int y = 1 + 26 + (mc.isDemo() ? 15 : 0);

        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
        mc.ingameGUI.drawTexturedModalRect(x, y, 141, 166, 24, 24);
        PotionWakingNight.INSTANCE.renderHUDEffect(effect, mc.ingameGUI, x, y, 0, 1.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
