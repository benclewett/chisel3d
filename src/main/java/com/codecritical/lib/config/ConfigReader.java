package com.codecritical.lib.config;

import com.codecritical.build.mandelbrot.PlateauTexture;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Dims3d;

import java.util.*;
import java.util.logging.Logger;

public class ConfigReader {
    static final Logger logger = Logger.getLogger("");

    final Properties properties;

    public ConfigReader(Properties properties) {
        this.properties = properties;
    }

    public ConfigReader() {
        this.properties = new Properties();
    }

    public ConfigReader add(String name, String value) {
        this.properties.setProperty(name.toString(), value);
        return this;
    }

    public OptionalInt asOptionalInt(Enum config) {
        var i = get(config);
        return i.map(s -> OptionalInt
                .of(Integer.parseInt(s)))
                .orElseGet(OptionalInt::empty);
    }

    public Integer asInt(Enum config) {
        var i = get(config);
        if (i.isEmpty()) {
            throw new RuntimeException("Missing value for: " + config);
        }
        return Integer.parseInt(i.get());
    }

    public OptionalLong asOptionalLong(Enum config) {
        var i = get(config);
        return i.map(s -> OptionalLong
                .of(Long.parseLong(s)))
                .orElseGet(OptionalLong::empty);
    }

    public Long asLong(Enum config) {
        var i = get(config);
        if (i.isEmpty()) {
            throw new RuntimeException("Missing value for: " + config);
        }
        return Long.parseLong(i.get());
    }

    public Optional<String> asOptionalString(Enum config) {
        return get(config);
    }

    public String asString(Enum config) {
        var i = get(config);
        if (i.isEmpty()) {
            throw new RuntimeException("Missing value for: " + config);
        }
        return i.get();
    }

    public OptionalDouble asOptionalDouble(Enum config) {
        var i = get(config);
        try {
            return i.map(s -> OptionalDouble
                            .of(Double.parseDouble(s)))
                    .orElseGet(OptionalDouble::empty);
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    public double asDouble(Enum config) {
        var i = get(config);
        if (i.isEmpty()) {
            throw new RuntimeException("Missing value for: " + config);
        }
        return Double.parseDouble(i.get());
    }

    public Dims3d asDims3d(Enum config) {
        var l = asImmutableList(config);
        if (3 != l.size()) {
            throw new RuntimeException("Config " + config + " should have 3 values");
        }
        return new Dims3d(
                Double.parseDouble(l.get(0)),
                Double.parseDouble(l.get(1)),
                Double.parseDouble(l.get(2))
        );
    }

    public ImmutableList<String> asImmutableList(Enum config) {
        var i = get(config);
        if (i.isEmpty()) {
            throw new RuntimeException("Missing value for: " + config);
        }
        var tokens = i.get().split("[:,]");
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (var t : tokens) {
            builder.add(t);
        }
        return builder.build();
    }

    private Optional<String> get(Enum config) {
        String key = config.getClass().getName() + "." + config.toString();
        key = key.replace("$", ".");
        key = key.substring(key.indexOf("Config"));
        if (!properties.containsKey(key)) {
            logger.severe("Missing config: " + key);
            throw new RuntimeException("Missing config: " + key);
        }
        var o = properties.get(key);
        return (o == null || (o.getClass() == String.class && o.equals("")))
                ? Optional.empty()
                : Optional.of(o.toString());
    }

    public <T extends Enum<T>> Optional<T> asEnum(Class<T> clazz, Enum config) {
        var val = get(config);
        if (val.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Enum.valueOf(clazz, val.get()));
    }

}
