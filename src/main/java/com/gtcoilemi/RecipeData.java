package com.gtcoilemi;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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

    private static final Map<Class<?>, Field> TAG_FIELDS = new HashMap<>();

    private RecipeData() {
    }

    public static CompoundTag tagOf(Object recipe) {
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

    static Object read(Object owner, Field field) {
        try {
            return field.get(owner);
        } catch (Throwable t) {
            return null;
        }
    }
}
