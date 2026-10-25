package com.gtcoilemi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Viewer-independent half of the mod: decides what a recipe needs (coil and/or
 * research data item) and where the extra info should sit on the recipe card.
 * The EMI decorator and the JEI decorator both draw from this, so the two
 * integrations cannot drift apart.
 */
public final class CoilInfo {

    public static final int SLOT_SIZE = 18;

    /**
     * Pixels of extra card height CoilRecipeSizeMixin adds below GTCEU's own
     * Duration/Total/Usage text. Keep in sync with the constant of the same
     * name in that Mixin class.
     */
    public static final int EXTRA_HEIGHT = 20;

    private static final Set<String> REPORTED = new HashSet<>();
    private static final Set<String> REPORTED_RESEARCH = new HashSet<>();

    private static final java.util.Set<Object> SLOTTED =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    public static void markSlotted(Object recipe) {
        synchronized (SLOTTED) {
            SLOTTED.add(recipe);
        }
    }

    public static boolean isSlotted(Object recipe) {
        synchronized (SLOTTED) {
            return SLOTTED.contains(recipe);
        }
    }

    private CoilInfo() {
    }

    public record Layout(int textX, int textY1, int textY2, int iconX, int iconY, int dataX, int arrowX, int arrowY) {
    }

    /** What a recipe needs. Either part may be absent. */
    public record Result(ItemStack research, ItemStack target, CoilIndex.Coil coil, String mode, int required) {
        public boolean hasTarget() {
            return target != null && !target.isEmpty();
        }

        public boolean hasCoil() {
            return coil != null;
        }

        public boolean hasResearch() {
            return research != null && !research.isEmpty();
        }
    }

    public static final int JEI_EXTRA_HEIGHT = 42;
    public static final int RIGHT_MARGIN = 12;

    private static boolean jei() {
        return !net.minecraftforge.fml.ModList.get().isLoaded("emi");
    }

    public static Layout layout(CoilConfig config, int displayWidth, int displayHeight) {
        boolean jei = jei();
        int padTop = displayHeight - (jei ? JEI_EXTRA_HEIGHT : EXTRA_HEIGHT);
        int textX = (jei ? 7 : 4) + config.textX;
        int textY1 = padTop + (jei ? 18 : 2) + config.textY;
        int textY2 = textY1 + 9;
        int iconX = displayWidth > 0
                ? displayWidth - SLOT_SIZE - RIGHT_MARGIN + config.offsetX
                : textX + 110 + config.offsetX;
        int iconY = padTop + (EXTRA_HEIGHT - SLOT_SIZE) / 2 + config.offsetY;
        int dataX = iconX - 40;
        return new Layout(textX, textY1, textY2, iconX, iconY, dataX, dataX + 20, iconY + 4);
    }

    /**
     * @param category the viewer's recipe category id (same id in EMI and JEI for GregTech recipe types)
     * @param recipe   the underlying game recipe, may be null if the viewer did not expose it
     * @return null when nothing should be drawn at all
     */
    public static Result resolve(CoilConfig config, ResourceLocation category, Object recipe) {
        if (!config.enabled) {
            return null;
        }

        ItemStack research = ItemStack.EMPTY;
        ItemStack target = ItemStack.EMPTY;
        if (config.showResearchIcon && !config.researchExcludedCategories.contains(category.toString())) {
            ItemStack dataItem = ResearchLookup.dataItemOf(recipe);
            if (dataItem != null && !dataItem.isEmpty()) {
                research = dataItem;
                ItemStack found = ResearchLookup.targetItemOf(recipe);
                if (found != null) {
                    target = found;
                }
                if (config.logMatches && REPORTED_RESEARCH.add(category.toString())) {
                    GTCoilEmi.LOGGER.info("[{}] {} -> drawing research requirement ({})", GTCoilEmi.MOD_ID, category,
                            dataItem.getItem());
                }
            }
        }

        CoilIndex.Coil coil = null;
        String mode = "temperature";
        int required = 0;

        coilPart:
        {
            if (config.excludedCategories.contains(category.toString())) {
                report(config, category, "skipped: GTCEU already shows coil info natively here");
                break coilPart;
            }

            CoilConfig.Entry entry = config.categories.get(category.toString());
            if (entry == null && config.strictNamespaces && !config.namespaces.contains(category.getNamespace())) {
                report(config, category, "skipped: namespace not allowed");
                break coilPart;
            }

            CompoundTag data = recipe == null ? null : RecipeData.tagOf(recipe);
            if (data == null) {
                report(config, category, "no recipe data found");
                break coilPart;
            }
            if (data.isEmpty()) {
                report(config, category, "recipe data tag is empty");
                break coilPart;
            }

            String key = entry != null ? entry.dataKey : autoDetectKey(config, data);
            mode = entry != null ? entry.mode : "temperature";
            if (key == null || !data.contains(key)) {
                report(config, category, "no known coil key, tag holds " + data.getAllKeys());
                break coilPart;
            }

            required = data.getInt(key);
            if (required <= 0) {
                report(config, category, key + " is " + required + ", nothing to show");
                break coilPart;
            }

            coil = switch (mode) {
                case "level" -> CoilIndex.forLevel(required);
                case "tier" -> CoilIndex.forTier(required);
                default -> CoilIndex.forTemperature(required);
            };
            if (coil == null) {
                report(config, category, "no coil reaches " + key + "=" + required);
                break coilPart;
            }

            report(config, category, "drawing " + coil.name().getString() + " for " + key + "=" + required);
        }

        if (coil == null && research.isEmpty()) {
            return null;
        }
        return new Result(research, target, coil, mode, required);
    }

    private static String autoDetectKey(CoilConfig config, CompoundTag data) {
        if (!config.autoDetect) {
            return null;
        }
        for (String candidate : config.autoDetectKeys) {
            if (data.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static void report(CoilConfig config, ResourceLocation category, String message) {
        if (!config.logMatches || !REPORTED.add(category.toString())) {
            return;
        }
        GTCoilEmi.LOGGER.info("[{}] {} -> {}", GTCoilEmi.MOD_ID, category, message);
    }
}
