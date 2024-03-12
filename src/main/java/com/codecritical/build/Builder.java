package com.codecritical.build;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.*;
import com.codecritical.lib.model.BuildPrintSurface;
import com.codecritical.lib.mapping.FastUnion;
import com.codecritical.lib.model.IBuildPrint;
import com.codecritical.parts.ExportStl;
import com.codecritical.parts.Hemisphere;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.logging.Logger;

public class Builder {
    static final Logger logger = Logger.getLogger("");

    private IMapArray map;
    private final ConfigReader config;
    private Optional<IMapArray> plateauTextureMap = Optional.empty();
    private PlateauCollections plateauCollection;
    private final ImmutableList.Builder<CSG> csg = ImmutableList.builder();

    public static Builder create(ConfigReader config, IMapArray map) {
        return new Builder(config, map);
    }

    private Builder(ConfigReader config, IMapArray map) {
        this.config = config;
        this.map = map;
        logger.info("Map build, points=" + map.stream().count());
    }

    public Builder normalise() {
        map = Mapping.normalise(map, false);
        logger.info(String.format("Normalise without zero (1%% above zero): min=%.3f, max=%.3f, mean=%.3f",
                map.getMin(), map.getMax(), map.getMean()));
        return this;
    }

    public Builder normalise(double min, double max) {
        map = Mapping.normalise(map, false, min, max);
        return this;
    }

    public Builder scale() {
        map = Mapping.scale(map, IScale.toPower(config.asDouble(Config.Fractal.Processing.SCALE_POWER)));
        map = Mapping.normalise(map);
        logger.info(String.format("Scale: min=%.3f, max=%.3f, mean=%.3f", map.getMin(), map.getMax(), map.getMean()));
        return this;
    }

    public enum ExpLnValue {
        NONE,
        LN,
        EXP
    }

    public Builder applyLog() {
        ExpLnValue expLnValue = (ExpLnValue)config.asOptionalEnum(ExpLnValue.class, Config.Fractal.Processing.APPLY_LOG).orElse(ExpLnValue.NONE);
        switch (expLnValue) {
            case NONE -> {
                return this;
            }
            case EXP -> {
                map = Mapping.mapFunction(map, p -> Math.exp(p.z));
                map = Mapping.normalise(map);
                logger.info(String.format("Apply e^z: min=%.3f, max=%.3f, mean=%.3f", map.getMin(), map.getMax(), map.getMean()));
            }
            case LN -> {
                map = Mapping.normalise(map, true, 1, 10);
                map = Mapping.mapFunction(map, p -> Math.log(p.z));
                map = Mapping.normalise(map);
                logger.info(String.format("Apply ln(z): min=%.3f, max=%.3f, mean=%.3f", map.getMin(), map.getMax(), map.getMean()));
            }
            default -> throw new RuntimeException("Bad option for Config.JuliaSet.Processing.APPLY_LOG: " + config.asInt(Config.Fractal.Processing.APPLY_LOG));
        }
        return this;
    }

    public Builder showRoughMap() {
        boolean showRoughMap = config.asBoolean(Config.Fractal.Model.SHOW_ROUGH_MAP);
        if (showRoughMap) {
            String rough = getRoughMap(map);
            logger.info("Normalised Map:\r\n" + rough);
        }
        return this;
    }

    public Builder buildPlateau() {

        plateauCollection = getPlateauCollection(map);

        plateauTextureMap = new PlateauTexture(config, map, plateauCollection)
                .getTexture();

        return this;
    }

    public Builder reportPlateau() {

        StringBuilder log = new StringBuilder();
        double minPlateauSize = config.asDouble(Config.Fractal.Processing.MIN_PLATEAU_COEFFICIENT);
        int allSize = plateauCollection.size();

        log.append(String.format("Plateaus all=%d. Plateaus greater than %.3f%% size=%d:",
                allSize, minPlateauSize * 100, plateauCollection.size()));
        plateauCollection.stream().forEach(p -> log.append("\r\n  ").append(p.toString()));

        logger.info(log.toString());

        return this;
    }

    public Builder mapToCsg() {
        IBuildPrint buildPrint = new BuildPrintSurface(config);
        ImmutableList<CSG> mapPrint = buildPrint.getPrint(map);
        logger.info("Print cells defined, count=" + mapPrint.size());

        csg.addAll(mapPrint);

        return this;
    }

    private static final String STL = ".stl";

    public Builder savePrint() {

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


    public Builder applyGaussian() {

        var plateauTextureName = PlateauTexture.getTextureName(config);
        boolean smoothTextureInsideHollow = plateauTextureName.equals(PlateauTexture.TextureName.HOLLOW)
                && config.asBoolean(Config.Fractal.Processing.PLATEAU_HOLLOW_SMOOTH_INSIDE);

        map = Gaussian.applyToMap(
                map,
                config.asOptionalDouble(Config.Fractal.Processing.GAUSSIAN_RADIUS),
                plateauCollection,
                plateauTextureMap,
                smoothTextureInsideHollow);
        logger.info(String.format("Gaussian: min=%.3f, max=%.3f, mean=%.3f", map.getMin(), map.getMax(), map.getMean()));

        map = Mapping.normalise(map);
        logger.info(String.format("Normalised: min=%.3f, max=%.3f, mean=%.3f", map.getMin(), map.getMax(), map.getMean()));

        return this;
    }

    public Builder applyPlateauTexture() {

        if (plateauTextureMap.isEmpty()) {
            return this;
        }

        var plateauTextureName = PlateauTexture.getTextureName(config);
        if (PlateauTexture.TextureName.NONE.equals(plateauTextureName)) {
            return this;
        }

        map = Mapping.applyPlateauTexture(plateauCollection, plateauTextureMap.get(), map);

        return this;
    }


    private String getRoughMap(IMapArray map) {
        StringBuilder sb = new StringBuilder();

        for (int j = 0; j < map.getJSize(); j++) {
            StringBuilder sbLine = new StringBuilder();
            for (int i = 0; i < map.getISize(); i++) {
                sbLine.append(
                        String.format("%02.0f ", map.get(i, j) * 99)
                );
            }
            sbLine.append("\r\n");
            sb.append(sbLine);
        }

        return sb.toString();
    }

    private PlateauCollections getPlateauCollection(IMapArray map) {
        double minPlateauSize = config.asDouble(Config.Fractal.Processing.MIN_PLATEAU_COEFFICIENT);

        PlateauCollections plateauSet = new PlateauCollections(map);

        plateauSet = new PlateauCollections(
                plateauSet.stream()
                        .filter(p -> p.sizeCoefficient() >= minPlateauSize)
                        .sorted(Comparator.comparingInt(Plateau::size).reversed())
        );

        return plateauSet;
    }

    public Builder addTwoGravitationalMass() {

        double x0 = 0.0;
        double x1 = config.asDouble(Config.StlPrint.X_SIZE);

        double width = x1 - x0;

        double massRadiusCoefficient = config.asDouble(Config.GravitationalWaves.MASS_RADIUS_COEFFICIENT);
        double spiralDegreesOffset = config.asDouble(Config.GravitationalWaves.SPIRAL_DEGREES_OFFSET);
        double waveRidgeCountInXAxis = config.asDouble(Config.GravitationalWaves.WAVE_RIDGE_COUNT_IN_X_AXIS);

        double massDistFromCentre = width / waveRidgeCountInXAxis / 5.0;    // Why 5?  Nobody knows.
        double massRadius = massRadiusCoefficient * width;

        double baseThickness = config.asDouble(Config.StlPrint.BASE_THICKNESS);
        double z0 = baseThickness;
        double z1 = config.asDouble(Config.StlPrint.Z_SIZE) + baseThickness;

        double[] massAngles = new double[]{
                spiralDegreesOffset - Math.PI / 2,
                spiralDegreesOffset + Math.PI / 2
        };

        double xCentre = (x0 + x1) / 2;

        double[] massCentreX = new double[2];
        double[] massCentreY = new double[2];

        for (int i = 0; i < 2; i++) {
            massCentreX[i] = massDistFromCentre * Math.sin(massAngles[i]) + xCentre;
            massCentreY[i] = 0;
        }

        var mass1 = new Hemisphere(massCentreX[0], massCentreY[0], massRadius, z0, z1).getCsg();
        var mass2 = new Hemisphere(massCentreX[1], massCentreY[1], massRadius, z0, z1).getCsg();

        csg.add(mass1);
        csg.add(mass2);

        return this;
    }

    public Builder addBoundary() {

        OptionalDouble borderHeight = config.asOptionalDouble(Config.StlPrint.BORDER_HEIGHT);
        OptionalDouble borderWidth = config.asOptionalDouble(Config.StlPrint.BORDER_WIDTH);

        if (borderHeight.isEmpty() || borderWidth.isEmpty()) {
            logger.info("No border");
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

        topBorder = topBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, ySize / 2.0 + halfBorderWidth, halfBorderHeight)));
        bottomBorder = bottomBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, -ySize / 2.0 - halfBorderWidth, halfBorderHeight)));

        leftBorder = leftBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(xSize / 2.0 + halfBorderWidth, 0, halfBorderHeight)));
        rightBorder = rightBorder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(-xSize / 2.0 - halfBorderWidth, 0, halfBorderHeight)));

        var csgBorder = topBorder.union(leftBorder).union(rightBorder).union(bottomBorder);

        csg.add(csgBorder);

        return this;
    }

    /**
     * This doesn't work where boundary bends in on it's self as the anti-clockwise sorting miss-orders
     */
    public Builder trimOutsideBase() {
        map = Mapping.trimOutsideBase(map);
        return this;
    }
}
