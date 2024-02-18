package com.codecritical.build.lib.mapping;

import com.google.common.base.MoreObjects;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * Trim exposed points with only one neighbour.  These don't print well
     * and also can chain to form scraggly chains which break the gaussian smoothing.
     */
    public void trimScraggly() {
        do {
            if (plateau.size() <= 3) {
                plateau.clear();
                break;
            }
            var scraggly = findScraggly();
            if (scraggly.size() == 0) {
                break;
            } else {
                scraggly.forEach(plateau::remove);
            }
        } while (plateau.size() > 0);
    }

    private Set<PlateauBit> findScraggly() {
        return plateau.stream()
                .filter(this::isScraggly)
                .collect(Collectors.toSet());
    }

    private boolean isScraggly(PlateauBit p) {
        int c = 0;
        c += (plateau.contains(new PlateauBit(p.i, p.j - 1))) ? 1 : 0;
        c += (plateau.contains(new PlateauBit(p.i, p.j + 1))) ? 1 : 0;
        c += (plateau.contains(new PlateauBit(p.i + 1, p.j))) ? 1 : 0;
        c += (plateau.contains(new PlateauBit(p.i - 1, p.j))) ? 1 : 0;
        return (c <= 1);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("size", size())
                .add("sizePercent", String.format("%.3f%%", sizeCoefficient() * 100.0))
                .toString();
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
