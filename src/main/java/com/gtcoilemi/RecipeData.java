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

/**
 * Pulls the extra NBT a GregTech recipe carries (the tag written by
 * {@code .addData("key", value)} in KubeJS) out of an EMI recipe, without
 * compiling against GregTech.
 *
 * GregTech's GTEmiRecipe keeps the GTRecipe in a package-private field and does
 * not override getBackingRecipe(), so the field is read directly; the other two
 * routes are fallbacks.
 */
public final class RecipeData {

    /** Marker for "this class has no usable field", so misses are cached too. */
    private static final Field NONE;

    static {
        Field none = null;
        try {
            none = RecipeData.class.getDeclaredField("NONE");
        } catch (NoSuchFieldException ignored) {
            // cannot happen
        }
        NONE = none;
    }

    private static final Map<Class<?>, Field> RECIPE_FIELDS = new HashMap<>();
    private static final Map<Class<?>, Field> TAG_FIELDS = new HashMap<>();

    private RecipeData() {
    }

    public static CompoundTag of(EmiRecipe emiRecipe) {
        Recipe<?> recipe = backingRecipeOf(emiRecipe);
        return recipe == null ? null : tagOf(recipe);
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
        Field field = RECIPE_FIELDS.computeIfAbsent(emiRecipe.getClass(), RecipeData::findRecipeField);
        if (field == NONE) {
            return null;
        }
        Object value = read(emiRecipe, field);
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

    private static CompoundTag tagOf(Recipe<?> recipe) {
        Field field = TAG_FIELDS.computeIfAbsent(recipe.getClass(), RecipeData::findTagField);
        if (field == NONE) {
            return null;
        }
        Object value = read(recipe, field);
        return value instanceof CompoundTag tag ? tag : null;
    }

    /** GregTech names it {@code data}; the second pass survives a rename. */
    private static Field findTagField(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == CompoundTag.class && "data".equals(field.getName())) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == CompoundTag.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return NONE;
    }

    private static Object read(Object owner, Field field) {
        try {
            return field.get(owner);
        } catch (Throwable t) {
            return null;
        }
    }
}
