package wakingnights.potion;

import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.oredict.OreDictionary;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;

/**
 * Brewing an Awkward Potion with a configured ingredient into one of the Waking potions, for the drinkable, splash
 * and lingering bottle each. Forge's BrewingRecipe, so JEI shows it; it only matches the item of a bottle though, so
 * this also checks it really is an Awkward Potion.
 */
public class LiquidBrewingRecipe extends BrewingRecipe {
    private static final Item[] BOTTLES = {Items.POTIONITEM, Items.SPLASH_POTION, Items.LINGERING_POTION};

    private LiquidBrewingRecipe(ItemStack input, ItemStack ingredient, ItemStack output) {
        super(input, ingredient, output);
    }

    /** Called once all items exist (post init) */
    public static void registerAll() {
        register(ForgeConfigHandler.server.liquidDreamsIngredient, PotionLiquid.DREAMS_TYPE);
        register(ForgeConfigHandler.server.liquidNightmaresIngredient, PotionLiquid.NIGHTMARES_TYPE);
    }

    /** Ingredient as <mod>:<item> or <mod>:<item>:<metadata>, empty for no recipe */
    private static void register(String ingredient, PotionType output) {
        if(ingredient == null || ingredient.trim().isEmpty()) return;
        String id = ingredient.trim();
        int meta = OreDictionary.WILDCARD_VALUE;
        String[] parts = id.split(":");
        if(parts.length == 3) {
            try {
                meta = Integer.parseInt(parts[2]);
                id = parts[0] + ":" + parts[1];
            }
            catch(NumberFormatException e) {
                WakingNights.LOGGER.warn("Invalid brewing ingredient metadata: {}", ingredient);
                return;
            }
        }
        Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
        if(item == null) {
            WakingNights.LOGGER.warn("Brewing ingredient {} for {} doesn't exist, no recipe", ingredient, output.getRegistryName());
            return;
        }
        ItemStack ingredientStack = new ItemStack(item, 1, meta);
        for(Item bottle : BOTTLES) {
            BrewingRecipeRegistry.addRecipe(new LiquidBrewingRecipe(PotionUtils.addPotionToItemStack(new ItemStack(bottle), PotionTypes.AWKWARD),
                    ingredientStack, PotionUtils.addPotionToItemStack(new ItemStack(bottle), output)));
        }
        WakingNights.LOGGER.info("Brewing recipe: Awkward Potion + {} = {}", ingredient, output.getRegistryName());
    }

    @Override
    public boolean isInput(ItemStack stack) {
        return super.isInput(stack) && PotionUtils.getPotionFromItem(stack) == PotionTypes.AWKWARD;
    }
}
