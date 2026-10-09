package com.gtcoilemi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * GTRecipeWidget (GTCEU's EMI/JEI recipe card for machine recipes) fixes its
 * card size once, in its constructor:
 *
 *   super(getXOffset(recipe), 0,
 *         recipe.recipeType.getRecipeUI().getJEISize().width,
 *         recipe.recipeType.getRecipeUI().getJEISize().height);
 *
 * That size never auto-grows to fit content - it's a flat value taken from
 * the recipe type's declared JEI size. CoilDecorator draws two extra lines
 * of text (temperature + coil) below GTCEU's own Duration/Total/Usage text,
 * so every card needs a bit more room at the bottom for that to have
 * somewhere to go without overlapping GTCEU's own content.
 *
 * This targets GTRecipeWidget by fully-qualified name (not a compile-time
 * class reference) so the project doesn't need GTCEU or LDLib as compile
 * dependencies, matching the reflection-based approach used elsewhere in
 * this mod (see RecipeData.java). It widens every GTCEU recipe card
 * uniformly rather than conditionally, trading a few pixels of unused
 * padding on recipes without coil data for not needing local-variable
 * capture (MixinExtras) just to read the recipe being constructed.
 */
@Mixin(targets = "com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget")
public class CoilRecipeSizeMixin {

    /** Keep in sync with CoilDecorator.EXTRA_HEIGHT. */
    private static final int EXTRA_HEIGHT = 20;

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/lowdragmc/lowdraglib/gui/widget/WidgetGroup;<init>(IIII)V"
            ),
            index = 3
    )
    private static int gtcoilemi$growHeight(int height) {
        return height + EXTRA_HEIGHT;
    }
}
