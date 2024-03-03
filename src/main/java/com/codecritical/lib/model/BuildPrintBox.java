package com.codecritical.lib.model;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class BuildPrintBox implements IBuildPrint {
    static final Logger logger = Logger.getLogger("");

    private final double xMin, xMax, yMin, yMax, zMin, zMax, overlap;
    private final int maxIterations, iCount, jCount;
    private final double xCellSize, yCellSize;
    private final int baseThickness;

    public BuildPrintBox(ConfigReader config) {
        double x = config.asDouble(Config.StlPrint.X_SIZE);
        double y = config.asDouble(Config.StlPrint.Y_SIZE);
        this.xMin = -x/2;
        this.xMax = x/2;
        this.yMin = -y/2;
        this.yMax = y/2;
        this.zMin = 0.0;
        this.zMax = config.asDouble(Config.StlPrint.Z_SIZE);
        this.maxIterations = config.asInt(Config.Fractal.Model.MAX_ITERATIONS);
        this.iCount = config.asInt(Config.Fractal.Model.I_COUNT);
        this.jCount = config.asInt(Config.Fractal.Model.J_COUNT);
        this.overlap = config.asDouble(Config.StlPrint.BOX_OVERLAP);
        this.baseThickness = config.asInt(Config.StlPrint.BASE_THICKNESS);

        this.xCellSize = (xMax - xMin) / iCount;
        this.yCellSize = (yMax - yMin) / jCount;
    }

    private void buildPrint(ImmutableList.Builder<Abstract3dModel> builder, IMapArray map) {
        map.streamPoints().forEach(p -> {
            assert(p.z >= 0.0 && p.z <= 1.0);
            double zCellSize = (p.z * (zMax - baseThickness)) + baseThickness;
            Abstract3dModel cube = new Cube(new Dims3d(
                    xCellSize * (1 + overlap),
                    yCellSize * (1 + overlap),
                    zCellSize))
                    .move(new Coords3d(p.i, p.j, zCellSize / 2));
            builder.add(cube);
        });
    }

    @Override
    public ImmutableList<CSG> getPrint(IMapArray map) {
        ImmutableList<Abstract3dModel> parts = getParts(map);
        return parts.stream()
                .map(Abstract3dModel::toCSG)
                .collect(ImmutableList.toImmutableList());
    }

    private ImmutableList<Abstract3dModel> getParts(IMapArray map) {
        ImmutableList.Builder<Abstract3dModel> builder = ImmutableList.builder();
        buildPrint(builder, map);
        return builder.build();
    }
}
