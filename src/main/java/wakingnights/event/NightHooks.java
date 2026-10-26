package wakingnights.event;

import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The hooks night types can name, each only there when its mod is loaded.
 */
public final class NightHooks {
    private static final Map<String, NightHook> HOOKS = new HashMap<>();

    private NightHooks() {}

    public static void init() {
        register("bloodmoon", "bloodmoon", BloodmoonHook::new);
    }

    private static void register(String id, String modId, Supplier<NightHook> hook) {
        if(Loader.isModLoaded(modId)) HOOKS.put(id, hook.get());
    }

    /** null if there's no such hook, or its mod isn't loaded */
    @Nullable
    public static NightHook get(@Nullable String id) {
        return id == null ? null : HOOKS.get(id);
    }
}
