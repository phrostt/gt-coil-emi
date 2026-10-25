package com.gtcoilemi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Reads every Blood Magic ritual (name, crystal tier, activation and upkeep cost, rune layout)
 * from Blood Magic's own ritual manager by reflection, so the mod needs no Blood Magic
 * dependency to compile. Rituals are code, not recipes, so neither JEI nor EMI shows them.
 */
public final class RitualData {

    private RitualData() {
    }

    /** One rune block of a ritual; offsets are relative to the master ritual stone. */
    public record Rune(int x, int y, int z, String type) {
    }

    public record Ritual(String id, String nameKey, int crystal, int activation, int refreshCost, int refreshTime,
                         List<Rune> runes, List<String> ranges) {
    }

    public static List<Ritual> load() {
        List<Ritual> out = new ArrayList<>();
        try {
            Class<?> bm = Class.forName("wayoftime.bloodmagic.BloodMagic");
            Class<?> ritualClass = Class.forName("wayoftime.bloodmagic.ritual.Ritual");
            Object manager = bm.getField("RITUAL_MANAGER").get(null);
            Method getId = manager.getClass().getMethod("getId", ritualClass);
            Collection<?> rituals = (Collection<?>) manager.getClass().getMethod("getSortedRituals").invoke(manager);
            for (Object ritual : rituals) {
                try {
                    out.add(read(ritualClass, manager, getId, ritual));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    GTCoilEmi.LOGGER.warn("[{}] could not read a Blood Magic ritual: {}", GTCoilEmi.MOD_ID, e.toString());
                }
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            GTCoilEmi.LOGGER.info("[{}] Blood Magic ritual manager not available, no ritual tab: {}", GTCoilEmi.MOD_ID, e.toString());
        }
        GTCoilEmi.LOGGER.info("[{}] found {} Blood Magic rituals", GTCoilEmi.MOD_ID, out.size());
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Ritual read(Class<?> ritualClass, Object manager, Method getId, Object ritual)
            throws ReflectiveOperationException {
        String id = String.valueOf(getId.invoke(manager, ritual));
        String key = (String) ritualClass.getMethod("getTranslationKey").invoke(ritual);
        int crystal = ((Number) ritualClass.getMethod("getCrystalLevel").invoke(ritual)).intValue();
        int activation = ((Number) ritualClass.getMethod("getActivationCost").invoke(ritual)).intValue();
        int refreshCost = 0;
        int refreshTime = 0;
        try {
            refreshCost = ((Number) ritualClass.getMethod("getRefreshCost").invoke(ritual)).intValue();
            refreshTime = ((Number) ritualClass.getMethod("getRefreshTime").invoke(ritual)).intValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // some rituals compute these from live state; leave them at zero
        }
        List<String> ranges = (List<String>) ritualClass.getMethod("getListOfRanges").invoke(ritual);

        List<Rune> runes = new ArrayList<>();
        Class<?> componentClass = Class.forName("wayoftime.bloodmagic.ritual.RitualComponent");
        Method getOffset = componentClass.getMethod("getOffset");
        Method getType = componentClass.getMethod("getRuneType");
        Consumer<Object> sink = component -> {
            try {
                net.minecraft.core.BlockPos pos = (net.minecraft.core.BlockPos) getOffset.invoke(component);
                String type = String.valueOf(getType.invoke(component)).toLowerCase(Locale.ROOT);
                runes.add(new Rune(pos.getX(), pos.getY(), pos.getZ(), type));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        };
        ritualClass.getMethod("gatherComponents", Consumer.class).invoke(ritual, sink);
        return new Ritual(id, key, crystal, activation, refreshCost, refreshTime, runes, new ArrayList<>(ranges));
    }
}
