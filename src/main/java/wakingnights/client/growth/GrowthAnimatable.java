package wakingnights.client.growth;

import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

/**
 * One per growth shape. Every growth on every mob gets its own animation data in the factory (by unique id),
 * so each one runs its own loop at its own speed and starting point.
 */
public class GrowthAnimatable implements IAnimatable {
    private static final String IDLE = "idle";

    private final AnimationFactory factory = new AnimationFactory(this);

    //Set by prepare() right before the animation data of a new growth gets created in registerControllers
    private double nextSpeed = 1.0D;
    private double nextPhaseTicks = 0.0D;

    public void prepare(double speed, double phaseTicks) {
        this.nextSpeed = speed;
        this.nextPhaseTicks = phaseTicks;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new GrowthController(this, this.nextSpeed, this.nextPhaseTicks));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    private static class GrowthController extends AnimationController<GrowthAnimatable> {
        private final double phaseTicks;
        private boolean phased = false;

        GrowthController(GrowthAnimatable animatable, double speed, double phaseTicks) {
            super(animatable, "growth_controller", 0, event -> {
                event.getController().setAnimation(new AnimationBuilder().loop(IDLE));
                return PlayState.CONTINUE;
            });
            this.animationSpeed = speed;
            this.phaseTicks = phaseTicks;
        }

        /** The first time the loop starts, move its start back so the growth begins somewhere in the middle of it. */
        @Override
        protected double adjustTick(double tick) {
            boolean starting = this.shouldResetTick && this.getAnimationState() == AnimationState.Running;
            double adjusted = super.adjustTick(tick);
            if(starting && !this.phased) {
                this.phased = true;
                this.tickOffset -= this.phaseTicks / this.animationSpeed;
            }
            return adjusted;
        }
    }
}
