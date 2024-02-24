package com.codecritical.parts;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ShaftMultiBox implements IParts {

    private final CSG box;
    private final Dims3d size;
    private final Coords3d origin;

    public ShaftMultiBox(Coords3d origin, Dims3d size) {
        this.origin = origin;
        this.size = size;
        this.box = new Cube(size)
                .toCSG()
                .transformed(TransformationFactory.getTranlationMatrix(origin));
    }

    @Override
    public CSG getCsg() {
        return box;
    }

    @Override
    public String toString() {
        return super.toString() +
                this.getClass().getName() +
                " origin=" + origin +
                " size=" + size;
    }

    public Dims3d getSize() {
        return size;
    }

    public Coords3d getOrigin() {
        return origin;
    }
}
