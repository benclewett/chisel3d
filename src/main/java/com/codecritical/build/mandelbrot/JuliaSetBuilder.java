package com.codecritical.build.mandelbrot;

import com.codecritical.build.Builder;
import com.codecritical.lib.config.ConfigReader;

import java.util.logging.Logger;

public class JuliaSetBuilder {
    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;

    public JuliaSetBuilder(ConfigReader config) {
        this.config = config;

        try {
            build();
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private void build() {
        var map = new JuliaSetMap(config).getMap();

        Builder.create(config, map)
                .normalise()
                .scale()
                .applyLog()
                .showRoughMap()
                .buildPlateau()
                .reportPlateau()
                .applyPlateauTexture()
                .applyGaussian()
                // .trimOutsideBase()
                .addBoundary()
                .mapToCsg()
                .savePrint();
    }

}
