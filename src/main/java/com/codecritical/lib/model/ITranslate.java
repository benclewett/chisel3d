package com.codecritical.lib.model;

@FunctionalInterface
public interface ITranslate {

    double[] translate(double[] point);

    ITranslate ONE_TO_ONE = point -> point;

    ITranslate POLAR_COORDINATES = point -> new double[] {
            Math.cos(point[0] * Math.PI + Math.PI) * point[1],
            Math.sin(point[0] * Math.PI + Math.PI) * point[1]
    };

    ITranslate INSIDE_OUT = point -> {
        // https://mathworld.wolfram.com/Inversion.html
        double r = 1 / Math.max (0.01, Math.sqrt(point[0] * point[0] + point[1] * point[1]) + 0.4);
        double alpha = Math.atan2(point[1], point[0]);
        return new double[] {
                Math.cos(alpha) * r,
                Math.sin(alpha) * r
        };
    };
}
