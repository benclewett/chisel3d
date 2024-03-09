package com.codecritical.build.gravitationalwaves;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */


import com.codecritical.build.Builder;
import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;

import java.util.logging.Logger;

public class GravitationalWaveBuilder {
    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;

    public GravitationalWaveBuilder(ConfigReader config) {
        this.config = config;

        try {
            build();
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private void build() {
        var map = new GravitationalWavesMap(config).getMap();

        double waveHeight = config.asDouble(Config.GravitationalWaves.WAVE_HEIGHT);

        Builder.create(config, map)
                .normalise(0.0, waveHeight)
                .showRoughMap()
                .mapToCsg()
                .addTwoGravitationalMass()
                .addBoundary()
                .savePrint();
    }

}
