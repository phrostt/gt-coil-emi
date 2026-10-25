package com.gtcoilemi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One Blood Magic ritual as an EMI page: name, costs, required stones, and the rune layout. */
public class RitualEmiRecipe implements EmiRecipe {

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            new ResourceLocation(GTCoilEmi.MOD_ID, "blood_magic_rituals"), EmiStack.of(RitualLayout.masterStone())) {
        @Override
        public Component getName() {
            return Component.translatable("gtcoilemi.ritual.title");
        }
    };

    private final RitualData.Ritual ritual;
    private final ResourceLocation id;

    public RitualEmiRecipe(RitualData.Ritual ritual) {
        this.ritual = ritual;
        this.id = new ResourceLocation(GTCoilEmi.MOD_ID, "/ritual/" + ritual.id().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        List<EmiIngredient> inputs = new ArrayList<>();
        for (ItemStack stack : RitualLayout.slotStacks(ritual)) {
            inputs.add(EmiStack.of(stack));
        }
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return RitualLayout.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return RitualLayout.HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, RitualLayout.WIDTH, RitualLayout.HEIGHT, (ctx, mouseX, mouseY, delta) -> paint(ctx));
        int x = RitualLayout.SLOT_X;
        for (ItemStack stack : RitualLayout.slotStacks(ritual)) {
            widgets.addSlot(EmiStack.of(stack), x, RitualLayout.SLOT_Y);
            x += RitualLayout.SLOT;
        }
        widgets.addTooltipText(RitualLayout.info(ritual), 0, 0, RitualLayout.WIDTH, 12);
    }

    private void paint(Object ctx) {
        GuiGraphics g;
        if (ctx instanceof GuiGraphics direct) {
            g = direct;
        } else {
            try {
                g = (GuiGraphics) ctx.getClass().getMethod("raw").invoke(ctx);
            } catch (ReflectiveOperationException e) {
                return;
            }
        }
        RitualLayout.paint(g, ritual);
    }
}
