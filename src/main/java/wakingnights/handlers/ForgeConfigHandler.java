package wakingnights.handlers;

import fermiumbooter.annotations.MixinConfig;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import wakingnights.WakingNights;

@Config(modid = WakingNights.MODID)
public class ForgeConfigHandler {

	@Config.Comment("Server-Side Options")
	@Config.Name("Server Options")
	public static ServerConfig server = new ServerConfig();

	@Config.Comment("Client-Side Options")
	@Config.Name("Client Options")
	public static ClientConfig client = new ClientConfig();

	@MixinConfig(name = WakingNights.MODID) //Needed on config classes that contain MixinToggles for those mixins to be added
	public static class ServerConfig {

		@Config.Comment("Enables the vanilla mixins")
		@Config.Name("Enable Vanilla Mixins")
		@MixinConfig.MixinToggle(earlyMixin = "mixins.wakingnights.vanilla.json", defaultValue = true)
		public boolean enableVanillaMixin = true;

		@Config.Comment({
				"Nights needed for Waking Night I, II and III after a player's grace period.",
				"Must contain exactly 3 increasing values."
		})
		@Config.Name("Waking Night Thresholds")
		public int[] thresholds = {3, 5, 8};

		@Config.Comment({
				"Nights needed for Waking Night I, II and III during a player's grace period,",
				"which lasts until they have been through their first Waking Night event.",
				"Must contain exactly 3 increasing values."
		})
		@Config.Name("Grace Period Thresholds")
		public int[] graceThresholds = {5, 9, 14};

		@Config.Comment({
				"Lowest comfort tier (as shown in game, Comfort I = 1) that counts as a comfortable night.",
				"At or above it, a skipped night lowers the Waking Night level by one. Below it, the counter goes up by one."
		})
		@Config.Name("Minimum Comfort Tier")
		@Config.RangeInt(min = 1, max = 100)
		public int minComfortTier = 4;

		@Config.Comment("Effect that holds the comfort tier in its amplifier (ChunkComfort)")
		@Config.Name("Comfort Effect")
		public String comfortEffect = "chunkcomfort:comfort";

		@Config.Comment("Percentage of a night a player has to be online and awake for a Brave Night (lowers the level by one)")
		@Config.Name("Brave Night Awake Percentage")
		@Config.RangeInt(min = 0, max = 100)
		public int braveNightAwakePercentage = 75;

		@Config.Comment("If true, time spent awake only counts towards a Brave Night while outside a comfort zone")
		@Config.Name("Brave Night Requires Leaving Comfort")
		public boolean braveNightOutsideComfort = false;

		@Config.Comment("Ticks between rolls for Waking Night sounds and insomnia")
		@Config.Name("Effect Check Interval")
		@Config.RangeInt(min = 20)
		public int effectCheckInterval = 200;

		@Config.Comment("Sounds that randomly play near players at Waking Night I and up, at night")
		@Config.Name("Waking Night Sounds")
		public String[] sounds = {
				"minecraft:ambient.cave",
				"minecraft:entity.zombie.ambient",
				"minecraft:block.wooden_door.close",
				"minecraft:entity.spider.step",
				"minecraft:block.chest.open",
				"minecraft:entity.enderman.ambient"
		};

		@Config.Comment("Chance per check for a sound to play")
		@Config.Name("Sound Chance")
		@Config.RangeDouble(min = 0.0, max = 1.0)
		public double soundChance = 0.15;

		@Config.Comment("Effect randomly given at Waking Night II and up, at night")
		@Config.Name("Insomnia Effect")
		public String insomniaEffect = "lycanitesmobs:insomnia";

		@Config.Comment("Chance per check for insomnia to be given")
		@Config.Name("Insomnia Chance")
		@Config.RangeDouble(min = 0.0, max = 1.0)
		public double insomniaChance = 0.05;

		@Config.Comment("Duration of the insomnia effect in ticks")
		@Config.Name("Insomnia Duration")
		@Config.RangeInt(min = 1)
		public int insomniaDuration = 2400;
	}

	public static class ClientConfig {

	}

	@Mod.EventBusSubscriber
	private static class EventHandler{
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if(event.getModID().equals(WakingNights.MODID)) {
				ConfigManager.sync(WakingNights.MODID, Config.Type.INSTANCE);
			}
		}
	}
}
