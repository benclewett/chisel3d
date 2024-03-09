package com.codecritical.parts;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.context.ColorHandlingContext;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.Sphere;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Hemisphere implements IParts {

    private final CSG csg;
    private final Dims3d size;
    private final Coords3d origin;


    public Hemisphere(double x, double y, double radius, double z0, double z1) {
        this.size = new Dims3d(radius, radius, z1 - z0);
        this.origin = new Coords3d(x, y, z0);
        this.csg = buildShape();
    }

    private CSG buildShape() {
        // Create a sphere with origin on the bottom plane
        var sphere = new Sphere(Radius.fromRadius(1.0)).toCSG();
        sphere = sphere.transformed(TransformationFactory.getScaleMatrix(size));
        sphere = sphere.transformed(TransformationFactory.getTranlationMatrix(origin));

        // Create a cube to cut off the bottom (size is 2x radius)
        var cube = new Cube(new Dims3d(size.getX() * 2, size.getY() * 2, size.getZ())).toCSG();
        cube = cube.transformed(TransformationFactory.getTranlationMatrix(origin));
        cube = cube.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, 0, -size.getZ() / 2)));

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
