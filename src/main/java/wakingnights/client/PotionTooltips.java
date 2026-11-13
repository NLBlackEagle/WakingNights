package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.level.Restless;
import wakingnights.potion.PotionLiquid;

import java.util.List;

/**
 * Tooltips: the configured ones on the Waking potions (drinkable, splash, lingering and tipped arrows), what Liquid
 * Dreams clears on a line below it, and the Restless level on items listed in Restless Items
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class PotionTooltips {
    private static final int MAX_WIDTH = 200;
    private static final String[] NUMERALS = {"I", "II", "III"};

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        List<String> lines = event.getToolTip();
        PotionType type = PotionUtils.getPotionFromItem(event.getItemStack());
        String tooltip = type == PotionLiquid.DREAMS_TYPE ? ForgeConfigHandler.server.liquidDreamsTooltip
                : type == PotionLiquid.NIGHTMARES_TYPE ? ForgeConfigHandler.server.liquidNightmaresTooltip
                : null;
        if(tooltip != null && !tooltip.trim().isEmpty()) {
            //& colour codes, like &6 for gold; wrapped lines keep their colour
            String formatted = tooltip.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
            addWrapped(lines, formatted);
        }
        if(type == PotionLiquid.DREAMS_TYPE) addWrapped(lines, TextFormatting.BLUE + I18n.format("wakingnights.tooltip.liquid_dreams_clears"));

        int restless = Restless.getItemLevel(event.getItemStack());
        if(restless > 0) {
            addWrapped(lines, TextFormatting.RED + I18n.format("wakingnights.tooltip.restless_item", NUMERALS[Math.min(restless, NUMERALS.length) - 1]));
        }
    }

    private static void addWrapped(List<String> lines, String text) {
        lines.addAll(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(text, MAX_WIDTH));
    }
}
