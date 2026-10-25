package com.gtcoilemi;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared geometry and text of the meteor tab, so the EMI and JEI versions look the same. */
final class MeteorLayout {

    static final int WIDTH = 176;
    static final int TEXT_X = 4;
    static final int TITLE_Y = 3;
    static final int SLOT_X = 4;
    static final int SLOT_Y = 20;
    static final int LINES_Y = 16;
    static final int ICON = 18;
    static final int PER_ROW = 9;
    static final int LAYER_LABEL = 11;

    /**
     * JEI fixes a category height up front, before any recipe is known, so this has to fit the
     * tallest plausible meteor: header plus about five layers of one to two rows.
     */
    static final int JEI_HEIGHT = 200;

    private MeteorLayout() {
    }

    /** One drawn block slot: where it goes and which entry it shows. */
    record Cell(int x, int y, MeteorData.Entry entry) {
    }

    /** The label above one layer's row of blocks. */
    record Label(int x, int y, Component text) {
    }

    static List<Component> lines(MeteorData.Meteor meteor) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gtcoilemi.meteor.catalyst").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gtcoilemi.meteor.blood", String.format("%,d", meteor.syphon()))
                .withStyle(ChatFormatting.RED));
        lines.add(Component.translatable("gtcoilemi.meteor.activation", String.format("%,d", CoilConfig.get().meteorActivationCost))
                .withStyle(ChatFormatting.DARK_RED));
        if (meteor.explosion() > 0) {
            lines.add(Component.translatable("gtcoilemi.meteor.explosion", trim(meteor.explosion()))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("gtcoilemi.meteor.radius", meteor.radius()).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    static int headerHeight(MeteorData.Meteor meteor) {
        return Math.max(SLOT_Y + ICON + 4, LINES_Y + lines(meteor).size() * 10 + 4);
    }

    static int height(MeteorData.Meteor meteor) {
        int y = headerHeight(meteor);
        for (MeteorData.Layer layer : meteor.layers()) {
            y += LAYER_LABEL + rows(layer) * ICON + 4;
        }
        return y;
    }

    static List<Label> labels(MeteorData.Meteor meteor) {
        List<Label> labels = new ArrayList<>();
        int y = headerHeight(meteor);
        int n = 1;
        for (MeteorData.Layer layer : meteor.layers()) {
            labels.add(new Label(TEXT_X, y, Component.translatable("gtcoilemi.meteor.layer", n, layer.radius())
                    .withStyle(ChatFormatting.GOLD)));
            y += LAYER_LABEL + rows(layer) * ICON + 4;
            n++;
        }
        return labels;
    }

    static List<Cell> cells(MeteorData.Meteor meteor) {
        List<Cell> cells = new ArrayList<>();
        int y = headerHeight(meteor);
        for (MeteorData.Layer layer : meteor.layers()) {
            int i = 0;
            for (MeteorData.Entry entry : layer.entries()) {
                if (entry.icon().isEmpty()) {
                    continue;
                }
                cells.add(new Cell(TEXT_X + (i % PER_ROW) * ICON, y + LAYER_LABEL + (i / PER_ROW) * ICON, entry));
                i++;
            }
            y += LAYER_LABEL + rows(layer) * ICON + 4;
        }
        return cells;
    }

    /** Tooltip lines for a block slot: what it is and how likely it is. */
    static List<Component> tooltip(MeteorData.Entry entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.label()).withStyle(ChatFormatting.GRAY));
        switch (entry.role()) {
            case "shell" -> lines.add(Component.translatable("gtcoilemi.meteor.shell").withStyle(ChatFormatting.AQUA));
            case "fill" -> lines.add(Component.translatable("gtcoilemi.meteor.fill", trim(entry.chance()))
                    .withStyle(ChatFormatting.GREEN));
            default -> lines.add(Component.translatable("gtcoilemi.meteor.chance", trim(entry.chance()))
                    .withStyle(ChatFormatting.GREEN));
        }
        return lines;
    }

    private static int rows(MeteorData.Layer layer) {
        int shown = 0;
        for (MeteorData.Entry entry : layer.entries()) {
            if (!entry.icon().isEmpty()) {
                shown++;
            }
        }
        return Math.max(1, (shown + PER_ROW - 1) / PER_ROW);
    }

    private static String trim(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format("%.1f", value);
    }
}
