package wakingnights.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import wakingnights.level.NightTracker;
import wakingnights.level.WakingLevel;
import wakingnights.level.WakingNightsWorldData;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Debug command:
 * /wakingnights info [player]
 * /wakingnights set <player> <nights>
 * /wakingnights grace <player> <true|false>
 * /wakingnights skip            - runs the skipped-night check for everyone online
 * /wakingnights brave           - runs the Brave Night check for the current night
 * /wakingnights start | end     - starts or ends a Waking Night event
 */
public class CommandWakingNights extends CommandBase {

    private static final String USAGE = "/wakingnights <info|set|grace|skip|brave|start|end>";

    @Override
    public String getName() {
        return "wakingnights";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if(args.length == 0) throw new WrongUsageException(USAGE);
        World overworld = DimensionManager.getWorld(0);
        List<EntityPlayerMP> players = server.getPlayerList().getPlayers();

        switch(args[0]) {
            case "info": {
                EntityPlayerMP player = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
                sendInfo(sender, player);
                break;
            }
            case "set": {
                if(args.length < 3) throw new WrongUsageException("/wakingnights set <player> <nights>");
                EntityPlayerMP player = getPlayer(server, sender, args[1]);
                WakingLevel.setCounter(player, parseInt(args[2], 0));
                sendInfo(sender, player);
                break;
            }
            case "grace": {
                if(args.length < 3) throw new WrongUsageException("/wakingnights grace <player> <true|false>");
                EntityPlayerMP player = getPlayer(server, sender, args[1]);
                WakingLevel.setInGrace(player, parseBoolean(args[2]));
                sendInfo(sender, player);
                break;
            }
            case "skip":
                NightTracker.onNightSkipped(players);
                sender.sendMessage(new TextComponentString("Ran the skipped-night check for " + players.size() + " player(s)"));
                break;
            case "brave":
                NightTracker.onNightPassed(players, overworld.getWorldTime() / 24000);
                sender.sendMessage(new TextComponentString("Ran the Brave Night check for " + players.size() + " player(s)"));
                break;
            case "start":
                NightTracker.startWakingNight(WakingNightsWorldData.get(overworld), players);
                break;
            case "end":
                NightTracker.endWakingNight(WakingNightsWorldData.get(overworld), players);
                break;
            default:
                throw new WrongUsageException(USAGE);
        }
    }

    private static void sendInfo(ICommandSender sender, EntityPlayerMP player) {
        int[] thresholds = WakingLevel.getThresholds(player);
        long night = DimensionManager.getWorld(0).getWorldTime() / 24000;
        sender.sendMessage(new TextComponentString(String.format(
                "%s: Waking Night %d, %d night(s), thresholds %d/%d/%d%s, comfortable: %s, awake this night: %ds",
                player.getName(), WakingLevel.getLevel(player), WakingLevel.getCounter(player),
                thresholds[0], thresholds[1], thresholds[2], WakingLevel.isInGrace(player) ? " (grace)" : "",
                WakingLevel.isComfortable(player), WakingLevel.getAwakeTicks(player, night) / 20)));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1) return getListOfStringsMatchingLastWord(args, "info", "set", "grace", "skip", "brave", "start", "end");
        if(args.length == 2 && (args[0].equals("info") || args[0].equals("set") || args[0].equals("grace"))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if(args.length == 3 && args[0].equals("grace")) return getListOfStringsMatchingLastWord(args, "true", "false");
        return Collections.emptyList();
    }
}
