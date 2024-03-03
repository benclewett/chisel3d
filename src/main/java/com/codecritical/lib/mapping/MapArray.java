package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import com.google.common.hash.Hashing;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class MapArray implements IMapArray {
    private final Double[] mapArray;
    private final int iSize;
    private final int jSize;

    public MapArray(int iSize, int jSize) {
        this(iSize, jSize, null);
    }

    public MapArray(int iSize, int jSize, @CheckForNull Stream<Double> map) {
        this.iSize = iSize;
        if (this.iSize < 2) {
            throw new RuntimeException("xRange must be 2 or above.");
        }
        this.jSize = jSize;
        if (this.jSize < 2) {
            throw new RuntimeException("yRange must be 2 or above");
        }
        this.mapArray = (map == null)
                ? new Double[iSize * jSize]
                : map.toArray(Double[]::new);
        for (int n = 0; n < mapArray.length; n++) {
            if (mapArray[n] == null) {
                mapArray[n] = 0.0;
            }
        }
    }

    public MapArray(IMapArray map) {
        this.iSize = map.getISize();
        this.jSize = map.getJSize();
        this.mapArray = map.stream().toArray(Double[]::new);
    }

    @Override
    public double get(int i, int j) {
        return mapArray[i + j * iSize];
    }
    @Override
    public MapArray.Point getPoint(int i, int j) {
        return new Point(i, j, mapArray[i + j * iSize]);
    }

    @Override
    public boolean isNull(int i, int j) {
        return (mapArray[i + j * iSize] == null);
    }

    public double get(Point p) {
        return mapArray[p.i + p.j * iSize];
    }

    public void set(Point p) {
        set(p.i, p.j, p.z);
    }
    public void set(int i, int j, @CheckForNull Double z) {
        mapArray[i + j * iSize] = z;
    }

    public int getISize() {
        return iSize;
    }

    public int getJSize() {
        return jSize;
    }

    public int size() {
        return mapArray.length;
    }

    @Override
    public Stream<Double> stream() {
        return Arrays.stream(mapArray);
    }

    @Override
    public Stream<Point> streamPoints() {
        List<Point> l = new ArrayList<>();
        for (int i = 0; i < iSize; i++) {
            for (int j = 0; j < jSize; j++) {
                l.add(new Point(i, j, get(i, j)));
            }
        }
        return l.stream();
    }

    public double getMax() {
        return Arrays.stream(mapArray)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(0.0);
    }

    public double getMin() {
        return Arrays.stream(mapArray)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(0.0);
    }

    public double getMean() {
        return Arrays.stream(mapArray)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics()
                .getAverage();
    }

    public IMapArray setAllValues(double z) {
        Arrays.fill(mapArray, z);
        return this;
    }

    public static class Point {
        public final int i, j;
        public final double z;
        public Point(int i, int j, double z) {
            this.i = i;
            this.j = j;
            this.z = z;
        }
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("i", i)
                    .add("j", j)
                    .add("z", z)
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, j);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point that = (Point) o;
            return i == that.i && j == that.j;
        }
    }

    @Override
    public OptionalDouble getIfInRange(int i, int j) {
        if (i < 0 || j < 0 || i >= iSize || j >= jSize) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(get(i, j));
        }
    }

    @Override
    public boolean isInRange(int i, int j) {
        return (i >= 0 && j >= 0 && i < iSize && j < jSize);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("i", iSize)
                .add("j", jSize)
                .add("size", mapArray.length)
                .toString();
    }
}
