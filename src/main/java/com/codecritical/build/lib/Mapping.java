package com.codecritical.build.lib;

import com.codecritical.build.mandelbrot.IScale;
import com.codecritical.build.mandelbrot.PlateauSet;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import org.testng.Assert;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.OptionalDouble;

@ParametersAreNonnullByDefault
public class Mapping {
    public static IMapArray normalise(IMapArray map, boolean allowZero) {

        var max = map.stream()
                .max(Comparator.naturalOrder())
                .orElse(0.0);

        var min = map.stream()
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        var min2 = (allowZero) ? min : min - (max - min) * 0.01;

        var range = max - min;

        return new MapArray(
                map.getISize(),
                map.getJSize(),
                map.stream().map(p -> (p - min2) / range)
        );
    }

    public static IMapArray scale(IMapArray map, IScale toPower) {
        return new MapArray(
                map.getISize(),
                map.getJSize(),
                map.stream().map(toPower::scale)
        );
    }

    /** Applies a 2D gaussian weighting.  This currently uses a fudged 1D gaussian, which is a bit wrong.  TODO  */
    public static IMapArray gaussian(IMapArray map, OptionalDouble gaussianRadius, PlateauSet plateauSet) {
        if (gaussianRadius.isEmpty()) {
            return map;
        }
        return getGaussianMapped(map, gaussianRadius.getAsDouble(), plateauSet);
    }

    private static IMapArray getGaussianMapped(IMapArray map, double radiusConst, PlateauSet plateauSet) {

        MapArray newMap = new MapArray(map);

        var gaussianMap = createGaussianMap(radiusConst);

        map.streamPoints().forEach(p -> sumGaussian(newMap, map, p, gaussianMap));

        return newMap;

    }

    private static void sumGaussian(MapArray newMap, IMapArray map, MapArray.Point p, IMapArray gaussianMap) {

        int gaussianMapRadius = gaussianMap.getISize() / 2;

        Assert.assertEquals(gaussianMap.getISize(), gaussianMapRadius * 2 + 1);
        Assert.assertEquals(gaussianMap.getJSize(), gaussianMapRadius * 2 + 1);

        double mean = getMean(p, map, gaussianMapRadius);

        double newZ = 0;
        for (int i = p.i - gaussianMapRadius; i <= p.i + gaussianMapRadius; i++) {
            for (int j = p.j - gaussianMapRadius; j <= p.j + gaussianMapRadius; j++) {
                double oldZ = map.getIfInRange(i, j).orElse(mean);
                double gauss = gaussianMap.get(i - p.i + gaussianMapRadius, j - p.j + gaussianMapRadius);
                newZ += oldZ * gauss;
            }
        }
        newMap.set(p.i, p.j, newZ);
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
    public static IMapArray createGaussianMap(double radiusConst) {

        int mapSize = (int)(radiusConst * 5);
        if (mapSize < 3) {
            mapSize = 3;
        }
        if (mapSize % 2 == 0) {
            mapSize += 1;
        }
        int mapMiddle = (int)(mapSize / 2.0);

        MapArray map = new MapArray(mapSize, mapSize);
        var points = map.streamPoints().toList();
        for (var p : points) {
            var r = Math.sqrt(Math.pow((double)mapMiddle - p.i, 2) + Math.pow((double)mapMiddle - p.j, 2));
            map.set(p.i, p.j, gaussian(r / radiusConst));
        }

        // Normalise so total == 1
        var sum = map.stream().mapToDouble(Double::doubleValue).sum();

        MapArray map2 = new MapArray(mapSize, mapSize);
        map.streamPoints().forEach(p -> {
            map2.set(p.i, p.j, p.z / sum);
        });

        return map2;
    }

    public static double gaussian(double r) {
        return Math.exp(-r*r/2) / Math.sqrt(2*Math.PI);
    }


}
