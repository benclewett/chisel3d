package com.codecritical;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.build.burningship.BurningShipBuilder;
import com.codecritical.build.gravitationalwaves.GravitationalWaveBuilder;
import com.codecritical.build.infinitemachine.InfiniteMachineBuilder;
import com.codecritical.build.mandelbrot.JuliaSetBuilder;
import com.codecritical.build.mandelbrot.MandelbrotBuddhaBuilder;
import com.codecritical.build.mandelbrot.MandelbrotVanillaBuilder;
import com.codecritical.build.mandelbrot.MandelbrotCubicBuilder;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.config.Config;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class Container {
    static final Logger logger = Logger.getLogger("");
    private static final String CONFIG_FILE = "config.default";

    private final ConfigReader config;
    private final Random random;

    public Container(Main.EModel model) {

        logger.info("Model: " + model);

        config = loadConfig();

        random = new Random(config.asLong(Config.RANDOM_SEED));

        switch (model) {
            case INFINITE_MACHINE -> new InfiniteMachineBuilder(config, random);
            case MANDELBROT -> new MandelbrotVanillaBuilder(config);
            case MANDELBROT_CUBIC -> new MandelbrotCubicBuilder(config);
            case MANDELBROT_BUDDHA -> new MandelbrotBuddhaBuilder(config);
            case GRAVITATIONAL_WAVES -> new GravitationalWaveBuilder(config);
            case BURNING_SHIP -> new BurningShipBuilder(config);
            case JULIA_SET -> new JuliaSetBuilder(config);
            default -> throw new IllegalStateException("Unexpected value: " + model);
        }
    }

    private ConfigReader loadConfig() {
        try {
            var properties = new Properties();
            properties.load(new FileInputStream(CONFIG_FILE));
            return new ConfigReader(properties);
        } catch (Exception ex) {
            logger.severe(String.format("%s", ex));
            throw new RuntimeException(ex);
        }
    }

}
