package com.gtcoilemi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

/**
 * EMI discovers this class through the {@link EmiEntrypoint} annotation on Forge.
 */
@EmiEntrypoint
public class CoilInfoPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        CoilConfig.load();
        CoilIndex.reset();
        registry.addRecipeDecorator(new CoilDecorator());
        if (CoilConfig.get().meteorTab) {
            java.util.List<MeteorData.Meteor> meteors = MeteorData.load(registry.getRecipeManager());
            if (!meteors.isEmpty()) {
                registry.addCategory(MeteorEmiRecipe.CATEGORY);
                registry.addWorkstation(MeteorEmiRecipe.CATEGORY, dev.emi.emi.api.stack.EmiStack.of(MeteorData.ritualStone()));
                for (MeteorData.Meteor meteor : meteors) {
                    registry.addRecipe(new MeteorEmiRecipe(meteor));
                }
            }
        }
        if (CoilConfig.get().ritualTab) {
            java.util.List<RitualData.Ritual> rituals = RitualData.load();
            if (!rituals.isEmpty()) {
                registry.addCategory(RitualEmiRecipe.CATEGORY);
                registry.addWorkstation(RitualEmiRecipe.CATEGORY, dev.emi.emi.api.stack.EmiStack.of(RitualLayout.masterStone()));
                for (RitualData.Ritual ritual : rituals) {
                    registry.addRecipe(new RitualEmiRecipe(ritual));
                }
            }
        }
        GTCoilEmi.LOGGER.info("[{}] recipe decorator registered", GTCoilEmi.MOD_ID);
    }
}
