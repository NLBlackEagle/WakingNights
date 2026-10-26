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

		@Config.Comment("Percentage of a night a player has to be online and awake to count as staying awake (lowers the level by one)")
		@Config.Name("Stay Awake Percentage")
		@Config.RangeInt(min = 0, max = 100)
		public int stayAwakePercentage = 75;

		@Config.Comment({
				"Minutes without doing anything (looking around, walking, clicking, chatting) after which a player counts as AFK.",
				"AFK time doesn't count towards staying awake through a night or surviving a Waking Night."
		})
		@Config.Name("AFK Minutes")
		@Config.RangeInt(min = 1, max = 60)
		public int afkMinutes = 5;

		@Config.Comment("If true, time spent awake only counts while outside a comfort zone")
		@Config.Name("Stay Awake Requires Leaving Comfort")
		public boolean stayAwakeOutsideComfort = false;

		@Config.Comment("Ticks between rolls for Waking Night sounds and insomnia")
		@Config.Name("Effect Check Interval")
		@Config.RangeInt(min = 20)
		public int effectCheckInterval = 200;

		@Config.Comment({
				"Spot sounds that randomly play somewhere near players at Waking Night I, at night.",
				"Format: <sound id> or <sound id>=<volume, 0 to 1>. Any sound id works; wakingnights:insomnia picks one of its sounds at random each time.",
				"The default volumes bring every sound to about the same loudness (that of the quietest, footsteps on wood)."
		})
		@Config.Name("Waking Night I Sounds")
		public String[] levelOneSounds = {
				"minecraft:block.wooden_door.close=0.58",
				"minecraft:block.chest.open=0.27",
				"minecraft:block.wood.step=1.0",
				"minecraft:entity.spider.step=0.89",
				"minecraft:ambient.cave=0.4"
		};

		@Config.Comment("Spot sounds that randomly play somewhere near players at Waking Night II, at night. Same format as Waking Night I.")
		@Config.Name("Waking Night II Sounds")
		public String[] levelTwoSounds = {
				"minecraft:entity.zombie.ambient=0.51",
				"minecraft:entity.endermen.ambient=0.46",
				"minecraft:entity.witch.ambient=0.42",
				"minecraft:entity.skeleton.step=0.8",
				"minecraft:block.wooden_door.open=0.95",
				"wakingnights:insomnia=1.0"
		};

		@Config.Comment("Spot sounds that randomly play somewhere near players at Waking Night III, at night. Same format as Waking Night I.")
		@Config.Name("Waking Night III Sounds")
		public String[] levelThreeSounds = {
				"minecraft:entity.zombie.attack_door_wood=0.17",
				"minecraft:entity.endermen.stare=0.3",
				"minecraft:entity.elder_guardian.curse=0.28",
				"minecraft:ambient.cave=0.4",
				"wakingnights:insomnia=1.0",
				"wakingnights:insomnia=1.0"
		};

		@Config.Comment({
				"Background ambience at night, all around the player (stereo sounds aren't placed in the world).",
				"Empty to turn it off. wakingnights:background picks one of its sounds at random each time."
		})
		@Config.Name("Background Sound")
		public String backgroundSound = "wakingnights:background";

		@Config.Comment("Waking Night level from which players hear the background ambience at night. Everyone hears it during a Waking Night.")
		@Config.Name("Background Sound Level")
		@Config.RangeInt(min = 1, max = 3)
		public int backgroundMinLevel = 3;

		@Config.Comment("Shortest time between two background ambience sounds, in seconds")
		@Config.Name("Background Sound Min Seconds")
		@Config.RangeInt(min = 1)
		public int backgroundMinSeconds = 30;

		@Config.Comment("Longest time between two background ambience sounds, in seconds")
		@Config.Name("Background Sound Max Seconds")
		@Config.RangeInt(min = 1)
		public int backgroundMaxSeconds = 90;

		@Config.Comment("Chance per check for a sound to play")
		@Config.Name("Sound Chance")
		@Config.RangeDouble(min = 0.0, max = 1.0)
		public double soundChance = 0.15;

		@Config.Comment("Effect randomly given at Waking Night II and up, at night")
		@Config.Name("Insomnia Effect")
		public String insomniaEffect = "lycanitesmobs:insomnia";

		@Config.Comment({
				"The kinds of Waking Night, one is picked at dusk. The night picked last time has its weight divided by 3.",
				"sky, fog, clouds and lightning are hex RGB colours the night turns to while the sun sets.",
				"light is a hex RGB colour the light from the sky is multiplied by (FFFFFF = unchanged, darker = darker);",
				"torches and other block light keep their colour. moon is the moon's hex RGB colour (FFFFFF = unchanged, 000000 = a dark disc).",
				"lightningPerMinute is the average number of strikes around each player once the sun is gone",
				"(visual only, no fire or damage). thunderPitch multiplies vanilla's thunder pitch, below 1 is deeper",
				"(Minecraft plays nothing lower than 0.5, vanilla thunder is 0.8 to 1.0).",
				"hook starts another mod's night along with it once the sun is gone, and stops it at dawn: bloodmoon (Bloodmoon mod).",
				"special is what mobs that turn special during this night become: blooded, blighted, mixed or none.",
				"despawn: whether those special mobs vanish at dawn (true or false).",
				"startSound: sound played all around every player when the night truly starts, alongside its banner",
				"(textures/gui/events/<id>.png). Optional: hitEffects=<potion> <amplifier> <ticks>;... (see Special Mob Hit Effects).",
				"Optional own spawns once the sun is gone: spawns=<entity id> <weight>;... placed out of sight around each player,",
				"spawnsPerMinute (attempts around each player) and spawnCap (most alive per player). Every one is blooded or",
				"blighted at random, hunts the player, is marked overworld_allowed (EagleMixins leaves it alone) and is removed",
				"once the night is over, also when it was in an unloaded chunk then.",
				"Format: id=<name>, weight=<number>, sky=<hex>, fog=<hex>, clouds=<hex>, lightning=<hex>, light=<hex>, moon=<hex>, lightningPerMinute=<number>, thunderPitch=<number>, hook=<hook>, special=<type>, despawn=<true|false>, startSound=<sound id>"
		})
		@Config.Name("Night Types")
		public String[] nightTypes = {
				"id=bloodmoon, weight=1, sky=300000, fog=200000, clouds=000000, lightning=000000, light=B03A3A, moon=000000, lightningPerMinute=6, thunderPitch=0.6, hook=bloodmoon, special=blooded, despawn=true, startSound=wakingnights:event.bloodmoon_start",
				"id=lycanites, weight=1, sky=0A1F07, fog=000000, clouds=000000, lightning=143C0A, light=216721, moon=FFFFFF, lightningPerMinute=6, thunderPitch=0.6, special=blighted, despawn=true, startSound=wakingnights:event.lycanites_start",
				"id=blight, weight=0.25, sky=000000, fog=000000, clouds=000000, lightning=7A2AB0, light=216721, moon=A04CFF, lightningPerMinute=6, thunderPitch=0.6, special=blighted, despawn=true, startSound=wakingnights:event.blight_start, "
						+ "spawns=srparasites:sim_human 10;srparasites:sim_villager 8;srparasites:sim_cow 6;srparasites:sim_pig 6;srparasites:sim_sheep 6;"
						+ "srparasites:sim_wolf 6;srparasites:sim_adventurer 4;srparasites:pri_longarms 4;srparasites:pri_manducater 3;"
						+ "srparasites:fer_human 3;srparasites:fer_villager 3;srparasites:sim_bigspider 2;srparasites:sim_enderman 2, "
						+ "spawnsPerMinute=12, spawnCap=12"
		};

		@Config.Comment({
				"Chance in percent for a hostile mob spawning near a player during a Waking Night (once the sun is gone)",
				"to turn special (blooded or blighted, per night type). Special mobs know where the player is and hunt them."
		})
		@Config.Name("Special Mob Chance")
		@Config.RangeDouble(min = 0.0, max = 100.0)
		public double specialChance = 12.0;

		@Config.Comment("Most special mobs hunting one player at the same time")
		@Config.Name("Special Mobs Alive Per Player")
		@Config.RangeInt(min = 0)
		public int specialMaxAlive = 15;

		@Config.Comment("Seconds after a player dies before the next special mob can come for them")
		@Config.Name("Special Mob Grace After Death")
		@Config.RangeInt(min = 0)
		public int specialDeathGraceSeconds = 60;

		@Config.Comment("Least time between two new special mobs for one player, in seconds")
		@Config.Name("Seconds Between Special Mobs")
		@Config.RangeInt(min = 0)
		public int specialSecondsBetween = 3;

		@Config.Comment("Special mobs' max health is multiplied by this")
		@Config.Name("Special Mob Health Multiplier")
		@Config.RangeDouble(min = 0.1)
		public double specialHealthMultiplier = 1.5;

		@Config.Comment("Special mobs' attack damage is multiplied by this")
		@Config.Name("Special Mob Damage Multiplier")
		@Config.RangeDouble(min = 0.1)
		public double specialDamageMultiplier = 1.25;

		@Config.Comment("Special mobs' movement speed is multiplied by this")
		@Config.Name("Special Mob Speed Multiplier")
		@Config.RangeDouble(min = 0.1)
		public double specialSpeedMultiplier = 1.15;

		@Config.Comment("Added to special mobs' knockback resistance (1 = can't be knocked back)")
		@Config.Name("Special Mob Knockback Resistance")
		@Config.RangeDouble(min = 0.0, max = 1.0)
		public double specialKnockbackResistance = 0.5;

		@Config.Comment("Added to special mobs' armor")
		@Config.Name("Special Mob Armor")
		@Config.RangeDouble(min = 0.0)
		public double specialArmor = 4.0;

		@Config.Comment({
				"Effects special mobs give when they hit a player (also with arrows), as <potion id> <amplifier> <ticks>.",
				"Amplifier 0 is level I, 20 ticks is a second. Potions from mods that aren't installed are skipped.",
				"A night type can have its own with hitEffects=... in its Night Types entry."
		})
		@Config.Name("Special Mob Hit Effects")
		public String[] specialHitEffects = {
				"srparasites:bleed 0 100"
		};

		@Config.Comment({
				"What blooded mobs drop when a player kills them, one drop per line as <item id> <chance in percent>.",
				"Items from mods that aren't installed are skipped."
		})
		@Config.Name("Blooded Drops")
		public String[] bloodedDrops = {
				"wardlights:blood_essence 100"
		};

		@Config.Comment("What blighted mobs drop when a player kills them, one drop per line as <item id> <chance in percent>")
		@Config.Name("Blighted Drops")
		public String[] blightedDrops = {
				"wardlights:blight_essence 100"
		};

		@Config.Comment("If true, special mobs that walk can break wooden doors (only on Hard difficulty with mobGriefing on, like vanilla zombies)")
		@Config.Name("Special Mobs Break Doors")
		public boolean specialBreakDoors = true;

		@Config.Comment("If true, a special mob a player looks straight at stops dead and stares back, until they look away")
		@Config.Name("Special Mobs Freeze When Watched")
		public boolean specialFreezeWhenWatched = true;

		@Config.Comment("Closer than this (blocks), special mobs keep fighting even when watched")
		@Config.Name("Special Mob Freeze Min Distance")
		@Config.RangeDouble(min = 0.0)
		public double specialFreezeMinDistance = 6.0;

		@Config.Comment("How close to a player a mob has to spawn to be able to turn special for them, in blocks")
		@Config.Name("Special Mob Range")
		@Config.RangeDouble(min = 1.0)
		public double specialRange = 64.0;

		@Config.Comment({
				"Mobs that never turn special (entity ids). Bosses never do anyway.",
				"The defaults are ranged mobs that aren't caught by Special Mobs Exclude Ranged."
		})
		@Config.Name("Special Mob Blacklist")
		public String[] specialBlacklist = {
				"minecraft:wither",
				"minecraft:ender_dragon",
				"minecraft:elder_guardian",
				"minecraft:blaze",
				"minecraft:ghast",
				"minecraft:shulker",
				"minecraft:guardian",
				"minecraft:evocation_illager"
		};

		@Config.Comment({
				"If true, mobs with a ranged attack never turn special: everything Minecraft marks as ranged (skeletons,",
				"strays, witches, illusioners and modded mobs that use the same marker). Other ranged mobs go in the blacklist."
		})
		@Config.Name("Special Mobs Exclude Ranged")
		public boolean specialExcludeRanged = true;

		@Config.Comment({
				"If true, players at Waking Night III always have the insomnia effect (if it exists, e.g. with Lycanites),",
				"so they can see why they can't sleep. Sleeping is blocked at Waking Night III either way."
		})
		@Config.Name("Level III Insomnia")
		public boolean levelThreeInsomnia = true;

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

		@Config.Comment({
				"Growth size per mob, as entity id=multiplier. Mobs not listed use 1.0.",
				"Growths are sized from the body part they sit on; use this for mobs where that comes out too big or small."
		})
		@Config.Name("Growth Size Multipliers")
		public String[] growthSizeMultipliers = {
				"minecraft:spider=0.5",
				"minecraft:cave_spider=0.5",
				"minecraft:horse=0.6",
				"minecraft:donkey=0.6",
				"minecraft:mule=0.6",
				"minecraft:skeleton_horse=0.6",
				"minecraft:zombie_horse=0.6"
		};

		@Config.Comment({
				"At Waking Night II, a see-through figure very rarely appears at the edge of your sight at night, and is gone",
				"when you look straight at it. On average once per this many minutes spent at Waking Night II at night. 0 turns it off."
		})
		@Config.Name("Apparition Average Minutes")
		@Config.RangeDouble(min = 0.0)
		public double apparitionAverageMinutes = 60.0;

		@Config.Comment("Mobs the apparition can look like (entity ids)")
		@Config.Name("Apparition Entities")
		public String[] apparitionEntities = {
				"minecraft:zombie",
				"minecraft:skeleton",
				"minecraft:zombie_villager",
				"minecraft:witch",
				"minecraft:enderman",
				"minecraft:villager"
		};
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
