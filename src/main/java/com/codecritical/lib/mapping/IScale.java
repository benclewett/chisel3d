package com.codecritical.lib.mapping;

@FunctionalInterface
public interface IScale {
    double scale(double d);

    static IScale NO_BIAS = b -> b;

    static IScale squareRootBias(double max) {
        double maxSquareRoot = Math.sqrt(max);
        return h -> Math.sqrt(h) * maxSquareRoot;
    }

    static IScale toPower(double power) {
        return h -> Math.pow(h, power);
    }
}
