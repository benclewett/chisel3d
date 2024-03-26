package com.codecritical.lib.model;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */


import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class JuliaSet3D {
    protected static final Logger logger = Logger.getLogger("");

    protected final int maxIterations;
    protected final double i0, i1, j0, j1, k0, k1;
    protected final double iScale, jScale, iShift, jShift, kScale, kShift;
    protected final int iCount, jCount, kCount;
    protected final double iDelta, jDelta, kDelta;

    protected ImmutableSet<MapPoint3D> map;


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

        logger.info(String.format("""
                        Using:\r
                        Config.Fractal.Model.I0=%.20f\r
                        Config.Fractal.Model.I1=%.20f\r
                        Config.Fractal.Model.J0=%.20f\r
                        Config.Fractal.Model.J1=%.20f\r
                        Config.Fractal.Model.K0=%.20f\r
                        Config.Fractal.Model.K1=%.20f""",
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

        map = buildMap();

        logger.info("Points created: " + map.size());

        map = map.stream()
                .filter(p -> !unjoinedPoint(p, map))
                .collect(ImmutableSet.toImmutableSet());
        logger.info("Points after removal of unjoined: " + map.size());

        map = extendToPillars(map);
        logger.info("Points converted to pillars: " + map.size());

        map = map.stream()
                .map(this::pointToCoordinates)
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<MapPoint3D> extendToPillars(ImmutableSet<MapPoint3D> map) {

        Map<MapPoint3D, MapPoint3D> pillars = new HashMap<>();
        map.forEach(p -> pillars.put(p, p));

        double i0, i1, j0, j1, k0, k1;
        i0 = j0 = k0 = Double.MAX_VALUE;
        i1 = j1 = k1 = Double.MIN_VALUE;
        for (var p : map) {
            i0 = Math.min(i0, p.i);
            i1 = Math.max(i1, p.i);
            j0 = Math.min(j0, p.j);
            j1 = Math.max(j1, p.j);
            k0 = Math.min(k0, p.k);
            k1 = Math.max(k1, p.k);
        };

        for (double j = j0; j <= j1; j += 1.0) {
            for (double k = k0; k <= k1; k += 1.0) {
                for (double i = i0; i <= i1; i += 1.0) {
                    MapPoint3D p = pillars.get(new MapPoint3D(i, j, k));
                    if (p == null || p.iLength != 1.0) {
                        continue;
                    }
                    MapPoint3D iStart, iEnd;
                    iStart = iEnd = p;

                    MapPoint3D iTest = p.mutate().decI().build();
                    while (pillars.containsKey(iTest) && pillars.get(iTest).iLength == 1.0) {
                        iTest = iTest.mutate().decI().build();
                        iStart = iTest;
                    }

                    iTest = p.mutate().incI().build();
                    while (pillars.containsKey(iTest) && pillars.get(iTest).iLength == 1.0) {
                        iTest = iTest.mutate().incI().build();
                        iEnd = iTest;
                    }

                    if (iStart.i != iEnd.i) {
                        // Hit, we have a pillar
                        int iLength = (int)(iEnd.i - iStart.i);
                        for (double iPillar = iStart.i; iPillar <= iEnd.i; iPillar += 1.0) {
                            MapPoint3D pNew = iStart.mutate()
                                    .setI(iPillar)
                                    .setILength((iPillar == iStart.i) ? iLength : 0)
                                    .build();
                            pillars.put(pNew, pNew);
                        }
                    }
                }
            }
        }

        return pillars.values().stream()
                .filter(p -> p.iLength != 0)
                .collect(ImmutableSet.toImmutableSet());
    }

    private MapPoint3D pointToCoordinates(MapPoint3D point) {
        return point.mutate()
                .setI(i -> i * iDelta + i0)
                .setJ(j -> j * jDelta + j0)
                .setK(k -> k * kDelta + k0)
                .build();
    }

    final static int[] UNJOINED_RANGE = new int[] {-1, 1};

    private boolean unjoinedPoint(MapPoint3D point, ImmutableSet<MapPoint3D> newMap) {

        for (int i : UNJOINED_RANGE) {
            MapPoint3D p = new MapPoint3D(point.i + i,point.j, point.k);
            if (newMap.contains(p)) {
                return false;
            }
        }

        for (int j : UNJOINED_RANGE) {
            MapPoint3D p = new MapPoint3D(point.i,point.j + j, point.k);
            if (newMap.contains(p)) {
                return false;
            }
        }

        for (int k : UNJOINED_RANGE) {
            MapPoint3D p = new MapPoint3D(point.i,point.j, point.k + k);
            if (newMap.contains(p)) {
                return false;
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

    public ImmutableSet<MapPoint3D> getMap() {
        return map;
    }

    public Stream<MapPoint3D> stream() {
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
}
