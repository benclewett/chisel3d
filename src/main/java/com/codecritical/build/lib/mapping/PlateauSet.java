package com.codecritical.build.lib.mapping;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class PlateauSet {

    final ImmutableList<Plateau> plateaus;

    /** Clone a plateau set */
    public PlateauSet(Stream<Plateau> plateauStream) {
        plateaus = plateauStream.collect(ImmutableList.toImmutableList());
    }

    /** Create new plateau set from map. */
    public PlateauSet(IMapArray map) {
        plateaus = buildPlateau(map);
       // plateaus.forEach(Plateau::trimScraggly);
    }

    private ImmutableList<Plateau> buildPlateau(IMapArray map) {
        ImmutableList.Builder<Plateau> builder = new ImmutableList.Builder<>();

        Plateau[] plateau = new Plateau[1];
        Plateau allMap = new Plateau(map.size());

        var max = map.getMax();

        map.streamPoints().forEach(p -> {
            plateau[0] = null;
            Square square = Square.getUnfoundSquare(allMap, p);
            if (square != null) {
                testSquare(allMap, builder, map, plateau, square, max);
            }
        });

        return builder.build();
    }

    private void testSquare(Plateau allMap, ImmutableList.Builder<Plateau> builder, IMapArray map, Plateau[] plateau, @Nonnull Square s, double max) {

        if (!map.isInRange(s.i0, s.j0) || !map.isInRange(s.i1, s.j1)) {
            // Not a square on the grid.
            return;
        }

        int countMiss = 0;
        for (Plateau.PlateauBit p : s.getPlateauBits()) {
            if (map.get(p.i, p.j) != max) {
                // Fallen off the plateau.  End of tree here.
                return;
            }
            boolean hitOnThisPlateau = (plateau[0] != null && plateau[0].isSet(p.i, p.j));
            if (!hitOnThisPlateau && allMap.isSet(p.i, p.j)) {
                // Owned by another plateau.
                return;
            }
            // Ensure there are new cells to check
            countMiss += (!hitOnThisPlateau) ? 1 : 0;
        }
        if (countMiss == 0) {
            // Been here before.
            return;
        }

        // Hit, we are on a plateau and valid

        s.forEach(allMap::add);

        if (plateau[0] == null) {
            plateau[0] = new Plateau(map.size());
            builder.add(plateau[0]);
        }
        s.forEach(plateau[0]::add);

        // Recursive call
        testSquare(allMap, builder, map, plateau, new Square(s.i0 - 1, s.i1 - 1, s.j0, s.j1), max);
        testSquare(allMap, builder, map, plateau, new Square(s.i0 + 1, s.i1 + 1, s.j0, s.j1), max);
        testSquare(allMap, builder, map, plateau, new Square(s.i0, s.i1, s.j0 - 1, s.j1 - 1), max);
        testSquare(allMap, builder, map, plateau, new Square(s.i0, s.i1, s.j0 + 1, s.j1 + 1), max);
    }

    public boolean isPlateau(int i, int j) {
        return plateaus.stream()
                .anyMatch(p -> p.isSet(i, j));
    }

    public boolean isPlateau(MapArray.Point p) {
        return isPlateau(p.i, p.j);
    }

    public Stream<Plateau> stream() {
        return plateaus.stream();
    }

    public int size() {
        return plateaus.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("size", this.plateaus.size())
                .toString();
    }

    record Square(int i0, int i1, int j0, int j1) {

        @CheckForNull
        public static Square getUnfoundSquare(Plateau allMap, MapArray.Point p) {
            int i0 = p.i;
            int i1 = i0 + 1;
            int j0 = p.j;
            int j1 = j0 + 1;
            if (allMap.isSet(i0, j0) || allMap.isSet(i0, j1) || allMap.isSet(i1, j0) || allMap.isSet(i1, j1)) {
                return null;
            } else {
                return new Square(i0, i1, j0, j1);
            }
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("i0", i0)
                    .add("i1", i1)
                    .add("j0", j0)
                    .add("j1", j1)
                    .toString();
        }

        public void forEach(BiConsumer<Integer, Integer> action) {
            action.accept(i0, j0);
            action.accept(i0, j1);
            action.accept(i1, j0);
            action.accept(i1, j1);
        }

        public Iterable<? extends Plateau.PlateauBit> getPlateauBits() {
            return Arrays.stream(new Plateau.PlateauBit[] {
                    new Plateau.PlateauBit(i0, j0),
                    new Plateau.PlateauBit(i0, j1),
                    new Plateau.PlateauBit(i1, j0),
                    new Plateau.PlateauBit(i1, j1),
            }).toList();
        }
    }
}
