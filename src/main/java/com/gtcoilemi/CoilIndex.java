package com.gtcoilemi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;

/**
 * Builds the list of heating coils available in the current pack.
 *
 * Coils are found by walking the block registry and reading the coil type off
 * each candidate block. Nothing is hardcoded, so coils registered from KubeJS
 * are picked up exactly like the built-in ones - a KubeJS coil is an ordinary
 * CoilBlock holding a SimpleCoilType, with the same properties.
 */
public final class CoilIndex {

    /** A single heating coil and the three properties recipes can gate on. */
    public record Coil(ItemStack stack, Component name, int temperature, int level, int tier) {
    }

    private static List<Coil> cache;

    private CoilIndex() {
    }

    public static void reset() {
        cache = null;
    }

    public static List<Coil> all() {
        if (cache == null) {
            cache = discover();
        }
        return cache;
    }

    /** Lowest coil that reaches at least {@code temperature}, or null if none does. */
    public static Coil forTemperature(int temperature) {
        return lowestMatching(temperature, Coil::temperature);
    }

    /** Lowest coil with at least the given multi-smelter level. */
    public static Coil forLevel(int level) {
        return lowestMatching(level, Coil::level);
    }

    /** Lowest coil with at least the given coil tier. */
    public static Coil forTier(int tier) {
        return lowestMatching(tier, Coil::tier);
    }

    private static Coil lowestMatching(int required, ToIntFunction<Coil> property) {
        Coil best = null;
        for (Coil coil : all()) {
            int value = property.applyAsInt(coil);
            if (value < required) {
                continue;
            }
            if (best == null || value < property.applyAsInt(best)) {
                best = coil;
            }
        }
        return best;
    }

    private static List<Coil> discover() {
        List<Coil> found = new ArrayList<>();

        for (var entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();

            if (!looksLikeCoil(id, block)) {
                continue;
            }
            Object coilType = findCoilType(block);
            if (coilType == null) {
                continue;
            }

            int temperature = intProperty(coilType, "getCoilTemperature");
            int level = intProperty(coilType, "getLevel");
            int tier = intProperty(coilType, "getTier");
            if (temperature <= 0 && level <= 0 && tier <= 0) {
                continue;
            }

            ItemStack stack = new ItemStack(block);
            if (stack.isEmpty()) {
                continue;
            }
            found.add(new Coil(stack, stack.getHoverName(), temperature, level, tier));
        }

        found.sort(Comparator.comparingInt(Coil::temperature)
                .thenComparingInt(Coil::tier)
                .thenComparingInt(Coil::level));

        if (found.isEmpty()) {
            GTCoilEmi.LOGGER.warn("[{}] no heating coils found - is GregTech installed?", GTCoilEmi.MOD_ID);
        } else {
            GTCoilEmi.LOGGER.info("[{}] found {} heating coils", GTCoilEmi.MOD_ID, found.size());
            for (Coil coil : found) {
                GTCoilEmi.LOGGER.debug("[{}]   {} -> {} K, level {}, tier {}",
                        GTCoilEmi.MOD_ID, coil.name().getString(), coil.temperature(), coil.level(), coil.tier());
            }
        }
        return found;
    }

    private static boolean looksLikeCoil(ResourceLocation id, Block block) {
        return id.getPath().toLowerCase(Locale.ROOT).contains("coil")
                || block.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("coil");
    }

    /**
     * CoilBlock publishes its properties through a public {@code coilType} field
     * rather than a getter, so fields are checked first. The remaining lookups
     * are fallbacks in case a future GregTech version changes that.
     */
    private static Object findCoilType(Block block) {
        Object named = readField(block, "coilType");
        if (isCoilType(named)) {
            return named;
        }
        for (Class<?> type = block.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = read(block, field);
                if (isCoilType(value)) {
                    return value;
                }
            }
        }
        Object fromGetter = invokeNoArg(block, "getCoilType");
        return isCoilType(fromGetter) ? fromGetter : null;
    }

    /** A coil type is anything that can tell us its temperature. */
    private static boolean isCoilType(Object value) {
        if (value == null) {
            return false;
        }
        try {
            value.getClass().getMethod("getCoilTemperature");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static int intProperty(Object owner, String getter) {
        Object value = invokeNoArg(owner, getter);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Object readField(Object owner, String name) {
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                return read(owner, type.getDeclaredField(name));
            } catch (NoSuchFieldException e) {
                // declared further up the hierarchy, or not at all
            }
        }
        return null;
    }

    private static Object read(Object owner, Field field) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeNoArg(Object owner, String name) {
        try {
            Method method = owner.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(owner);
        } catch (Throwable t) {
            return null;
        }
    }
}
