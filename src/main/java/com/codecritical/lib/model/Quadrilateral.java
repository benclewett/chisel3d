package com.codecritical.lib.model;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.vrl.Polygon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Build Square by adding either 1 or 4 coplanar polygons.  Used to encapsulate a square tile.
 * (In the future this will allow tiles to be divided, to allow plastic deformation of the model.)
 * <br>
 * <pre>
 *   1 v3 +-----------------+ v2
 *        | \     p2      / |
 *        |   \         /   |
 *        |     \     /     |
 *        |       \ /       |
 *   y/j  |  p3   v4    p4  |
 *        |       / \       |
 *        |     /     \     |
 *        |   /         \   |
 *        | /      p1     \ |
 *   0 v0 +-----------------+ v1
 *        0        x/i      1
 * </pre>
 */
@ParametersAreNonnullByDefault
public class Quadrilateral {

    private static final Color COLOR = Color.WHITE;

    public final ImmutableList<Polygon> polygons;
    public final Coords3d v0, v1, v2, v3;

    /** Parr coordinates in, in anti-clockwise direction. */
    public Quadrilateral(Coords3d v0, Coords3d v1, Coords3d v2, Coords3d v3) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.polygons = buildPolygons();
    }

    public static Iterable<Polygon> create(Coords3d v0, Coords3d v1, Coords3d v2, Coords3d v3) {
        return new Quadrilateral(v0, v1, v2, v3).polygons;
    }

    private ImmutableList<Polygon> buildPolygons() {

        // Not a quadrilateral?
        Set<Coords3d> pUnique = new HashSet<>();
        pUnique.add(v0);
        pUnique.add(v1);
        pUnique.add(v2);
        pUnique.add(v3);
        if (pUnique.size() == 3) {
            // Add them in the correct order.
            List<Coords3d> l = new ArrayList<>();
            l.add(v0);
            if (!l.contains(v1)) {
                l.add(v1);
            }
            if (!l.contains(v2)) {
                l.add(v2);
            }
            if (!l.contains(v3)) {
                l.add(v3);
            }
            return ImmutableList.of(Polygon.fromPolygons(l, COLOR));
        } else if (pUnique.size() < 3) {
            return ImmutableList.of();
        }

        ImmutableList.Builder<Polygon> builder = ImmutableList.builder();

        if ((v0.getZ() == v1.getZ() && v2.getZ() == v3.getZ()) || (v0.getZ() == v1.getZ() && v2.getZ() == v3.getZ())) {
            // Coplanar, needs just one polygon.
            addPolygons(builder, v0, v1, v2, v3);
        } else {
            // Complex.  Break up into 4x polygons.
            var v4 = new Coords3d(
                    (v0.getX() + v1.getX() + v2.getX() + v3.getX()) / 4.0,
                    (v0.getY() + v1.getY() + v2.getY() + v3.getY()) / 4.0,
                    (v0.getZ() + v1.getZ() + v2.getZ() + v3.getZ()) / 4.0
            );
            addPolygons(builder, v1, v4, v0);
            addPolygons(builder, v2, v3, v4);
            addPolygons(builder, v4, v3, v0);
            addPolygons(builder, v2, v4, v1);
        }

        return builder.build();
    }

    private void addPolygons(ImmutableList.Builder<Polygon> polygons, Coords3d... vs) {
        polygons.add(Polygon.fromPolygons(List.of(vs), COLOR));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("polygons", this.polygons)
                .add("v0", this.v0)
                .add("v1", this.v1)
                .add("v2", this.v2)
                .add("v3", this.v3)
                .toString();
    }

    public Collection<Polygon> getPolygons() {
        return polygons;
    }
}
