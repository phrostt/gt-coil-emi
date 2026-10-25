package com.gtcoilemi;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared geometry, text and drawing of the ritual tab, so the EMI and JEI versions look the same. */
final class RitualLayout {

    static final int WIDTH = 176;
    static final int HEIGHT = 200;
    static final int TEXT_X = 4;
    static final int TITLE_Y = 3;
    static final int SLOT_X = 4;
    static final int SLOT_Y = 14;
    static final int SLOT = 18;
    static final int LINES_Y = 36;
    static final int GRID_TOP = 78;
    static final int GRID_HEIGHT = HEIGHT - GRID_TOP - 4;

    private static final String[] RUNE_ORDER = {"blank", "water", "fire", "earth", "air", "dusk", "dawn"};

    private RitualLayout() {
    }

    static int color(String type) {
        return switch (type) {
            case "water" -> 0xFF3C8DE0;
            case "fire" -> 0xFFE04A3C;
            case "earth" -> 0xFF4CAF50;
            case "air" -> 0xFFE8E8E8;
            case "dusk" -> 0xFF3A3A48;
            case "dawn" -> 0xFFE8B84A;
            default -> 0xFF8A8A8A;
        };
    }

    static ItemStack runeStack(String type, int count) {
        String path = switch (type) {
            case "water" -> "waterritualstone";
            case "fire" -> "fireritualstone";
            case "earth" -> "earthritualstone";
            case "air" -> "airritualstone";
            case "dusk" -> "duskritualstone";
            case "dawn" -> "lightritualstone";
            default -> "ritualstone";
        };
        return stack(path, count);
    }

    static ItemStack masterStone() {
        return stack("masterritualstone", 1);
    }

    static ItemStack crystal(int tier) {
        String path = switch (tier) {
            case 0 -> "activationcrystalweak";
            case 1 -> "activationcrystalawakened";
            default -> null;
        };
        return path == null ? ItemStack.EMPTY : stack(path, 1);
    }

    private static ItemStack stack(String path, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("bloodmagic", path));
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    static Map<String, Integer> counts(RitualData.Ritual ritual) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String type : RUNE_ORDER) {
            int n = 0;
            for (RitualData.Rune rune : ritual.runes()) {
                if (rune.type().equals(type)) {
                    n++;
                }
            }
            if (n > 0) {
                counts.put(type, n);
            }
        }
        return counts;
    }

    /** The items shown as slots along the top: master stone, activation crystal, then each rune with its count. */
    static List<ItemStack> slotStacks(RitualData.Ritual ritual) {
        List<ItemStack> stacks = new ArrayList<>();
        ItemStack master = masterStone();
        if (!master.isEmpty()) {
            stacks.add(master);
        }
        ItemStack crystal = crystal(ritual.crystal());
        if (!crystal.isEmpty()) {
            stacks.add(crystal);
        }
        for (Map.Entry<String, Integer> e : counts(ritual).entrySet()) {
            ItemStack rune = runeStack(e.getKey(), e.getValue());
            if (!rune.isEmpty()) {
                stacks.add(rune);
            }
        }
        return stacks;
    }

    static List<Component> lines(RitualData.Ritual ritual) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gtcoilemi.ritual.activation", String.format("%,d", ritual.activation()))
                .withStyle(ChatFormatting.RED));
        if (ritual.refreshCost() > 0 && ritual.refreshTime() > 0) {
            lines.add(Component.translatable("gtcoilemi.ritual.upkeep", String.format("%,d", ritual.refreshCost()),
                    ritual.refreshTime()).withStyle(ChatFormatting.DARK_RED));
        }
        lines.add(Component.translatable("gtcoilemi.ritual.crystal", ritual.crystal() + 1).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gtcoilemi.ritual.runes", ritual.runes().size()).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private static String runePath(String type) {
        return switch (type) {
            case "water" -> "waterritualstone";
            case "fire" -> "fireritualstone";
            case "earth" -> "earthritualstone";
            case "air" -> "airritualstone";
            case "dusk" -> "duskritualstone";
            case "dawn" -> "lightritualstone";
            case "master" -> "masterritualstone";
            default -> "ritualstone";
        };
    }

    private static BlockState stateOf(String type) {
        Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("bloodmagic", runePath(type)));
        if (block == null || block == Blocks.AIR) {
            block = Blocks.STONE;
        }
        return block.defaultBlockState();
    }

    static void paint(GuiGraphics g, RitualData.Ritual ritual) {
        Minecraft mc = Minecraft.getInstance();
        g.drawString(mc.font, Component.translatable(ritual.nameKey()).withStyle(ChatFormatting.GOLD), TEXT_X, TITLE_Y,
                0xFFFFFF, true);
        List<Component> lines = lines(ritual);
        int textX = SLOT_X + 2;
        int y = LINES_Y;
        for (Component line : lines) {
            g.drawString(mc.font, line, textX, y, 0xFFFFFF, true);
            y += 10;
        }
        paintBlocks(g, mc, ritual);
    }

    private static void paintBlocks(GuiGraphics g, Minecraft mc, RitualData.Ritual ritual) {
        int minX = 0;
        int maxX = 0;
        int minY = 0;
        int maxY = 0;
        int minZ = 0;
        int maxZ = 0;
        for (RitualData.Rune rune : ritual.runes()) {
            minX = Math.min(minX, rune.x());
            maxX = Math.max(maxX, rune.x());
            minY = Math.min(minY, rune.y());
            maxY = Math.max(maxY, rune.y());
            minZ = Math.min(minZ, rune.z());
            maxZ = Math.max(maxZ, rune.z());
        }
        float spanX = maxX - minX + 1;
        float spanZ = maxZ - minZ + 1;
        float spanY = maxY - minY + 1;
        float diag = (float) Math.sqrt(spanX * spanX + spanZ * spanZ);
        float scale = Math.min(16f, Math.min((WIDTH - 12) / diag, (GRID_HEIGHT - 6) / (diag * 0.5f + spanY)));
        float cx = (minX + maxX + 1) / 2f;
        float cy = (minY + maxY + 1) / 2f;
        float cz = (minZ + maxZ + 1) / 2f;
        float angle = (System.currentTimeMillis() % 24000L) / 24000f * 360f;

        PoseStack pose = g.pose();
        MultiBufferSource.BufferSource buffers = g.bufferSource();
        pose.pushPose();
        pose.translate(WIDTH / 2f, GRID_TOP + GRID_HEIGHT / 2f, 150f);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(30f));
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        List<Object[]> blocks = new ArrayList<>();
        for (RitualData.Rune rune : ritual.runes()) {
            blocks.add(new Object[]{rune.x(), rune.y(), rune.z(), rune.type()});
        }
        blocks.add(new Object[]{0, 0, 0, "master"});
        for (Object[] b : blocks) {
            pose.pushPose();
            pose.translate((Integer) b[0] - cx, (Integer) b[1] - cy, (Integer) b[2] - cz);
            mc.getBlockRenderer().renderSingleBlock(stateOf((String) b[3]), pose, buffers, 0xF000F0,
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        g.flush();
        pose.popPose();
        Lighting.setupFor3DItems();
    }

    static List<Component> tooltip(RitualData.Ritual ritual, double mouseX, double mouseY) {
        return List.of();
    }

    static List<Component> info(RitualData.Ritual ritual) {
        return List.of(Component.translatable(ritual.nameKey() + ".info").withStyle(ChatFormatting.GRAY));
    }
}
