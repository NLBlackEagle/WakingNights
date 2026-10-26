package wakingnights.event;

import wakingnights.WakingNights;
import wakingnights.capability.SpecialType;
import wakingnights.handlers.ForgeConfigHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A kind of Waking Night (Bloodmoon, Lycanites, ...), read from the "Night Types" config.
 * Entries look like: id=bloodmoon, weight=1, sky=000000, fog=000000, clouds=000000, lightning=000000, light=FFFFFF, moon=FFFFFF, lightningPerMinute=6, thunderPitch=0.6, hook=bloodmoon,
 * special=blooded (blooded, blighted, mixed or none: what mobs that turn special during this night become), despawn=true,
 * hitEffects=srparasites:bleed 0 100;minecraft:slowness 0 60 (optional, overrides the Special Mob Hit Effects config),
 * startSound=wakingnights:event.bloodmoon_start (optional, played when the night truly starts),
 * spawns=srparasites:sim_human 10;srparasites:sim_cow 6, spawnsPerMinute=12, spawnCap=12 (optional, its own spawns)
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
    /** This night's own spawns (EventSpawner): entity ids with their weights, empty for none */
    public final List<String> spawns;
    public final List<Double> spawnWeights;
    /** Average spawn attempts per minute around each player, and the most of them alive per player */
    public final double spawnsPerMinute;
    public final int spawnCap;

    private static String[] parsedConfig;
    private static List<NightType> types = Collections.emptyList();

    private NightType(String id, double weight, int sky, int fog, int clouds, int lightning, int light, int moon, double lightningPerMinute, float thunderPitch,
                      @Nullable String hook, @Nullable SpecialType special, boolean mixedSpecial, boolean despawn, @Nullable List<HitEffect> hitEffects, @Nullable String startSound,
                      List<String> spawns, List<Double> spawnWeights, double spawnsPerMinute, int spawnCap) {
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
        this.spawnWeights = spawnWeights;
        this.spawnsPerMinute = spawnsPerMinute;
        this.spawnCap = spawnCap;
    }

    /** A random entity id from this night's spawns, by weight, null if there are none */
    @Nullable
    public String pickSpawn(Random rand) {
        double total = 0;
        for(double weight : this.spawnWeights) total += weight;
        if(total <= 0) return null;
        double target = rand.nextDouble() * total;
        for(int i = 0; i < this.spawns.size(); i++) {
            target -= this.spawnWeights.get(i);
            if(target < 0) return this.spawns.get(i);
        }
        return this.spawns.get(this.spawns.size() - 1);
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
        List<String> spawns = new ArrayList<>();
        List<Double> spawnWeights = new ArrayList<>();
        double spawnsPerMinute = 0.0D;
        int spawnCap = 10;
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
                    //<entity id> <weight>;<entity id> <weight>;...
                    case "spawns":
                        for(String spawn : value.split(";")) {
                            String[] parts = spawn.trim().split("\\s+");
                            if(parts.length != 2) continue;
                            spawns.add(parts[0]);
                            spawnWeights.add(Math.max(0.0D, Double.parseDouble(parts[1])));
                        }
                        break;
                    case "spawnsPerMinute": spawnsPerMinute = Math.max(0.0D, Double.parseDouble(value)); break;
                    case "spawnCap": spawnCap = Math.max(0, Integer.parseInt(value)); break;
                    default: break;
                }
            }
        }
        catch(NumberFormatException e) {
            WakingNights.LOGGER.warn("Invalid night type: {}", entry);
            return null;
        }
        if(id == null || id.isEmpty()) {
            WakingNights.LOGGER.warn("Night type without an id: {}", entry);
            return null;
        }
        return new NightType(id, weight, sky, fog, clouds, lightning, light, moon, lightningPerMinute, thunderPitch, hook, special, mixedSpecial, despawn, hitEffects, startSound, spawns, spawnWeights, spawnsPerMinute, spawnCap);
    }

    /** Hex RGB, with or without a leading # */
    private static int parseColor(String value) {
        return Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16) & 0xFFFFFF;
    }
}
