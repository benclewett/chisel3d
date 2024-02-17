package com.codecritical.parts;

import java.util.Random;

public enum Axis {
    X_PLUS(1, 0, 0),
    X_MINUS(-1, 0, 0),
    Y_PLUS(0, 1, 0),
    Y_MINUS(0, -1, 0),
    Z_PLUS(0, 0, 1),
    Z_MINUS(0, 0, -1);

    final int x, y, z;
    Axis(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Axis getRandomAxis(Random random) {
        int r = random.nextInt(6);
        return switch (r) {
            case 0 -> X_PLUS;
            case 1 -> X_MINUS;
            case 2 -> Y_PLUS;
            case 3 -> Y_MINUS;
            case 4 -> Z_PLUS;
            case 5 -> Z_MINUS;
            default -> throw new RuntimeException("Bad enum: " + r);
        };
    }
}
