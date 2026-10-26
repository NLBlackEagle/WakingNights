package wakingnights.client.growth;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

/**
 * Smoke that drifts slowly upwards from a special mob, like pollen.
 * Uses the vanilla smoke sprites, so it needs no texture of its own.
 */
public class GrowthParticle extends Particle {
    //Sky and block light both at 15, in the packed form getBrightnessForRender uses
    private static final int FULL_BRIGHT = 0xF000F0;

    private final float baseScale;
    private final float wobblePhase;

    /** Dark red to near black */
    public static GrowthParticle blood(World world, double x, double y, double z) {
        GrowthParticle particle = new GrowthParticle(world, x, y, z, 0.5F, 0.01D);
        float darkness = particle.rand.nextFloat();
        particle.setRBGColorF(0.45F - darkness * 0.35F, 0.03F, 0.03F + darkness * 0.02F);
        return particle;
    }

    /** Pale green, a bit smaller and faster rising */
    public static GrowthParticle spore(World world, double x, double y, double z) {
        GrowthParticle particle = new GrowthParticle(world, x, y, z, 0.3F, 0.018D);
        float shade = particle.rand.nextFloat() * 0.1F;
        particle.setRBGColorF(0.62F + shade, 0.85F + shade, 0.55F + shade);
        return particle;
    }

    private GrowthParticle(World world, double x, double y, double z, float minScale, double minRise) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX = (this.rand.nextDouble() - 0.5D) * 0.01D;
        this.motionY = minRise + this.rand.nextDouble() * 0.015D;
        this.motionZ = (this.rand.nextDouble() - 0.5D) * 0.01D;
        this.particleAlpha = 0.9F;

        this.baseScale = minScale + this.rand.nextFloat() * 0.5F;
        this.particleScale = this.baseScale;
        this.particleMaxAge = 30 + this.rand.nextInt(30);
        this.wobblePhase = this.rand.nextFloat() * (float) Math.PI * 2.0F;
        this.canCollide = false;
        this.setParticleTextureIndex(7);
    }

    /** Always fully lit, whatever the light where it is: glows in the dark, so special mobs can be spotted from afar */
    @Override
    public int getBrightnessForRender(float partialTicks) {
        return FULL_BRIGHT;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if(this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        float progress = (float) this.particleAge / (float) this.particleMaxAge;
        //The smoke sprites go from big (7) to small (0)
        this.setParticleTextureIndex(7 - (int) (progress * 8.0F));
        this.particleAlpha = 0.9F * (1.0F - progress * progress);
        this.particleScale = this.baseScale * (1.0F - progress * 0.3F);

        //Slowly drifting up, swaying a little from side to side
        float wobble = (float) this.particleAge * 0.15F + this.wobblePhase;
        this.motionX += Math.cos(wobble) * 0.0008D;
        this.motionZ += Math.sin(wobble) * 0.0008D;
        this.motionY += 0.0006D;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.96D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.96D;
    }
}
