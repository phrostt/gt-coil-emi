package com.gtcoilemi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeDecorator;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * EMI side: draws the required coil and the research data item as extra slots
 * in the strip CoilRecipeSizeMixin pads onto every GregTech recipe card. What
 * to draw is decided by {@link CoilInfo}, shared with the JEI integration.
 */
public class CoilDecorator implements EmiRecipeDecorator {

    @Override
    public void decorateRecipe(EmiRecipe recipe, WidgetHolder widgets) {
        CoilConfig config = CoilConfig.get();
        if (!config.enabled) {
            return;
        }

        ResourceLocation category = recipe.getCategory().getId();
        CoilInfo.Result info = CoilInfo.resolve(config, category, EmiRecipeData.backingRecipeOf(recipe));
        if (info == null) {
            return;
        }
        CoilInfo.Layout layout = CoilInfo.layout(config, recipe.getDisplayWidth(), recipe.getDisplayHeight());

        if (info.hasResearch()) {
            ItemStack dataItem = info.research();
            if (info.hasTarget()) {
                widgets.addSlot(EmiStack.of(dataItem), layout.dataX(), layout.iconY())
                        .catalyst(true)
                        .appendTooltip(Component.translatable("gtcoilemi.tooltip.research_data").withStyle(ChatFormatting.GOLD));
                widgets.addText(Component.literal("\u2192"), layout.arrowX() + 3, layout.iconY() + 5, 0xFF9A9A9A, false);
                widgets.addSlot(EmiStack.of(info.target()), layout.iconX(), layout.iconY())
                        .catalyst(true)
                        .appendTooltip(Component.translatable("gtcoilemi.tooltip.research_target").withStyle(ChatFormatting.GOLD));
            } else {
                widgets.addSlot(EmiStack.of(dataItem), layout.iconX(), layout.iconY())
                        .catalyst(true)
                        .appendTooltip(Component.translatable("gtcoilemi.tooltip.research").withStyle(ChatFormatting.GOLD))
                        .appendTooltip(dataItem.getHoverName().copy().withStyle(ChatFormatting.GRAY));
            }
        }

        if (info.hasCoil()) {
            CoilIndex.Coil coil = info.coil();
            java.util.List<dev.emi.emi.api.stack.EmiIngredient> options = new java.util.ArrayList<>();
            for (CoilIndex.Coil option : CoilIndex.allMatching(info.mode(), info.required())) {
                options.add(EmiStack.of(option.stack()));
            }
            if (options.isEmpty()) {
                options.add(EmiStack.of(coil.stack()));
            }
            widgets.addSlot(dev.emi.emi.api.stack.EmiIngredient.of(options), layout.iconX(), layout.iconY())
                    .catalyst(true)
                    .appendTooltip(Component.translatable("gtcoilemi.tooltip.requires", coil.name())
                            .withStyle(ChatFormatting.GOLD))
                    .appendTooltip(Component.translatable("gtcoilemi.tooltip." + info.mode(), info.required())
                            .withStyle(ChatFormatting.GRAY));
            if (config.showText) {
                Component tempLine = Component.translatable("gtcoilemi.text." + info.mode(),
                        String.format("%,d", info.required()));
                Component coilLine = Component.translatable("gtcoilemi.text.coil", coil.name().getString());
                widgets.addText(tempLine, layout.textX(), layout.textY1(), 0xFFFFFF, true);
                widgets.addText(coilLine, layout.textX(), layout.textY2(), 0xFFFFFF, true);
            }
        }
    }
}
