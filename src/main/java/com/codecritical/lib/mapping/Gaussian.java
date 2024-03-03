package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class Gaussian {

    /**
     * Applies a 2D gaussian weighting.
     * <br>
     * The area inside the plateauSet is ignored.  This will be replaced by the texture from plateauMap, or MAX is this is null.
     */
    public static IMapArray applyToMap(
            IMapArray map,
            OptionalDouble gaussianRadius,
            @CheckForNull PlateauCollections plateauSet,
            Optional<IMapArray> plateauTextureMap,
            boolean smoothTextureHollowInside
    ) {
        if (gaussianRadius.isEmpty()) {
            return map;
        }

        // Normalise for the size of the map
        double radius = gaussianRadius.getAsDouble() * Math.sqrt(map.getISize() * map.getISize() + map.getJSize() * map.getJSize());

        return getGaussianMapped(map, radius, plateauSet, plateauTextureMap, smoothTextureHollowInside);
    }

    private static IMapArray getGaussianMapped(
            IMapArray map,
            double radiusConst,
            @CheckForNull PlateauCollections plateauSet,
            Optional<IMapArray> plateauTextureMap,
            boolean smoothTextureHollowInside
    ) {

        MapArray newMap = new MapArray(map);

        var gaussianMap = createGaussianMap(radiusConst);

        map.streamPoints().forEach(p -> newMap.set(p.i, p.j,
                sumGaussian(
                        map,
                        p,
                        gaussianMap,
                        plateauSet,
                        plateauTextureMap,
                        smoothTextureHollowInside))
        );

        return newMap;
    }

    private static double sumGaussian(
            IMapArray map,
            MapArray.Point p,
            IMapArray gaussianMap,
            @CheckForNull PlateauCollections plateauSet,
            Optional<IMapArray> plateauTextureMap,
            boolean smoothTextureHollowInside) {

        int radius = gaussianMap.getISize() / 2;

        boolean smoothHollowEdge =
                smoothTextureHollowInside &&
                        onHollowEdge(p, plateauSet, radius, plateauTextureMap);

        if (!smoothHollowEdge && plateauSet != null && plateauSet.isPlateau(p)) {
            return map.get(p);
        }

        double mean = getMean(p, map, radius);

        return getGaussianSum(map, p, gaussianMap, radius, mean);
    }

    private static boolean onHollowEdge(MapArray.Point p, @CheckForNull PlateauCollections plateauSet, int r, Optional<IMapArray> plateauTextureMap) {
        if (plateauSet == null || plateauTextureMap.isEmpty()) {
            return false;
        }

        boolean onHollowHigh = false, onHollowLow = false;

        for (int i = -r; i < r + 1; i++) {
            for (int j = -r; j < r + 1; j++) {
                if (plateauSet.isPlateau(i + p.i, j + p.j)) {
                    if (1.0 == plateauTextureMap.get().get(i + p.i, j + p.j)) {
                        onHollowHigh = true;
                    } else {
                        onHollowLow = true;
                    }
                    if (onHollowLow && onHollowHigh) {
                        return true;
                    }
                }
            }
        }
        return false;
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
            map.set(p.i, p.j, gaussianFunction(r / radiusConst));
        });

        // Normalise so total == 1
        var sum = map.stream().mapToDouble(Double::doubleValue).sum();
        MapArray mapN = new MapArray(mapSize, mapSize);
        map.streamPoints().forEach(p -> mapN.set(p.i, p.j, p.z / sum));

        return mapN;
    }

    public static double gaussianFunction(double r) {
        return Math.exp(-r*r/2) / Math.sqrt(2*Math.PI);
    }

}
