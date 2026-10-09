package com.gtcoilemi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeDecorator;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws the required coil as an extra slot in the corner of every GregTech
 * recipe whose data carries a coil requirement.
 *
 * Every exit path reports once per recipe category, so a recipe that is not
 * decorated says why in the log instead of failing silently.
 */
public class CoilDecorator implements EmiRecipeDecorator {

    private static final int SLOT_SIZE = 18;

    /**
     * Pixels of extra card height CoilRecipeSizeMixin adds below GTCEU's own
     * Duration/Total/Usage text. Keep this in sync with the constant of the
     * same name in that Mixin class - it is the one source of truth for how
     * much room we actually have to draw into.
     */
    private static final int EXTRA_HEIGHT = 20;

    private final Set<String> reported = new HashSet<>();
    private final Set<String> reportedResearch = new HashSet<>();

    @Override
    public void decorateRecipe(EmiRecipe recipe, WidgetHolder widgets) {        
        CoilConfig config = CoilConfig.get();
        if (!config.enabled) {
            return;
        }

        ResourceLocation category = recipe.getCategory().getId();

        // Shared by the research badge below and the coil icon further down -
        // both anchor to the same EXTRA_HEIGHT strip CoilRecipeSizeMixin pads
        // onto every recipe card, at the exact same spot (config.offsetX/Y).
        int padTop = recipe.getDisplayHeight() - EXTRA_HEIGHT;
        int textX = 4 + config.textX;
        int textY1 = padTop + 2 + config.textY;
        int textY2 = textY1 + 9;
        int iconX = textX + 110 + config.offsetX;
        int iconY = padTop + (EXTRA_HEIGHT - SLOT_SIZE) / 2 + config.offsetY;

        // Research requirement is a completely separate recipe property from
        // the coil/temperature data below (GTCEU's ResearchCondition, not an
        // .addData() tag), so it's checked independently and isn't affected
        // by any of the coil-specific early returns that follow.
        decorateResearch(recipe, widgets, config, category, iconX, iconY);

        if (config.excludedCategories.contains(category.toString())) {
            report(config, category, "skipped: GTCEU already shows coil info natively here");
            return;
        }

        CoilConfig.Entry entry = config.categories.get(category.toString());
        if (entry == null && config.strictNamespaces && !config.namespaces.contains(category.getNamespace())) {
            report(config, category, "skipped: namespace not allowed");
            return;
        }

        CompoundTag data = RecipeData.of(recipe);
        if (data == null) {
            report(config, category, "no recipe data found (emi recipe class " + recipe.getClass().getName() + ")");
            return;
        }
        if (data.isEmpty()) {
            report(config, category, "recipe data tag is empty");
            return;
        }

        String key = entry != null ? entry.dataKey : autoDetectKey(config, data);
        String mode = entry != null ? entry.mode : "temperature";
        if (key == null || !data.contains(key)) {
            report(config, category, "no known coil key, tag holds " + data.getAllKeys());
            return;
        }

        int required = data.getInt(key);
        if (required <= 0) {
            report(config, category, key + " is " + required + ", nothing to show");
            return;
        }

        CoilIndex.Coil coil = switch (mode) {
            case "level" -> CoilIndex.forLevel(required);
            case "tier" -> CoilIndex.forTier(required);
            default -> CoilIndex.forTemperature(required);
        };
        if (coil == null) {
            report(config, category, "no coil reaches " + key + "=" + required);
            return;
        }

        report(config, category, "drawing " + coil.name().getString() + " for " + key + "=" + required);

        widgets.addSlot(EmiStack.of(coil.stack()), iconX, iconY)
                .catalyst(true)
                .appendTooltip(Component.translatable("gtcoilemi.tooltip.requires", coil.name())
                        .withStyle(ChatFormatting.GOLD))
                .appendTooltip(Component.translatable("gtcoilemi.tooltip." + mode, required)
                        .withStyle(ChatFormatting.GRAY));
        if (config.showText) {
            Component tempLine = Component.translatable("gtcoilemi.text." + mode, String.format("%,d", required));
            Component coilLine = Component.translatable("gtcoilemi.text.coil", coil.name().getString());
            widgets.addText(tempLine, textX, textY1, 0xFFFFFF, true);
            widgets.addText(coilLine, textX, textY2, 0xFFFFFF, true);
        }
    }

    /** Draws the research data-item badge at the exact same spot as the coil icon, if this recipe needs one. */
    private void decorateResearch(EmiRecipe recipe, WidgetHolder widgets, CoilConfig config, ResourceLocation category,
                                   int iconX, int iconY) {
        if (!config.showResearchIcon || config.researchExcludedCategories.contains(category.toString())) {
            return;
        }

        ItemStack dataItem = ResearchLookup.dataItemOf(RecipeData.backingRecipeOf(recipe));
        if (dataItem == null || dataItem.isEmpty()) {
            return;
        }

        if (config.logMatches && reportedResearch.add(category.toString())) {
            GTCoilEmi.LOGGER.info("[{}] {} -> drawing research requirement ({})", GTCoilEmi.MOD_ID, category,
                    dataItem.getItem());
        }

        widgets.addSlot(EmiStack.of(dataItem), iconX, iconY)
                .catalyst(true)
                .appendTooltip(Component.translatable("gtcoilemi.tooltip.research").withStyle(ChatFormatting.GOLD))
                .appendTooltip(dataItem.getHoverName().copy().withStyle(ChatFormatting.GRAY));
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

    private void report(CoilConfig config, ResourceLocation category, String message) {
        if (!config.logMatches || !reported.add(category.toString())) {
            return;
        }
        GTCoilEmi.LOGGER.info("[{}] {} -> {}", GTCoilEmi.MOD_ID, category, message);
    }
}
