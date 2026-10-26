package wakingnights.potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wakingnights.WakingNights;

import java.util.ArrayList;
import java.util.List;

/**
 * Status icon showing a player's Waking Night level (amplifier = level - 1). Display only: the level itself is
 * stored in the player's NBT, and this effect is reapplied from it.
 */
public class PotionWakingNight extends Potion {

    public static final PotionWakingNight INSTANCE = new PotionWakingNight();

    private static final ResourceLocation MOON_PHASES = new ResourceLocation("textures/environment/moon_phases.png");
    //Pixels of black left out around the moon in each phase's cell
    private static final int MOON_INSET = 8;

    public PotionWakingNight() {
        super(true, 0x3B1F5C);
        this.setRegistryName(WakingNights.MODID, "waking_night");
        this.setPotionName("effect." + WakingNights.MODID + ".waking_night");
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return false;
    }

    //Milk shouldn't remove it, it would come right back anyway
    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        drawMoon(x + 6, y + 7, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        drawMoon(x + 3, y + 3, alpha);
    }

    /**
     * Minecraft's own moon, in tonight's phase. Its texture is a bright moon on solid black, so it's added onto what's
     * behind it like the sky does, which leaves out the black.
     */
    @SideOnly(Side.CLIENT)
    private static void drawMoon(int x, int y, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        int phase = mc.world == null ? 0 : mc.world.getMoonPhase();
        mc.getTextureManager().bindTexture(MOON_PHASES);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        //Each phase is a 32x32 cell of the 128x64 texture, the moon itself is in its middle
        float u = (phase % 4) * 32 + MOON_INSET, v = (phase / 4 % 2) * 32 + MOON_INSET;
        int size = 32 - MOON_INSET * 2;
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, size, size, 18, 18, 128, 64);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
