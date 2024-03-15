package com.codecritical.lib.config;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

public enum Config {
    RANDOM_SEED,
    OUTPUT_FILENAME;

    public enum StlPrint {
        X_SIZE,
        Y_SIZE,
        Z_SIZE,
        PIXEL_SIZE_XY,
        PIXEL_SIZE_Z,
        BOX_OVERLAP,
        BASE_THICKNESS,
        SHAPE,
        BORDER_HEIGHT,
        BORDER_WIDTH,
        BLOCK_SIZE_3D
    }

    public enum Fractal { ;

        public enum Model {
            MAX_ITERATIONS,
            I0,
            I1,
            J0,
            J1,
            K0,
            K1,
            I_SCALE,
            J_SCALE,
            K_SCALE,
            I_SHIFT,
            J_SHIFT,
            K_SHIFT,
            POLAR_COORDINATES,
            INSIDE_OUT,
            SHOW_ROUGH_MAP
        }

        public enum Processing {
            SCALE_POWER,
            PLATEAU_TEXTURE_MAP,
            MIN_PLATEAU_COEFFICIENT,
            PLATEAU_HOLLOW_RADIUS,
            GAUSSIAN_RADIUS,
            PLATEAU_HOLLOW_DEPTH,
            PLATEAU_HOLLOW_INCLUDE_EDGE,
            PLATEAU_HOLLOW_SMOOTH_INSIDE,
            TRIM_OUTSIDE_BASE,
            PROJECT_CENTRE_SPHERE,
            APPLY_LOG
        }

        public enum JuliaSet {
            IC,
            RC
        }

    }

    public enum InfiniteMachineConfig {
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
        PERSPECTIVE_ANGLE
    }
}
