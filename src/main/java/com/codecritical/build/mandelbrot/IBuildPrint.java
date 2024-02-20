package com.codecritical.build.mandelbrot;

import com.codecritical.lib.mapping.IMapArray;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.vrl.CSG;

public interface IBuildPrint {
    ImmutableList<CSG> getPrint(IMapArray map);
}
