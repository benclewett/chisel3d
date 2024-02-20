package com.codecritical.build.mandelbrot;

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.*;
import com.codecritical.lib.model.FastUnion;
import com.codecritical.parts.ExportStl;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Comparator;
import java.util.Optional;
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
        Build.createMandelbrot(config)
            .normalise()
            .scale()
            .showRoughMap()
            .buildPlateau()
            .reportPlateau()
            .applyGaussian()
            .savePrint();
    }

    static class Build {

        IMapArray map;
        final ConfigReader config;
        Optional<IMapArray> plateauTexture = Optional.empty();
        PlateauCollections plateauCollection;

        static Build createMandelbrot(ConfigReader config) {
            return new Build(config);
        }

        private Build(ConfigReader config) {
            this.config = config;
            this.map = new BuildMandelbrot(config).getMap();
            logger.info("Map build, points=" + map.stream().count());
        }

        public Build normalise() {
            map = Mapping.normalise(map, false);
            return this;
        }

        public Build scale() {
            map = Mapping.scale(map, IScale.toPower(config.asDouble(Config.Mandelbrot.Processing.SCALE_POWER)));
            map = Mapping.normalise(map, true);
            return this;
        }

        public Build showRoughMap() {
            String rough = getRoughMap(map);
            logger.info("Normalised Map:\r\n" + rough);
            return this;
        }

        public Build buildPlateau() {

            plateauCollection = getPlateauCollection(map);

            plateauTexture = new PlateauTexture(config, map, plateauCollection).getTexture(0.0);

            return this;
        }

        public Build reportPlateau() {

            StringBuilder log = new StringBuilder();
            double minPlateauSize = config.asDouble(Config.Mandelbrot.Processing.MIN_PLATEAU_COEFFICIENT);
            int allSize = plateauCollection.size();

            log.append(String.format("Plateaus all=%d. Plateaus greater than %.3f%% size=%d:",
                    allSize, minPlateauSize * 100, plateauCollection.size()));
            plateauCollection.stream().forEach(p -> log.append("\r\n  ").append(p.toString()));

            logger.info(log.toString());

            return this;
        }

        public Build savePrint() {
            // IBuildPrint buildPrint = new BuildPrintBox(config);
            IBuildPrint buildPrint = new BuildPrintSurface(config);
            var print = buildPrint.getPrint(map);
            logger.info("Print cells defined, count=" + print.size());

            var csg = FastUnion.fastUnion(print);
            logger.info("Union complete.");

            ExportStl.export(config.asString(Config.Mandelbrot.Print.OUTPUT_FILENAME), csg);

            return this;
        }


        public Build applyGaussian() {
            map = Mapping.gaussian(map, config.asOptionalDouble(Config.Mandelbrot.Processing.GAUSSIAN_RADIUS), plateauCollection, plateauTexture);
            map = Mapping.normalise(map, true);
            logger.info("Gaussian applied.");

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
            double minPlateauSize = config.asDouble(Config.Mandelbrot.Processing.MIN_PLATEAU_COEFFICIENT);

            PlateauCollections plateauSet = new PlateauCollections(map);

            plateauSet = new PlateauCollections(
                    plateauSet.stream()
                            .filter(p -> p.sizeCoefficient() >= minPlateauSize)
                            .sorted(Comparator.comparingInt(Plateau::size).reversed())
            );

            return plateauSet;
        }
    }
}
