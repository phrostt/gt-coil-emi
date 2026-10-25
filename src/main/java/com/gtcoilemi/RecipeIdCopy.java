package com.gtcoilemi;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * Pack-dev helper: shift + left click on a recipe's output item in the JEI recipe screen
 * copies the recipe id to the clipboard, for every recipe category. Holds no JEI types, so it is
 * safe to load without JEI; the JEI lookup lives in {@link JeiRecipeIdLookup}.
 */
public final class RecipeIdCopy {

    private static final String JEI_RECIPES_GUI = "mezz.jei.gui.recipes.RecipesGui";

    private RecipeIdCopy() {
    }

    static final KeyMapping COPY_KEY = new KeyMapping("key.gtcoilemi.copy_recipe_id", net.minecraftforge.client.settings.KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_I, "key.categories.gtcoilemi");

    static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(COPY_KEY);
    }

    static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!CoilConfig.get().copyRecipeId) {
            return;
        }
        Screen screen = event.getScreen();
        if (!JEI_RECIPES_GUI.equals(screen.getClass().getName())) {
            return;
        }
        if (!COPY_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        String id;
        try {
            id = JeiRecipeIdLookup.findAny(screen, x, y);
        } catch (Throwable t) {
            GTCoilEmi.LOGGER.warn("[{}] recipe id lookup failed", GTCoilEmi.MOD_ID, t);
            return;
        }
        if (id == null || id.isEmpty()) {
            GTCoilEmi.LOGGER.info("[{}] copy key pressed, but the mouse is not over a recipe", GTCoilEmi.MOD_ID);
            return;
        }
        mc.keyboardHandler.setClipboard(id);
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable("gtcoilemi.copied_recipe_id", id));
        }
        event.setCanceled(true);
    }

    static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true,
                ScreenEvent.KeyPressed.Pre.class, RecipeIdCopy::onKey);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true,
                ScreenEvent.MouseButtonPressed.Pre.class, RecipeIdCopy::onClick);
        GTCoilEmi.LOGGER.info("[{}] shift-click recipe id copy registered", GTCoilEmi.MOD_ID);
    }

    static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !Screen.hasShiftDown()) {
            return;
        }
        if (!CoilConfig.get().copyRecipeId) {
            GTCoilEmi.LOGGER.info("[{}] shift-click ignored, copyRecipeId is off in the config", GTCoilEmi.MOD_ID);
            return;
        }
        Screen screen = event.getScreen();
        GTCoilEmi.LOGGER.info("[{}] shift-click on screen {}", GTCoilEmi.MOD_ID, screen.getClass().getName());
        if (!JEI_RECIPES_GUI.equals(screen.getClass().getName())) {
            return;
        }
        String id;
        GTCoilEmi.LOGGER.info("[{}] shift-click on JEI recipe screen at {}, {}", GTCoilEmi.MOD_ID, event.getMouseX(), event.getMouseY());
        try {
            id = JeiRecipeIdLookup.find(screen, event.getMouseX(), event.getMouseY());
        } catch (Throwable t) {
            GTCoilEmi.LOGGER.warn("[{}] recipe id lookup failed", GTCoilEmi.MOD_ID, t);
            return;
        }
        if (id == null || id.isEmpty()) {
            GTCoilEmi.LOGGER.info("[{}] no output slot with a recipe id under the mouse", GTCoilEmi.MOD_ID);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.keyboardHandler.setClipboard(id);
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable("gtcoilemi.copied_recipe_id", id));
        }
        event.setCanceled(true);
    }
}
