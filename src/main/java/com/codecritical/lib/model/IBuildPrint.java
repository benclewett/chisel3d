package com.codecritical.lib.model;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.mapping.IMapArray;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface IBuildPrint {
    ImmutableList<CSG> getPrint(IMapArray map);
}
