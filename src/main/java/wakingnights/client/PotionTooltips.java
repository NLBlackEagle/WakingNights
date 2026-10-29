package wakingnights.client;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;
import wakingnights.potion.PotionLiquid;

/** The configured tooltips on the Waking potions (drinkable, splash, lingering and tipped arrows) */
@Mod.EventBusSubscriber(modid = WakingNights.MODID, value = Side.CLIENT)
public class PotionTooltips {
    private static final int MAX_WIDTH = 200;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        PotionType type = PotionUtils.getPotionFromItem(event.getItemStack());
        String tooltip = type == PotionLiquid.DREAMS_TYPE ? ForgeConfigHandler.server.liquidDreamsTooltip
                : type == PotionLiquid.NIGHTMARES_TYPE ? ForgeConfigHandler.server.liquidNightmaresTooltip
                : null;
        if(tooltip == null || tooltip.trim().isEmpty()) return;
        //& colour codes, like &6 for gold; wrapped lines keep their colour
        String formatted = tooltip.replaceAll("&([0-9a-fk-orA-FK-OR])", "\u00a7$1");
        event.getToolTip().addAll(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(formatted, MAX_WIDTH));
    }
}
