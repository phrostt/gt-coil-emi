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
        GTCoilEmi.LOGGER.info("[{}] recipe decorator registered", GTCoilEmi.MOD_ID);
    }
}
