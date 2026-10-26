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

    private static final ResourceLocation ICON = new ResourceLocation(WakingNights.MODID, "textures/effects/waking_night.png");

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
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICON);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }
}
