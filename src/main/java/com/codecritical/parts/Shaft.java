package com.codecritical.parts;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.Angles3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Shaft implements IParts {

    final CSG shaft;
    final Dims3d size;
    final Dims3d origin;

    public Shaft(Dims3d origin, double length, double radius, Axis axis) {
        this.origin = origin;
        this.size = new Dims3d(length, radius * 2, radius * 2);
        var cylinder = new Cylinder(length, Radius.fromRadius(radius))
                .toCSG();

        cylinder = switch (axis) {
            case X_PLUS, X_MINUS -> cylinder.transformed(TransformationFactory.getRotationMatrix(Angles3d.ROTATE_PLUS_Y));
            case Y_PLUS, Y_MINUS -> cylinder.transformed(TransformationFactory.getRotationMatrix(Angles3d.ROTATE_PLUS_X));
            case Z_PLUS, Z_MINUS -> cylinder;
        };

        this.shaft = cylinder
                .transformed(TransformationFactory.getTranlationMatrix(origin));

    }

    @Override
    public CSG getCsg() {
        return shaft;
    }

    @Override
    public Dims3d getSize() {
        return size;
    }

    @Override
    public Dims3d getOrigin() {
        return origin;
    }
}
