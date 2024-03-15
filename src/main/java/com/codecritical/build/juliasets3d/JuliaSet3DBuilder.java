package com.codecritical.build.juliasets3d;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.build.Builder3D;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.model.JuliaSet3D;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class JuliaSet3DBuilder {
    static final Logger logger = Logger.getLogger("");

    private final ConfigReader config;

    public JuliaSet3DBuilder(ConfigReader config) {
        this.config = config;

        try {
            build();
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }

    }

    private void build() {

        var map = new MandelbrotStandard3dMap(config);

        Builder3D.create(config, map)
                .buildModel()
                .savePrint();
    }
}
