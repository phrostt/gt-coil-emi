package com.gtcoilemi;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Client-only helper that shows which GregTech heating coil a recipe needs,
 * drawn on top of GregTech's own EMI recipe categories.
 */
@Mod(GTCoilEmi.MOD_ID)
public class GTCoilEmi {

    public static final String MOD_ID = "gtcoilemi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GTCoilEmi() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            LOGGER.info("[{}] loaded, waiting for EMI", MOD_ID);
        }
    }
}
