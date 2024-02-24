package com.codecritical.lib.config;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

public enum Config {
    RANDOM_SEED;

    public enum StlPrint {
        OUTPUT_FILENAME,
        X_MIN,
        X_MAX,
        Y_MIN,
        Y_MAX,
        Z_MIN,
        Z_MAX,
        BOX_OVERLAP,
        BASE_THICKNESS;
    }

    public enum Mandelbrot {
        OUTPUT_FILENAME;

        public enum Model {
            MAX_ITERATIONS,
            I0,
            I1,
            J0,
            J1,
            I_COUNT,
            J_COUNT;
        }

        public enum Processing {
            SCALE_POWER,
            PLATEAU_TEXTURE_MAP,
            MIN_PLATEAU_COEFFICIENT,
            PLATEAU_HOLLOW_RADIUS,
            GAUSSIAN_RADIUS,
            PLATEAU_HOLLOW_DEPTH;
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

    public enum GravitationalWaves {
        I0,
        I1,
        J0,
        J1,
        I_COUNT,
        J_COUNT,
        SPIRAL_DEGREES_OFFSET,
        WAVE_FADE_IN_WIDTH,
        WAVE_RIDGE_COUNT_IN_X_AXIS,
        MASS_RADIUS_PERCENT;

        public enum Print {
            OUTPUT_FILENAME
        }
    }
}
