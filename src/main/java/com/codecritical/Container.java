package com.codecritical;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.build.gravitationalwaves.GravitationalWaveBuilder;
import com.codecritical.build.infinitemachine.InfiniteMachineBuilder;
import com.codecritical.build.juliasets.JuliaSetsBuilder;
import com.codecritical.build.juliasets3d.JuliaSet3DBuilder;
import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;

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

    public Container(Main.ModelName model) {

        logger.info("Model: " + model);

        config = loadConfig();

        random = new Random(config.asLong(Config.RANDOM_SEED));

        switch (model) {
            case INFINITE_MACHINE -> new InfiniteMachineBuilder(config, random);
            case MANDELBROT, MANDELBROT_CUBIC, MANDELBROT_BUDDHA, JULIA_SET, BURNING_SHIP, MANDELBROT_TRICORN -> new JuliaSetsBuilder(config, model);
            case GRAVITATIONAL_WAVES -> new GravitationalWaveBuilder(config);
            case MANDELBROT_3D -> new JuliaSet3DBuilder(config);
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
