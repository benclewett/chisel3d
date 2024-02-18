package com.codecritical.build.mandelbrot;


import com.codecritical.build.lib.mapping.IMapArray;
import com.codecritical.build.lib.mapping.MapArray;
import com.codecritical.build.lib.config.Config;
import com.codecritical.build.lib.config.ConfigReader;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

/**
 * Return a texture map, or any object, to sit in plateau space rather than a flat surface.
 */
@ParametersAreNonnullByDefault
public class PlateauMap {

    final ConfigReader config;
    final IMapArray map;
    final Optional<String> textureMapName;

    public PlateauMap(ConfigReader config, IMapArray map) {
        this.config = config;
        this.map = map;
        this.textureMapName = config.asOptionalString(Config.Mandelbrot.Print.PLATEAU_TEXTURE_MAP);
    }

    public Optional<IMapArray> get() {
        if (textureMapName.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(switch (textureMapName.get()) {
            default -> throw new RuntimeException("Texture map " + textureMapName.get() + " is not known");
            case "HIGH" -> getHigh();
            case "LOW" -> getLow();
        });
    }

    /** Low is the default of all 0.0 */
    private IMapArray getLow() {
        return new MapArray(map.getISize(), map.getJSize()).setAllValues(0.0);
    }

    /** High is the top value, = 1.0 */
    private IMapArray getHigh() {
        return new MapArray(map.getISize(), map.getJSize()).setAllValues(1.0);
    }

}
