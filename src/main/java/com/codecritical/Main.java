package com.codecritical;

import java.util.Arrays;

public class Main {

    enum EModel {
        INFINITE_MACHINE,
        MANDELBROT
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            throw new RuntimeException("Please pass the model type as the only argument: " + Arrays.asList(EModel.values()));
        }

        EModel model;
        try {
            model = EModel.valueOf(args[0]);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot convert '" + args[0] + "' to one of " + Arrays.asList(EModel.values()));
        }

        new Container(model);
    }
}