package wakingnights.client.growth;

import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;
import wakingnights.WakingNights;

/**
 * One Blockbench growth: geo/<name>.geo.json with its looping "idle" clip in animations/<name>.animation.json
 * (GeckoLib only loads animations from that folder).
 * Modelled standing on the floor grid, attachment point at 0,0,0 and pointing up (+Y).
 */
public class GrowthShape {
    /** Width of the model in Blockbench units (pixels), what the size on a mob is worked out from */
    public final float width;
    /** Length of the idle loop in ticks, to pick a random starting point in it */
    public final double loopTicks;

    private final GrowthModel model;
    private final GrowthAnimatable animatable = new GrowthAnimatable();
    private final IGeoRenderer<GrowthAnimatable> renderer;

    public GrowthShape(String name, float width, double loopSeconds) {
        this.width = width;
        this.loopTicks = loopSeconds * 20.0D;
        this.model = new GrowthModel(
                new ResourceLocation(WakingNights.MODID, "geo/" + name + ".geo.json"),
                new ResourceLocation(WakingNights.MODID, "animations/" + name + ".animation.json"),
                GrowthLayer.FALLBACK_TEXTURE);

        //AnimationControllers look up their model through the fetchers, and only items and entities have one by default
        AnimationController.addModelFetcher((AnimationController.ModelFetcher<GrowthAnimatable>) object -> object == this.animatable ? this.model : null);

        GrowthModel growthModel = this.model;
        this.renderer = new IGeoRenderer<GrowthAnimatable>() {
            @Override
            public GeoModelProvider getGeoModelProvider() {
                return growthModel;
            }

            @Override
            public ResourceLocation getTextureLocation(GrowthAnimatable instance) {
                return growthModel.getTextureLocation(instance);
            }
        };
    }

    /**
     * Renders one growth at the current GL position. Each growth has its own uniqueId, so it keeps its own animation
     * timing; speed and phaseTicks are only used the first time a uniqueId is seen.
     */
    public void render(int uniqueId, double speed, double phaseTicks, float partialTicks) {
        GeoModel geoModel = this.model.getModel(this.model.getModelLocation(this.animatable));
        this.animatable.prepare(speed, phaseTicks);
        this.model.setLivingAnimations(this.animatable, uniqueId, GrowthLayer.newEvent(this.animatable, partialTicks));
        this.renderer.render(geoModel, this.animatable, partialTicks, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}
