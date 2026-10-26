package wakingnights.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import wakingnights.capability.CapabilitySpecialEntityHandler;
import wakingnights.capability.SpecialType;
import wakingnights.event.NightType;
import wakingnights.event.SpecialSpawner;
import wakingnights.level.ActivityTracker;
import wakingnights.level.NightTracker;
import wakingnights.level.WakingLevel;
import wakingnights.level.WakingNightsWorldData;
import wakingnights.network.MessageApparition;
import wakingnights.network.PacketHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Debug command:
 * /wakingnights info [player]
 * /wakingnights set <player> <nights>
 * /wakingnights grace <player> <true|false>
 * /wakingnights skip            - runs the skipped-night check for everyone online
 * /wakingnights awake           - runs the stayed-awake check for the current night
 * /wakingnights start [type] | end - starts (a random or the given night type) or ends a Waking Night event
 * /wakingnights status         - the Waking Night going on, and what happened to the hostile spawns during it
 * /wakingnights apparition [player] - shows a Waking Night II apparition to the player right away
 * /wakingnights special [type]  - makes the mob you're looking at blooded, blighted or none (cycles without a type)
 */
public class CommandWakingNights extends CommandBase {

    private static final String USAGE = "/wakingnights <info|set|grace|skip|awake|start|end|special|apparition|status>";

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
            case "awake":
                NightTracker.onNightPassed(players, overworld.getWorldTime() / 24000);
                sender.sendMessage(new TextComponentString("Ran the stayed-awake check for " + players.size() + " player(s)"));
                break;
            case "start": {
                WakingNightsWorldData data = WakingNightsWorldData.get(overworld);
                if(args.length > 1) {
                    NightType type = NightType.get(args[1]);
                    if(type == null) throw new CommandException("Unknown night type: " + args[1]);
                    NightTracker.startWakingNight(data, players, type);
                }
                else NightTracker.startWakingNight(data, players);
                NightTracker.announceWakingNight(data, players);
                break;
            }
            case "end":
                NightTracker.endWakingNight(WakingNightsWorldData.get(overworld), players, overworld.getWorldTime() / 24000);
                break;
            case "status": {
                WakingNightsWorldData data = WakingNightsWorldData.get(overworld);
                long timeOfDay = overworld.getWorldTime() % 24000;
                sender.sendMessage(new TextComponentString(String.format("Waking Night: %s, time of day %d (%s)",
                        data.isEventActive() ? data.getCurrentType() : "none", timeOfDay,
                        NightTracker.isAfterNightfall(timeOfDay) ? "after nightfall" : "before nightfall or day")));
                sender.sendMessage(new TextComponentString("Hostile spawns this Waking Night: " + SpecialSpawner.getOutcomes()));
                break;
            }
            case "apparition": {
                EntityPlayerMP player = args.length > 1 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
                PacketHandler.INSTANCE.sendTo(new MessageApparition(), player);
                break;
            }
            case "special": {
                EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                EntityLiving target = getLookedAtEntity(player, 32);
                if(target == null) throw new CommandException("Not looking at a mob");
                SpecialType type;
                if(args.length > 1) {
                    try {
                        type = SpecialType.valueOf(args[1].toUpperCase(Locale.ROOT));
                    }
                    catch(IllegalArgumentException e) {
                        throw new WrongUsageException("/wakingnights special [blooded|blighted|none]");
                    }
                }
                else {
                    SpecialType[] types = SpecialType.values();
                    type = types[(CapabilitySpecialEntityHandler.getType(target).ordinal() + 1) % types.length];
                }
                CapabilitySpecialEntityHandler.setType(target, type);
                if(type != SpecialType.NONE) SpecialSpawner.applyBoosts(target);
                sender.sendMessage(new TextComponentString(target.getName() + " is now " + type.name().toLowerCase(Locale.ROOT)));
                break;
            }
            default:
                throw new WrongUsageException(USAGE);
        }
    }

    @Nullable
    private static EntityLiving getLookedAtEntity(EntityPlayerMP player, double range) {
        Vec3d eyes = player.getPositionEyes(1.0F);
        Vec3d look = player.getLookVec();
        Vec3d end = eyes.add(look.scale(range));
        AxisAlignedBB area = player.getEntityBoundingBox().expand(look.x * range, look.y * range, look.z * range).grow(1.0D);

        EntityLiving closest = null;
        double closestDistance = range * range;
        for(Entity entity : player.world.getEntitiesWithinAABBExcludingEntity(player, area)) {
            if(!(entity instanceof EntityLiving)) continue;
            RayTraceResult hit = entity.getEntityBoundingBox().grow(0.3D).calculateIntercept(eyes, end);
            if(hit == null) continue;
            double distance = eyes.squareDistanceTo(hit.hitVec);
            if(distance < closestDistance) {
                closest = (EntityLiving) entity;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static void sendInfo(ICommandSender sender, EntityPlayerMP player) {
        int[] thresholds = WakingLevel.getThresholds(player);
        long night = DimensionManager.getWorld(0).getWorldTime() / 24000;
        sender.sendMessage(new TextComponentString(String.format(
                "%s: Waking Night %d, %d night(s), thresholds %d/%d/%d%s, comfortable: %s, active: %s, awake this night: %ds",
                player.getName(), WakingLevel.getLevel(player), WakingLevel.getCounter(player),
                thresholds[0], thresholds[1], thresholds[2], WakingLevel.isInGrace(player) ? " (grace)" : "",
                WakingLevel.isComfortable(player), ActivityTracker.isActive(player), WakingLevel.getAwakeTicks(player, night) / 20)));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1) return getListOfStringsMatchingLastWord(args, "info", "set", "grace", "skip", "awake", "start", "end", "special", "apparition", "status");
        if(args.length == 2 && (args[0].equals("info") || args[0].equals("set") || args[0].equals("grace"))) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if(args.length == 3 && args[0].equals("grace")) return getListOfStringsMatchingLastWord(args, "true", "false");
        if(args.length == 2 && args[0].equals("special")) return getListOfStringsMatchingLastWord(args, "blooded", "blighted", "none");
        if(args.length == 2 && args[0].equals("start")) {
            List<String> ids = new ArrayList<>();
            for(NightType type : NightType.getAll()) ids.add(type.id);
            return getListOfStringsMatchingLastWord(args, ids);
        }
        return Collections.emptyList();
    }
}
