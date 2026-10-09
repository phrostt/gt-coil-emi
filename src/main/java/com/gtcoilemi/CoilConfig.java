package com.gtcoilemi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * config/gtcoilemi.json
 *
 * Custom multiblocks store their coil requirement under whatever NBT key the
 * pack author picked, so the mapping from recipe category to data key lives in
 * config rather than in code. Auto-detection covers the common key names.
 */
public class CoilConfig {

    /** How the number stored in the recipe should be matched against a coil. */
    public static class Entry {
        /** NBT key written by {@code .addData(...)} in the recipe. */
        public String dataKey = "ebf_temp";
        /** One of: temperature, level, tier. */
        public String mode = "temperature";
    }

    public boolean enabled = true;

    /** Restrict by namespace at all; matching is otherwise driven by the data key. */
    public boolean strictNamespaces = false;

    /** Log one line per recipe category explaining what the decorator found. */
    public boolean logMatches = true;

    /** Recipe category namespaces to decorate. */
    public List<String> namespaces = new ArrayList<>(List.of("gtceu", "kubejs"));

    /** Try these keys when a category has no explicit entry below. */
    public boolean autoDetect = true;
    public List<String> autoDetectKeys = new ArrayList<>(List.of(
            "ebf_temp", "RequiredTemp", "required_temp", "coil_temp", "temperature"));

    /** Explicit per-category configuration, keyed by EMI category id. */
    public Map<String, Entry> categories = new LinkedHashMap<>();

    /**
     * Categories GTCEU already shows coil/temperature requirements for natively
     * (electric blast furnace, cracker, pyrolyse oven) - never decorate these,
     * or the coil would be drawn twice.
     */
    public Set<String> excludedCategories = new LinkedHashSet<>(List.of(
            "gtceu:electric_blast_furnace", "gtceu:cracker", "gtceu:pyrolyse_oven"));

    /** Draw the research data-item (stick/orb/module) badge for recipes gated by GTCEU's research condition. */
    public boolean showResearchIcon = true;

    /**
     * Categories to skip for the research badge - Assembly Line already shows
     * its own Data Access Hatch slot natively, so decorating it here would
     * just duplicate what's already visible.
     */
    public Set<String> researchExcludedCategories = new LinkedHashSet<>(List.of("gtceu:assembly_line"));

    /** Nudge the drawn slot if it collides with a category's own widgets. */
    public int offsetX = 0;
    public int offsetY = 0;
    
    /** Draw the requirement as plain text on the recipe page too, not just in a tooltip. */
    public boolean showText = true;

    /** Where the text block starts, relative to the recipe widget's top-left corner. */
    public int textX = 4;
    public int textY = 4;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CoilConfig instance;

    public static CoilConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(GTCoilEmi.MOD_ID + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                instance = GSON.fromJson(reader, CoilConfig.class);
            } catch (IOException | RuntimeException e) {
                GTCoilEmi.LOGGER.error("[{}] could not read {}, using defaults", GTCoilEmi.MOD_ID, file, e);
            }
        }
        if (instance == null) {
            instance = defaults();
            write(file);
        }
    }

    private static CoilConfig defaults() {
        CoilConfig config = new CoilConfig();

        // electric_blast_furnace, cracker and pyrolyse_oven are NOT listed here -
        // GTCEU already draws their coil requirement natively; see excludedCategories.

        // Example for a KubeJS machine using .addData("RequiredTemp", 1000):
        Entry example = new Entry();
        example.dataKey = "RequiredTemp";
        example.mode = "temperature";
        config.categories.put("gtceu:example_smelting", example);

        return config;
    }

    private static void write(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            GTCoilEmi.LOGGER.error("[{}] could not write {}", GTCoilEmi.MOD_ID, file, e);
        }
    }
}
