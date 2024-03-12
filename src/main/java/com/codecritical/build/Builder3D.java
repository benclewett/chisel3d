package com.codecritical.build;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.FastUnion;
import com.codecritical.lib.model.JuliaSet3D;
import com.codecritical.parts.ExportStl;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class Builder3D {
    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;
    private final ImmutableList.Builder<CSG> csg = ImmutableList.builder();
    private final JuliaSet3D map;
    private final double xRange, yRange, zRange;
    private final double xMin, xMax, yMin, yMax, zMin, zMax;
    private final double blockSize;
    private final Dims3d blockSize3D;


    public static Builder3D create(ConfigReader config, JuliaSet3D map) {
        return new Builder3D(config, map);
    }

    private Builder3D(ConfigReader config, JuliaSet3D map) {
        this.config = config;
        this.map = map;

        this.xRange = config.asDouble(Config.StlPrint.X_SIZE);
        this.yRange = config.asDouble(Config.StlPrint.Y_SIZE);
        this.zRange = config.asDouble(Config.StlPrint.Z_SIZE);

        this.xMin = -xRange / 2;
        this.xMax = xRange / 2;
        this.yMin = -yRange / 2;
        this.yMax = yRange / 2;
        this.zMin = 0.0;
        this.zMax = zRange;

        this.blockSize = config.asDouble(Config.StlPrint.BLOCK_SIZE_3D);
        this.blockSize3D = new Dims3d(blockSize, blockSize, blockSize);
    }

    public Builder3D buildModel() {
        map.stream()
                .map(this::translateCoordinateSet)
                .map(this::createCube)
                .forEach(csg::add);
        return this;
    }

    private CSG createCube(Coords3d coords3d) {
        var cube = new Cube(blockSize3D).toCSG();
        cube = cube.transformed(TransformationFactory.getTranlationMatrix(coords3d));
        return cube;
    }

    private Coords3d translateCoordinateSet(Coords3d c) {
        return new Coords3d(
                mapX(c.getX()),
                mapY(c.getY()),
                mapZ(c.getZ())
        );
    }

    private double mapX(double i) {
        return (i - map.getIMin()) / map.getISize() * xRange + xMin;
    }

    private double mapY(double j) {
        return (j - map.getJMin()) / map.getJSize() * yRange + yMin;
    }

    private double mapZ(double k) {
        return (k - map.getJMin()) / map.getKSize() * zRange + zMin;
    }

    private static final String STL = ".stl";

    public Builder3D savePrint() {

        String fileName = config.asString(Config.OUTPUT_FILENAME);

        if (!fileName.toLowerCase().endsWith(STL)) {
            fileName = fileName + STL;
        }

        var csgUnion = FastUnion.fastUnion(csg.build());
        logger.info("Union complete.");

        ExportStl.export(fileName, csgUnion);
        logger.info("STL file written to: " + fileName);

        return this;
    }

}
