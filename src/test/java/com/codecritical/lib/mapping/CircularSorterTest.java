package com.codecritical.lib.mapping;

import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.IntStream;

class CircularSorterTest {
    static final Logger logger = Logger.getLogger("");

    final static Random RANDOM = new Random(0);
    final static int SIZE = 10;
    final static double MIN = -10;
    final static double MAX = 10;
    final static double RANGE = MAX - MIN;
    final static Coords3d CENTRE = new Coords3d(0, 0, 0);

    @Test
    void testZPlane() {

        CircularSorter circularSorter = new CircularSorter(CENTRE);

        var list = getRandomSet();

        // Sort
        var listSorted = circularSorter.sortAntiClockwiseZPlane(list);

        StringBuilder sb = new StringBuilder();
        sb.append("Sorted Anti-Clockwise:\r\n");
        sb.append("+--------+--------+--------+\r\n");
        sb.append("+ x      | y      | alpha  |\r\n");
        sb.append("+--------+--------+--------+\r\n");
        listSorted.forEach(p -> {
            sb.append(String.format("| %6.1f | %6.1f | %6.1f |\r\n",
                    p.getX(),
                    p.getY(),
                    Math.atan2(CENTRE.getY() - p.getY(), CENTRE.getX() - p.getX()) / Math.PI / 2 * 360
            ));
        });
        sb.append("+--------+--------+--------+\r\n");
        logger.info(sb.toString());

        int[] outOfSequence = new int[] {0};
        double[] prevAlpha = new double[] {0};
        listSorted.forEach(p -> {
            double alpha = Math.atan2(CENTRE.getY() - p.getY(), CENTRE.getX() - p.getX());
            if (prevAlpha[0] != 0 && prevAlpha[0] < alpha) {
                outOfSequence[0]++;
            }
            prevAlpha[0] = alpha;
        });

        // We allow one out of sequence for when we loop
        Assert.assertTrue(outOfSequence[0] < 2, "Sort anticlockwise failed.");

    }

    private ImmutableList<Coords3d> getRandomSet() {
        List<Coords3d> list = new ArrayList<>();
        IntStream.range(0, SIZE)
                .forEach(i -> list.add(new Coords3d(
                        RANDOM.nextDouble() * RANGE + MIN,
                        RANDOM.nextDouble() * RANGE + MIN,
                        RANDOM.nextDouble() * RANGE + MIN
                )));
        return list.stream()
                .collect(ImmutableList.toImmutableList());
    }

}
