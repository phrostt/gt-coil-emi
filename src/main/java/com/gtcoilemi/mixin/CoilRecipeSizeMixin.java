package com.gtcoilemi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI")
public class CoilRecipeSizeMixin {

    @ModifyArg(
            method = "getJEISize",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/lowdragmc/lowdraglib/utils/Size;<init>(II)V"
            ),
            index = 1
    )
    private int gtcoilemi$growHeight(int height) {
        int extra = 20;
        try {
            if (net.minecraftforge.fml.loading.LoadingModList.get().getModFileById("emi") == null) {
                extra = 42;
            }
        } catch (Throwable t) {
            extra = 20;
        }
        return height + extra;
    }
}
