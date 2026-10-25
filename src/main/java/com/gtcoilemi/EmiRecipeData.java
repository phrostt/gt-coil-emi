package com.gtcoilemi;

import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** EMI-only half of recipe lookup; kept apart so JEI-only installs never load EMI classes. */
public final class EmiRecipeData {

    private static final Field NONE;

    static {
        Field none = null;
        try {
            none = EmiRecipeData.class.getDeclaredField("NONE");
        } catch (NoSuchFieldException ignored) {
        }
        NONE = none;
    }

    private static final Map<Class<?>, Field> RECIPE_FIELDS = new HashMap<>();

    private EmiRecipeData() {
    }

    public static CompoundTag of(EmiRecipe emiRecipe) {
        Recipe<?> recipe = backingRecipeOf(emiRecipe);
        return recipe == null ? null : RecipeData.tagOf(recipe);
    }

    /**
     * The underlying GTRecipe (or whatever else backs this EMI recipe), for
     * callers that need more off it than the {@code data} tag - e.g. reading
     * its research condition. Same three-step lookup {@link #of} uses.
     */
    public static Recipe<?> backingRecipeOf(EmiRecipe emiRecipe) {
        Recipe<?> recipe = fromField(emiRecipe);
        if (recipe == null) {
            recipe = emiRecipe.getBackingRecipe();
        }
        if (recipe == null) {
            recipe = lookup(emiRecipe.getId());
        }
        return recipe;
    }

    /** Reads whichever field of the EMI recipe holds the underlying game recipe. */
    private static Recipe<?> fromField(EmiRecipe emiRecipe) {
        Field field = RECIPE_FIELDS.computeIfAbsent(emiRecipe.getClass(), EmiRecipeData::findRecipeField);
        if (field == NONE) {
            return null;
        }
        Object value = RecipeData.read(emiRecipe, field);
        return value instanceof Recipe<?> recipe ? recipe : null;
    }

    private static Field findRecipeField(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Recipe.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return NONE;
    }

    private static Recipe<?> lookup(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        Optional<? extends Recipe<?>> recipe = minecraft.level.getRecipeManager().byKey(id);
        return recipe.orElse(null);
    }

}
