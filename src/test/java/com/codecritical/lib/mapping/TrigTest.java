package com.codecritical.lib.mapping;

import eu.printingin3d.javascad.coords.Coords3d;
import org.junit.jupiter.api.Test;
import org.testng.Assert;

import java.util.Optional;

class TrigTest {

    private static final Coords3d COORDS_NAN = new Coords3d(Double.NaN, Double.NaN, Double.NaN);

    @Test
    void testIntersectHit() {

        // Perfect cross meeting at (2,2)
        Coords3d a0 = new Coords3d(1, 1, 1);
        Coords3d a1 = new Coords3d(3, 3, 3);
        Coords3d b0 = new Coords3d(1, 3, 1);
        Coords3d b1 = new Coords3d(3, 1, 3);

        Optional<Coords3d> c = Trig.lineSegmentIntersect(a0, a1, b0, b1);

        Assert.assertTrue(c.isPresent(), "Option has returned empty.");

        Assert.assertEquals(c.get().getX(), 2);
        Assert.assertEquals(c.get().getY(), 2);
        Assert.assertEquals(c.get().getZ(), 2);
    }

    @Test
    void testIntersectMissY1() {

        // Miss as out of range
        Coords3d a0 = new Coords3d(1, 1, 0);
        Coords3d a1 = new Coords3d(3, 3, 0);
        Coords3d b0 = new Coords3d(1, 3+4, 0);
        Coords3d b1 = new Coords3d(3, 1+4, 0);

        Optional<Coords3d> c = Trig.lineSegmentIntersect(a0, a1, b0, b1);

        Assert.assertTrue(c.isEmpty(), "Option has not returned empty, but returned: " + c.orElse(COORDS_NAN) + ".");
    }

    @Test
    void testIntersectMissY2() {

        // Miss as out of range
        Coords3d a0 = new Coords3d(1, 1, 0);
        Coords3d a1 = new Coords3d(3, 3, 0);
        Coords3d b0 = new Coords3d(1, 3-4, 0);
        Coords3d b1 = new Coords3d(3, 1-4, 0);

        Optional<Coords3d> c = Trig.lineSegmentIntersect(a0, a1, b0, b1);

        Assert.assertTrue(c.isEmpty(), "Option has not returned empty, but returned: " + c.orElse(COORDS_NAN) + ".");
    }

    @Test
    void testIntersectMissX1() {

        // Miss as out of range
        Coords3d a0 = new Coords3d(1, 1, 0);
        Coords3d a1 = new Coords3d(3, 3, 0);
        Coords3d b0 = new Coords3d(1+4, 3, 0);
        Coords3d b1 = new Coords3d(3+4, 1, 0);

        Optional<Coords3d> c = Trig.lineSegmentIntersect(a0, a1, b0, b1);

        Assert.assertTrue(c.isEmpty(), "Option has not returned empty, but returned: " + c.orElse(COORDS_NAN) + ".");
    }

    @Test
    void testIntersectMissX2() {

        // Miss as out of range
        Coords3d a0 = new Coords3d(1, 1, 0);
        Coords3d a1 = new Coords3d(3, 3, 0);
        Coords3d b0 = new Coords3d(1-4, 3, 0);
        Coords3d b1 = new Coords3d(3-4, 1, 0);

        Optional<Coords3d> c = Trig.lineSegmentIntersect(a0, a1, b0, b1);

        Assert.assertTrue(c.isEmpty(), "Option has not returned empty, but returned: " + c.orElse(COORDS_NAN) + ".");
    }

    @Test
    void testIntersectParallel() {

        // Miss as out of range
        Coords3d a0 = new Coords3d(1, 1, 0);
        Coords3d a1 = new Coords3d(3, 3, 0);
        Coords3d b0 = new Coords3d(1, 1, 0);
        Coords3d b1 = new Coords3d(3, 3, 0);

        Optional<Coords3d> c = Trig.lineSegmentIntersect(a0, a1, b0, b1);

        Assert.assertTrue(c.isEmpty(), "Option has not returned empty, but returned: " + c.orElse(COORDS_NAN) + ".");
    }
}
