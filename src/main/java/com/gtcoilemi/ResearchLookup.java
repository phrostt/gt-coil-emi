package com.gtcoilemi;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds the research data-item (data stick / data orb / data module, or
 * whatever a recipe's own {@code .dataStack(...)} call set) that a GregTech
 * recipe's research condition demands, without compiling against GregTech.
 *
 * GTRecipe keeps a public {@code conditions} list; a ResearchCondition in
 * that list holds a public {@code data} field (GregTech's own ResearchData,
 * which is {@code Iterable<ResearchEntry>}) whose entries carry the
 * {@code dataItem} the player has to have already researched. Everything
 * here is read by field/method name, the same reflection approach
 * {@link RecipeData} uses for the coil/temperature tag - this project
 * deliberately has no GregTech dependency on the compile classpath.
 */
public final class ResearchLookup {

    private static final Field NONE;

    static {
        Field none = null;
        try {
            none = ResearchLookup.class.getDeclaredField("NONE");
        } catch (NoSuchFieldException ignored) {
            // cannot happen
        }
        NONE = none;
    }

    private static final Map<Class<?>, Field> CONDITIONS_FIELDS = new HashMap<>();
    private static final Map<Class<?>, Field> CONDITION_DATA_FIELDS = new HashMap<>();
    private static final Map<Class<?>, Method> DATA_ITEM_METHODS = new HashMap<>();

    private ResearchLookup() {
    }

    /**
     * The data item this recipe's research condition demands, or null if the
     * recipe has no research condition (or it couldn't be read).
     */
    public static ItemStack dataItemOf(Recipe<?> recipe) {
        if (recipe == null) {
            return null;
        }

        Field conditionsField = CONDITIONS_FIELDS.computeIfAbsent(recipe.getClass(),
                ResearchLookup::findIterableField);
        if (conditionsField == NONE) {
            return null;
        }

        Object conditionsValue = read(recipe, conditionsField);
        if (!(conditionsValue instanceof Iterable<?> conditions)) {
            return null;
        }

        for (Object condition : conditions) {
            if (condition != null && "ResearchCondition".equals(condition.getClass().getSimpleName())) {
                ItemStack item = dataItemOfCondition(condition);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    private static ItemStack dataItemOfCondition(Object condition) {
        Field dataField = CONDITION_DATA_FIELDS.computeIfAbsent(condition.getClass(),
                ResearchLookup::findIterableField);
        if (dataField == NONE) {
            return null;
        }

        Object data = read(condition, dataField);
        if (!(data instanceof Iterable<?> entries)) {
            return null;
        }

        for (Object entry : entries) {
            if (entry == null) {
                continue;
            }
            Method getter = DATA_ITEM_METHODS.computeIfAbsent(entry.getClass(), ResearchLookup::findDataItemMethod);
            if (getter == null) {
                continue;
            }
            try {
                Object result = getter.invoke(entry);
                if (result instanceof ItemStack stack && !stack.isEmpty()) {
                    return stack;
                }
            } catch (Throwable ignored) {
                // try the next entry rather than failing the whole lookup
            }
        }
        return null;
    }

    /** Looks for a field literally named "conditions" or "data" that holds something Iterable. */
    private static Field findIterableField(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Iterable.class.isAssignableFrom(field.getType())
                        && (field.getName().equals("conditions") || field.getName().equals("data"))) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return NONE;
    }

    /** ResearchEntry exposes this via Lombok's @Getter - called by name since the type isn't on the classpath. */
    private static Method findDataItemMethod(Class<?> type) {
        try {
            Method method = type.getMethod("getDataItem");
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object read(Object owner, Field field) {
        try {
            return field.get(owner);
        } catch (Throwable t) {
            return null;
        }
    }
}
