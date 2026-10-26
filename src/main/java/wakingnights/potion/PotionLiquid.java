package wakingnights.potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import wakingnights.WakingNights;

/**
 * The Waking potions. They do nothing while active; what they do happens when the player wakes up (NightTracker):
 * - Liquid Dreams (white): the Waking Night level can't rise, and Liquid Nightmares can't be given meanwhile
 * - Liquid Nightmares (black): Waking Night III after waking up
 * Registered with potion types, so vanilla makes the drinkable, splash and lingering potions and tipped arrows.
 */
public class PotionLiquid extends Potion {
    public static final PotionLiquid DREAMS = new PotionLiquid("liquid_dreams", false, 0xFFFFFF, 4, 1);
    public static final PotionLiquid NIGHTMARES = new PotionLiquid("liquid_nightmares", true, 0x000000, 5, 1);

    //20 minutes, also how long it keeps Liquid Nightmares away
    public static final PotionType DREAMS_TYPE = new PotionType("liquid_dreams", new PotionEffect(DREAMS, 24000)).setRegistryName(WakingNights.MODID, "liquid_dreams");
    //5 minutes, long enough to drink it and go to bed
    public static final PotionType NIGHTMARES_TYPE = new PotionType("liquid_nightmares", new PotionEffect(NIGHTMARES, 6000)).setRegistryName(WakingNights.MODID, "liquid_nightmares");

    /** iconX and iconY pick one of vanilla's effect icons (Night Vision for dreams, Blindness for nightmares) */
    private PotionLiquid(String name, boolean bad, int color, int iconX, int iconY) {
        super(bad, color);
        this.setRegistryName(WakingNights.MODID, name);
        this.setPotionName("effect." + WakingNights.MODID + "." + name);
        this.setIconIndex(iconX, iconY);
        if(!bad) this.setBeneficial();
    }

    /** Liquid Dreams has its own icon (textures/effects/liquid_dreams.png) instead of a vanilla one */
    @Override
    public boolean hasStatusIcon() {
        return this != DREAMS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        if(this != DREAMS) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(DREAMS_ICON);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        if(this != DREAMS) return;
        Minecraft.getMinecraft().getTextureManager().bindTexture(DREAMS_ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }

    private static final ResourceLocation DREAMS_ICON = new ResourceLocation(WakingNights.MODID, "textures/effects/liquid_dreams.png");
}
