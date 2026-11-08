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

		@Config.Comment("Lets growths on special mobs follow Mo' Bends' animated limbs")
		@Config.Name("Enable Mo' Bends Compatibility")
		@MixinConfig.MixinToggle(lateMixin = "mixins.wakingnights.mobends.json", defaultValue = true)
		@MixinConfig.CompatHandling(modid = "mobends", desired = true, disableMixin = true, warnIngame = false, reason = "Only needed with Mo' Bends")
		public boolean enableMoBendsMixin = true;

		@Config.Comment("Lets a Waking Night's own parasites past SRP's dimension whitelist and evolution phase spawn rules (other parasites keep following them)")
		@Config.Name("Enable SRParasites Compatibility")
		@MixinConfig.MixinToggle(lateMixin = "mixins.wakingnights.srparasites.json", defaultValue = true)
		@MixinConfig.CompatHandling(modid = "srparasites", desired = true, disableMixin = true, warnIngame = false, reason = "Only needed with SRParasites")
		public boolean enableSRParasitesMixin = true;

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

		@Config.Comment({
				"Percentage of a normal night a player has to be online, awake and active (not AFK) to count as staying awake,",
				"which lowers Waking Night I and II by one at dawn. 0: anyone online at dawn stays awake, AFK or not."
		})
		@Config.Name("Stay Awake Percentage Normal Nights")
		@Config.RangeInt(min = 0, max = 100)
		public int stayAwakePercentageNormal = 0;

		@Config.Comment("Percentage of a Waking Night a player at Waking Night III has to be online, awake and active (not AFK) to survive it")
		@Config.Name("Stay Awake Percentage Waking Nights")
		@Config.RangeInt(min = 0, max = 100)
		public int stayAwakePercentageWaking = 30;

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
				"Optional own spawns once the sun is gone: spawns=<entity id> <weight> [<from>];... in groups around each",
				"player (from: only from that time the night happens on, counted per world, e.g. 3 for the 3rd time and after; left out, from the 1st):",
				"spawnInterval=<seconds or min-max> between groups, spawnGroup=<count or min-max> per group, spawnCap=<count or",
				"min-max> most around each player (a range is picked once per night). Out of sight at first; spawnInSightAfter=<seconds>",
				"after nightfall they may also spawn in view, and with spawnCloseAtHalf=true closer and closer, with no distance",
				"left halfway through the night. spawnSpecialChance (0-100, default 100) is the percentage of them that turn special",
				"(blooded or blighted at random, they hunt the player). All are marked EagleMixinsOverworldAllowed (EagleMixins",
				"leaves them alone) and removed once the night is over, also when they were in an unloaded chunk then.",
				"Optional weather=none|rain|storm (from dusk until dawn), snow=true (its rain falls as snow everywhere, only",
				"drawn: no snow layers) and temperature=<number> (added to SimpleDifficulty's temperature, negative is colder).",
				"Optional specialMobs=<entity id>;... : only these mobs can turn special that night (a trailing * matches the",
				"start of the id, like lycanitesmobs:*). Left out, any hostile mob can (like Bloodmoon).",
				"Optional ramp=<multiplier>;<multiplier>;... : the 1st, 2nd, ... time the night happens (counted per world), its own",
				"spawns come in that much of their amount (cap and group size) and strength (health and damage); the last value",
				"holds from then on. Left out, always 1.0.",
				"Optional spawns.<dimension>=... : that dimension's own spawn list instead of spawns= (same format).",
				"Optional dimensions=<id>;<id>;... : where it can happen (e.g. 0;-1;1 for overworld, nether and end). Left out, only",
				"the overworld (0). During a Waking Night every dimension of any night type has one going on: the picked one where",
				"it happens, elsewhere one picked (by weight) among those that happen there. Each runs its own special mobs, own",
				"spawns, hooked events (Lycanites), temperature, banner and start sound; sky and weather follow the overworld's.",
				"Format: id=<name>, weight=<number>, sky=<hex>, fog=<hex>, clouds=<hex>, lightning=<hex>, light=<hex>, moon=<hex>, lightningPerMinute=<number>, thunderPitch=<number>, hook=<hook>, special=<type>, despawn=<true|false>, startSound=<sound id>"
		})
		@Config.Name("Night Types")
		public String[] nightTypes = {
				"id=bloodmoon, weight=1, sky=300000, fog=200000, clouds=000000, lightning=000000, light=B03A3A, moon=000000, lightningPerMinute=6, thunderPitch=0.6, hook=bloodmoon, special=blooded, despawn=true, startSound=wakingnights:event.bloodmoon_start, ramp=1.0;1.0;1.0;1.0;1.0, dimensions=0",
				"id=lycanites, weight=1, sky=0C1610, fog=000000, clouds=000000, lightning=3C5A46, light=8FA896, moon=D8E8DC, lightningPerMinute=6, thunderPitch=0.6, hook=lycanites, special=blighted, despawn=true, startSound=wakingnights:event.lycanites_start, specialMobs=lycanitesmobs:*, ramp=1.0;1.0;1.0;1.0;1.0, dimensions=0",
				"id=blight, weight=0.25, sky=000000, fog=000000, clouds=000000, lightning=7A2AB0, light=5A2E2E, moon=A04CFF, lightningPerMinute=6, thunderPitch=0.6, special=blighted, despawn=true, startSound=wakingnights:event.blight_start, specialMobs=srparasites:*, "
						+ "spawns=srparasites:sim_human 10;srparasites:sim_villager 8;srparasites:sim_cow 6;srparasites:sim_pig 6;srparasites:sim_sheep 6;"
						+ "srparasites:sim_wolf 6;srparasites:fer_human 4;srparasites:fer_villager 4;"
						+ "srparasites:sim_enderman 3 2;srparasites:sim_adventurer 3 2;srparasites:sim_bigspider 2 2;srparasites:pri_arachnida 2 2;"
						+ "srparasites:pri_longarms 2 3;srparasites:pri_manducater 1 3;srparasites:pri_bolster 1 3, "
						+ "spawnInterval=3-5, spawnGroup=2-8, spawnCap=35, ramp=0.6;0.8;1.0, dimensions=0, "
						+ "spawns.1=srparasites:sim_human 8;srparasites:sim_villager 6;srparasites:sim_cow 4;srparasites:sim_pig 4;srparasites:sim_sheep 4;"
						+ "srparasites:sim_wolf 4;srparasites:fer_human 3;srparasites:fer_villager 3;srparasites:pri_yelloweye 6;"
						+ "srparasites:sim_enderman 6 2;srparasites:sim_adventurer 2 2;srparasites:sim_bigspider 2 2;srparasites:pri_arachnida 2 2;"
						+ "srparasites:carrier_light 3 2;srparasites:carrier_flying 2 2;"
						+ "srparasites:pri_longarms 2 3;srparasites:pri_manducater 1 3;srparasites:pri_bolster 1 3;"
						+ "srparasites:carrier_heavy 1 3;srparasites:bomber_light 1 3",
				"id=dread, weight=0.25, sky=0A1640, fog=060C24, clouds=0A1030, lightning=40E0FF, light=C8E4FF, moon=40F0FF, lightningPerMinute=6, thunderPitch=0.6, special=mixed, despawn=true, startSound=wakingnights:event.dread_start, "
						+ "spawns=iceandfire:dread_thrall 10;iceandfire:dread_ghoul 8;iceandfire:dread_scuttler 4;"
						+ "iceandfire:dread_beast 3 2;iceandfire:dread_horse 2 2;iceandfire:dread_knight 2 3;iceandfire:dread_lich 1 3, "
						+ "spawnInterval=3-5, spawnGroup=2-8, spawnCap=25-45, spawnInSightAfter=30, spawnCloseAtHalf=true, spawnSpecialChance=12, weather=storm, snow=true, temperature=-5, specialMobs=iceandfire:dread_*, ramp=1.0;1.0;1.0;1.0;1.0, dimensions=0"
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

		@Config.Comment("Special mobs' melee damage is multiplied by this, whatever mod they're from (projectiles aren't)")
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

		@Config.Comment("Special mobs' armor is multiplied by this (mobs without armor get none)")
		@Config.Name("Special Mob Armor Multiplier")
		@Config.RangeDouble(min = 0.1)
		public double specialArmorMultiplier = 1.5;

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
				"Lycanites Mobs events a night type with hook=lycanites starts, in waves from nightfall until dawn: one picked",
				"at random each wave. Use /wakingnights lycanites to list every event Lycanites has loaded (also in the log).",
				"Only world events work. A poopparty wave turns the sky brown with yellow lightning while it lasts.",
				"Left out by default: boss altar events (amalgalich, asmodeus, rahovart), seasonal ones",
				"(halloween, roasting, rudolph, saltytree, satanclaws), cinderfall, dragonsroar and eruption, and",
				"blackplague and shadowgames: those set the time to 20000, which skips most of the night."
		})
		@Config.Name("Lycanites Events")
		public String[] lycanitesEvents = {
				"aberrant_assimilation", "arachnophobia", "bamstorm", "bladeflurry", "boulderdash", "hellsfury",
				"marchofthegorgomites", "primalfury", "raptorrampage", "reptileruckus", "rootriot", "seastorm", "sharknado",
				"subzero", "theswarm", "tsunami", "windstorm", "wingedvenom", "wintersgrasp", "poopparty"
		};

		@Config.Comment({
				"Lycanites events for other dimensions than the overworld, as <dimension>=<event>;<event>;... A dimension not",
				"listed uses Lycanites Events. Lycanites events can refuse some dimensions in their own config, then they stay quiet there."
		})
		@Config.Name("Lycanites Events Per Dimension")
		public String[] lycanitesEventsPerDimension = {
				"-1=hellsfury;marchofthegorgomites;aberrant_assimilation;wingedvenom;reptileruckus;boulderdash;poopparty",
				"1=windstorm;theswarm;aberrant_assimilation;wingedvenom;bamstorm;poopparty"
		};

		@Config.Comment("If true, a Lycanites wave's mobs vanish when the next wave starts, and the last wave's at dawn (tamed ones stay)")
		@Config.Name("Lycanites Despawn Waves")
		public boolean lycanitesDespawnWaves = true;

		@Config.Comment("Seconds after nightfall before the first Lycanites event wave, so it doesn't start over the Waking Night banner")
		@Config.Name("Lycanites First Event Seconds")
		@Config.RangeInt(min = 0)
		public int lycanitesFirstEventSeconds = 10;

		@Config.Comment("Seconds of quiet between Lycanites event waves")
		@Config.Name("Lycanites Pause Seconds")
		@Config.RangeInt(min = 0)
		public int lycanitesPauseSeconds = 20;

		@Config.Comment({
				"Lycanites event waves per night, picked at random between these each night (both included). The waves are",
				"stretched to fill the night between them. Events come from the whitelist without repeats: each plays once",
				"before any plays again."
		})
		@Config.Name("Lycanites Waves Min")
		@Config.RangeInt(min = 1)
		public int lycanitesWavesMin = 2;

		@Config.Name("Lycanites Waves Max")
		@Config.RangeInt(min = 1)
		public int lycanitesWavesMax = 3;

		@Config.Comment({
				"Brewed into Liquid Dreams from an Awkward Potion: <mod>:<item> or <mod>:<item>:<metadata>.",
				"Empty for no brewing recipe. Needs a restart."
		})
		@Config.Name("Liquid Dreams Brewing Ingredient")
		public String liquidDreamsIngredient = "";

		@Config.Comment({
				"Brewed into Liquid Nightmares from an Awkward Potion: <mod>:<item> or <mod>:<item>:<metadata>.",
				"Empty for no brewing recipe. Needs a restart."
		})
		@Config.Name("Liquid Nightmares Brewing Ingredient")
		public String liquidNightmaresIngredient = "biomesoplenty:flower_0:2";

		@Config.Comment("Tooltip on Liquid Dreams potions, empty for none. & colour codes work (&6 = gold).")
		@Config.Name("Liquid Dreams Tooltip")
		public String liquidDreamsTooltip = "&6Can be obtained through any Herbalist in the Herbalist Hut";

		@Config.Comment("Tooltip on Liquid Nightmares potions, empty for none. & colour codes work (&6 = gold).")
		@Config.Name("Liquid Nightmares Tooltip")
		public String liquidNightmaresTooltip = "&6An evil concoction brewed from the worst of flora found in biomes such as the Corrupted Scarlet Grove";

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
				"minecraft:zombie_horse=0.6",
				"iceandfire:dread_beast=0.25"
		};

		@Config.Comment({
				"At Waking Night II, a see-through figure very rarely appears at the edge of your sight at night, and is gone",
				"when you look straight at it. On average once per this many minutes spent at Waking Night II at night. 0 turns it off."
		})
		@Config.Name("Apparition Average Minutes")
		@Config.RangeDouble(min = 0.0)
		public double apparitionAverageMinutes = 60.0;

		@Config.Comment({
				"If true, the apparition can look like any hostile mob any mod registered (parasites, Lycanites creatures, ...), not",
				"bosses or anything bigger than 3 wide and 4 high. It's only drawn on the client, never added to the world. If",
				"false, it's one of the Apparition Entities."
		})
		@Config.Name("Apparition Any Mob")
		public boolean apparitionAnyMob = true;

		@Config.Comment("Mobs the apparition can look like (entity ids), with Apparition Any Mob off")
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
