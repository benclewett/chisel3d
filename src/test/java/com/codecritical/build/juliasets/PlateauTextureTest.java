package com.codecritical.build.juliasets;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.mapping.MapArray;
import com.codecritical.lib.mapping.PlateauCollections;
import com.codecritical.lib.mapping.PlateauTexture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;
import java.util.stream.IntStream;

class PlateauTextureTest {

    static final Logger logger = Logger.getLogger("");

    static final int MAP_SIZE = 20;
    private static final double MIN = 0.0;
    private static final double MAX = 1.0;
    private IMapArray map;

    @BeforeEach
    void beforeEach() {
        var mapNew = new MapArray(MAP_SIZE, MAP_SIZE);
        mapNew.setAllValues(MIN);
        // Hole touching edge
        IntStream.range(2, 10).forEach(i ->
                IntStream.range(0, MAP_SIZE - 5).forEach(j ->
                        mapNew.set(i, j, MAX)
                )
        );
        // True holes
        IntStream.range(12, 18).forEach(i ->
                IntStream.range(2, 8).forEach(j ->
                        mapNew.set(i, j, MAX)
                )
        );
        IntStream.range(12, 17).forEach(i ->
                IntStream.range(10, 15).forEach(j ->
                        mapNew.set(i, j, MAX)
                )
        );
        this.map = mapNew;
    }

    @Test
    void testHollow() {

        showMap("Raw Map:", map);

        PlateauCollections plateauCollection = new PlateauCollections(map);

        showMap("Plateau (as a map)", plateauCollection.asMapArray());

        ConfigReader config = new ConfigReader()
                .add("Config.StlPrint.X_MAX", 10.0)
                .add("Config.StlPrint.Y_MAX", 10.0)
                .add("Config.StlPrint.X_MIN", 0.0)
                .add("Config.StlPrint.Y_MIN", 0.0)
                .add("Config.JuliaSet.Processing.PLATEAU_TEXTURE_MAP", "HOLLOW")
                .add("Config.JuliaSet.Processing.PLATEAU_HOLLOW_RADIUS", 1.0)
                .add("Config.JuliaSet.Processing.PLATEAU_HOLLOW_DEPTH", 0.5);

        var plateauTexture = new PlateauTexture(config, map, plateauCollection).getTexture();

        var texture = plateauTexture.orElse(null);

        if (texture == null) {
            throw new RuntimeException("texture is null");
        }

        showMap("Texture", texture);

        MapArray mapUnion = new MapArray(map.getISize(), map.getJSize());
        mapUnion.streamPoints().forEach(p -> mapUnion.set(
                p.i,
                p.j,
                (plateauCollection.isPlateau(p)) ? texture.get(p.i, p.j) : map.get(p.i, p.j))
        );

        showMap("Union", mapUnion);
    }

    private void showMap(String title, IMapArray m) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        IntStream.range(0, m.getJSize()).forEach(j -> {
            sb.append("\r\n    ");
            IntStream.range(0, m.getISize())
                    .forEach(i -> sb.append(mapToChar(m.get(i, j))));
        });
        logger.info(sb.toString());
    }

    private String mapToChar(double v) {
        if (v == MIN) {
            return ". ";
        } else if (v == MAX) {
            return "[]";
        } else {
            return "<>";
        }
    }
}
