package com.codecritical.build.mandelbrot;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.MapArray;
import com.codecritical.lib.model.Fractal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MandelbrotCubicMap extends Fractal {

    public MandelbrotCubicMap(ConfigReader config) {
        super(config);
    }

    @Override
    protected Double buildPoint(double rC, double iC) {

        // With complex numbers: z = z^3 + c
        // Iterate until exit circle, or give up.

        // z = z^3 + c
        // => (rZ + iZ)(rZ + iZ)(rZ + iZ) + rC + iC
        // => (rZ^2 + 2*rZ*iZ - [i]Z^2)(rZ + iZ) + rC + iC
        // => rZ^3 + 2*rZ^2*iZ - rZ*[i]Z^2 + rZ^2*iZ - 2*rZ*[i]Z^2 - iZ^3 + rC + iC
        // Real:       Zr = rZ^3 - 3*iZ^2*rZ + rC
        // Imaginary:  zi = 2*rZ^2*iZ + rZ^s*iZ - iZ^3 + iC

        int iterations = 0;

        double rZ = 0, iZ = 0, rZtmp;
        while (rZ * rZ + iZ * iZ <= 4 && iterations < maxIterations) {
            rZtmp = rZ * rZ * rZ - 3 * iZ * iZ * rZ + rC;
            iZ =  2 * rZ * rZ * iZ + rZ * rZ * iZ - iZ * iZ * iZ + iC;
            rZ = rZtmp;
            iterations++;
        }

        return (double)iterations;
    }

}
