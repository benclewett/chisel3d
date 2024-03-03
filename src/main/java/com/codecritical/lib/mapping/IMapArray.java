package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalDouble;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public interface IMapArray {
    double get(int i, int j);
    MapArray.Point getPoint(int i, int j);
    double get(MapArray.Point p);
    boolean isNull(int i, int j);
    int getISize();
    int getJSize();
    double getMax();
    double getMin();
    double getMean();

    int size();

    Stream<Double> stream();

    Stream<MapArray.Point> streamPoints();

    OptionalDouble getIfInRange(int i, int j);

    boolean isInRange(int i, int j);
}
