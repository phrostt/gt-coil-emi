package com.gtcoilemi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** One meteor recipe as an EMI page: ritual name, catalyst slot, blood cost. */
public class MeteorEmiRecipe implements EmiRecipe {

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            new ResourceLocation(GTCoilEmi.MOD_ID, "blood_magic_meteor"), EmiStack.of(MeteorData.ritualStone())) {
        @Override
        public Component getName() {
            return Component.translatable("gtcoilemi.meteor.title");
        }
    };

    private final MeteorData.Meteor meteor;
    private final ResourceLocation id;

    public MeteorEmiRecipe(MeteorData.Meteor meteor) {
        this.meteor = meteor;
        this.id = new ResourceLocation(GTCoilEmi.MOD_ID, "/meteor/" + meteor.id().getNamespace() + "/" + meteor.id().getPath());
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
        return List.of(EmiIngredient.of(meteor.catalyst()));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public int getDisplayWidth() {
        return MeteorLayout.WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return MeteorLayout.height(meteor);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addText(Component.translatable(MeteorData.RITUAL_NAME_KEY).withStyle(ChatFormatting.GOLD),
                MeteorLayout.TEXT_X, MeteorLayout.TITLE_Y, 0xFFFFFF, true);
        widgets.addSlot(EmiIngredient.of(meteor.catalyst()), MeteorLayout.SLOT_X, MeteorLayout.SLOT_Y);
        List<Component> lines = MeteorLayout.lines(meteor);
        for (int i = 0; i < lines.size(); i++) {
            widgets.addText(lines.get(i), MeteorLayout.TEXT_X + 24, MeteorLayout.LINES_Y + i * 10, 0xFFFFFF, true);
        }
        for (MeteorLayout.Label label : MeteorLayout.labels(meteor)) {
            widgets.addText(label.text(), label.x(), label.y(), 0xFFFFFF, true);
        }
        for (MeteorLayout.Cell cell : MeteorLayout.cells(meteor)) {
            var slot = widgets.addSlot(EmiIngredient.of(cell.entry().icon()), cell.x(), cell.y());
            for (Component line : MeteorLayout.tooltip(cell.entry())) {
                slot.appendTooltip(line);
            }
        }
    }
}
