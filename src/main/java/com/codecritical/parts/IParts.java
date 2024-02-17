package com.codecritical.parts;

import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.vrl.CSG;

public interface IParts {

    CSG getCsg();

    Dims3d getSize();

    Dims3d getOrigin();

}
