package wakingnights;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.command.CommandWakingNights;
import software.bernie.geckolib3.GeckoLib;
import wakingnights.event.NightHooks;
import wakingnights.potion.LiquidBrewingRecipe;
import wakingnights.compat.SimpleDifficultyTemperature;
import wakingnights.handlers.ModRegistry;
import wakingnights.network.PacketHandler;
import wakingnights.proxy.CommonProxy;

@Mod(modid = WakingNights.MODID, version = WakingNights.VERSION, name = WakingNights.NAME, dependencies = "required-after:fermiumbooter@[1.5.2,);required-after:geckolib3;")
public class WakingNights {
    public static final String MODID = "wakingnights";
    public static final String VERSION = "0.1.0";
    public static final String NAME = "Waking Nights";
    public static final Logger LOGGER = LogManager.getLogger();
    public static boolean completedLoading = false;

    @SidedProxy(clientSide = "wakingnights.proxy.ClientProxy", serverSide = "wakingnights.proxy.CommonProxy")
    public static CommonProxy PROXY;

	@Instance(MODID)
	public static WakingNights instance;

	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GeckoLib.initialize();
        ModRegistry.init();
        PacketHandler.init();
        WakingNights.PROXY.preInit();

        CapabilitySpecialEntityHandler.registerCapability();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        NightHooks.init();
        LiquidBrewingRecipe.registerAll();
        //SimpleDifficulty is only compiled against
        if(Loader.isModLoaded("simpledifficulty")) SimpleDifficultyTemperature.register();
        completedLoading = true;
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWakingNights());
    }
}
