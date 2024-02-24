package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;
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
                .max(Comparator.naturalOrder())
                .orElse(0.0);

        var min = map.stream()
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        var min2 = (allowZero)
                ? min
                : min - (max - min) * MIN_WHEN_NO_ZERO;

        var range = max - min2;

        return new MapArray(
                map.getISize(),
                map.getJSize(),
                map.stream().map(p -> ((p - min2) / range) * (maxOut - minOut) + minOut)
        );
    }

    public static IMapArray scale(IMapArray map, IScale toPower) {
        return new MapArray(
                map.getISize(),
                map.getJSize(),
                map.stream().map(toPower::scale)
        );
    }

    /**
     * Applies a 2D gaussian weighting.
     * <br>
     * The area inside the plateauSet is ignored.  This will be replaced by the texture from plateauMap, or MAX is this is null.
     */
    public static IMapArray gaussian(IMapArray map, OptionalDouble gaussianRadius, @CheckForNull PlateauCollections plateauSet, Optional<IMapArray> plateauTextureMap) {
        if (gaussianRadius.isEmpty()) {
            return map;
        }

        // Normalise for the size of the map
        double radius = gaussianRadius.getAsDouble() * Math.sqrt(map.getISize() * map.getISize() + map.getJSize() * map.getJSize());

        return getGaussianMapped(map, radius, plateauSet, plateauTextureMap);
    }

    private static IMapArray getGaussianMapped(IMapArray map, double radiusConst, @CheckForNull PlateauCollections plateauSet, Optional<IMapArray> plateauTextureMap) {

        MapArray newMap = new MapArray(map);

        var gaussianMap = createGaussianMap(radiusConst);

        map.streamPoints().forEach(p -> sumGaussian(newMap, map, p, gaussianMap, plateauSet, plateauTextureMap));

        return newMap;

    }

    private static void sumGaussian(MapArray mapOut, IMapArray mapIn, MapArray.Point p, IMapArray gaussianMap, @CheckForNull PlateauCollections plateauSet, Optional<IMapArray> plateauTextureMap) {

        if (plateauSet != null && plateauSet.isPlateau(p)) {
            if (plateauTextureMap.isPresent()) {
                mapOut.set(p.i, p.j, plateauTextureMap.get().get(p.i, p.j));
            } else {
                mapOut.set(p);
            }
            return;
        }

        int radius = gaussianMap.getISize() / 2;
        double mean = getMean(p, mapIn, radius);

        mapOut.set(p.i, p.j, getGaussianSum(mapIn, p, gaussianMap, radius, mean));
    }

    private static double getGaussianSum(IMapArray map, MapArray.Point p, IMapArray gaussianMap, int r, double mean) {
        return IntStream.range(-r, r + 1)
                .mapToDouble(i -> IntStream.range(-r, r + 1)
                        .mapToDouble(
                                j -> map.getIfInRange(i + p.i, j + p.j).orElse(mean)
                                        * gaussianMap.get(i + r, j + r)
                        ).sum()
                ).sum();
    }

    private static double getMean(MapArray.Point p, IMapArray map, int gaussianMapRadius) {
        double mean = 0;
        int count = 0;
        for (int i = p.i - gaussianMapRadius; i <= p.i + gaussianMapRadius; i++) {
            for (int j = p.j - gaussianMapRadius; j <= p.j + gaussianMapRadius; j++) {
                var z = map.getIfInRange(i, j);
                if (z.isPresent()) {
                    mean += z.getAsDouble();
                    count++;
                }
            }
        }
        if (count != 0) {
            mean /= count;
        }
        return mean;
    }

    @VisibleForTesting
    static IMapArray createGaussianMap(double radiusConst) {

        int mapSize = (int)(radiusConst * 5);
        if (mapSize < 3) {
            mapSize = 3;
        }

        // Must be odd so that we have a centre cell at max value.
        if (mapSize % 2 == 0) {
            mapSize += 1;
        }
        int mapMiddle = (int)(mapSize / 2.0);

        MapArray map = new MapArray(mapSize, mapSize);
        map.streamPoints().forEach(p -> {
            var r = Math.sqrt(Math.pow((double)mapMiddle - p.i, 2) + Math.pow((double)mapMiddle - p.j, 2));
            map.set(p.i, p.j, gaussian(r / radiusConst));
        });

        // Normalise so total == 1
        var sum = map.stream().mapToDouble(Double::doubleValue).sum();
        MapArray mapN = new MapArray(mapSize, mapSize);
        map.streamPoints().forEach(p -> mapN.set(p.i, p.j, p.z / sum));

        return mapN;
    }

    public static double gaussian(double r) {
        return Math.exp(-r*r/2) / Math.sqrt(2*Math.PI);
    }
}
