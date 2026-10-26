package wakingnights.handlers;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;
import wakingnights.potion.PotionLiquid;
import wakingnights.potion.PotionWakingNight;

@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class ModRegistry {

        public static void init() {

        }

        @SubscribeEvent
        public static void registerPotionEvent(RegistryEvent.Register<Potion> event) {
                event.getRegistry().register(PotionWakingNight.INSTANCE);
                event.getRegistry().register(PotionLiquid.DREAMS);
                event.getRegistry().register(PotionLiquid.NIGHTMARES);
        }

        @SubscribeEvent
        public static void registerPotionTypeEvent(RegistryEvent.Register<PotionType> event) {
                event.getRegistry().register(PotionLiquid.DREAMS_TYPE);
                event.getRegistry().register(PotionLiquid.NIGHTMARES_TYPE);
        }
}
