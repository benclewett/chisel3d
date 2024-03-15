package com.codecritical.lib.model;

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import eu.printingin3d.javascad.coords.Coords3d;

import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class JuliaSet3D {
    protected static final Logger logger = Logger.getLogger("");

    protected final int maxIterations;
    protected final double i0, i1, j0, j1, k0, k1;
    protected final double iScale, jScale, iShift, jShift, kScale, kShift;
    protected final int iCount, jCount, kCount;
    protected final double iDelta, jDelta, kDelta;

    protected ImmutableList<Coords3d> map;
    protected ImmutableSet<MapPoint3D> pointMap;


    protected JuliaSet3D(ConfigReader config) {
        double i0tmp = config.asDouble(Config.Fractal.Model.I0);
        double i1tmp = config.asDouble(Config.Fractal.Model.I1);
        double j0tmp = config.asDouble(Config.Fractal.Model.J0);
        double j1tmp = config.asDouble(Config.Fractal.Model.J1);
        double k0tmp = config.asDouble(Config.Fractal.Model.K0);
        double k1tmp = config.asDouble(Config.Fractal.Model.K1);
        logger.info(String.format("Config: i0=%.20f i1=%.20f j0=%.20f j1=%.20f k0=%.20f k1=%.20f",
                i0tmp, i1tmp, j0tmp, j1tmp, k0tmp, k1tmp));

        this.iScale = config.asDouble(Config.Fractal.Model.I_SCALE);
        this.jScale = config.asDouble(Config.Fractal.Model.J_SCALE);
        this.kScale = config.asDouble(Config.Fractal.Model.K_SCALE);
        this.iShift = config.asDouble(Config.Fractal.Model.I_SHIFT);
        this.jShift = config.asDouble(Config.Fractal.Model.J_SHIFT);
        this.kShift = config.asDouble(Config.Fractal.Model.K_SHIFT);
        logger.info(String.format("Scale:  i=%f j=%f k=%f, Shift: i=%f j=%f k=%f.",
                iScale, jScale, kShift, iShift, jShift, kShift));

        double iLen = (i1tmp - i0tmp) * iScale;
        double iMid = (i1tmp + i0tmp) / 2;
        this.i0 = iMid - (iLen / 2) + (i1tmp - i0tmp) * iShift;
        this.i1 = iMid + (iLen / 2) + (i1tmp - i0tmp) * iShift;

        double jLen = (j1tmp - j0tmp) * jScale;
        double jMid = (j1tmp + j0tmp) / 2;
        this.j0 = jMid - (jLen / 2) + (j1tmp - j0tmp) * jShift;
        this.j1 = jMid + (jLen / 2) + (j1tmp - j0tmp) * jShift;

        double kLen = (k1tmp - k0tmp) * jScale;
        double kMid = (k1tmp + k0tmp) / 2;
        this.k0 = kMid - (kLen / 2) + (k1tmp - k0tmp) * kShift;
        this.k1 = kMid + (kLen / 2) + (k1tmp - k0tmp) * kShift;

        logger.info(String.format("Width: i=%.20f j=%.20f k=%.20f",
                iLen, jLen, kLen));

        logger.info(String.format("Using:\r\n" +
                        "Config.JuliaSet.Model.I0=%.20f\r\n" +
                        "Config.JuliaSet.Model.I1=%.20f\r\n" +
                        "Config.JuliaSet.Model.J0=%.20f\r\n" +
                        "Config.JuliaSet.Model.J1=%.20f\r\n" +
                        "Config.JuliaSet.Model.K0=%.20f\r\n" +
                        "Config.JuliaSet.Model.K1=%.20f",
                i0, i1, j0, j1, k0, k1));

        this.maxIterations = config.asInt(Config.Fractal.Model.MAX_ITERATIONS);

        double blockSize = config.asDouble(Config.StlPrint.BLOCK_SIZE_3D);
        double xSize = config.asDouble(Config.StlPrint.X_SIZE);
        double ySize = config.asDouble(Config.StlPrint.Y_SIZE);
        double zSize = config.asDouble(Config.StlPrint.Z_SIZE);
        this.iCount = (int) (xSize / blockSize);
        this.jCount = (int) (ySize / blockSize);
        this.kCount = (int) (zSize / blockSize);

        this.iDelta = (i1 - i0) / (double) iCount;
        this.jDelta = (j1 - j0) / (double) jCount;
        this.kDelta = (k1 - k0) / (double) kCount;

        logger.info(this.toString());

        build();
    }

    void build() {

        pointMap = buildMap();

        logger.info("Points created: " + pointMap.size());

        pointMap = pointMap.stream()
                .filter(p -> !unjoinedPoint(p, pointMap))
                .collect(ImmutableSet.toImmutableSet());
        logger.info("Points after removal of unjoined: " + pointMap.size());

        map = pointMap.stream()
                .map(this::pointToCoordinates)
                .collect(ImmutableList.toImmutableList());
    }

    private Coords3d pointToCoordinates(MapPoint3D point) {
        return new Coords3d(
                point.i * iDelta + i0,
                point.j * jDelta + j0,
                point.k * kDelta + k0
        );
    }

    private boolean unjoinedPoint(MapPoint3D point, ImmutableSet<MapPoint3D> newMap) {
        for (short i = -1; i <= 1; i += 2) {
            for (short j = -1; j <= 1; j += 2) {
                for (short k = -1; k <= 1; k += 2) {
                    MapPoint3D p = new MapPoint3D((short)(point.i + i), (short)(point.j + j), (short)(point.k + k));
                    if (newMap.contains(p)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected ImmutableSet<MapPoint3D> buildMap() {
        ImmutableSet.Builder<MapPoint3D> builder = ImmutableSet.builder();
        for (short j = 0; j < jCount; j++) {
            for (short i = 0; i < iCount; i++) {
                for (short k = 0; k < kCount; k++) {
                    boolean b = buildPoint(i * iDelta + i0, j * jDelta + j0, k * kDelta + k0);
                    if (b) {
                        builder.add(new MapPoint3D(i, j, k));
                    }
                }
            }
        }
        return builder.build();
    }

    protected abstract boolean buildPoint(double i, double j, double k);

    public ImmutableList<Coords3d> getMap() {
        return map;
    }

    public Stream<Coords3d> stream() {
        return map.stream();
    }

    public double getISize() {
        return this.i1 - this.i0;
    }

    public double getJSize() {
        return this.j1 - this.j0;
    }

    public double getKSize() {
        return this.k1 - this.k0;
    }

    public double getIMin() {
        return this.i0;
    }

    public double getJMin() {
        return this.j0;
    }

    public double getKMin() {
        return this.k0;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("maxIterations", maxIterations)
                .add("i0", i0)
                .add("i1", i1)
                .add("j0", j0)
                .add("j1", j1)
                .add("k0", k0)
                .add("k1", k1)
                .add("iCount", iCount)
                .add("jCount", jCount)
                .add("kCount", kCount)
                .toString();
    }

    public record MapPoint3D(short i, short j, short k) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapPoint3D mapPoint = (MapPoint3D) o;
            return i == mapPoint.i && j == mapPoint.j && k == mapPoint.k;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, j, k);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("i", i)
                    .add("j", j)
                    .add("k", k)
                    .toString();
        }
    }
}
