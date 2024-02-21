package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testng.Assert;

import java.util.OptionalDouble;

class MappingTest {

    @Test
    void testGaussian() {

        System.out.println("+--------+---------+---------+");
        System.out.printf("| %-6s | %-7s | %-7s |%n", "radius", "gauss", "sum");
        System.out.println("+--------+---------+---------+");

        double sum = 0;
        for (int i = -50; i <= 50; i++) {
            double r = i / 10.0;
            double g = Mapping.gaussian(r);
            sum += g / 10.0;
            System.out.printf("| %6.1f | %5.5f | %5.5f |%n",
                    r, g, sum);
        }

        System.out.println("+--------+---------+---------+");

        Assert.assertEquals(String.format("%5.4f",sum), "1.0000");
    }

    @ParameterizedTest
    @CsvSource({"0.1", "0.5", "1", "2"})
    void testGaussianMapping(double gaussianRadius) {

        var map = Mapping.createGaussianMap(gaussianRadius);

        double sum = map.stream().mapToDouble(Double::doubleValue).sum();
        System.out.println("Test with radios = " + gaussianRadius + ", sum = " + String.format("%.3f", sum));
        showMap(map);

    }

    @ParameterizedTest
    @CsvSource({"0.1", "0.5", "1", "2"})
    void testGaussianFullMapping(double gaussianRadius) {

        // A map of all 1.000 should map to all 1.000
        MapArray map = new MapArray(10, 10);
        map.streamPoints().forEach(p -> map.set(p.i, p.j, 1.0));

        var gMap = Mapping.createGaussianMap(gaussianRadius);
        System.out.println("Gaussian Map, radius=" + String.format("%.1f", gaussianRadius)
                + " (Sum=" + String.format("%.3f", gMap.stream().mapToDouble(Double::doubleValue).sum()) + ")");
        showMap(gMap);

        System.out.println("Before");
        showMap(map);
        OptionalDouble radius = OptionalDouble.of(1.0);

        var newMap = Mapping.gaussian(map, radius, null, null);

        System.out.printf("After:%n");
        showMap(newMap);

        newMap.streamPoints().forEach(p -> {
            Assert.assertEquals(String.format("%.3f", p.z), "1.000");
        });
    }

    private void showMap(IMapArray map) {
        for (int i = 0; i < map.getISize(); i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < map.getJSize(); j++) {
                sb.append(String.format(" %.3f", map.get(i, j)));
            }
            System.out.println("  " + sb);
        }
    }
}
