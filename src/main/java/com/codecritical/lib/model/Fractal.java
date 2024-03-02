package com.codecritical.lib.model;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.mapping.MapArray;
import com.google.common.base.MoreObjects;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public abstract class Fractal {

    static final Logger logger = Logger.getLogger("");

    protected final int maxIterations;
    protected final double i0, i1, j0, j1, iScale, jScale;
    protected final int iCount, jCount;
    protected final double iDelta, jDelta;

    protected final MapArray map;
    protected final boolean polarCoordinates;


    protected Fractal(ConfigReader config) {

        double i0tmp = config.asDouble(Config.Fractal.Model.I0);
        double i1tmp = config.asDouble(Config.Fractal.Model.I1);
        double j0tmp = config.asDouble(Config.Fractal.Model.J0);
        double j1tmp = config.asDouble(Config.Fractal.Model.J1);
        logger.info(String.format("Config: i0=%.20f i1=%.20f j0=%.20f j1=%.20f", i0tmp, i1tmp, j0tmp, j1tmp));

        this.iScale = config.asDouble(Config.Fractal.Model.I_SCALE);
        this.jScale = config.asDouble(Config.Fractal.Model.J_SCALE);
        logger.info(String.format("Scale:  i=%f j=%f", iScale, jScale));

        double iLen = (i1tmp - i0tmp) * iScale;
        double iMid = (i1tmp + i0tmp) / 2;
        this.i0 = iMid - (iLen / 2);
        this.i1 = iMid + (iLen / 2);

        double jLen = (j1tmp - j0tmp) * jScale;
        double jMid = (j1tmp + j0tmp) / 2;
        this.j0 = jMid - (jLen / 2);
        this.j1 = jMid + (jLen / 2);

        logger.info(String.format("Width: i=%.20f j=%.20f", iLen, jLen));

        logger.info(String.format("Using: i0=%.20f i1=%.20f j0=%.20f j1=%.20f", i0, i1, j0, j1));

        this.maxIterations = config.asInt(Config.Fractal.Model.MAX_ITERATIONS);
        this.iCount = config.asInt(Config.Fractal.Model.I_COUNT);
        this.jCount = config.asInt(Config.Fractal.Model.J_COUNT);

        this.iDelta = (i1 - i0) / (double) iCount;
        this.jDelta = (j1 - j0) / (double) jCount;

        this.polarCoordinates = config.asBoolean(Config.Fractal.Model.POLAR_COORDINATES);

        logger.info(this.toString());

        this.map = new MapArray(iCount, jCount);

        buildMap();
    }

    private void buildMap() {
        for (int j = 0; j < jCount; j++) {
            for (int i = 0; i < iCount; i++) {
                double ii = i * iDelta + i0;
                double jj = j * jDelta + j0;
                map.set(i, j, buildPoint(
                        mapI(ii, jj),
                        mapJ(ii, jj))
                );
            }
        }
    }

    private double mapI(double i, double j) {
        if (polarCoordinates) {
            return Math.cos(i * Math.PI + Math.PI) * j;
        } else {
            return i;
        }
    }

    private double mapJ(double i, double j) {
        if (polarCoordinates) {
            return Math.sin(i * Math.PI + Math.PI) * j;
        } else {
            return j;
        }
    }

    protected abstract Double buildPoint(double i, double j);

    public IMapArray getMap() {
        return map;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxIterations", maxIterations)
                .add("i0", i0)
                .add("i1", i1)
                .add("j0", j0)
                .add("j1", j1)
                .add("iCount", iCount)
                .add("jCount", jCount)
                .add("polarCoordinates", polarCoordinates)
                .toString();
    }
}