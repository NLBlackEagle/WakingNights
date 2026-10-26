package wakingnights.client.growth;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.SpecialType;

import java.util.Random;

/**
 * Special mobs give off a slow stream of GrowthParticles from inside their hitbox: blooded mostly dark red with the odd
 * pale spore mixed in, blighted the other way around. Client side only.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class GrowthParticleHandler {
    //Particles per tick for a zombie-sized mob, bigger mobs get more
    private static final float MAIN_PER_TICK = 0.25F;
    private static final float MINOR_PER_TICK = 0.05F;

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        //Invisible mobs keep their particles on purpose: it gives away that something special is there
        if(!entity.world.isRemote) return;
        SpecialType type = CapabilitySpecialEntityHandler.getType(entity);
        if(type == SpecialType.NONE) return;

        Minecraft mc = Minecraft.getMinecraft();
        //0 = all, 1 = decreased, 2 = minimal
        int particleSetting = mc.gameSettings.particleSetting;
        if(particleSetting >= 2) return;

        float multiplier = Math.max(1.0F, entity.width * entity.height) * (particleSetting == 1 ? 0.4F : 1.0F);
        //Blooded: mostly dark red with the odd spore, blighted: the other way around
        boolean blighted = type == SpecialType.BLIGHTED;
        spawn(entity, (blighted ? MINOR_PER_TICK : MAIN_PER_TICK) * multiplier, GrowthParticle::blood);
        spawn(entity, (blighted ? MAIN_PER_TICK : MINOR_PER_TICK) * multiplier, GrowthParticle::spore);
    }

    /** Spawns on average perTick particles this tick at random spots inside the hitbox. */
    private static void spawn(EntityLivingBase entity, float perTick, ParticleFactory factory) {
        Random rand = entity.getRNG();
        AxisAlignedBB box = entity.getEntityBoundingBox();
        for(float chance = perTick; chance > 0.0F; chance -= 1.0F) {
            if(rand.nextFloat() >= chance) continue;
            double x = box.minX + rand.nextDouble() * (box.maxX - box.minX);
            double y = box.minY + rand.nextDouble() * (box.maxY - box.minY);
            double z = box.minZ + rand.nextDouble() * (box.maxZ - box.minZ);
            Minecraft.getMinecraft().effectRenderer.addEffect(factory.create(entity.world, x, y, z));
        }
    }

    private interface ParticleFactory {
        GrowthParticle create(World world, double x, double y, double z);
    }
}
