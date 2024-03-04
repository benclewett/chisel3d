package com.codecritical.build.mandelbrot;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.ConfigReader;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

class BasicMandelbrotTest {

    static final Logger logger = Logger.getLogger("");

    static final int I_COUNT = 80;
    static final int J_COUNT = 20;
    static final int MAX_ITERATIONS = 100;
    static final double I0 = -2.1;
    static final double I1 = 0.6;
    static final double J0 = -1.0;
    static final double J1 = 1.0;

    @Test
    void test() {

        ConfigReader config = new ConfigReader()
                .add("Config.Fractal.Model.MAX_ITERATIONS", MAX_ITERATIONS)
                .add("Config.Fractal.Model.I0", I0)
                .add("Config.Fractal.Model.I1", I1)
                .add("Config.Fractal.Model.J0", J0)
                .add("Config.Fractal.Model.J1", J1)
                .add("Config.Fractal.Model.I_COUNT", I_COUNT)
                .add("Config.Fractal.Model.J_COUNT", J_COUNT);

        var map = new MandelbrotVanillaMap(config).getMap();

        StringBuilder sb = new StringBuilder();

        sb.append("Fractal:\r\n  +");
        for (int i = 0; i < map.getISize(); i++) {
            sb.append("-");
        }
        sb.append("+");
        for (int j = 0; j < map.getJSize(); j++) {
            sb.append("\r\n  |");
            for (int i = 0; i < map.getISize(); i++) {
                sb.append((map.get(i, j) == MAX_ITERATIONS) ? "*" : " ");
            }
            sb.append("|");
        }
        sb.append("\r\n  +");
        for (int i = 0; i < map.getISize(); i++) {
            sb.append("-");
        }
        sb.append("+");

        logger.info(sb.toString());
    }
}
