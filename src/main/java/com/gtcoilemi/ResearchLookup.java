package com.gtcoilemi;

import net.minecraft.world.item.ItemStack;

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

    public static ItemStack dataItemOf(Object recipe) {
        Object entry = firstEntry(recipe);
        if (entry == null) {
            return null;
        }
        Method getter = DATA_ITEM_METHODS.computeIfAbsent(entry.getClass(), ResearchLookup::findDataItemMethod);
        if (getter == null) {
            return null;
        }
        try {
            Object result = getter.invoke(entry);
            return result instanceof ItemStack stack && !stack.isEmpty() ? stack : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** The item the research id points at (what must be scanned or researched), or null if the id is not an item. */
    public static ItemStack targetItemOf(Object recipe) {
        Object entry = firstEntry(recipe);
        if (entry == null) {
            return null;
        }
        try {
            Object id = entry.getClass().getMethod("getResearchId").invoke(entry);
            if (!(id instanceof String text)) {
                return null;
            }
            ItemStack found = indexed(text);
            return found != null ? found : itemFromResearchId(text);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Map<String, ItemStack> index;

    /**
     * Research ids set with a custom id (not derived from the scanned item) can't be turned back into an
     * item by parsing, so this reads GregTech's own research recipes (research station and scanner): the
     * recipe whose output data item carries the id is the one whose item input is what gets researched.
     */
    private static synchronized ItemStack indexed(String id) {
        if (index == null) {
            index = new HashMap<>();
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) {
                index = null;
                return null;
            }
            try {
                buildIndex(level.getRecipeManager());
            } catch (Throwable t) {
                GTCoilEmi.LOGGER.warn("[{}] could not index GregTech research recipes: {}", GTCoilEmi.MOD_ID, t.toString());
            }
            GTCoilEmi.LOGGER.info("[{}] indexed {} research targets", GTCoilEmi.MOD_ID, index.size());
        }
        return index.get(id);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void buildIndex(net.minecraft.world.item.crafting.RecipeManager manager) {
        for (String path : new String[]{"research_station", "scanner"}) {
            net.minecraft.world.item.crafting.RecipeType type = net.minecraftforge.registries.ForgeRegistries.RECIPE_TYPES
                    .getValue(new net.minecraft.resources.ResourceLocation("gtceu", path));
            if (type == null) {
                continue;
            }
            for (Object recipe : manager.getAllRecipesFor(type)) {
                String researchId = null;
                for (ItemStack out : stacksIn(recipe, "outputs")) {
                    net.minecraft.nbt.CompoundTag tag = out.getTag();
                    if (tag != null && tag.contains("assembly_line_research", 10)) {
                        researchId = tag.getCompound("assembly_line_research").getString("research_id");
                        break;
                    }
                }
                if (researchId == null || researchId.isEmpty() || index.containsKey(researchId)) {
                    continue;
                }
                for (ItemStack in : stacksIn(recipe, "inputs")) {
                    String name = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(in.getItem()).getPath();
                    if (!name.contains("data_")) {
                        ItemStack copy = in.copy();
                        copy.setCount(1);
                        index.put(researchId, copy);
                        break;
                    }
                }
            }
        }
    }

    /** First stack of every content entry in a GTRecipe's public {@code inputs} / {@code outputs} capability map. */
    private static java.util.List<ItemStack> stacksIn(Object recipe, String fieldName) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        try {
            Object map = recipe.getClass().getField(fieldName).get(recipe);
            if (!(map instanceof Map<?, ?> caps)) {
                return stacks;
            }
            for (Object list : caps.values()) {
                if (!(list instanceof Iterable<?> contents)) {
                    continue;
                }
                for (Object content : contents) {
                    Object inner = content;
                    try {
                        inner = content.getClass().getField("content").get(content);
                    } catch (ReflectiveOperationException ignored) {
                        // not a Content wrapper
                    }
                    if (inner instanceof net.minecraft.world.item.crafting.Ingredient ingredient) {
                        ItemStack[] items = ingredient.getItems();
                        if (items.length > 0 && !items[0].isEmpty()) {
                            stacks.add(items[0]);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // leave what was read
        }
        return stacks;
    }

    private static ItemStack itemFromResearchId(String text) {
        String rest = text.replaceFirst("^\\d+x_", "");
        for (int i = rest.indexOf('_'); i >= 0; i = rest.indexOf('_', i + 1)) {
            net.minecraft.resources.ResourceLocation id =
                    net.minecraft.resources.ResourceLocation.tryParse(rest.substring(0, i) + ":" + rest.substring(i + 1));
            if (id != null && net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(id)) {
                return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id));
            }
        }
        return null;
    }

    private static Object firstEntry(Object recipe) {
        if (recipe == null) {
            return null;
        }
        Field conditionsField = CONDITIONS_FIELDS.computeIfAbsent(recipe.getClass(), ResearchLookup::findIterableField);
        if (conditionsField == NONE) {
            return null;
        }
        Object conditionsValue = read(recipe, conditionsField);
        if (!(conditionsValue instanceof Iterable<?> conditions)) {
            return null;
        }
        for (Object condition : conditions) {
            if (condition == null || !"ResearchCondition".equals(condition.getClass().getSimpleName())) {
                continue;
            }
            Field dataField = CONDITION_DATA_FIELDS.computeIfAbsent(condition.getClass(), ResearchLookup::findIterableField);
            if (dataField == NONE) {
                continue;
            }
            Object data = read(condition, dataField);
            if (data instanceof Iterable<?> entries) {
                for (Object entry : entries) {
                    if (entry != null) {
                        return entry;
                    }
                }
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
