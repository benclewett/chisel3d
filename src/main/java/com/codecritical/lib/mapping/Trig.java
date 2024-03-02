package com.codecritical.lib.mapping;

import eu.printingin3d.javascad.coords.Coords3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
public class Trig {

    public static Optional<Coords3d> lineSegmentIntersect(Coords3d a0, Coords3d a1, Coords3d b0, Coords3d b1) {
        double aX = a0.getX() - a1.getX();
        double bX = b0.getX() - b1.getX();
        double aY = a0.getY() - a1.getY();
        double bY = b0.getY() - b1.getY();

        if (minX(a0, a1) > maxX(b0, b1) || maxX(a0, a1) < minX(b0, b1)) {
            // Miss on Z axis
            return Optional.empty();
        } else if (minY(a0, a1) > maxY(b0, b1) || maxY(a0, a1) < minY(b0, b1)) {
            // Miss on Y axis
            return Optional.empty();
        }

        double c = aX * bY - aY * bX;

        if (c == 0) {
            // Parallel
            return Optional.empty();
        } else {
            // Intersection
            double a = a0.getX() * a1.getY() - a0.getY() * a1.getX();
            double b = b0.getX() * b1.getY() - b0.getY() * b1.getX();

            double x = (a * bX - b * aX) / c;
            double y = (a * bY - b * aY) / c;

            // Interpolate 'z' from 'x' along line 'a':  z = z-range * x_coefficient + z0.
            double z = (a1.getZ() - a0.getX()) * (a1.getX() - x) / (a1.getX() - a0.getX()) + a0.getZ();

            return Optional.of(new Coords3d(x, y, z));
        }
    }

    private static double minX(Coords3d a, Coords3d b) {
        return Math.min(a.getX(), b.getX());
    }
    private static double maxX(Coords3d a, Coords3d b) {
        return Math.max(a.getX(), b.getX());
    }
    private static double minY(Coords3d a, Coords3d b) {
        return Math.min(a.getY(), b.getY());
    }
    private static double maxY(Coords3d a, Coords3d b) {
        return Math.max(a.getY(), b.getY());
    }
}
