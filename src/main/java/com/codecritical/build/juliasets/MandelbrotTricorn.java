package com.codecritical.build.juliasets;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.model.JuliaSet;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class MandelbrotTricorn extends JuliaSet {
    public MandelbrotTricorn(ConfigReader config) {
        super(config, true);
    }

    @Override
    protected Double buildPoint(double rC, double iC) {

        // With complex numbers: z = z^2 + c
        // Iterate until exit circle, or give up.

        int iterations = 0;

        double rZ = 0, iZ = 0, iZtmp;
        while (rZ * rZ + iZ * iZ <= 4 && iterations < maxIterations) {
            iZtmp = rZ * rZ - iZ * iZ + rC;
            iZ = -2 * rZ * iZ + iC;
            rZ = iZtmp;
            iterations++;
        }

        return (double)iterations;
    }

}
