package com.codecritical.parts;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.Sphere;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Hemisphere implements IParts {

    private final CSG csg;
    private final Dims3d size;
    private final Coords3d origin;


    public Hemisphere(double x, double y, double radius, double z0, double z1) {
        this.size = new Dims3d(radius, radius, z1 - z0);
        this.origin = new Coords3d(x, y, z0);
        this.csg = buildShape(radius, z0, z1);
    }

    private CSG buildShape(double radius, double z0, double z1) {
        // Create a sphere with origin on the bottom plane
        var sphere = new Sphere(Radius.fromRadius(radius)).toCSG();
        sphere.transformed(TransformationFactory.getScaleMatrix(new Coords3d(radius, radius, z1 - z0)));

        // Create a cube to cut off the bottom
        var cube = new Cube(radius).toCSG();
        cube.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, 0, -radius)));

        return sphere.difference(cube);
    }

    @Override
    public CSG getCsg() {
        return csg;
    }

    @Override
    public Dims3d getSize() {
        return size;
    }

    @Override
    public Coords3d getOrigin() {
        return origin;
    }
}
