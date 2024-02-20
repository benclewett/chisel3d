package com.codecritical.lib.model;

import com.google.common.base.MoreObjects;
import eu.printingin3d.javascad.coords.Coords3d;

import java.util.HashSet;
import java.util.stream.Stream;

public class Coords3dCache {

    final HashSet<Coords3d> cache = new HashSet<>();

    public Coords3dCache() {
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
