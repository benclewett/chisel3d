package com.codecritical.build.mandelbrot;

import com.codecritical.build.lib.IMapArray;
import com.google.common.collect.ImmutableList;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class PlateauSet {

    final ImmutableList<Plateau> plateaus;

    public PlateauSet(Stream<Plateau> plateauStream) {
        plateaus = plateauStream.collect(ImmutableList.toImmutableList());
    }

    public PlateauSet(IMapArray map) {
        plateaus = buildPlateau(map);
    }

    private ImmutableList<Plateau> buildPlateau(IMapArray map) {
        ImmutableList.Builder<Plateau> builder = new ImmutableList.Builder<>();

        Plateau[] plateau = new Plateau[1];
        Plateau allMap = new Plateau(map.size());

        for (int i = 0; i < map.getISize(); i++) {
            for (int j = 0; j < map.getJSize(); j++) {
                plateau[0] = null;
                testCell(allMap, builder, map, plateau, i, j);
            }
        }

        return builder.build();
    }

    private void testCell(Plateau allMap, ImmutableList.Builder<Plateau> builder, IMapArray array, Plateau[] plateau, int i, int j) {
        if (i < 0 || i >= array.getISize() || j < 0 || j >= array.getJSize()) {
            return;
        }

        if (allMap.isSet(i, j)) {
            // Already mapped
            return;
        }
        allMap.add(i, j);

        if (!array.isMax(i, j)) {
            // Fallen off the plateau.  End of tree here.
            return;
        }

        // Hit, we are on a plateau.

        if (plateau[0] == null) {
            plateau[0] = new Plateau(array.size());
            builder.add(plateau[0]);
        }
        plateau[0].add(i, j);

        // recursive call
        testCell(allMap, builder, array, plateau, i - 1, j);
        testCell(allMap, builder, array, plateau, i + 1, j);
        testCell(allMap, builder, array, plateau, i, j + 1);
        testCell(allMap, builder, array, plateau, i, j + 1);
    }

    public boolean isPlateau(int i, int j) {
        return plateaus.stream()
                .anyMatch(p -> p.isSet(i, j));
    }

    public Stream<Plateau> stream() {
        return plateaus.stream();
    }

    public int size() {
        return plateaus.size();
    }
}
