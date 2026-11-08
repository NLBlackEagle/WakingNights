package wakingnights.event;

import net.minecraft.util.ResourceLocation;
import wakingnights.WakingNights;
import wakingnights.capability.SpecialType;
import wakingnights.handlers.ForgeConfigHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

/**
 * A kind of Waking Night (Bloodmoon, Lycanites, ...), read from the "Night Types" config.
 * Entries look like: id=bloodmoon, weight=1, sky=000000, fog=000000, clouds=000000, lightning=000000, light=FFFFFF, moon=FFFFFF, lightningPerMinute=6, thunderPitch=0.6, hook=bloodmoon,
 * special=blooded (blooded, blighted, mixed or none: what mobs that turn special during this night become), despawn=true,
 * hitEffects=srparasites:bleed 0 100;minecraft:slowness 0 60 (optional, overrides the Special Mob Hit Effects config),
 * startSound=wakingnights:event.bloodmoon_start (optional, played when the night truly starts),
 * spawns=srparasites:sim_human 10;srparasites:sim_enderman 2 2 (the last part: only from the 2nd time the night happens), spawnInterval=3-5, spawnGroup=2-8, spawnCap=35 (or 25-45),
 * spawnSpecialChance=100, spawnInSightAfter=30, spawnCloseAtHalf=true (optional, its own spawns; spawns.1=... is the list for
 * dimension 1 instead), weather=none|rain|storm, snow=true (rain falls as snow everywhere, only drawn),
 * temperature=-5 (SimpleDifficulty temperature change while it lasts),
 * specialMobs=lycanitesmobs:*;iceandfire:dread_* (only these mobs turn special this night, all hostile ones if left out),
 * ramp=0.6;0.8;1.0 (its own spawns' amount and strength the 1st, 2nd, ... time it happens, the last value from then on),
 * dimensions=0;-1;1 (where it can happen; the overworld if left out. A dimension the picked night doesn't happen in gets
 * one of the night types that do, at the same time)
 */
public class NightType {
    public final String id;
    public final double weight;
    /** RGB colours the sky, fog, clouds and lightning turn to during this night */
    public final int sky, fog, clouds, lightning;
    /** RGB colour the light from the sky is multiplied by (white = unchanged), torches and other block light keep theirs */
    public final int light;
    /** RGB colour of the moon (white = unchanged, black = a dark disc) */
    public final int moon;
    /** Average lightning strikes per minute around each player (visual only) */
    public final double lightningPerMinute;
    /** Multiplies vanilla's thunder pitch, below 1 is deeper. Minecraft plays nothing lower than 0.5. */
    public final float thunderPitch;
    /** Another mod's night started along with this one (see NightHooks), null for none */
    @Nullable
    public final String hook;
    /** What the mobs that turn special during this night become, null for none (or mixed) */
    @Nullable
    public final SpecialType special;
    /** Blooded or blighted at random */
    public final boolean mixedSpecial;
    /** Whether the mobs that turned special during this night vanish at dawn */
    public final boolean despawn;
    /** Effects special mobs give when they hit a player during this night, null to use the Special Mob Hit Effects config */
    @Nullable
    public final List<HitEffect> hitEffects;
    /** Sound played all around every player when this night truly starts (nightfall), null for none */
    @Nullable
    public final String startSound;
    /** This night's own spawns (EventSpawner), and the lists of dimensions that have their own (spawns.<dimension>=) */
    public final SpawnList spawns;
    public final Map<Integer, SpawnList> dimensionSpawns;
    /** Seconds between groups around each player, and how many each group has (random between min and max) */
    public final double spawnIntervalMin, spawnIntervalMax;
    public final int spawnGroupMin, spawnGroupMax;
    /** The most of them around each player, picked between these for each night */
    public final int spawnCapMin, spawnCapMax;
    /** Seconds after nightfall from when they may also spawn in view, -1 for never */
    public final int spawnInSightAfter;
    /** Whether they spawn closer and closer once they may spawn in view, with no distance left halfway through the night */
    public final boolean spawnCloseAtHalf;
    /** Percentage of its own spawns that are special (blooded or blighted at random) */
    public final int spawnSpecialChance;
    public final Weather weather;
    /** Whether its rain is drawn as snow everywhere */
    public final boolean snow;
    /** Added to SimpleDifficulty's temperature in the overworld while it lasts */
    public final float temperature;
    /** Its own spawns' amount and strength the 1st, 2nd, ... time this night happens; the last value from then on */
    public final List<Double> ramp;
    /** Dimensions where its mobs turn special, its own spawns come and its hooked events happen */
    public final List<Integer> dimensions;
    /** Entity ids that can turn special this night (a trailing * matches the start), empty for any hostile mob */
    public final List<String> specialMobs;

    public enum Weather { NONE, RAIN, STORM }

    private static String[] parsedConfig;
    private static List<NightType> types = Collections.emptyList();

    private NightType(String id, double weight, int sky, int fog, int clouds, int lightning, int light, int moon, double lightningPerMinute, float thunderPitch,
                      @Nullable String hook, @Nullable SpecialType special, boolean mixedSpecial, boolean despawn, @Nullable List<HitEffect> hitEffects, @Nullable String startSound,
                      SpawnList spawns, Map<Integer, SpawnList> dimensionSpawns, double[] spawnInterval, int[] spawnGroup, int[] spawnCap,
                      int spawnInSightAfter, boolean spawnCloseAtHalf, int spawnSpecialChance,
                      Weather weather, boolean snow, float temperature, List<String> specialMobs, List<Double> ramp, List<Integer> dimensions) {
        this.id = id;
        this.weight = weight;
        this.sky = sky;
        this.fog = fog;
        this.clouds = clouds;
        this.lightning = lightning;
        this.light = light;
        this.moon = moon;
        this.lightningPerMinute = lightningPerMinute;
        this.thunderPitch = thunderPitch;
        this.hook = hook;
        this.special = special;
        this.mixedSpecial = mixedSpecial;
        this.despawn = despawn;
        this.hitEffects = hitEffects;
        this.startSound = startSound;
        this.spawns = spawns;
        this.dimensionSpawns = dimensionSpawns;
        this.spawnIntervalMin = spawnInterval[0];
        this.spawnIntervalMax = spawnInterval[1];
        this.spawnGroupMin = spawnGroup[0];
        this.spawnGroupMax = spawnGroup[1];
        this.spawnCapMin = spawnCap[0];
        this.spawnCapMax = spawnCap[1];
        this.spawnInSightAfter = spawnInSightAfter;
        this.spawnCloseAtHalf = spawnCloseAtHalf;
        this.spawnSpecialChance = spawnSpecialChance;
        this.weather = weather;
        this.snow = snow;
        this.temperature = temperature;
        this.specialMobs = specialMobs;
        this.ramp = ramp;
        this.dimensions = dimensions;
    }

    /** Whether this night happens in that dimension */
    public boolean happensIn(int dimension) {
        return this.dimensions.contains(dimension);
    }

    /** Its ramp value for the given time this night happens (1 = the first time) */
    public double getRamp(int timesHappened) {
        if(this.ramp.isEmpty()) return 1.0D;
        return this.ramp.get(Math.max(0, Math.min(timesHappened, this.ramp.size()) - 1));
    }

    /** Whether a mob with this id can turn special this night */
    public boolean allowsSpecial(@Nullable ResourceLocation id) {
        if(this.specialMobs.isEmpty()) return true;
        if(id == null) return false;
        String name = id.toString();
        for(String pattern : this.specialMobs) {
            if(pattern.endsWith("*") ? name.startsWith(pattern.substring(0, pattern.length() - 1)) : name.equals(pattern)) return true;
        }
        return false;
    }

    /** The own spawns in a dimension: its own list if it has one, else the night's */
    public SpawnList getSpawns(int dimension) {
        return this.dimensionSpawns.getOrDefault(dimension, this.spawns);
    }

    /** Entity ids with their weights, and from which time the night happens (1 = the first) each comes in */
    public static final class SpawnList {
        private final List<String> ids = new ArrayList<>();
        private final List<Double> weights = new ArrayList<>();
        private final List<Integer> fromTimes = new ArrayList<>();

        /** <entity id> <weight> [<from time>];... */
        static SpawnList parse(String value) {
            SpawnList list = new SpawnList();
            for(String spawn : value.split(";")) {
                String[] parts = spawn.trim().split("\\s+");
                if(parts.length < 2) continue;
                list.ids.add(parts[0]);
                list.weights.add(Math.max(0.0D, Double.parseDouble(parts[1])));
                list.fromTimes.add(parts.length > 2 ? parseFromTime(spawn, parts[2]) : 1);
            }
            return list;
        }

        /** The old part-of-night window (40-100) is no longer read: such a spawn comes in from the first time */
        private static int parseFromTime(String spawn, String value) {
            if(value.contains("-")) {
                WakingNights.LOGGER.warn("Spawn '{}': part-of-night windows are no longer used, the last part is now from which time the night happens it comes in (e.g. 3); it comes in from the first time", spawn.trim());
                return 1;
            }
            return Math.max(1, Integer.parseInt(value.trim()));
        }

        public boolean isEmpty() {
            return this.ids.isEmpty();
        }

        /** A random entity id that comes in by the given time the night happens (1 = the first), by weight, null if there's none */
        @Nullable
        public String pick(Random rand, int timesHappened) {
            double total = 0;
            for(int i = 0; i < this.ids.size(); i++) {
                if(this.comesIn(i, timesHappened)) total += this.weights.get(i);
            }
            if(total <= 0) return null;
            double target = rand.nextDouble() * total;
            String last = null;
            for(int i = 0; i < this.ids.size(); i++) {
                if(!this.comesIn(i, timesHappened)) continue;
                last = this.ids.get(i);
                target -= this.weights.get(i);
                if(target < 0) return last;
            }
            return last;
        }

        private boolean comesIn(int spawn, int timesHappened) {
            return timesHappened >= this.fromTimes.get(spawn);
        }
    }

    public static List<NightType> getAll() {
        String[] configured = ForgeConfigHandler.server.nightTypes;
        //The config screen replaces the array when it's changed
        if(configured != parsedConfig) {
            List<NightType> parsed = new ArrayList<>();
            for(String entry : configured) {
                NightType type = parse(entry);
                if(type != null) parsed.add(type);
            }
            types = parsed;
            parsedConfig = configured;
        }
        return types;
    }

    @Nullable
    public static NightType get(String id) {
        for(NightType type : getAll()) {
            if(type.id.equals(id)) return type;
        }
        return null;
    }

    /** Weighted pick. The night picked last time has its weight divided by 3, so the same night rarely repeats. */
    @Nullable
    public static NightType pick(@Nullable String lastId, Random rand) {
        List<NightType> all = getAll();
        double total = 0;
        for(NightType type : all) total += type.getWeight(lastId);
        if(total <= 0) return null;

        double target = rand.nextDouble() * total;
        for(NightType type : all) {
            target -= type.getWeight(lastId);
            if(target < 0) return type;
        }
        return all.get(all.size() - 1);
    }

    /**
     * The night type of every dimension any night type happens in: the main one where it happens, elsewhere one picked
     * by weight among those that do happen there
     */
    public static Map<Integer, String> assignDimensions(@Nullable NightType main, Random rand) {
        Map<Integer, String> assigned = new HashMap<>();
        for(NightType type : getAll()) {
            for(int dimension : type.dimensions) {
                if(assigned.containsKey(dimension)) continue;
                if(main != null && main.happensIn(dimension)) {
                    assigned.put(dimension, main.id);
                    continue;
                }
                List<NightType> there = new ArrayList<>();
                double total = 0;
                for(NightType candidate : getAll()) {
                    if(!candidate.happensIn(dimension)) continue;
                    there.add(candidate);
                    total += candidate.weight;
                }
                double target = rand.nextDouble() * total;
                NightType picked = there.get(there.size() - 1);
                for(NightType candidate : there) {
                    target -= candidate.weight;
                    if(target < 0) {
                        picked = candidate;
                        break;
                    }
                }
                assigned.put(dimension, picked.id);
            }
        }
        return assigned;
    }

    /** Whether mobs turn into this kind of special during this night */
    public boolean produces(SpecialType type) {
        return type != SpecialType.NONE && (this.mixedSpecial || this.special == type);
    }

    /** Whether this kind of special stays around outside its nights: a night type making them has despawn=false */
    public static boolean isKept(SpecialType type) {
        for(NightType nightType : getAll()) {
            if(!nightType.despawn && nightType.produces(type)) return true;
        }
        return false;
    }

    private double getWeight(@Nullable String lastId) {
        return this.id.equals(lastId) ? this.weight / 3.0D : this.weight;
    }

    @Nullable
    private static NightType parse(String entry) {
        String id = null;
        double weight = 1.0D;
        int sky = 0, fog = 0, clouds = 0, lightning = 0, light = 0xFFFFFF, moon = 0xFFFFFF;
        double lightningPerMinute = 6.0D;
        float thunderPitch = 0.6F;
        String hook = null;
        SpecialType special = null;
        boolean mixedSpecial = false;
        boolean despawn = true;
        List<HitEffect> hitEffects = null;
        String startSound = null;
        SpawnList spawns = new SpawnList();
        Map<Integer, SpawnList> dimensionSpawns = new HashMap<>();
        double[] spawnInterval = {0.0D, 0.0D};
        int[] spawnGroup = {1, 1};
        int[] spawnCap = {10, 10};
        int spawnInSightAfter = -1;
        boolean spawnCloseAtHalf = false;
        int spawnSpecialChance = 100;
        Weather weather = Weather.NONE;
        boolean snow = false;
        float temperature = 0.0F;
        List<String> specialMobs = new ArrayList<>();
        List<Double> ramp = new ArrayList<>();
        List<Integer> dimensions = new ArrayList<>();
        try {
            for(String part : entry.split(",")) {
                String[] keyValue = part.split("=", 2);
                if(keyValue.length != 2) continue;
                String value = keyValue[1].trim();
                switch(keyValue[0].trim()) {
                    case "id": id = value; break;
                    case "weight": weight = Double.parseDouble(value); break;
                    case "sky": sky = parseColor(value); break;
                    case "fog": fog = parseColor(value); break;
                    case "clouds": clouds = parseColor(value); break;
                    case "lightning": lightning = parseColor(value); break;
                    case "light": light = parseColor(value); break;
                    case "moon": moon = parseColor(value); break;
                    case "lightningPerMinute": lightningPerMinute = Math.max(0.0D, Double.parseDouble(value)); break;
                    case "thunderPitch": thunderPitch = Math.max(0.0F, Float.parseFloat(value)); break;
                    case "hook": hook = value.isEmpty() || value.equals("none") ? null : value; break;
                    case "special":
                        mixedSpecial = value.equalsIgnoreCase("mixed");
                        if(value.equalsIgnoreCase("blooded")) special = SpecialType.BLOODED;
                        else if(value.equalsIgnoreCase("blighted")) special = SpecialType.BLIGHTED;
                        break;
                    case "despawn": despawn = Boolean.parseBoolean(value); break;
                    //Several separated by ";", "none" for no effects at all this night
                    case "hitEffects": hitEffects = HitEffect.parseAll(Arrays.asList(value.split(";"))); break;
                    case "startSound": startSound = value.isEmpty() || value.equals("none") ? null : value; break;
                    //<entity id> <weight> [<from time the night happens>];...
                    case "spawns": spawns = SpawnList.parse(value); break;
                    //Older entries: on average this many per minute, one at a time
                    case "spawnsPerMinute": {
                        double perMinute = Double.parseDouble(value);
                        if(perMinute > 0) spawnInterval = new double[]{60.0D / perMinute, 60.0D / perMinute};
                        break;
                    }
                    case "spawnInterval": {
                        int[] range = parseRange(value);
                        spawnInterval = new double[]{range[0], range[1]};
                        break;
                    }
                    case "spawnGroup": spawnGroup = parseRange(value); break;
                    case "spawnCap": spawnCap = parseRange(value); break;
                    case "spawnInSightAfter": spawnInSightAfter = Integer.parseInt(value); break;
                    case "spawnCloseAtHalf": spawnCloseAtHalf = Boolean.parseBoolean(value); break;
                    case "spawnSpecialChance": spawnSpecialChance = Math.max(0, Math.min(100, Integer.parseInt(value))); break;
                    case "weather":
                        if(value.equalsIgnoreCase("rain")) weather = Weather.RAIN;
                        else if(value.equalsIgnoreCase("storm")) weather = Weather.STORM;
                        else weather = Weather.NONE;
                        break;
                    case "snow": snow = Boolean.parseBoolean(value); break;
                    case "temperature": temperature = Float.parseFloat(value); break;
                    case "dimensions":
                        for(String dimension : value.split(";")) {
                            if(!dimension.trim().isEmpty()) dimensions.add(Integer.parseInt(dimension.trim()));
                        }
                        break;
                    case "ramp":
                        for(String step : value.split(";")) {
                            if(!step.trim().isEmpty()) ramp.add(Math.max(0.0D, Double.parseDouble(step.trim())));
                        }
                        break;
                    case "specialMobs":
                        for(String mob : value.split(";")) {
                            if(!mob.trim().isEmpty()) specialMobs.add(mob.trim());
                        }
                        break;
                    default:
                        //spawns.<dimension>=...: that dimension's own list
                        if(keyValue[0].trim().startsWith("spawns.")) {
                            dimensionSpawns.put(Integer.parseInt(keyValue[0].trim().substring("spawns.".length())), SpawnList.parse(value));
                        }
                        break;
                }
            }
        }
        catch(NumberFormatException e) {
            WakingNights.LOGGER.warn("Invalid night type: {}", entry);
            return null;
        }
        if(dimensions.isEmpty()) dimensions.add(0);
        if(id == null || id.isEmpty()) {
            WakingNights.LOGGER.warn("Night type without an id: {}", entry);
            return null;
        }
        return new NightType(id, weight, sky, fog, clouds, lightning, light, moon, lightningPerMinute, thunderPitch, hook, special, mixedSpecial, despawn, hitEffects, startSound, spawns, dimensionSpawns, spawnInterval, spawnGroup, spawnCap, spawnInSightAfter, spawnCloseAtHalf, spawnSpecialChance, weather, snow, temperature, specialMobs, ramp, dimensions);
    }

    /** "5" or "3-8": min and max, at least 0 and max at least min */
    private static int[] parseRange(String value) {
        String[] parts = value.split("-", 2);
        int min = Math.max(0, Integer.parseInt(parts[0].trim()));
        int max = parts.length > 1 ? Math.max(min, Integer.parseInt(parts[1].trim())) : min;
        return new int[]{min, max};
    }

    /** Hex RGB, with or without a leading # */
    private static int parseColor(String value) {
        return Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16) & 0xFFFFFF;
    }
}
