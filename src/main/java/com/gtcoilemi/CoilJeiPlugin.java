package com.gtcoilemi;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.category.extensions.IRecipeCategoryDecorator;
import mezz.jei.api.registration.IAdvancedRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JEI side: the same coil and research info the EMI decorator draws, for packs
 * that run JEI instead of EMI. Registered only when EMI is not loaded, since
 * with both installed EMI owns the recipe screen and JEI's would never show.
 */
@JeiPlugin
public class CoilJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(GTCoilEmi.MOD_ID, "jei");

    private static final RecipeType<MeteorData.Meteor> METEOR_TYPE =
            RecipeType.create(GTCoilEmi.MOD_ID, "blood_magic_meteor", MeteorData.Meteor.class);

    private static final RecipeType<RitualData.Ritual> RITUAL_TYPE =
            RecipeType.create(GTCoilEmi.MOD_ID, "blood_magic_rituals", RitualData.Ritual.class);

    private static boolean ritualTabActive() {
        CoilConfig config = CoilConfig.get();
        return config.ritualTab && config.jeiEnabled && !ModList.get().isLoaded("emi");
    }

    private static boolean meteorTabActive() {
        CoilConfig config = CoilConfig.get();
        return config.meteorTab && config.jeiEnabled && !ModList.get().isLoaded("emi");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (meteorTabActive()) {
            registration.addRecipeCategories(new MeteorCategory(registration.getJeiHelpers().getGuiHelper()));
        }
        if (ritualTabActive()) {
            registration.addRecipeCategories(new RitualCategory(registration.getJeiHelpers().getGuiHelper()));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (ritualTabActive()) {
            List<RitualData.Ritual> rituals = RitualData.load();
            if (!rituals.isEmpty()) {
                registration.addRecipes(RITUAL_TYPE, rituals);
            }
        }
        if (!meteorTabActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        List<MeteorData.Meteor> meteors = MeteorData.load(mc.level.getRecipeManager());
        if (!meteors.isEmpty()) {
            registration.addRecipes(METEOR_TYPE, meteors);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (ritualTabActive() && !RitualLayout.masterStone().isEmpty()) {
            registration.addRecipeCatalyst(RitualLayout.masterStone(), RITUAL_TYPE);
        }
        if (meteorTabActive()) {
            registration.addRecipeCatalyst(MeteorData.ritualStone(), METEOR_TYPE);
        }
    }

    private static final class RitualCategory implements IRecipeCategory<RitualData.Ritual> {

        private final IGuiHelper helper;
        private final IDrawable background;
        private final IDrawable icon;

        private RitualCategory(IGuiHelper helper) {
            this.helper = helper;
            this.background = helper.createBlankDrawable(RitualLayout.WIDTH, RitualLayout.HEIGHT);
            ItemStack iconStack = RitualLayout.masterStone();
            this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                    iconStack.isEmpty() ? MeteorData.ritualStone() : iconStack);
        }

        @Override
        public RecipeType<RitualData.Ritual> getRecipeType() {
            return RITUAL_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gtcoilemi.ritual.title");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RitualData.Ritual recipe, IFocusGroup focuses) {
            int x = RitualLayout.SLOT_X;
            for (ItemStack stack : RitualLayout.slotStacks(recipe)) {
                builder.addSlot(RecipeIngredientRole.INPUT, x + 1, RitualLayout.SLOT_Y + 1)
                        .setBackground(helper.getSlotDrawable(), -1, -1)
                        .addItemStack(stack);
                x += RitualLayout.SLOT;
            }
        }

        @Override
        public void draw(RitualData.Ritual recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                         double mouseX, double mouseY) {
            RitualLayout.paint(guiGraphics, recipe);
        }

        @Override
        public List<Component> getTooltipStrings(RitualData.Ritual recipe, IRecipeSlotsView recipeSlotsView,
                                                 double mouseX, double mouseY) {
            if (mouseY < RitualLayout.SLOT_Y - 2) {
                return RitualLayout.info(recipe);
            }
            return RitualLayout.tooltip(recipe, mouseX, mouseY);
        }
    }

    private static final class MeteorCategory implements IRecipeCategory<MeteorData.Meteor> {

        private final IGuiHelper helper;
        private final IDrawable background;
        private final IDrawable icon;

        private MeteorCategory(IGuiHelper helper) {
            this.helper = helper;
            this.background = helper.createBlankDrawable(MeteorLayout.WIDTH, MeteorLayout.JEI_HEIGHT);
            this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, MeteorData.ritualStone());
        }

        @Override
        public RecipeType<MeteorData.Meteor> getRecipeType() {
            return METEOR_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gtcoilemi.meteor.title");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MeteorData.Meteor recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, MeteorLayout.SLOT_X + 1, MeteorLayout.SLOT_Y + 1)
                    .setBackground(helper.getSlotDrawable(), -1, -1)
                    .addIngredients(recipe.catalyst());
            for (MeteorLayout.Cell cell : MeteorLayout.cells(recipe)) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, cell.x() + 1, cell.y() + 1)
                        .setBackground(helper.getSlotDrawable(), -1, -1)
                        .addIngredients(cell.entry().icon())
                        .addTooltipCallback((view, tooltip) -> tooltip.addAll(MeteorLayout.tooltip(cell.entry())));
            }
        }

        @Override
        public void draw(MeteorData.Meteor recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                         double mouseX, double mouseY) {
            Minecraft mc = Minecraft.getInstance();
            guiGraphics.drawString(mc.font,
                    Component.translatable(MeteorData.RITUAL_NAME_KEY).withStyle(ChatFormatting.GOLD),
                    MeteorLayout.TEXT_X, MeteorLayout.TITLE_Y, 0xFFFFFF, true);
            List<Component> lines = MeteorLayout.lines(recipe);
            for (int i = 0; i < lines.size(); i++) {
                guiGraphics.drawString(mc.font, lines.get(i), MeteorLayout.TEXT_X + 24,
                        MeteorLayout.LINES_Y + i * 10, 0xFFFFFF, true);
            }
            for (MeteorLayout.Label label : MeteorLayout.labels(recipe)) {
                guiGraphics.drawString(mc.font, label.text(), label.x(), label.y(), 0xFFFFFF, true);
            }
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void registerAdvanced(IAdvancedRegistration registration) {
        CoilConfig config = CoilConfig.get();
        if (!config.enabled || !config.jeiEnabled) {
            return;
        }
        if (ModList.get().isLoaded("emi")) {
            GTCoilEmi.LOGGER.info("[{}] EMI is loaded, skipping JEI decorators", GTCoilEmi.MOD_ID);
            return;
        }
        CoilIndex.reset();
        for (java.lang.reflect.Method m : IRecipeCategoryDecorator.class.getMethods()) {
            GTCoilEmi.LOGGER.info("[{}] JEI decorator method: {}", GTCoilEmi.MOD_ID, m);
        }

        Class<?> recipeClass;
        try {
            recipeClass = Class.forName("com.gregtechceu.gtceu.api.recipe.GTRecipe");
        } catch (ClassNotFoundException e) {
            GTCoilEmi.LOGGER.info("[{}] GregTech not present, nothing to decorate in JEI", GTCoilEmi.MOD_ID);
            return;
        }

        Set<ResourceLocation> types = new LinkedHashSet<>(GtRecipeTypes.discover());
        for (String id : config.jeiRecipeTypes) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                types.add(rl);
            }
        }

        int count = 0;
        for (ResourceLocation id : types) {
            RecipeType type = RecipeType.create(id.getNamespace(), id.getPath(), (Class) recipeClass);
            registration.addRecipeCategoryDecorator(type, new Decorator(id));
            count++;
        }
        GTCoilEmi.LOGGER.info("[{}] JEI decorators registered for {} GregTech recipe types", GTCoilEmi.MOD_ID, count);
    }

    private static final class Decorator implements IRecipeCategoryDecorator<Object> {

        private final ResourceLocation category;
        private int cardHeight;
        private int cardWidth;

        private Decorator(ResourceLocation category) {
            this.category = category;
        }

        private CoilInfo.Result resolve(Object recipe) {
            return CoilInfo.resolve(CoilConfig.get(), category, recipe);
        }

        @Override
        public void draw(Object recipe, IRecipeCategory<Object> recipeCategory, IRecipeSlotsView recipeSlotsView,
                         GuiGraphics guiGraphics, double mouseX, double mouseY) {
            CoilConfig config = CoilConfig.get();
            CoilInfo.Result info = resolve(recipe);
            if (info == null) {
                return;
            }
            cardHeight = recipeCategory.getHeight();
            cardWidth = recipeCategory.getWidth();
            CoilInfo.Layout layout = CoilInfo.layout(config, cardWidth, cardHeight);
            Minecraft mc = Minecraft.getInstance();

            boolean slotted = CoilInfo.isSlotted(recipe);
            if (info.hasCoil()) {
                if (!slotted) {
                    List<CoilIndex.Coil> options = CoilIndex.allMatching(info.mode(), info.required());
                    ItemStack icon = options.isEmpty() ? info.coil().stack()
                            : options.get((int) ((System.currentTimeMillis() / 1000) % options.size())).stack();
                    guiGraphics.renderItem(icon, layout.iconX() + 1, layout.iconY() + 1);
                }
            } else if (info.hasTarget()) {
                if (!slotted) {
                    guiGraphics.renderItem(info.research(), layout.dataX() + 1, layout.iconY() + 1);
                    guiGraphics.renderItem(info.target(), layout.iconX() + 1, layout.iconY() + 1);
                }
                drawArrow(guiGraphics, layout.arrowX(), layout.arrowY());
            } else if (!slotted) {
                guiGraphics.renderItem(info.research(), layout.iconX() + 1, layout.iconY() + 1);
            }

            if (info.hasCoil() && config.showText) {
                Component tempLine = Component.translatable("gtcoilemi.text." + info.mode(),
                        String.format("%,d", info.required()));
                Component coilLine = Component.translatable("gtcoilemi.text.coil", info.coil().name().getString());
                guiGraphics.drawString(mc.font, tempLine, layout.textX(), layout.textY1(), 0xFFFFFF, true);
                guiGraphics.drawString(mc.font, coilLine, layout.textX(), layout.textY2(), 0xFFFFFF, true);
            }
        }

        @Override
        public void decorateTooltips(ITooltipBuilder tooltip, Object recipe, IRecipeCategory<Object> recipeCategory,
                                     IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
            cardHeight = recipeCategory.getHeight();
            cardWidth = recipeCategory.getWidth();
            List<Component> lines = CoilInfo.isSlotted(recipe) ? List.of() : tooltipFor(recipe, mouseX, mouseY);
            if (!lines.isEmpty()) {
                tooltip.addAll(lines);
            }
        }

        private static void drawArrow(GuiGraphics g, int x, int y) {
            int color = 0xFF9A9A9A;
            g.fill(x, y + 3, x + 10, y + 5, color);
            int[] heights = {9, 7, 5, 3, 1};
            for (int c = 0; c < heights.length; c++) {
                int h = heights[c];
                int top = y + 4 - h / 2;
                g.fill(x + 10 + c, top, x + 11 + c, top + h, color);
            }
        }

        private List<Component> tooltipFor(Object recipe, double mouseX, double mouseY) {
            if (cardHeight <= 0) {
                return List.of();
            }
            CoilConfig config = CoilConfig.get();
            CoilInfo.Layout layout = CoilInfo.layout(config, cardWidth, cardHeight);
            if (mouseX < layout.iconX() || mouseX >= layout.iconX() + CoilInfo.SLOT_SIZE
                    || mouseY < layout.iconY() || mouseY >= layout.iconY() + CoilInfo.SLOT_SIZE) {
                return List.of();
            }
            CoilInfo.Result info = resolve(recipe);
            if (info == null) {
                return List.of();
            }
            List<Component> lines = new ArrayList<>();
            if (info.hasCoil()) {
                lines.add(Component.translatable("gtcoilemi.tooltip.requires", info.coil().name())
                        .withStyle(ChatFormatting.GOLD));
                lines.add(Component.translatable("gtcoilemi.tooltip." + info.mode(), info.required())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("gtcoilemi.tooltip.research").withStyle(ChatFormatting.GOLD));
                lines.add(info.research().getHoverName().copy().withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }
    }
}
