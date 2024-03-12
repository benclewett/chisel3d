package com.codecritical.build.juliasets;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */


import com.codecritical.Main;
import com.codecritical.build.Builder2D;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.IMapArray;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class JuliaSetsBuilder {

    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;

    public JuliaSetsBuilder(ConfigReader config, Main.ModelName model) {
        this.config = config;

        try {
            IMapArray map = buildModel(model);

            build(map);
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private IMapArray buildModel(Main.ModelName model) {
        return switch (model) {
            case MANDELBROT -> new MandelbrotStandardMap(config).getMap();
            case MANDELBROT_CUBIC -> new MandelbrotCubicMap(config).getMap();
            case MANDELBROT_BUDDHA -> new MandelbrotBuddhaMap(config).getMap();
            case JULIA_SET -> new JuliaSetStandardMap(config).getMap();
            case BURNING_SHIP -> new BurningShipMap(config).getMap();
            default -> throw new RuntimeException("Unknown option: " + model);
        };
    }

    private void build(IMapArray map) {

        Builder2D.create(config, map)
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
