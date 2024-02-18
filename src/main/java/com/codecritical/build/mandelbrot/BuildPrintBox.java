package com.codecritical.build.mandelbrot;

import com.codecritical.build.lib.mapping.IMapArray;
import com.codecritical.build.lib.config.Config;
import com.codecritical.build.lib.config.ConfigReader;
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
        this.xMin = config.asDouble(Config.Mandelbrot.Print.X_MIN);
        this.xMax = config.asDouble(Config.Mandelbrot.Print.X_MAX);
        this.yMin = config.asDouble(Config.Mandelbrot.Print.Y_MIN);
        this.yMax = config.asDouble(Config.Mandelbrot.Print.Y_MAX);
        this.zMin = config.asDouble(Config.Mandelbrot.Print.Z_MIN);
        this.zMax = config.asDouble(Config.Mandelbrot.Print.Z_MAX);
        this.maxIterations = config.asInt(Config.Mandelbrot.MAX_ITERATIONS);
        this.iCount = config.asInt(Config.Mandelbrot.I_COUNT);
        this.jCount = config.asInt(Config.Mandelbrot.J_COUNT);
        this.overlap = config.asDouble(Config.Mandelbrot.Print.BOX_OVERLAP);
        this.baseThickness = config.asInt(Config.Mandelbrot.Print.BASE_THICKNESS);

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
