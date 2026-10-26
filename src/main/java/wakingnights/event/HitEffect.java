package wakingnights.event;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import wakingnights.WakingNights;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * An effect special mobs give when they hit a player, written as "<potion id> <amplifier> <ticks>",
 * e.g. "srparasites:bleed 0 100" (amplifier 0 is level I, 100 ticks is 5 seconds).
 */
public class HitEffect {
    private final String potion;
    private final int amplifier;
    private final int ticks;

    private HitEffect(String potion, int amplifier, int ticks) {
        this.potion = potion;
        this.amplifier = amplifier;
        this.ticks = ticks;
    }

    /** null if the potion doesn't exist (its mod isn't installed) */
    @Nullable
    public PotionEffect create() {
        Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(this.potion));
        return potion == null ? null : new PotionEffect(potion, this.ticks, this.amplifier);
    }

    /** Entries that can't be read are left out (and logged) */
    public static List<HitEffect> parseAll(Iterable<String> entries) {
        List<HitEffect> effects = new ArrayList<>();
        for(String entry : entries) {
            String trimmed = entry.trim();
            if(trimmed.isEmpty() || trimmed.equalsIgnoreCase("none")) continue;
            String[] parts = trimmed.split("\\s+");
            try {
                if(parts.length != 3) throw new NumberFormatException();
                effects.add(new HitEffect(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            }
            catch(NumberFormatException e) {
                WakingNights.LOGGER.warn("Invalid hit effect, should be <potion> <amplifier> <ticks>: {}", entry);
            }
        }
        return effects;
    }
}
