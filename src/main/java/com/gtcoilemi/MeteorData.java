package com.gtcoilemi;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Reads Blood Magic's meteor recipes ("bloodmagic:meteor", the recipes behind
 * the Mark of the Falling Tower ritual) by reflection, so the mod needs no
 * Blood Magic dependency to compile. Blood Magic's own JEI integration does not
 * show these recipes natively.
 */
public final class MeteorData {

    public static final ResourceLocation TYPE_ID = new ResourceLocation("bloodmagic", "meteor");
    public static final String RITUAL_NAME_KEY = "ritual.bloodmagic.meteorRitual";

    private static final int MAX_TAG_ICONS = 40;

    private MeteorData() {
    }

    /** One block (or tag, or fluid) a layer can contain. {@code chance} is 0-100, or -1 for a shell. */
    public record Entry(String label, Ingredient icon, float chance, String role) {
    }

    public record Layer(int radius, List<Entry> entries) {
    }

    public record Meteor(ResourceLocation id, Ingredient catalyst, int syphon, float explosion, int radius,
                         List<Layer> layers) {
    }

    public static List<Meteor> load(RecipeManager manager) {
        List<Meteor> out = new ArrayList<>();
        RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(TYPE_ID);
        if (type == null || manager == null) {
            return out;
        }
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (recipe.getType() != type) {
                continue;
            }
            try {
                Method getInput = recipe.getClass().getMethod("getInput");
                Method getSyphon = recipe.getClass().getMethod("getSyphon");
                Ingredient catalyst = (Ingredient) getInput.invoke(recipe);
                int syphon = (Integer) getSyphon.invoke(recipe);
                float explosion = readFloat(recipe, "explosionRadius");
                List<Layer> layers = readLayers(readField(recipe, "layerList"));
                int radius = 0;
                for (Layer layer : layers) {
                    radius = Math.max(radius, layer.radius());
                }
                out.add(new Meteor(recipe.getId(), catalyst, syphon, explosion, radius, layers));
            } catch (ReflectiveOperationException | RuntimeException e) {
                GTCoilEmi.LOGGER.warn("[{}] could not read meteor recipe {}: {}", GTCoilEmi.MOD_ID, recipe.getId(), e.toString());
            }
        }
        GTCoilEmi.LOGGER.info("[{}] found {} Blood Magic meteor recipes", GTCoilEmi.MOD_ID, out.size());
        return out;
    }

    /** The weak activation crystal, used as the tab icon and workstation; falls back to a barrier if Blood Magic changes the id. */
    public static ItemStack ritualStone() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("bloodmagic", "activationcrystalweak"));
        if (item == null || item == Items.AIR) {
            return new ItemStack(Items.BARRIER);
        }
        return new ItemStack(item);
    }

    private static List<Layer> readLayers(Object layerList) {
        List<Layer> layers = new ArrayList<>();
        if (!(layerList instanceof List<?> list)) {
            return layers;
        }
        for (Object layer : list) {
            try {
                int radius = ((Number) readField(layer, "layerRadius")).intValue();
                int additional = ((Number) readField(layer, "additionalTotalWeight")).intValue();
                int minWeight = ((Number) readField(layer, "minWeight")).intValue();
                Object weightList = readField(layer, "weightList");
                Object fill = readField(layer, "fillBlock");
                Object shell = readField(layer, "shellBlock");

                List<Map.Entry<?, ?>> weighted = new ArrayList<>();
                int sum = 0;
                if (weightList instanceof List<?> wl) {
                    for (Object pair : wl) {
                        if (pair instanceof Map.Entry<?, ?> e && e.getValue() instanceof Number w) {
                            weighted.add(e);
                            sum += w.intValue();
                        }
                    }
                }
                int total = Math.max(minWeight, additional + sum);

                List<Entry> entries = new ArrayList<>();
                float fillChance = total <= 0 ? 100f : 100f * Math.max(0, total - sum) / total;
                Entry fillEntry = container(fill, fillChance, "fill");
                if (fillEntry != null) {
                    entries.add(fillEntry);
                }
                List<Entry> extras = new ArrayList<>();
                for (Map.Entry<?, ?> e : weighted) {
                    float chance = total <= 0 ? 0f : 100f * ((Number) e.getValue()).intValue() / total;
                    Entry entry = container(e.getKey(), chance, "weighted");
                    if (entry != null) {
                        extras.add(entry);
                    }
                }
                extras.sort(Comparator.comparingDouble(Entry::chance).reversed());
                entries.addAll(extras);
                Entry shellEntry = container(shell, -1f, "shell");
                if (shellEntry != null) {
                    entries.add(shellEntry);
                }
                layers.add(new Layer(radius, entries));
            } catch (RuntimeException e) {
                GTCoilEmi.LOGGER.warn("[{}] skipped an unreadable meteor layer: {}", GTCoilEmi.MOD_ID, e.toString());
            }
        }
        layers.sort(Comparator.comparingInt(Layer::radius));
        return layers;
    }

    /** Turns a Blood Magic block container into a display entry, using its serialised entry string ("id", "#tag[#index]" or ";fluid"). */
    private static Entry container(Object container, float chance, String role) {
        if (container == null) {
            return null;
        }
        String entry;
        try {
            entry = (String) container.getClass().getMethod("getEntry").invoke(container);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
        if (entry == null || entry.isEmpty()) {
            return null;
        }
        if (entry.startsWith("#")) {
            String[] parts = entry.substring(1).split("#");
            ResourceLocation tagId = ResourceLocation.tryParse(parts[0]);
            if (tagId == null) {
                return null;
            }
            int index = -1;
            if (parts.length > 1) {
                try {
                    index = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    // no static index
                }
            }
            List<ItemStack> stacks = new ArrayList<>();
            var tags = ForgeRegistries.BLOCKS.tags();
            if (tags != null) {
                List<Block> blocks = new ArrayList<>();
                tags.getTag(TagKey.create(Registries.BLOCK, tagId)).forEach(blocks::add);
                if (index >= 0 && index < blocks.size()) {
                    blocks = List.of(blocks.get(index));
                }
                for (Block block : blocks) {
                    if (stacks.size() >= MAX_TAG_ICONS) {
                        break;
                    }
                    Item item = block.asItem();
                    if (item != Items.AIR) {
                        stacks.add(new ItemStack(item));
                    }
                }
            }
            return new Entry("#" + tagId, stacks.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stacks.stream()), chance, role);
        }
        if (entry.startsWith(";")) {
            ResourceLocation fluidId = ResourceLocation.tryParse(entry.substring(1));
            Fluid fluid = fluidId == null ? null : ForgeRegistries.FLUIDS.getValue(fluidId);
            Item bucket = fluid == null ? Items.AIR : fluid.getBucket();
            return new Entry(fluidId == null ? entry : fluidId.toString(),
                    bucket == Items.AIR ? Ingredient.EMPTY : Ingredient.of(new ItemStack(bucket)), chance, role);
        }
        ResourceLocation blockId = ResourceLocation.tryParse(entry);
        Block block = blockId == null ? null : ForgeRegistries.BLOCKS.getValue(blockId);
        Item item = block == null ? Items.AIR : block.asItem();
        return new Entry(blockId == null ? entry : blockId.toString(),
                item == Items.AIR ? Ingredient.EMPTY : Ingredient.of(new ItemStack(item)), chance, role);
    }

    private static float readFloat(Object owner, String name) {
        Object value = readField(owner, name);
        return value instanceof Number n ? n.floatValue() : 0f;
    }

    private static Object readField(Object owner, String name) {
        try {
            for (Class<?> c = owner.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(owner);
                } catch (NoSuchFieldException ignored) {
                    // try superclass
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // fall through
        }
        return null;
    }
}
