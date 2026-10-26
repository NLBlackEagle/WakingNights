package wakingnights.client.growth;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.SpecialType;
import wakingnights.mixin.vanilla.RenderLivingBaseAccessor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Adds the growth layer to a mob's renderer the first time that renderer draws something,
 * which also covers renderers other mods register late.
 * Also counts entity renders, so parts can tell whether they were drawn in the current one (see IRenderedPart).
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class GrowthLayerHandler {
    private static final Set<RenderLivingBase<?>> WITH_LAYER = Collections.newSetFromMap(new WeakHashMap<>());
    private static int renderStamp = 0;
    private static boolean capturingMatrices = false;
    private static boolean tinted = false;

    //Skin colour of special mobs, multiplied with their texture
    private static final float[] BLOODED_TINT = {1.0F, 0.78F, 0.78F};
    private static final float[] BLIGHTED_TINT = {0.88F, 0.96F, 0.7F};

    public static int getRenderStamp() {
        return renderStamp;
    }

    /** Only while a special mob is drawn, reading the GL matrix for every part of every mob would be wasteful */
    public static boolean isCapturingMatrices() {
        return capturingMatrices;
    }

    /** Tints the mob's skin until its layers start (RenderLivingBaseMixin), so armour and growths keep their colours */
    public static void stopTint() {
        if(!tinted) return;
        tinted = false;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        renderStamp++;
        SpecialType type = CapabilitySpecialEntityHandler.getType(event.getEntity());
        capturingMatrices = type != SpecialType.NONE;
        if(type == SpecialType.BLOODED) startTint(BLOODED_TINT);
        else if(type == SpecialType.BLIGHTED) startTint(BLIGHTED_TINT);
        if(!(event.getEntity() instanceof EntityLiving)) return;
        RenderLivingBase<?> renderer = event.getRenderer();
        if(WITH_LAYER.add(renderer)) ((RenderLivingBaseAccessor) renderer).getLayerRenderers().add(new GrowthLayer(renderer));
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        capturingMatrices = false;
        stopTint();
    }

    private static void startTint(float[] tint) {
        tinted = true;
        GlStateManager.color(tint[0], tint[1], tint[2], 1.0F);
    }
}
