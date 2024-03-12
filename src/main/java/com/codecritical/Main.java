package com.codecritical;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import java.util.Arrays;

public class Main {

    public enum ModelName {
        INFINITE_MACHINE,
        MANDELBROT,
        MANDELBROT_CUBIC,
        MANDELBROT_BUDDHA,
        GRAVITATIONAL_WAVES,
        BURNING_SHIP,
        JULIA_SET,
        MANDELBROT_3D
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            throw new RuntimeException("Please pass the model type as the only argument: " + Arrays.asList(ModelName.values()));
        }

        ModelName model;
        try {
            model = ModelName.valueOf(args[0]);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot convert '" + args[0] + "' to one of " + Arrays.asList(ModelName.values()));
        }

        new Container(model);
    }
}