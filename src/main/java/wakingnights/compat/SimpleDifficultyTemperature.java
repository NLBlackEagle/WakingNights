package wakingnights.compat;

import com.charles445.simpledifficulty.api.temperature.ITemperatureModifier;
import com.charles445.simpledifficulty.api.temperature.TemperatureRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import wakingnights.WakingNights;
import wakingnights.event.NightType;
import wakingnights.level.WakingNightsWorldData;

/**
 * A night type's temperature=... changes SimpleDifficulty's temperature everywhere in its dimensions while its
 * Waking Night goes on (negative is colder).
 * SimpleDifficulty is only compiled against: only touch this class when it's loaded.
 */
public class SimpleDifficultyTemperature implements ITemperatureModifier {
    public static void register() {
        TemperatureRegistry.registerModifier(new SimpleDifficultyTemperature());
    }

    @Override
    public float getWorldInfluence(World world, BlockPos pos) {
        if(world.isRemote) return 0.0F;
        WakingNightsWorldData data = WakingNightsWorldData.get(world);
        if(!data.isEventActive()) return 0.0F;
        String typeId = data.getType(world.provider.getDimension());
        NightType type = typeId == null ? null : NightType.get(typeId);
        return type == null ? 0.0F : type.temperature;
    }

    @Override
    public float getPlayerInfluence(EntityPlayer player) {
        return 0.0F;
    }

    @Override
    public String getName() {
        return WakingNights.MODID + ":waking_night";
    }
}
