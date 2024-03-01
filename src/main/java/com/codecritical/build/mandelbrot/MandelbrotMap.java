package com.codecritical.build.mandelbrot;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.model.Fractal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MandelbrotMap extends Fractal {

    public MandelbrotMap(ConfigReader config) {
        super(config);
    }

    @Override
    protected Double buildPoint(double imaginaryC, double realC) {

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
}
