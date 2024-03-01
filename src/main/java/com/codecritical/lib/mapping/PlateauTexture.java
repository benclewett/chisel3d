package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Return a texture map, or any object, to sit in plateau space rather than a flat surface.
 */
@ParametersAreNonnullByDefault
public class PlateauTexture {

    static final Logger logger = Logger.getLogger("");

    /** HOLE texture, where a hole when more than this mm from the edge of the plateau. */
    // final static double HOLE_COUNT_RADIUS = 1.0;
    final static double HIGH = 1.0;
    final static double LOW = 0.0;

    final ConfigReader config;
    final ETextureName eTextureMapName;
    final IMapArray map;
    final PlateauCollections plateauCollection;
    final int holeRadiosCountOnMap;
    final double hollowCountRadius;
    final double hollowDepth;

    enum ETextureName {
        NONE,
        HIGH,
        LOW,
        HOLLOW
    }

    public PlateauTexture(ConfigReader config, IMapArray map, PlateauCollections plateauCollection) {
        this.config = config;
        this.map = map;
        this.plateauCollection = plateauCollection;
        this.eTextureMapName = (ETextureName)config.asOptionalEnum(ETextureName.class, Config.Fractal.Processing.PLATEAU_TEXTURE_MAP)
                .orElse(ETextureName.NONE);
        this.hollowCountRadius = config.asDouble(Config.Fractal.Processing.PLATEAU_HOLLOW_RADIUS);
        this.hollowDepth = config.asDouble(Config.Fractal.Processing.PLATEAU_HOLLOW_DEPTH);
        this.holeRadiosCountOnMap = (int)(hollowCountRadius / config.asDouble(Config.StlPrint.X_SIZE)
                * map.getISize());
        logger.info(this.toString());

        assert(this.hollowDepth >= 0.0 && this.hollowDepth <= 1.0);
    }

    public Optional<IMapArray> getTexture() {
        return switch (eTextureMapName) {
            case NONE -> Optional.empty();
            case HIGH -> Optional.of(getHigh(map));
            case LOW -> Optional.of(getLow(map));
            case HOLLOW -> Optional.of(getHollow(map, hollowDepth));
            default -> throw new RuntimeException("Texture map " + eTextureMapName + " is not known");
        };
    }

    //region Hollow

    /** Low when less that r from edge of plateau, otherwise high. */
    private IMapArray getHollow(IMapArray map, double hollowDepth) {

        var mapOut = new MapArray(map.getISize(), map.getJSize());

        var circle = getCircleOfPoints();

        map.streamPoints().forEach(p -> mapOut.set(p.i, p.j, getHollowDepth(p, circle, hollowDepth)));

        return mapOut;
    }

    private Double getHollowDepth(MapArray.Point p, ImmutableList<MapPoint> circle, double hollowDepth) {
        return (inCentreOfPlateau(p.i, p.j, circle)) ? hollowDepth : HIGH;
    }

    private boolean inCentreOfPlateau(int i, int j, ImmutableList<MapPoint> circle) {
        if (!plateauCollection.isPlateau(i, j)) {
                return false;
        }
        return circle.stream()
                .allMatch(c -> map.isInRange(c.i + i, c.j + j) && plateauCollection.isPlateau(c.i + i, c.j + j));
   }

    private ImmutableList<MapPoint> getCircleOfPoints() {
        ImmutableList.Builder<MapPoint> builder = new ImmutableList.Builder<>();
        IntStream.range(-holeRadiosCountOnMap, holeRadiosCountOnMap + 1).forEach(i ->
                IntStream.range(-holeRadiosCountOnMap, holeRadiosCountOnMap + 1).forEach(j -> {
                    if (i*i + j*j <= holeRadiosCountOnMap * holeRadiosCountOnMap) {
                        builder.add(new MapPoint(i, j));
                    }
                })
        );
        return builder.build();
    }

    //endregion

    /** Low is the default of all 0.0 */
    private IMapArray getLow(IMapArray map) {
        return new MapArray(map.getISize(), map.getJSize()).setAllValues(LOW);
    }

    /** High is the top value, = 1.0 */
    private IMapArray getHigh(IMapArray map) {
        return new MapArray(map.getISize(), map.getJSize()).setAllValues(HIGH);
    }

    record MapPoint(int i, int j) {
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("i", i)
                    .add("j", j)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("holeCountRadius", hollowCountRadius)
                .add("holeRadiusCountOnMap", holeRadiosCountOnMap)
                .add("plateauCollection.size", plateauCollection.size())
                .add("textureMapName", eTextureMapName)
                .toString();
    }
}
