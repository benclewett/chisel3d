package com.codecritical.build.juliasets;

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
                .add("Config.JuliaSet.Model.MAX_ITERATIONS", MAX_ITERATIONS)
                .add("Config.JuliaSet.Model.I0", I0)
                .add("Config.JuliaSet.Model.I1", I1)
                .add("Config.JuliaSet.Model.J0", J0)
                .add("Config.JuliaSet.Model.J1", J1)
                .add("Config.JuliaSet.Model.I_COUNT", I_COUNT)
                .add("Config.JuliaSet.Model.J_COUNT", J_COUNT);

        var map = new MandelbrotStandardMap(config).getMap();

        StringBuilder sb = new StringBuilder();

        sb.append("JuliaSet:\r\n  +");
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
