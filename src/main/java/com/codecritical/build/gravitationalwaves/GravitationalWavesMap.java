package com.codecritical.build.gravitationalwaves;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.mapping.MapArray;
import com.google.common.base.MoreObjects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalDouble;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class GravitationalWavesMap {
    static final Logger logger = Logger.getLogger("");

    private final double i0, i1, j0, j1;
    private final double iCentre, jCentre;
    private final int iCount, jCount;
    private final double spiralDegreesOffset;
    private final double kWave;
    private final OptionalDouble waveFadeInWidth;
    private final double waveRidgeCountInXAxis;
    private final double iDelta, jDelta;

    final IMapArray map;

    public GravitationalWavesMap(ConfigReader config) {
        this.i0 = config.asDouble(Config.GravitationalWaves.I0);
        this.i1 = config.asDouble(Config.GravitationalWaves.I1);
        this.j0 = config.asDouble(Config.GravitationalWaves.J0);
        this.j1 = config.asDouble(Config.GravitationalWaves.J1);
        this.iCount = config.asInt(Config.GravitationalWaves.I_COUNT);
        this.jCount = config.asInt(Config.GravitationalWaves.J_COUNT);
        this.spiralDegreesOffset = config.asDouble(Config.GravitationalWaves.SPIRAL_DEGREES_OFFSET);
        this.waveFadeInWidth = config.asOptionalDouble(Config.GravitationalWaves.WAVE_FADE_IN_WIDTH);
        this.waveRidgeCountInXAxis = config.asDouble(Config.GravitationalWaves.WAVE_RIDGE_COUNT_IN_X_AXIS);

        this.iCentre = (i1 + i0) / 2;
        this.jCentre = (j1 + j0) / 2;
        this.kWave = Math.PI * 2 / iCentre * waveRidgeCountInXAxis; // Waves per (centre -> XMax)

        this.iDelta = (i1 - i0) / (double)iCount;
        this.jDelta = (j1 - j0) / (double)jCount;

        this.map = buildMap();
    }

    public IMapArray buildMap() {
        MapArray mapNew = new MapArray(iCount, jCount);
        for (int j = 0; j < jCount; j++) {
            for (int i = 0; i < iCount; i++) {
                mapNew.set(i, j, getPoint(
                        i * iDelta + i0,
                        j * jDelta + j0)
                );
            }
        }
        return mapNew;
    }

    public double getPoint(double i, double j) {

        double iOffset = i - iCentre;
        double jOffset = j - jCentre;

        double alpha = Math.atan2(iOffset, jOffset) + spiralDegreesOffset;
        double radius = Math.sqrt(iOffset * iOffset + jOffset * jOffset);

        double depthCo = (Math.cos(alpha * 2 + radius * kWave) / 2) + 0.5; // x2 for two spirals.  Range 0 -> 1

        double centreSlope, centreOffset;
        if (waveFadeInWidth.isEmpty() || radius > waveFadeInWidth.getAsDouble()) {
            centreSlope = 1;
            centreOffset = 0;
        } else {
            centreSlope = radius / waveFadeInWidth.getAsDouble();       // 1 (outside) to 0 (centre)
            centreOffset = (1 - centreSlope) * -1.0;        // 0 (outside) to -zRange (centre)
        }

        depthCo = depthCo * centreSlope + 1.0 - centreOffset;    // Range top -> bottom

        return depthCo;
    }

    public IMapArray getMap() {
        return map;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("i0", i0)
                .add("i1", i1)
                .add("j0", j0)
                .add("j1", j1)
                .add("iCount", iCount)
                .add("jCount", jCount)
                .toString();
    }
}
