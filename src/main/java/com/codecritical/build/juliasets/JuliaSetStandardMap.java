package com.codecritical.build.juliasets;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.model.JuliaSet;
import com.google.common.base.MoreObjects;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class JuliaSetStandardMap extends JuliaSet {

    final double rC;
    final double iC;

    protected JuliaSetStandardMap(ConfigReader config) {
        super(config, false);

        this.rC = config.asDouble(Config.Fractal.JuliaSet.RC);
        this.iC = config.asDouble(Config.Fractal.JuliaSet.IC);

        super.logger.info(this.toString());
        super.buildMap();
    }

    @Override
    protected Double buildPoint(double rZ, double iZ) {
        // With complex numbers: z = z^2 + c
        // Iterate until exit circle, or give up.

        int iterations = 0;

        double iZtmp;
        while (rZ * rZ + iZ * iZ <= 4 && iterations < maxIterations) {
            iZtmp = rZ * rZ - iZ * iZ + rC;
            iZ = 2 * rZ * iZ + iC;
            rZ = iZtmp;
            iterations++;
        }

        return (double)iterations;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(super.toString())
                .add("rC", this.rC)
                .add("iC", this.iC)
                .toString();
    }
}
