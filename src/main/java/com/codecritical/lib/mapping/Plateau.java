package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.MoreObjects;

import java.util.HashSet;
import java.util.Objects;

/* A selection of one or more adjacent points which are at max-iterations. */
public class Plateau {

    final HashSet<PlateauBit> plateau = new HashSet<>();
    final int mapSize;

    public Plateau(int mapSize) {
        this.mapSize = mapSize;
    }

    public boolean add(int i, int j) {
        return plateau.add(new PlateauBit(i, j));
    }

    public boolean isSet(int i, int j) {
        return plateau.contains(new PlateauBit(i, j));
    }

    public int size() {
        return plateau.size();
    }

    public double sizeCoefficient() {
        return (double)plateau.size() / mapSize;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("size", size())
                .add("sizePercent", String.format("%.3f%%", sizeCoefficient() * 100.0))
                .toString();
    }

    public IMapArray asMapArray() {
        MapArray mapOut = new MapArray(
                plateau.stream().map(p -> p.i).max(Integer::compare).get(),
                plateau.stream().map(p -> p.j).max(Integer::compare).get()
        );
        mapOut.streamPoints().forEach(p -> {
            mapOut.set(p.i, p.j, (isSet(p.i, p.j)) ? 1.0 : 0.0);
        });
        return mapOut;
    }

    static class PlateauBit {
        final int i, j;
        public PlateauBit(int i, int j) {
            this.i = i;
            this.j = j;
        }

        public PlateauBit(MapArray.Point p) {
            this.i = p.i;
            this.j = p.j;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlateauBit that = (PlateauBit) o;
            return i == that.i && j == that.j;
        }

        @Override
        public int hashCode() {
            return Objects.hash(i, j);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("i", i)
                    .add("j", j)
                    .toString();
        }
    }
}
