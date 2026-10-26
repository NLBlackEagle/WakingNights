package wakingnights;

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
import wakingnights.handlers.ModRegistry;
import wakingnights.proxy.CommonProxy;

@Mod(modid = WakingNights.MODID, version = WakingNights.VERSION, name = WakingNights.NAME, dependencies = "required-after:fermiumbooter@[1.5.2,);")
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
        ModRegistry.init();
        WakingNights.PROXY.preInit();

        CapabilitySpecialEntityHandler.registerCapability();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        completedLoading = true;
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWakingNights());
    }
}
