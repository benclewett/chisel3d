package com.codecritical.build.mandelbrot;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.build.Builder;
import com.codecritical.lib.config.ConfigReader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class MandelbrotCubicBuilder {
    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;

    public MandelbrotCubicBuilder(ConfigReader config) {
        this.config = config;

        try {
            build();
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private void build() {
        var map = new MandelbrotCubicMap(config).getMap();

        Builder.create(config, map)
                .normalise()
                .scale()
                .showRoughMap()
                .buildPlateau()
                .reportPlateau()
                .applyPlateauTexture()
                .applyGaussian()
                .mapToCsg()
                .savePrint();
    }

}
