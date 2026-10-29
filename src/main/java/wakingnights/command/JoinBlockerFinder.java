package wakingnights.command;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug (/wakingnights whoblocks): which mod's handler refuses a mob entering the world where the player stands.
 * Builds the mob without spawning it and hands it to every EntityJoinWorldEvent handler on its own, so the handler
 * that cancels it stands out. The mob never enters the world.
 */
final class JoinBlockerFinder {
    private JoinBlockerFinder() {}

    static List<String> find(World world, ResourceLocation id, double x, double y, double z) throws ReflectiveOperationException {
        List<String> report = new ArrayList<>();
        Entity entity = EntityList.createEntityByIDFromName(id, world);
        if(entity == null) {
            report.add(id + " doesn't exist");
            return report;
        }
        entity.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        //The listeners of Forge's main event bus: they're kept per bus, under its id
        Field busId = EventBus.class.getDeclaredField("busID");
        busId.setAccessible(true);
        EntityJoinWorldEvent probe = new EntityJoinWorldEvent(entity, world);
        IEventListener[] listeners = probe.getListenerList().getListeners(busId.getInt(MinecraftForge.EVENT_BUS));
        for(IEventListener listener : listeners) {
            //Priority markers between the handlers
            if(listener instanceof net.minecraftforge.fml.common.eventhandler.EventPriority) continue;
            EntityJoinWorldEvent event = new EntityJoinWorldEvent(entity, world);
            try {
                listener.invoke(event);
            }
            catch(RuntimeException e) {
                report.add("Error in " + listener + ": " + e);
                continue;
            }
            if(event.isCanceled()) report.add("BLOCKED by " + listener);
        }
        if(report.isEmpty()) {
            report.add("No join handler refuses " + id + " here (" + listeners.length + " checked). If it still can't spawn, "
                    + "something else refuses it (a mixin on spawning, or the chunk isn't loaded).");
        }
        return report;
    }
}
