package com.gtcoilemi.mixin;

import com.gtcoilemi.CoilConfig;
import com.gtcoilemi.CoilIndex;
import com.gtcoilemi.CoilInfo;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory", remap = false)
public abstract class CoilJeiSlotMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(
            method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ljava/lang/Object;Lmezz/jei/api/recipe/IFocusGroup;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void gtcoilemi$addRequirementSlot(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses, CallbackInfo ci) {
        try {
            if (recipe == null || !recipe.getClass().getName().equals("com.gregtechceu.gtceu.api.recipe.GTRecipe")) {
                return;
            }
            if (!(this instanceof IRecipeCategory category)) {
                return;
            }
            CoilConfig config = CoilConfig.get();
            if (!config.enabled || !config.jeiEnabled
                    || net.minecraftforge.fml.ModList.get().isLoaded("emi")) {
                return;
            }
            CoilInfo.Result info = CoilInfo.resolve(config, category.getRecipeType().getUid(), recipe);
            if (info == null) {
                return;
            }
            CoilInfo.Layout layout = CoilInfo.layout(config, category.getWidth(), category.getHeight());

            if (info.hasCoil()) {
                List<ItemStack> stacks = new ArrayList<>();
                for (CoilIndex.Coil coil : CoilIndex.allMatching(info.mode(), info.required())) {
                    stacks.add(coil.stack());
                }
                if (stacks.isEmpty()) {
                    stacks.add(info.coil().stack());
                }
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, layout.iconX() + 1, layout.iconY() + 1)
                        .addItemStacks(stacks)
                        .addRichTooltipCallback((view, tooltip) -> {
                            tooltip.add(Component.translatable("gtcoilemi.tooltip.requires", info.coil().name())
                                    .withStyle(ChatFormatting.GOLD));
                            tooltip.add(Component.translatable("gtcoilemi.tooltip." + info.mode(), info.required())
                                    .withStyle(ChatFormatting.GRAY));
                        });
            } else if (info.hasTarget()) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, layout.dataX() + 1, layout.iconY() + 1)
                        .addItemStack(info.research())
                        .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                                Component.translatable("gtcoilemi.tooltip.research_data").withStyle(ChatFormatting.GOLD)));
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, layout.iconX() + 1, layout.iconY() + 1)
                        .addItemStack(info.target())
                        .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                                Component.translatable("gtcoilemi.tooltip.research_target").withStyle(ChatFormatting.GOLD)));
            } else {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, layout.iconX() + 1, layout.iconY() + 1)
                        .addItemStack(info.research())
                        .addRichTooltipCallback((view, tooltip) -> {
                            tooltip.add(Component.translatable("gtcoilemi.tooltip.research").withStyle(ChatFormatting.GOLD));
                            tooltip.add(info.research().getHoverName().copy().withStyle(ChatFormatting.GRAY));
                        });
            }
            CoilInfo.markSlotted(recipe);
        } catch (Throwable t) {
            // leave the decorator fallback in charge
        }
    }
}
