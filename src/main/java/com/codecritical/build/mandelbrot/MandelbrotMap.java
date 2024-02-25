package com.codecritical.build.mandelbrot;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.MapArray;
import com.google.common.base.MoreObjects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class MandelbrotMap {
    static final Logger logger = Logger.getLogger("");

    private final int maxIterations;
    private final double i0, i1, j0, j1;
    private final int iCount, jCount;
    private final double iDelta, jDelta;
    MapArray map;

    public MandelbrotMap(ConfigReader config) {
        this.maxIterations = config.asInt(Config.Mandelbrot.Model.MAX_ITERATIONS);
        this.i0 = config.asDouble(Config.Mandelbrot.Model.I0);
        this.i1 = config.asDouble(Config.Mandelbrot.Model.I1);
        this.j0 = config.asDouble(Config.Mandelbrot.Model.J0);
        this.j1 = config.asDouble(Config.Mandelbrot.Model.J1);
        this.iCount = config.asInt(Config.Mandelbrot.Model.I_COUNT);
        this.jCount = config.asInt(Config.Mandelbrot.Model.J_COUNT);

        this.iDelta = (i1 - i0) / (double)iCount;
        this.jDelta = (j1 - j0) / (double)jCount;

        this.map = new MapArray(iCount, jCount);

        logger.info(this.toString());

        buildMap();
    }

    private void buildMap() {
        for (int j = 0; j < jCount; j++) {
            for (int i = 0; i < iCount; i++) {
                map.set(i, j, buildPoint(
                        i * iDelta + i0,
                        j * jDelta + j0)
                );
            }
        }
    }

    private Double buildPoint(double imaginaryC, double realC) {

        // With complex numbers: z = z^2 + c
        // Iterate until exit circle, or give up.

        int iterations = 0;

        double imaginaryZ = 0, realZ = 0;
        while (imaginaryZ * imaginaryZ + realZ * realZ <= 4 && iterations < maxIterations) {
            double imaginaryZTmp = imaginaryZ * imaginaryZ - realZ * realZ + imaginaryC;
            realZ = 2 * imaginaryZ * realZ + realC;
            imaginaryZ = imaginaryZTmp;
            iterations++;
        }

        return (double)iterations;
    }

    public IMapArray getMap() {
        return map;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxIterations", maxIterations)
                .add("i0", i0)
                .add("i1", i1)
                .add("j0", j0)
                .add("j1", j1)
                .add("iCount", iCount)
                .add("jCount", jCount)
                .toString();
    }
}
