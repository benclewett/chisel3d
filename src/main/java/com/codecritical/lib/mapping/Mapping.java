package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class Mapping {

    public static double MIN_WHEN_NO_ZERO = 0.01;

    public static IMapArray normalise(IMapArray map) {
        return normalise(map, true, 0.0, 1.0);
    }
    public static IMapArray normalise(IMapArray map, boolean allowZero) {
        return normalise(map, allowZero, 0.0, 1.0);
    }
    public static IMapArray normalise(IMapArray map, boolean allowZero, double minOut, double maxOut) {

        var max = map.stream()
                .filter(z -> !z.isNaN() && !z.isInfinite())
                .max(Comparator.naturalOrder())
                .orElse(0.0);

        var min = map.stream()
                .filter(z -> !z.isNaN() && !z.isInfinite())
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        var min2 = (allowZero)
                ? min
                : min - (max - min) * MIN_WHEN_NO_ZERO;

        var range = max - min2;

        return new MapArray(
                map.getISize(),
                map.getJSize(),
                map.stream().map(p -> {
                    if (p.isNaN() || p.isInfinite()) {
                        return min;
                    } else {
                        return ((p - min2) / range) * (maxOut - minOut) + minOut;
                    }
                }));
    }

    public static IMapArray scale(IMapArray map, IScale toPower) {
        return new MapArray(
                map.getISize(),
                map.getJSize(),
                map.stream().map(toPower::scale)
        );
    }

    public static IMapArray applyPlateauTexture(PlateauCollections plateauCollection, IMapArray plateauTextureMap, IMapArray map) {
        MapArray newMap = new MapArray(map);

        map.streamPoints()
                .filter(plateauCollection::isPlateau)
                .forEach(p -> newMap.set(p.i, p.j, plateauTextureMap.get(p)));

        return newMap;
    }

    /** This doesn't work where boundary bends in on its self as the anti-clockwise sorting miss-orders */
    public static IMapArray trimOutsideBase(IMapArray map) {
        var newMap = new MapArray(map);
        Set<MapArray.Point> perimeterChecked = new HashSet<>();
        // Bottom & Top
        IntStream.range(0, map.getISize()).forEach(i -> {
            trimOutsideBaseEdge(i, 0, map, newMap, perimeterChecked);
            trimOutsideBaseEdge(i, map.getJSize() - 1, map, newMap, perimeterChecked);
        });
        // Left & right
        IntStream.range(0, map.getJSize()).forEach(j -> {
            trimOutsideBaseEdge(0, j, map, newMap, perimeterChecked);
            trimOutsideBaseEdge(map.getISize() - 1, j, map, newMap, perimeterChecked);
        });
        return newMap;
    }

    private static void trimOutsideBaseEdge(int i, int j, IMapArray map, MapArray newMap, Set<MapArray.Point> perimeterChecked) {
        if (map.isNull(i, j)) {
            return;
        }
        var p = map.getPoint(i, j);
        if (perimeterChecked.contains(p)) {
            return;
        }
        perimeterChecked.add(p);
        if (p.z != 0.0) {
            return;
        }

        // Remove position
        newMap.set(i, j, null);

        // Recursive
        if (i < map.getISize() - 1) {
            trimOutsideBaseEdge(i + 1, j, map, newMap, perimeterChecked);
        }
        if (i > 0) {
            trimOutsideBaseEdge(i - 1, j, map, newMap, perimeterChecked);
        }
        if (j < map.getJSize() - 1) {
            trimOutsideBaseEdge(i, j + 1, map, newMap, perimeterChecked);
        }
        if (j > 0) {
            trimOutsideBaseEdge(i, j - 1, map, newMap, perimeterChecked);
        }
    }

    public static IMapArray mapFunction(IMapArray map, Function<MapArray.Point, Double> func) {
        MapArray newMap = new MapArray(map);
        newMap.streamPoints()
                .forEach(p -> newMap.set(p.i, p.j, func.apply(p)));
        return newMap;
    }
}
