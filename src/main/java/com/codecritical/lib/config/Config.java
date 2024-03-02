package com.codecritical.lib.config;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

public enum Config {
    RANDOM_SEED;

    public enum StlPrint {
        X_SIZE,
        Y_SIZE,
        Z_SIZE,
        BOX_OVERLAP,
        BASE_THICKNESS,
        SHAPE
    }

    public enum Fractal {
        OUTPUT_FILENAME;

        public enum Model {
            MAX_ITERATIONS,
            I0,
            I1,
            J0,
            J1,
            I_COUNT,
            J_COUNT,
            I_SCALE,
            J_SCALE,
            POLAR_COORDINATES
        }

        public enum Processing {
            SCALE_POWER,
            PLATEAU_TEXTURE_MAP,
            MIN_PLATEAU_COEFFICIENT,
            PLATEAU_HOLLOW_RADIUS,
            GAUSSIAN_RADIUS,
            PLATEAU_HOLLOW_DEPTH
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
        MASS_RADIUS_COEFFICIENT,
        WAVE_HEIGHT,
        PERSPECTIVE_ANGLE;

        public enum Print {
            OUTPUT_FILENAME
        }
    }
}
