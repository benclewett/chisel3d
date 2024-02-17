package com.codecritical;

import com.codecritical.build.infinitemachine.InfiniteMachineBuilder;
import com.codecritical.build.mandelbrot.Builder;
import com.codecritical.build.lib.config.ConfigReader;
import com.codecritical.build.lib.config.Config;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

public class Container {
    static final Logger logger = Logger.getLogger("");
    private static final String CONFIG_FILE = "config.default";

    private final ConfigReader config;
    private final Random random;

    public Container(Main.EModel model) {

        config = loadConfig();

        random = new Random(config.asLong(Config.RANDOM_SEED));

        switch (model) {
            case INFINITE_MACHINE -> {
                new InfiniteMachineBuilder(config, random);
            }
            case MANDELBROT -> {
                new Builder(config);
            }
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
