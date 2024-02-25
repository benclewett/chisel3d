package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testng.Assert;

import java.util.logging.Logger;

class NormalisationTest {
    static final Logger logger = Logger.getLogger("");


    @ParameterizedTest
    @CsvSource({
            "0.1, 0.2, 0.3",
            "1.0, 0.5, 1.0",
            "10.0, 20.0, 5.0",
            "10.0, 1.0, 0.1"
    })
    void testAllowZero(double d0, double d1, double d2) {

        logger.info(String.format("In:  [%.3f, %.3f, %.3f]", d0, d1, d2));

        MapArray map = new MapArray(3, 3);
        for (int j = 0; j < 3; j++) {
            map.set(0, j, d0);
            map.set(1, j, d1);
            map.set(2, j, d2);
        }

        var mapN = Mapping.normalise(map);

        logger.info(String.format("Out: [%.3f, %.3f, %.3f]", mapN.get(0, 0), mapN.get(1, 0), mapN.get(2, 0)));

        double min = mapN.stream().min(Double::compare).orElse(-1.0);
        double max = mapN.stream().max(Double::compare).orElse(-1.0);
        logger.info(String.format("Min: %.3f Max: %.3f", min, max));

        assertEqual(min, 0.0);
        assertEqual(max, 1.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0.1, 0.2, 0.3",
            "1.0, 0.5, 1.0",
            "10.0, 20.0, 5.0",
            "10.0, 1.0, 0.1"
    })
    void testNoAllowZero(double d0, double d1, double d2) {

        logger.info(String.format("In:  [%.3f, %.3f, %.3f]", d0, d1, d2));

        MapArray map = new MapArray(3, 3);
        for (int j = 0; j < 3; j++) {
            map.set(0, j, d0);
            map.set(1, j, d1);
            map.set(2, j, d2);
        }

        var mapN = Mapping.normalise(map, false);

        logger.info(String.format("Out: [%.3f, %.3f, %.3f]", mapN.get(0, 0), mapN.get(1, 0), mapN.get(2, 0)));

        double min = mapN.stream().min(Double::compare).orElse(-1.0);
        double max = mapN.stream().max(Double::compare).orElse(-1.0);
        logger.info(String.format("Min: %.3f Max: %.3f", min, max));

        assertEqual(min, Mapping.MIN_WHEN_NO_ZERO);
        assertEqual(max, 1.0);
    }

    @ParameterizedTest
    @CsvSource({
            "0.1, 0.2, 0.3",
            "1.0, 0.5, 1.0",
            "10.0, 20.0, 5.0",
            "10.0, 1.0, 0.1"
    })
    void testSubRangeNormalise(double d0, double d1, double d2) {

        final double minExpected = 0.2;
        final double maxExpected = 0.6;

        logger.info(String.format("In:  [%.3f, %.3f, %.3f]", d0, d1, d2));

        MapArray map = new MapArray(3, 3);
        for (int j = 0; j < 3; j++) {
            map.set(0, j, d0);
            map.set(1, j, d1);
            map.set(2, j, d2);
        }

        var mapN = Mapping.normalise(map, true, minExpected, maxExpected);

        logger.info(String.format("Out: [%.3f, %.3f, %.3f]", mapN.get(0, 0), mapN.get(1, 0), mapN.get(2, 0)));

        double min = mapN.stream().min(Double::compare).orElse(-1.0);
        double max = mapN.stream().max(Double::compare).orElse(-1.0);
        logger.info(String.format("Min: %.3f Max: %.3f", min, max));

        assertEqual(min, minExpected);
        assertEqual(max, maxExpected);
    }

    @ParameterizedTest
    @CsvSource({
            "0.1, 0.2, 0.3",
            "1.0, 0.5, 1.0",
            "10.0, 20.0, 5.0",
            "10.0, 1.0, 0.1"
    })
    void testSuperRangeNormalise(double d0, double d1, double d2) {

        final double minExpected = 2.0;
        final double maxExpected = 6.0;

        logger.info(String.format("In:  [%.3f, %.3f, %.3f]", d0, d1, d2));

        MapArray map = new MapArray(3, 3);
        for (int j = 0; j < 3; j++) {
            map.set(0, j, d0);
            map.set(1, j, d1);
            map.set(2, j, d2);
        }

        var mapN = Mapping.normalise(map, true, minExpected, maxExpected);

        logger.info(String.format("Out: [%.3f, %.3f, %.3f]", mapN.get(0, 0), mapN.get(1, 0), mapN.get(2, 0)));

        double min = mapN.stream().min(Double::compare).orElse(-1.0);
        double max = mapN.stream().max(Double::compare).orElse(-1.0);
        logger.info(String.format("Min: %.3f Max: %.3f", min, max));

        assertEqual(min, minExpected);
        assertEqual(max, maxExpected);
    }

    private void assertEqual(double d0, double d1) {
        String s0 = String.format("%.3f", d0);
        String s1 = String.format("%.3f", d1);
        Assert.assertEquals(s0, s1);
    }

}
