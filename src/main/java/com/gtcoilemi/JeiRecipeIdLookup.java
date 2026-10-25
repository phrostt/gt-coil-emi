package com.gtcoilemi;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Method;
import java.util.Optional;

/** Finds the recipe id behind the output slot under the mouse on JEI's recipe screen, whatever the category. */
final class JeiRecipeIdLookup {

    private JeiRecipeIdLookup() {
    }

    private static IRecipeLayoutDrawable<?> layoutAt(Screen screen, double mouseX, double mouseY) throws ReflectiveOperationException {
        Method underMouse = screen.getClass().getMethod("getRecipeLayoutUnderMouse", double.class, double.class);
        Optional<?> withButtons = (Optional<?>) underMouse.invoke(screen, mouseX, mouseY);
        if (withButtons.isEmpty()) {
            return null;
        }
        Object holder = withButtons.get();
        Method getLayout = holder.getClass().getMethod("getRecipeLayout");
        getLayout.setAccessible(true);
        return (IRecipeLayoutDrawable<?>) getLayout.invoke(holder);
    }

    static String findAny(Screen screen, double mouseX, double mouseY) throws ReflectiveOperationException {
        IRecipeLayoutDrawable<?> layout = layoutAt(screen, mouseX, mouseY);
        return layout == null ? null : recipeId(layout);
    }

    static String find(Screen screen, double mouseX, double mouseY) throws ReflectiveOperationException {
        IRecipeLayoutDrawable<?> layout = layoutAt(screen, mouseX, mouseY);
        if (layout == null) {
            return null;
        }
        net.minecraft.client.renderer.Rect2i rect = layout.getRect();
        Optional<IRecipeSlotDrawable> slot = layout.getRecipeSlotUnderMouse(mouseX - rect.getX(), mouseY - rect.getY());
        if (slot.isEmpty()) {
            slot = layout.getRecipeSlotUnderMouse(mouseX, mouseY);
        }
        if (slot.isEmpty() || slot.get().getRole() != RecipeIngredientRole.OUTPUT) {
            return null;
        }
        return recipeId(layout);
    }

    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    private static String recipeId(IRecipeLayoutDrawable<?> layout) {
        Object recipe = layout.getRecipe();
        if (recipe == null) {
            return null;
        }
        try {
            Object id = recipe.getClass().getField("id").get(recipe);
            if (id != null) {
                return id.toString();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        if (recipe instanceof Recipe<?> vanilla) {
            return vanilla.getId().toString();
        }
        ResourceLocation name = ((mezz.jei.api.recipe.category.IRecipeCategory) layout.getRecipeCategory()).getRegistryName(recipe);
        return name == null ? null : name.toString();
    }
}
