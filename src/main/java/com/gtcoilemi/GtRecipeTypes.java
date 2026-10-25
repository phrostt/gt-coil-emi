package com.gtcoilemi;

import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lists GregTech's recipe type ids by reflection, so the JEI plugin can
 * register a decorator per type without compiling against GregTech. JEI needs
 * the full set up front, unlike EMI which hands over each recipe as it draws.
 * Anything missing here can be added by hand with "jeiRecipeTypes" in the config.
 */
final class GtRecipeTypes {

    private GtRecipeTypes() {
    }

    static List<ResourceLocation> discover() {
        List<ResourceLocation> out = new ArrayList<>();
        try {
            Class<?> registries = Class.forName("com.gregtechceu.gtceu.api.registry.GTRegistries");
            Object registry = registries.getField("RECIPE_TYPES").get(null);

            Object keys = invokeIfPresent(registry, "keys");
            if (keys instanceof Iterable<?> iterable) {
                for (Object key : iterable) {
                    if (key instanceof ResourceLocation rl) {
                        out.add(rl);
                    }
                }
            }
            if (out.isEmpty()) {
                Object values = invokeIfPresent(registry, "values");
                if (values instanceof Iterable<?> iterable) {
                    for (Object type : iterable) {
                        Field name = type.getClass().getField("registryName");
                        Object value = name.get(type);
                        if (value instanceof ResourceLocation rl) {
                            out.add(rl);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            GTCoilEmi.LOGGER.warn("[{}] could not list GregTech recipe types ({}), use jeiRecipeTypes in the config",
                    GTCoilEmi.MOD_ID, t.toString());
        }
        return out;
    }

    private static Object invokeIfPresent(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
