package com.codecritical.build.mandelbrot;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.mapping.IMapArray;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.vrl.CSG;

public interface IBuildPrint {
    ImmutableList<CSG> getPrint(IMapArray map);
}
