package com.codecritical.build.lib.config;

public enum Config {
    RANDOM_SEED;

    public enum Mandelbrot {
        OUTPUT_FILENAME,
        MAX_ITERATIONS,
        I0,
        I1,
        J0,
        J1,
        I_COUNT,
        J_COUNT,
        SCALE_POWER,
        GAUSSIAN_RADIUS;

        public enum Print {
            X_MIN,
            X_MAX,
            Y_MIN,
            Y_MAX,
            Z_MIN,
            Z_MAX,
            BOX_OVERLAP,
            BASE_THICKNESS,
            PLATEAU_TEXTURE_MAP,
            MIN_PLATEAU_COEFFICIENT
        }

    }

    public enum InfiniteMachineConfig {
        OUTPUT_FILENAME,
        DEPTH,
        ROOT_BOX_ORIGIN,
        ROOT_BOX_SIZE;

        public enum ShaftBoxBranch {
            MIN_BRANCH_COUNT,
            MAX_BRANCH_COUNT,
            SIZE_SCALE_MIN,
            SIZE_SCALE_MAX,
            SHAFT_LENGTH_MIN,
            SHAFT_LENGTH_MAX
        }
    }
    }
