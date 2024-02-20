package com.codecritical.build.mandelbrot;

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.mapping.MapArray;
import com.codecritical.lib.mapping.Plateau;
import com.codecritical.lib.mapping.PlateauCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.stream.IntStream;

class PlateauTextureTest {

    static final int MAP_SIZE = 20;
    private static final double MIN = 0.0;
    private static final double MAX = 1.0;
    IMapArray map;

    @BeforeEach
    void beforeEach() {
        var mapNew = new MapArray(MAP_SIZE, MAP_SIZE);
        mapNew.setAllValues(MIN);
        IntStream.range(5, MAP_SIZE - 5).forEach(i ->
                IntStream.range(0, MAP_SIZE - 5).forEach(j ->
                        mapNew.set(i, j, MAX)
                )
        );
        this.map = mapNew;
    }

    @Test
    void testHollow() {

        showMap("Raw Map:", map);

        PlateauCollections plateauCollection = new PlateauCollections(map);

        var plateauMap = plateauCollection.stream()
                .findFirst()
                .map(Plateau::asMapArray)
                .get();

        showMap("Plateau (as a map)", plateauMap);

        ConfigReader config = new ConfigReader()
                .add("Config.Mandelbrot.Processing.PLATEAU_TEXTURE_MAP", "HOLLOW")
                .add("Config.Mandelbrot.Print.X_MAX", "10")
                .add("Config.Mandelbrot.Print.Y_MAX", "10")
                .add("Config.Mandelbrot.Print.X_MIN", "0")
                .add("Config.Mandelbrot.Print.Y_MIN", "0");

        var plateauTexture = new PlateauTexture(config, map, plateauCollection).getTexture(0.5);

        var texture = plateauTexture.get();

        showMap("Texture", texture);

        MapArray mapUnion = new MapArray(map.getISize(), map.getJSize());
        mapUnion.streamPoints().forEach(p -> {
            mapUnion.set(
                    p.i,
                    p.j,
                (plateauCollection.isPlateau(p)) ? texture.get(p.i, p.j) : map.get(p.i, p.j));
        });

        showMap("Union", mapUnion);

    }


    private void showMap(String title, IMapArray m) {
        System.out.println(title);
        IntStream.range(0, m.getJSize()).forEach(j -> {
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            IntStream.range(0, m.getISize()).forEach(i -> sb.append(mapToChar(m.get(i, j))));
            System.out.println(sb.toString());
        });
    }

    private String mapToChar(double v) {
        if (v == MIN) {
            return "..";
        } else if (v == MAX) {
            return "##";
        } else {
            return "**";
        }
    }
}
