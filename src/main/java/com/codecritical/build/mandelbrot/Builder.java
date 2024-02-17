package com.codecritical.build.mandelbrot;

import com.codecritical.build.lib.*;
import com.codecritical.build.lib.config.Config;
import com.codecritical.build.lib.config.ConfigReader;
import com.codecritical.parts.ExportStl;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class Builder {
    static final Logger logger = Logger.getLogger("");
    private final ConfigReader config;

    public Builder(ConfigReader config) {
        this.config = config;

        try {
            build();
        } catch (Exception ex) {
            logger.severe("Exception: " + ex);
            throw new RuntimeException(ex);
        }
    }

    private void build() {

        var map = new BuildMandelbrot(config).getMap();
        logger.info("Map build, points=" + map.stream().count());

        map = Mapping.normalise(map, false);
        map = Mapping.scale(map, IScale.toPower(config.asDouble(Config.Mandelbrot.SCALE_POWER)));
        map = Mapping.normalise(map, true);

        String rough = getRoughMap(map);
        logger.info("Normalised Map:\r\n" + rough);

        PlateauSet plateauSet = getPlateauSet(map);

        map = Mapping.gaussian(map, config.asOptionalDouble(Config.Mandelbrot.GAUSSIAN_RADIUS), plateauSet);
        map = Mapping.normalise(map, true);
        logger.info("Gaussian applied.");

        // IBuildPrint buildPrint = new BuildPrintBox(config);
        IBuildPrint buildPrint = new BuildPrintSurface(config);
        var print = buildPrint.getPrint(map);

        logger.info("Print cells defined, count=" + print.size());

        var csg = FastUnion.fastUnion(print);
        logger.info("Union complete.");

        ExportStl.export(config.asString(Config.Mandelbrot.OUTPUT_FILENAME), csg);
    }

    private PlateauSet getPlateauSet(IMapArray map) {

        StringBuilder log = new StringBuilder();

        double minPlateauSize = config.asDouble(Config.Mandelbrot.Print.MIN_PLATEAU_COEFFICIENT);

        PlateauSet plateauSet = new PlateauSet(map);
        int allSize = plateauSet.size();

        plateauSet = new PlateauSet(
                plateauSet.stream()
                        .filter(p -> p.sizeCoefficient() >= minPlateauSize)
                        .sorted(Comparator.comparingInt(Plateau::size).reversed())
        );

        log.append(String.format("Plateaus all=%d. Plateaus greater than %.3f%% size=%d:",
                allSize, minPlateauSize * 100, plateauSet.size()));
        plateauSet.stream().forEach(p -> log.append("\r\n  ").append(p.toString()));

        logger.info(log.toString());

        return plateauSet;
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

}
