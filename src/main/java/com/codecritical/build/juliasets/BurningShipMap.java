package com.codecritical.build.juliasets;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.model.JuliaSet;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class BurningShipMap extends JuliaSet {

    public BurningShipMap(ConfigReader config) {
        super(config, true);
    }

    @Override
    protected Double buildPoint(double rC, double iC) {
        int iterations = 0;

        double iZ = 0, jZ = 0, iZtmp;
        while (iZ * iZ + jZ * jZ <= 4 && iterations < maxIterations) {
            iZtmp = iZ*iZ - jZ*jZ + rC;
            jZ = Math.abs(2 * iZ * jZ) + iC;
            iZ = iZtmp;
            iterations++;
        }

        return (double)iterations;
    }
}
