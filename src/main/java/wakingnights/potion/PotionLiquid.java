package wakingnights.potion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import wakingnights.WakingNights;
import wakingnights.level.WakingLevel;

import javax.annotation.Nullable;

/**
 * The Waking potions:
 * - Liquid Dreams (white): while it lasts, the Waking Night level doesn't rise when a night is skipped (NightTracker)
 * - Liquid Nightmares (black, instant): takes Liquid Dreams away, or without it raises the Waking Night level by one
 * Registered with potion types, so vanilla makes the drinkable, splash and lingering potions and tipped arrows.
 * Neither shows particles around the player.
 */
public class PotionLiquid extends Potion {
    public static final PotionLiquid DREAMS = new PotionLiquid("liquid_dreams", false, 0xFFFFFF);
    public static final PotionLiquid NIGHTMARES = new PotionLiquid("liquid_nightmares", true, 0x000000);

    //20 minutes. The potion has particles, or vanilla colours the bottle black; they're taken off the player (stripParticles)
    public static final PotionType DREAMS_TYPE = new PotionType("liquid_dreams", new PotionEffect(DREAMS, 24000)).setRegistryName(WakingNights.MODID, "liquid_dreams");
    public static final PotionType NIGHTMARES_TYPE = new PotionType("liquid_nightmares", new PotionEffect(NIGHTMARES)).setRegistryName(WakingNights.MODID, "liquid_nightmares");

    private static final ResourceLocation DREAMS_ICON = new ResourceLocation(WakingNights.MODID, "textures/effects/liquid_dreams.png");

    private PotionLiquid(String name, boolean bad, int color) {
        super(bad, color);
        this.setRegistryName(WakingNights.MODID, name);
        this.setPotionName("effect." + WakingNights.MODID + "." + name);
        if(!bad) this.setBeneficial();
    }

    @Override
    public boolean isInstant() {
        return this == NIGHTMARES;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return this == NIGHTMARES && duration >= 1;
    }

    @Override
    public void performEffect(EntityLivingBase entity, int amplifier) {
        if(this == NIGHTMARES) giveNightmares(entity);
    }

    @Override
    public void affectEntity(@Nullable Entity source, @Nullable Entity indirectSource, EntityLivingBase entity, int amplifier, double health) {
        if(this == NIGHTMARES) giveNightmares(entity);
    }

    /** Liquid Dreams on a player, without the particles of the potion it came from */
    public static void stripParticles(EntityPlayer player) {
        PotionEffect effect = player.getActivePotionEffect(DREAMS);
        if(effect == null || !effect.doesShowParticles()) return;
        //Merging an equal effect takes over its particle setting
        player.addPotionEffect(new PotionEffect(DREAMS, effect.getDuration(), effect.getAmplifier(), effect.getIsAmbient(), false));
    }

    private static void giveNightmares(EntityLivingBase entity) {
        if(entity.world.isRemote || !(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if(player.isPotionActive(DREAMS)) player.removePotionEffect(DREAMS);
        else WakingLevel.raiseLevel(player);
    }

    /**
     * Liquid Dreams has its own icon, drawn by the methods below. True like EagleMixins' effects: some effect displays
     * only draw an icon when there is one. Liquid Nightmares is over at once and never shows.
     */
    @Override
    public boolean hasStatusIcon() {
        return this == DREAMS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        if(this == DREAMS) drawIcon(DREAMS_ICON, x + 6, y + 7, 1.0F);
    }

    /** The older version some effect displays still call (like the pack's), so the icon shows there too */
    @SideOnly(Side.CLIENT)
    @Override
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        if(this == DREAMS) drawIcon(DREAMS_ICON, x + 6, y + 7, 1.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        if(this == DREAMS) drawIcon(DREAMS_ICON, x + 3, y + 3, alpha);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        if(this == DREAMS) drawIcon(DREAMS_ICON, x + 3, y + 3, alpha);
    }

    /**
     * A 12x12 icon texture, in the middle of the 18x18 icon spot (x and y), at its own size so its pixels stay sharp.
     * The colour is set first: text drawn before it leaves its colour behind.
     */
    @SideOnly(Side.CLIENT)
    static void drawIcon(ResourceLocation icon, int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        int offset = (18 - ICON_SIZE) / 2;
        Gui.drawModalRectWithCustomSizedTexture(x + offset, y + offset, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static final int ICON_SIZE = 12;
}
