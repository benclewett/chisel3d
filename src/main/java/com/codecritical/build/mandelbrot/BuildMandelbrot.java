package com.codecritical.build.mandelbrot;

import com.codecritical.build.lib.IMapArray;
import com.codecritical.build.lib.config.Config;
import com.codecritical.build.lib.config.ConfigReader;
import com.codecritical.build.lib.MapArray;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class BuildMandelbrot {
    static final Logger logger = Logger.getLogger("");

    private final int maxIterations;
    private final double i0, i1, j0, j1;
    private final int iCount, jCount;
    private final double iDelta, jDelta;
    MapArray map;

    public BuildMandelbrot(ConfigReader config) {
        this.maxIterations = config.asInt(Config.Mandelbrot.MAX_ITERATIONS);
        this.i0 = config.asDouble(Config.Mandelbrot.I0);
        this.i1 = config.asDouble(Config.Mandelbrot.I1);
        this.j0 = config.asDouble(Config.Mandelbrot.J0);
        this.j1 = config.asDouble(Config.Mandelbrot.J1);
        this.iCount = config.asInt(Config.Mandelbrot.I_COUNT);
        this.jCount = config.asInt(Config.Mandelbrot.J_COUNT);

        logger.info("MaxIterations: " + maxIterations);
        logger.info("i0=" + i0 + ", i1=" + i1);
        logger.info("j0=" + j0 + ", j1=" + j1);
        logger.info("iCount=" + iCount + ", jCount=" + jCount);

        this.iDelta = (i1 - i0) / (double)iCount;
        this.jDelta = (j1 - j0) / (double)jCount;

        this.map = new MapArray(iCount, jCount);

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
}
