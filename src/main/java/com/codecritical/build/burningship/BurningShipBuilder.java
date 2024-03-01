package com.codecritical.build.burningship;

import com.codecritical.build.Builder;
import com.codecritical.build.mandelbrot.MandelbrotMap;
import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

@ParametersAreNonnullByDefault
public class BurningShipBuilder {

    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;

    public BurningShipBuilder(ConfigReader config) {
        this.config = config;

        try {
            build();
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private void build() {
        var map = new BurningShipMap(config).getMap();

        Builder.create(config, map)
                .normalise()
                .scale()
                .showRoughMap()
                .buildPlateau()
                .reportPlateau()
                .applyGaussian()
                .mapToCsg()
                .savePrint(config.asString(Config.Fractal.OUTPUT_FILENAME));
    }

}
