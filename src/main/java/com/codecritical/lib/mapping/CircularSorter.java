package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

@ParametersAreNonnullByDefault
public class CircularSorter {
    private final Coords3d centre;

    public CircularSorter(Coords3d centre) {
        this.centre = centre;
    }

    public int sortAntiClockwiseZPlane(Coords3d c0, Coords3d c1) {
        double a0 = Math.atan2(c0.getY() - centre.getY(), c0.getX() - centre.getX());
        double a1 = Math.atan2(c1.getY() - centre.getY(), c1.getX() - centre.getX());
        return Double.compare(a1, a0);  // Reverse compare to get anti-clockwise.
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("centre", centre)
                .toString();
    }

    public ImmutableList<Coords3d> sortAntiClockwiseZPlane(ImmutableList<Coords3d> list) {
        return list.stream()
                .sorted(this::sortAntiClockwiseZPlane)
                .collect(ImmutableList.toImmutableList());
    }

    public ImmutableList<Coords3d> sortAntiClockwiseZPlane(Collection<Coords3d> list) {
        return list.stream()
                .sorted(this::sortAntiClockwiseZPlane)
                .collect(ImmutableList.toImmutableList());
    }
}
