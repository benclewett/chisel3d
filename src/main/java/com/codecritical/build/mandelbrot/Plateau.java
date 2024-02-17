package com.codecritical.build.mandelbrot;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import java.util.HashSet;

/* A selection of one or more adjacent points which are at max-iterations. */
public class Plateau {

    final HashSet<Long> plateau = new HashSet<>();
    final int mapSize;

    public Plateau(int mapSize) {
        this.mapSize = mapSize;
    }

    public void add(int i, int j) {
        plateau.add(plateauPointHash(i, j));
    }

    public boolean isSet(int i, int j) {
        return plateau.contains(plateauPointHash(i, j));
    }

    public int size() {
        return plateau.size();
    }

    public double sizeCoefficient() {
        return (double)plateau.size() / mapSize;
    }

    private long plateauPointHash(int i, int j) {
       return ((long)i << 32) + j;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("size", size())
                .add("sizePercent", String.format("%.3f%%", sizeCoefficient() * 100.0))
                .toString();
    }
}
