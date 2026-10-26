package wakingnights.level;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import wakingnights.WakingNights;
import wakingnights.handlers.ForgeConfigHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Whether a player is actually playing or AFK. Vanilla only counts clicks, chat, inventory and the like as activity,
 * so looking around and walking (on their own, not drifting in water or riding) are added here.
 * Only active time counts towards staying awake through a night or surviving a Waking Night.
 */
@Mod.EventBusSubscriber(modid = WakingNights.MODID)
public final class ActivityTracker {
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final float LOOK_THRESHOLD = 1.0F;
    private static final double WALK_THRESHOLD_SQ = 0.25D;

    private ActivityTracker() {}

    public static boolean isActive(EntityPlayerMP player) {
        State state = STATES.get(player.getUniqueID());
        long lastActive = Math.max(player.getLastActiveTime(), state == null ? 0 : state.lastActive);
        return MinecraftServer.getCurrentTimeMillis() - lastActive < ForgeConfigHandler.server.afkMinutes * 60000L;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerMP) || event.player.ticksExisted % 20 != 0) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        long now = MinecraftServer.getCurrentTimeMillis();
        State state = STATES.computeIfAbsent(player.getUniqueID(), id -> new State(player, now));

        boolean looked = Math.abs(player.rotationYaw - state.yaw) > LOOK_THRESHOLD || Math.abs(player.rotationPitch - state.pitch) > LOOK_THRESHOLD;
        double dx = player.posX - state.x, dz = player.posZ - state.z;
        boolean walked = !player.isRiding() && !player.isInWater() && dx * dx + dz * dz > WALK_THRESHOLD_SQ;
        if(looked || walked) state.lastActive = now;
        state.update(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.player.getUniqueID());
    }

    private static class State {
        private double x, z;
        private float yaw, pitch;
        private long lastActive;

        State(EntityPlayerMP player, long now) {
            this.update(player);
            this.lastActive = now;
        }

        void update(EntityPlayerMP player) {
            this.x = player.posX;
            this.z = player.posZ;
            this.yaw = player.rotationYaw;
            this.pitch = player.rotationPitch;
        }
    }
}
