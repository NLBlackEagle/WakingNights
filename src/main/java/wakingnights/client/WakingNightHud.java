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
import wakingnights.potion.PotionLiquid;
import wakingnights.potion.PotionWakingNight;

/**
 * Vanilla only shows an effect in the top right of the screen if it shows particles, and ours don't (players would be
 * surrounded by swirls all the time). So they're drawn here instead, after vanilla's icons in their row (beneficial
 * on top, harmful below), the same way vanilla draws them.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class WakingNightHud {
    private static final Potion[] SHOWN = {PotionLiquid.DREAMS, PotionWakingNight.INSTANCE};

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if(event.getType() != RenderGameOverlayEvent.ElementType.POTION_ICONS) return;
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.player == null) return;

        //Same slot counting as GuiIngame#renderPotionEffects
        int beneficial = 0, harmful = 0;
        for(PotionEffect other : mc.player.getActivePotionEffects()) {
            Potion potion = other.getPotion();
            if(!potion.shouldRenderHUD(other) || !other.doesShowParticles()) continue;
            if(potion.isBeneficial()) beneficial++;
            else harmful++;
        }

        ScaledResolution resolution = event.getResolution();
        int top = 1 + (mc.isDemo() ? 15 : 0);
        for(Potion potion : SHOWN) {
            PotionEffect effect = mc.player.getActivePotionEffect(potion);
            if(effect == null || effect.doesShowParticles()) continue;
            int x, y;
            if(potion.isBeneficial()) {
                x = resolution.getScaledWidth() - 25 * ++beneficial;
                y = top;
            }
            else {
                x = resolution.getScaledWidth() - 25 * ++harmful;
                y = top + 26;
            }
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
            mc.ingameGUI.drawTexturedModalRect(x, y, 141, 166, 24, 24);
            potion.renderHUDEffect(effect, mc.ingameGUI, x, y, 0, 1.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
