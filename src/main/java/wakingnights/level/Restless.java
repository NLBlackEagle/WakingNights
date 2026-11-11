package wakingnights.level;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.potion.PotionLiquid;
import wakingnights.potion.PotionRestless;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Restless: whitelisted biomes and items (Restless Biomes, Restless Items) make a player Restless, at the biome's level
 * plus every equipped item's level, at most III. Shown as the Restless effect, a warning: sleeping while Restless adds
 * that many levels to the Waking Night (Restless II at Waking Night I: Waking Night III). A biome with trigger 2 does
 * the same when a bed is right-clicked there. At most once per night.
 * Liquid Dreams cancels all of it. Comfort (from Minimum Comfort Tier) only cancels the biome's: items always count.
 * Each biome and item tells the player once per play session, above the hotbar.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public final class Restless {
    public static final int TRIGGER_SLEEP = 1;
    public static final int TRIGGER_BED = 2;

    private static final State NONE = new State(0, 0, false);

    //The biomes and items each player got the Restless message for this play session (cleared on logout)
    private static final Map<UUID, Set<String>> WARNED = new HashMap<>();
    //The Restless message each player is being shown right now, sent again until it has been up long enough
    private static final Map<UUID, Showing> SHOWING = new HashMap<>();
    //Last resend at 8 seconds, shown 3 more: about 11 seconds in all (vanilla's 3, times 3 and a bit)
    private static final int WARNING_SECONDS = 9;

    private static String[] parsedBiomes;
    //By <dimension>|<biome>, either one possibly a wildcard
    private static Map<String, BiomeEntry> biomes = new HashMap<>();
    private static String[] parsedItems;
    private static List<ItemEntry> items = new ArrayList<>();

    private static boolean baublesChecked;
    @Nullable
    private static Method baublesHandler;

    private Restless() {}

    /** How Restless a player is right now (biome and items added up, at most III), and whether a bed click counts too */
    public static final class State {
        public final int level;
        public final int biomeLevel;
        public final int itemLevel;
        public final boolean bedTrigger;

        private State(int biomeLevel, int itemLevel, boolean bedTrigger) {
            this.level = Math.min(biomeLevel + itemLevel, WakingLevel.MAX_LEVEL);
            this.biomeLevel = biomeLevel;
            this.itemLevel = itemLevel;
            this.bedTrigger = bedTrigger;
        }
    }

    /** Liquid Dreams keeps all Restless away; comfort only the biome's, items ignore it */
    public static State get(EntityPlayer player) {
        if(player.isPotionActive(PotionLiquid.DREAMS)) return NONE;
        int biomeLevel = 0;
        boolean bedTrigger = false;
        if(!WakingLevel.isComfortable(player)) {
            BiomeEntry biomeEntry = getBiomeEntry(player);
            if(biomeEntry != null) {
                biomeLevel = biomeEntry.level;
                bedTrigger = biomeEntry.trigger == TRIGGER_BED;
            }
        }
        int itemLevel = 0;
        for(ItemEntry item : getItems()) itemLevel += item.level * item.countEquipped(player);
        return biomeLevel + itemLevel <= 0 ? NONE : new State(biomeLevel, itemLevel, bedTrigger);
    }

    /** The night players are in now, by world day (the same number a night is ended with in NightTracker) */
    private static long getNight() {
        return NightTracker.getTime() / 24000;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if(player.ticksExisted % 20 != 0) return;
        if(player.isPlayerSleeping()) WakingLevel.setSlept(player, getNight());
        State state = get(player);
        WakingEffects.showIcon(player, PotionRestless.INSTANCE, state.level);
        warn(player, state);
    }

    /**
     * The first time each play session a biome or item makes the player Restless, they get its message above the
     * hotbar. Only while it actually counts: a biome cancelled by comfort is warned about once it isn't anymore.
     * One message per update, so a new biome and a new item a second apart each get theirs.
     */
    private static void warn(EntityPlayerMP player, State state) {
        if(keepShowing(player) || state.level <= 0) return;
        Set<String> warned = WARNED.computeIfAbsent(player.getUniqueID(), id -> new HashSet<>());
        if(state.biomeLevel > 0) {
            //Once per config entry, so a whole dimension (0,*) warns once rather than for every biome in it
            BiomeEntry biomeEntry = getBiomeEntry(player);
            if(biomeEntry != null && warned.add("biome:" + biomeEntry.key)) {
                sendWarning(player, "wakingnights.message.restless_biome");
                return;
            }
        }
        if(state.itemLevel <= 0) return;
        for(ItemEntry item : getItems()) {
            if(item.countEquipped(player) > 0 && warned.add("item:" + item.item.getRegistryName() + ":" + item.meta)) {
                sendWarning(player, "wakingnights.message.restless_item");
                return;
            }
        }
    }

    private static void sendWarning(EntityPlayerMP player, String key) {
        TextComponentTranslation message = new TextComponentTranslation(key);
        message.setStyle(new Style().setColor(TextFormatting.DARK_PURPLE).setItalic(true));
        //Once in chat (if enabled), so it can be read again; the resends only go above the hotbar
        Messages.send(player, message);
        if(Messages.actionBar()) SHOWING.put(player.getUniqueID(), new Showing(message));
    }

    /**
     * The action bar only shows a message for 3 seconds, so a warning is sent again every 2 seconds until it has been
     * up for about 10. Returns whether one is still showing, so the next warning waits its turn instead of cutting it off.
     */
    private static boolean keepShowing(EntityPlayerMP player) {
        Showing showing = SHOWING.get(player.getUniqueID());
        if(showing == null) return false;
        showing.seconds++;
        if(showing.seconds >= WARNING_SECONDS) {
            SHOWING.remove(player.getUniqueID());
            return false;
        }
        if(showing.seconds % 2 == 0) player.sendStatusMessage(showing.message, true);
        return true;
    }

    private static final class Showing {
        final TextComponentTranslation message;
        int seconds;

        Showing(TextComponentTranslation message) {
            this.message = message;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        WARNED.remove(event.player.getUniqueID());
        SHOWING.remove(event.player.getUniqueID());
    }

    /** Waking up from a skipped night: having slept while Restless adds its levels */
    public static void onNightSkipped(EntityPlayerMP player, long night) {
        if(!WakingLevel.hasSlept(player, night)) return;
        State state = get(player);
        if(state.level > 0) raise(player, state.level, night);
    }

    /** Trigger 2: any bed right-clicked in that biome. Before the bed reacts, so Waking Night III keeps them out of it. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        if(world.isRemote || !(player instanceof EntityPlayerMP)) return;
        IBlockState blockState = world.getBlockState(event.getPos());
        if(!blockState.getBlock().isBed(blockState, world, event.getPos(), player)) return;
        State state = get(player);
        if(state.level > 0 && state.bedTrigger) raise((EntityPlayerMP) player, state.level, getNight());
    }

    private static void raise(EntityPlayerMP player, int levels, long night) {
        if(WakingLevel.isRestlessDone(player, night)) return;
        WakingLevel.setRestlessDone(player, night);
        int before = WakingLevel.getLevel(player);
        WakingLevel.addLevels(player, levels);
        int after = WakingLevel.getLevel(player);
        if(after > before) NightTracker.tellReached(player, after);
    }

    // ------ Config ------

    private static final class BiomeEntry {
        //<dimension>|<biome> as configured, wildcards included
        final String key;
        final int level;
        final int trigger;

        BiomeEntry(String key, int level, int trigger) {
            this.key = key;
            this.level = level;
            this.trigger = trigger;
        }
    }

    private static final String ANY = "*";

    /**
     * The entry for the player's dimension and biome, the most specific one first: exact dimension before *, and
     * within that the exact biome, then <mod>:*, then *.
     */
    @Nullable
    private static BiomeEntry getBiomeEntry(EntityPlayer player) {
        ResourceLocation biome = player.world.getBiome(new BlockPos(player)).getRegistryName();
        if(biome == null) return null;
        Map<String, BiomeEntry> entries = getBiomes();
        if(entries.isEmpty()) return null;
        String[] dimensions = {String.valueOf(player.dimension), ANY};
        String[] biomeKeys = {biome.toString(), biome.getNamespace() + ":" + ANY, ANY};
        for(String dimension : dimensions) {
            for(String biomeKey : biomeKeys) {
                BiomeEntry entry = entries.get(dimension + "|" + biomeKey);
                if(entry != null) return entry;
            }
        }
        return null;
    }

    private enum Slot { ARMOR, HAND, BAUBLE }

    private static final class ItemEntry {
        final Item item;
        //-1: any metadata
        final int meta;
        final int level;
        final Slot slot;

        ItemEntry(Item item, int meta, int level, Slot slot) {
            this.item = item;
            this.meta = meta;
            this.level = level;
            this.slot = slot;
        }

        boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == item && (meta < 0 || stack.getMetadata() == meta);
        }

        /** Every equipped one adds its level */
        int countEquipped(EntityPlayer player) {
            int count = 0;
            switch(slot) {
                case ARMOR:
                    for(ItemStack stack : player.inventory.armorInventory) if(matches(stack)) count++;
                    break;
                case HAND:
                    if(matches(player.getHeldItemMainhand())) count++;
                    if(matches(player.getHeldItemOffhand())) count++;
                    break;
                case BAUBLE:
                    IItemHandler baubles = getBaubles(player);
                    if(baubles == null) break;
                    for(int i = 0; i < baubles.getSlots(); i++) if(matches(baubles.getStackInSlot(i))) count++;
                    break;
            }
            return count;
        }
    }

    /** Parsed again whenever the config changed (syncing it sets new arrays) */
    private static Map<String, BiomeEntry> getBiomes() {
        String[] config = ForgeConfigHandler.server.restlessBiomes;
        if(config == parsedBiomes) return biomes;
        parsedBiomes = config;
        biomes = new HashMap<>();
        if(config == null) return biomes;
        for(String line : config) {
            String[] parts = line.split(",");
            try {
                if(parts.length != 4) throw new IllegalArgumentException("expected <dimension id>,<biome>,<level>,<trigger>");
                String dimension = parts[0].trim();
                if(!dimension.equals(ANY)) dimension = String.valueOf(Integer.parseInt(dimension));
                String biome = parts[1].trim();
                if(!biome.equals(ANY)) {
                    //<mod>:<biome> or <mod>:*
                    ResourceLocation id = new ResourceLocation(biome);
                    biome = id.toString();
                    if(!id.getPath().equals(ANY) && !ForgeRegistries.BIOMES.containsKey(id)) WakingNights.LOGGER.warn("Restless biome {} doesn't exist (yet), it's kept in case it's added later", id);
                }
                int level = Integer.parseInt(parts[2].trim());
                int trigger = Integer.parseInt(parts[3].trim());
                if(level < 1 || level > WakingLevel.MAX_LEVEL) throw new IllegalArgumentException("level must be 1 to 3");
                if(trigger != TRIGGER_SLEEP && trigger != TRIGGER_BED) throw new IllegalArgumentException("trigger must be 1 or 2");
                String key = dimension + "|" + biome;
                biomes.put(key, new BiomeEntry(key, level, trigger));
            }
            catch(IllegalArgumentException e) {
                WakingNights.LOGGER.warn("Invalid Restless Biomes entry '{}': {}", line, e.getMessage());
            }
        }
        return biomes;
    }

    private static List<ItemEntry> getItems() {
        String[] config = ForgeConfigHandler.server.restlessItems;
        if(config == parsedItems) return items;
        parsedItems = config;
        items = new ArrayList<>();
        if(config == null) return items;
        for(String line : config) {
            String[] parts = line.split(",");
            try {
                if(parts.length != 3) throw new IllegalArgumentException("expected <item>,<level>,<slot>");
                //<mod>:<item> or <mod>:<item>:<metadata>
                String[] id = parts[0].trim().split(":");
                int meta = -1;
                if(id.length == 3) meta = Integer.parseInt(id[2]);
                else if(id.length != 2) throw new IllegalArgumentException("item must be <mod>:<item> or <mod>:<item>:<metadata>");
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id[0], id[1]));
                if(item == null) {
                    //Items from mods that aren't installed are skipped
                    WakingNights.LOGGER.debug("Restless item {} doesn't exist, skipped", parts[0].trim());
                    continue;
                }
                int level = Integer.parseInt(parts[1].trim());
                if(level < 1 || level > WakingLevel.MAX_LEVEL) throw new IllegalArgumentException("level must be 1 to 3");
                Slot slot = Slot.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
                items.add(new ItemEntry(item, meta, level, slot));
            }
            catch(IllegalArgumentException e) {
                WakingNights.LOGGER.warn("Invalid Restless Items entry '{}': {}", line, e.getMessage());
            }
        }
        return items;
    }

    /** Baubles' slots, through reflection so Baubles stays optional. Null without Baubles. */
    @Nullable
    private static IItemHandler getBaubles(EntityPlayer player) {
        if(!baublesChecked) {
            baublesChecked = true;
            if(Loader.isModLoaded("baubles")) {
                try {
                    baublesHandler = Class.forName("baubles.api.BaublesApi").getMethod("getBaublesHandler", EntityPlayer.class);
                }
                catch(ReflectiveOperationException e) {
                    WakingNights.LOGGER.warn("Couldn't find Baubles' slots, bauble Restless Items won't work", e);
                }
            }
        }
        if(baublesHandler == null) return null;
        try {
            Object handler = baublesHandler.invoke(null, player);
            return handler instanceof IItemHandler ? (IItemHandler) handler : null;
        }
        catch(ReflectiveOperationException e) {
            return null;
        }
    }
}
