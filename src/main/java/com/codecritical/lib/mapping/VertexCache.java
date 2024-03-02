package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.MoreObjects;
import eu.printingin3d.javascad.coords.Coords3d;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class VertexCache {

    final HashSet<Coords3d> cache = new HashSet<>();

    public VertexCache() {
    }

    public Coords3d get(double x, double y, double z) {
        Coords3d coords = new Coords3d(x, y, z);
        if (cache.contains(coords)) {
            return coords;
        } else {
            cache.add(coords);
            return coords;
        }
    }
    public Coords3d get(Coords3d coords) {
        if (cache.contains(coords)) {
            return coords;
        } else {
            cache.add(coords);
            return coords;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cache", cache.size())
                .toString();
    }

    public Stream<Coords3d> stream() {
        return cache.stream();
    }
}
