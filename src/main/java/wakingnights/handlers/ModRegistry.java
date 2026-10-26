package wakingnights.handlers;

import net.minecraft.potion.Potion;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;
import wakingnights.potion.PotionWakingNight;

@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public class ModRegistry {

        public static void init() {

        }

        @SubscribeEvent
        public static void registerPotionEvent(RegistryEvent.Register<Potion> event) {
                event.getRegistry().register(PotionWakingNight.INSTANCE);
        }
}
