package com.gtcoilemi.mixin;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "mezz.jei.common.util.SafeIngredientUtil", remap = false)
public class LdlibJeiTooltipMixin {

    private static final String LDLIB_RENDERER_PREFIX = "com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory$";

    @SuppressWarnings({"unchecked", "rawtypes", "deprecation", "removal"})
    @Inject(
            method = "getRichTooltip(Lmezz/jei/api/gui/builder/ITooltipBuilder;Lmezz/jei/api/runtime/IIngredientManager;Lmezz/jei/api/ingredients/IIngredientRenderer;Lmezz/jei/api/ingredients/ITypedIngredient;Lnet/minecraft/world/item/TooltipFlag$Default;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void gtcoilemi$ldlibSlotTooltip(ITooltipBuilder tooltip, IIngredientManager ingredientManager,
                                                   IIngredientRenderer ingredientRenderer, ITypedIngredient typedIngredient,
                                                   TooltipFlag.Default tooltipFlag, CallbackInfo ci) {
        if (ingredientRenderer == null || typedIngredient == null
                || !ingredientRenderer.getClass().getName().startsWith(LDLIB_RENDERER_PREFIX)) {
            return;
        }
        try {
            tooltip.setIngredient(typedIngredient);
            ingredientRenderer.getTooltip(tooltip, typedIngredient.getIngredient(), tooltipFlag);
            ci.cancel();
        } catch (Throwable t) {
            // fall through to JEI's own handling
        }
    }
}
