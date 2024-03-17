package com.codecritical.build;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.build.juliasets3d.MandelbrotStandard3dMap;
import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.FastUnion;
import com.codecritical.lib.model.JuliaSet3D;
import com.codecritical.lib.model.MapPoint3D;
import com.codecritical.parts.ExportStl;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import eu.printingin3d.javascad.coords.Angles3d;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalDouble;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class Builder3D {
    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;
    private ImmutableList.Builder<CSG> csg = ImmutableList.builder();
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

        this.blockSize = config.asDouble(Config.StlPrint.BLOCK_SIZE_3D) * 1.001;
        this.blockSize3D = new Dims3d(blockSize, blockSize, blockSize);
    }

    public Builder3D buildModel() {
        map.stream()
                .map(this::translateCoordinateSet)
                .map(this::mapPointToPillar)
                .forEach(csg::add);
        logger.info("CSG blocks created.");
        return this;
    }

    private CSG mapPointToPillar(MapPoint3D point) {
        var cube = new Cube(blockSize3D).toCSG();
        if (point.iLength != 1) {
            cube = cube
                    .transformed(TransformationFactory.getScaleMatrix(point.iLength, 1.0, 1.0))
                    .transformed(TransformationFactory.getTranlationMatrix(new Coords3d(blockSize3D.getX() * (point.iLength - 1) / 2.0, 0, 0)));
        }
        cube = cube.transformed(TransformationFactory.getTranlationMatrix(point.getCoors3d()));
        return cube;
    }

    private MapPoint3D translateCoordinateSet(MapPoint3D c) {
        return c.mutate()
                .setI(this::mapX)
                .setJ(this::mapY)
                .setK(this::mapZ)
                .build();
    }

    private double mapX(double i) {
        return (i - map.getIMin()) / map.getISize() * xRange + xMin;
    }

    private double mapY(double j) {
        return (j - map.getJMin()) / map.getJSize() * yRange + yMin;
    }

    private double mapZ(double k) {
        return (k - map.getKMin()) / map.getKSize() * zRange + zMin;
    }

    private static final String STL = ".stl";

    @CanIgnoreReturnValue
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

    public Builder3D addBase() {

        double baseThickness = config.asOptionalDouble(Config.StlPrint.BASE_THICKNESS).orElse(0);
        if (baseThickness == 0) {
            logger.info("No base?");
            return this;
        }

        var base = new Cube(1.0)
                .toCSG()
                .transformed(TransformationFactory.getScaleMatrix(new Coords3d(
                        xRange, yRange, baseThickness
                )))
                .transformed(TransformationFactory.getTranlationMatrix(new Coords3d(
                        0.0, 0.0, -baseThickness / 2.0
                )));

        if (
                config.isPresent(Config.StlPrint.Settings3D.SHADOW)
                        && config.isPresent(Config.StlPrint.Settings3D.SHADOW_Z)
                        && config.asBoolean(Config.StlPrint.Settings3D.SHADOW)
                        && config.isPresent(Config.StlPrint.Settings3D.SHADOW_DEPTH)
        ) {
            var shadowConfig = config.clone();
            double zLine = config.asDouble(Config.StlPrint.Settings3D.SHADOW_Z);
            double shadowDepth = config.asDouble(Config.StlPrint.Settings3D.SHADOW_DEPTH);
            shadowConfig.add("Config.Fractal.Model.K0", zLine);
            shadowConfig.add("Config.Fractal.Model.K1", zLine);
            shadowConfig.add("Config.StlPrint.Z_SIZE", blockSize);
            var shadow = new MandelbrotStandard3dMap(shadowConfig)
                    .getMap();
            var stlShadowPillars = shadow.stream()
                    .map(this::translateCoordinateSet)
                    .map(this::setKZero)
                    .map(this::mapPointToPillar)
                    .toList();
            var stlShadow = FastUnion.fastUnion(stlShadowPillars);
            stlShadow = stlShadow.transformed(TransformationFactory.getScaleMatrix(1, 1, shadowDepth / blockSize));
            stlShadow = stlShadow.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, 0, -shadowDepth / 2.0)));
            //  csg.add(stlShadow);
            base = base.difference(stlShadow);
            // base = stlShadow;
        }

        csg.add(base);

        return this;
    }

    private MapPoint3D setKZero(MapPoint3D p) {
        return p.mutate().setK(0.0).build();
    }

    public Builder3D addBoundary() {

        OptionalDouble borderHeight = config.asOptionalDouble(Config.StlPrint.BORDER_HEIGHT);
        OptionalDouble borderWidth = config.asOptionalDouble(Config.StlPrint.BORDER_WIDTH);
        double baseThickness = config.asOptionalDouble(Config.StlPrint.BASE_THICKNESS).orElse(0);

        if (borderHeight.isEmpty() || borderWidth.isEmpty()) {
            logger.info("No Boundary");
            return this;
        }

        logger.info(String.format("Border width=%.1f height=%.1f", borderWidth.getAsDouble(), borderHeight.getAsDouble()));

        double xSize = config.asDouble(Config.StlPrint.X_SIZE);
        double ySize = config.asDouble(Config.StlPrint.Y_SIZE);

        double halfBorderWidth = borderWidth.getAsDouble() / 2.0;
        double halfBorderHeight = borderHeight.getAsDouble() / 2.0;

        double slither = -0.1;

        var topBorder = new Cube(new Dims3d(xSize, borderWidth.getAsDouble() - slither, borderHeight.getAsDouble())).toCSG();
        var bottomBorder = new Cube(new Dims3d(xSize, borderWidth.getAsDouble() - slither, borderHeight.getAsDouble())).toCSG();

        var leftBorder = new Cube(new Dims3d(borderWidth.getAsDouble() - slither, ySize + 2 * borderWidth.getAsDouble(), borderHeight.getAsDouble())).toCSG();
        var rightBorder = new Cube(new Dims3d(borderWidth.getAsDouble() - slither, ySize + 2 * borderWidth.getAsDouble(), borderHeight.getAsDouble())).toCSG();

        topBorder = topBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, ySize / 2.0 + halfBorderWidth, halfBorderHeight - baseThickness)));
        bottomBorder = bottomBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, -ySize / 2.0 - halfBorderWidth, halfBorderHeight - baseThickness)));

        leftBorder = leftBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(xSize / 2.0 + halfBorderWidth, 0, halfBorderHeight - baseThickness)));
        rightBorder = rightBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(-xSize / 2.0 - halfBorderWidth, 0, halfBorderHeight - baseThickness)));

        var csgBorder = topBorder.union(leftBorder).union(rightBorder).union(bottomBorder);

        csg.add(csgBorder);

        return this;
    }

    private CSG addTilt(CSG model) {

        if (!config.isPresent(Config.StlPrint.Settings3D.TILT_DEGREES_IN_X)
                || !config.isPresent(Config.StlPrint.Settings3D.TILT_DEGREES_IN_Y)
                || !config.isPresent(Config.StlPrint.Settings3D.TILT_DEGREES_IN_Z)
                || !config.isPresent(Config.StlPrint.Settings3D.MODEL_MOVE)
        ) {
            return model;
        }

        final double degInX = config.asDouble(Config.StlPrint.Settings3D.TILT_DEGREES_IN_X);
        final double degInY = config.asDouble(Config.StlPrint.Settings3D.TILT_DEGREES_IN_Y);
        final double degInZ = config.asDouble(Config.StlPrint.Settings3D.TILT_DEGREES_IN_Z);
        final Coords3d coords = config.asCoors3d(Config.StlPrint.Settings3D.MODEL_MOVE);

        model = model
                .transformed(TransformationFactory.getRotationMatrix(new Angles3d(degInX, degInY, degInZ)))
                .transformed(TransformationFactory.getTranlationMatrix(coords));

        return model;
    }

    public Builder3D transformToCompleteModel() {

        CSG csgModel = FastUnion.fastUnion(csg.build());

        csgModel = addTilt(csgModel);

        csgModel = cutOffNegative(csgModel);

        csg = new ImmutableList.Builder<CSG>();
        csg.add(csgModel);

        return this;
    }

    private CSG cutOffNegative(CSG csgModel) {

        double baseThickness = config.asDouble(Config.StlPrint.BASE_THICKNESS);

        double depth = Math.max(xRange, yRange);

        CSG negative = new Cube(1.0)
                .toCSG()
                .transformed(TransformationFactory.getScaleMatrix(xRange, yRange, depth))
                .transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, 0, -depth / 2 - baseThickness)));

        return csgModel.difference(negative);
    }
}
