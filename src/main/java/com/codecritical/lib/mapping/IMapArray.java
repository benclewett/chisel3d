package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import java.util.OptionalDouble;
import java.util.stream.Stream;

public interface IMapArray {
    double get(int i, int j);

    int getISize();

    int getJSize();

    double getMax();

    int size();

    Stream<Double> stream();

    Stream<MapArray.Point> streamPoints();

    OptionalDouble getIfInRange(int i, int j);

    boolean isInRange(int i, int j);
}
