package com.codecritical.lib.mapping;

import com.google.common.base.MoreObjects;

import javax.annotation.CheckForNull;
import java.util.*;
import java.util.stream.Stream;

public class MapArray implements IMapArray {
    final public Double[] mapArray;
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
        return mapArray[i + j * jSize];
    }

    public void set(Point p) {
        set(p.i, p.j, p.z);
    }
    public void set(int i, int j, Double z) {
        mapArray[i + j * jSize] = z;
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
            for (int j = 0; j < iSize; j++) {
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
}
