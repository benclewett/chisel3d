package com.codecritical.lib.model;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */


import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.CircularSorter;
import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.mapping.Trig;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.Polygon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class BuildPrintSurface implements IBuildPrint {
    static final Logger logger = Logger.getLogger("");

    public enum BaseShape {
        SQUARE, CIRCLE
    }

    private static final Color COLOR = Color.WHITE;
    private final double xMin, xMax, yMin, yMax, zMin, zMax, xRange, yRange, zRange;
    private final double baseThickness;
    private final BaseShape baseShape;
    private final CircularSorter circularSorter;

    // For use when we are in CIRCLE mode
    private final Coords3d centre;
    private final double circleRadiusSquared;
    private final double circleRadius;
    private final ImmutableList<Coords3d> boundingCircumference;
    private final Optional<Coords3d> projectCentreSphere;
    private final ConfigReader config;

    private IMapArray map;

    public BuildPrintSurface(ConfigReader config) {
        this.config = config;
        this.xRange = config.asDouble(Config.StlPrint.X_SIZE);
        this.yRange = config.asDouble(Config.StlPrint.Y_SIZE);
        this.xMin = -xRange / 2;
        this.xMax = xRange / 2;
        this.yMin = -yRange / 2;
        this.yMax = yRange / 2;
        this.zMin = 0.0;
        this.zMax = config.asDouble(Config.StlPrint.Z_SIZE);
        this.zRange = this.zMax - this.zMin;
        this.baseThickness = config.asInt(Config.StlPrint.BASE_THICKNESS);
        this.baseShape = (BaseShape) config.asEnum(BaseShape.class, Config.StlPrint.SHAPE);
        this.centre = new Coords3d(
                (xMin + xMax) / 2,
                (yMin + yMax) / 2,
                (zMin + zMax) / 2
        );
        this.circularSorter = new CircularSorter(this.centre);
        this.circleRadius = (xMax - xMin) / 2;
        this.circleRadiusSquared = this.circleRadius * this.circleRadius;
        this.boundingCircumference = buildBoundingCircumference();
        this.projectCentreSphere = config.asOptionalCoors3d(Config.Fractal.Processing.PROJECT_CENTRE_SPHERE);

        logger.info(this.toString());
    }

    @Override
    public ImmutableList<CSG> getPrint(IMapArray map) {
        this.map = map;

        ImmutableList<Polygon> polygons = buildAllPolygons();
        var mandelbrot = new CSG(polygons);

        // Trying again....  Still don't work.
        /*
        var cylinder = new Cylinder(zRange * 2, Radius.fromRadius(Math.min(xRange, yRange) / 2.01)).toCSG();
        cylinder = cylinder.transformed(TransformationFactory.getTranlationMatrix(new Coords3d(0, 0, zRange / 2.0)));
        mandelbrot = mandelbrot.intersect(cylinder);
         */

        return ImmutableList.of(mandelbrot);
    }

    private ImmutableList<Polygon> buildAllPolygons() {
        ImmutableList.Builder<Polygon> builder = ImmutableList.builder();

        Set<Coords3d> perimeter = new HashSet<>();

        builder.addAll(buildAdjustedModelUpperSurface(perimeter));

        // TODO, this needs to be a rubber-band, or some better way: if the border is not strictly convex, this goes badly wrong.
        var perimeterSorted = circularSorter.sortAntiClockwiseZPlane(perimeter);

        builder.addAll(buildPerimeterPolygons(perimeterSorted));

        buildFloorPolygon(perimeterSorted).map(builder::add);

        return builder.build();
    }

    private ImmutableList<Polygon> buildAdjustedModelUpperSurface(Set<Coords3d> perimeter) {

        // Main work
        ImmutableList<Polygon> model = buildFixedModelUpperSurface(perimeter);

        if (projectCentreSphere.isPresent()) {
            model = projectSurfaceFromCentrePoint(model);
        }

        return model;
    }

    /** Create polygons from bottom of base (z=0) to bottom map (x=baseThickness) */
    private ImmutableList<Polygon> buildPerimeterPolygons(ImmutableList<Coords3d> perimeter) {

        if (perimeter.isEmpty()) {
            logger.severe("We don't seem to have a perimeter?");
            return ImmutableList.of();
        }

        ImmutableList.Builder<Polygon> builder = ImmutableList.builder();

        var prevVertex = perimeter.get(perimeter.size() - 1);
        for (var p : perimeter) {
            List<Coords3d> poly = new ArrayList<>();
            poly.add(new Coords3d(p.getX(), p.getY(), 0.0));
            poly.add(new Coords3d(prevVertex.getX(), prevVertex.getY(), 0.0));
            poly.add(new Coords3d(prevVertex.getX(), prevVertex.getY(), baseThickness));
            poly.add(new Coords3d(p.getX(), p.getY(), baseThickness));
            builder.add(Polygon.fromPolygons(poly, COLOR));
            prevVertex = p;
        }

        return builder.build();
    }

    /*
     * Add the floor to seal the model.  Point must have been ordered anti-clockwise.
     */
    private Optional<Polygon> buildFloorPolygon(ImmutableList<Coords3d> perimeter) {

        if (perimeter.isEmpty()) {
            return Optional.empty();
        }

        List<Coords3d> baseVertices = new ArrayList<>();

        perimeter.forEach(p -> baseVertices.add(new Coords3d(
                p.getX(),
                p.getY(),
                zMin
        )));

        return Optional.of(Polygon.fromPolygons(baseVertices, COLOR));
    }

    //region Get the surface and sides

    private ImmutableList<Polygon> buildFixedModelUpperSurface(Set<Coords3d> perimeter) {
        ImmutableList.Builder<Polygon> builder = new ImmutableList.Builder<>();
        IntStream.range(1, map.getJSize()).forEach(j ->
                IntStream.range(1, map.getISize()).forEach(i ->
                        buildSurfacePolygons(builder, i, j, perimeter)
                )
        );
        return builder.build();
    }

    /** Takes the values of i, j and constructs the polygon(s) between (i-1, j-1) to (i,j).  Corrects the base to leave no gap on the boundary. */
    private void buildSurfacePolygons(ImmutableList.Builder<Polygon> builder, int i, int j, Set<Coords3d> perimeter) {

        // Check for missing base (where base deleted to trim to shape.)  (This doesn't work very well.)
        if (map.isNull(i, j) || map.isNull(i - 1, j) || map.isNull(i, j - 1) || map.isNull(i - 1, j - 1)) {
            // Create perimeter for any consecutive non-null
            if (!map.isNull(i, j) && !map.isNull(i - 1, j)) {
                perimeter.add(new Coords3d(mapX(i), mapY(j), mapZ(map.get(i, j))));
                perimeter.add(new Coords3d(mapX(i - 1), mapY(j), mapZ(map.get(i - 1, j))));
            }
            if (!map.isNull(i, j) && !map.isNull(i, j - 1)) {
                perimeter.add(new Coords3d(mapX(i), mapY(j), mapZ(map.get(i, j))));
                perimeter.add(new Coords3d(mapX(i), mapY(j - 1), mapZ(map.get(i, j - 1))));
            }
            if (!map.isNull(i - 1, j) && !map.isNull(i - 1, j - 1)) {
                perimeter.add(new Coords3d(mapX(i - 1), mapY(j), mapZ(map.get(i - 1, j))));
                perimeter.add(new Coords3d(mapX(i - 1), mapY(j - 1), mapZ(map.get(i - 1, j - 1))));
            }
            if (!map.isNull(i, j - 1) && !map.isNull(i - 1, j - 1)) {
                perimeter.add(new Coords3d(mapX(i), mapY(j - 1), mapZ(map.get(i, j - 1))));
                perimeter.add(new Coords3d(mapX(i - 1), mapY(j - 1), mapZ(map.get(i - 1, j - 1))));
            }
            return;
        }

        if (BaseShape.CIRCLE.equals(baseShape)) {
            throw new RuntimeException("CIRCLE not yet supported.");
        }

        // Remember, a correctly defined polygons is ANTI-CLOCKWISE on the surface.
        // (Blender doesn't care, but UltiMaker Cura does.)

        /*
        if (BaseShape.CIRCLE.equals(BaseShape)) {
            // Pre-process cut-off on the radius of the circle.
            int[] vsInRadius = getVertexInRadius(vs, 4);
            int countIn = Arrays.stream(vsInRadius).sum();
            if (countIn == 0) {
                return EMPTY_POLYGON_LIST;
            } else if (countIn == 1) {
                // Inner corner of the square
                List<Coords3d> triangle = mapToTriangle(vsInRadius, vs, perimeter);
                polygons.add(Polygon.fromPolygons(triangle, COLOR));
                return polygons;
            } else if (countIn == 2) {
                // Square cut in half, which is 2x polygons for simplicity
                Coords3d[][] newSquare = mapToSquare(vsInRadius, vs, perimeter);
                polygons.add(Polygon.fromPolygons(Arrays.asList(newSquare[0]), COLOR));
                polygons.add(Polygon.fromPolygons(Arrays.asList(newSquare[1]), COLOR));
                return polygons;
            } else if (countIn == 3) {
                // TODO
            } else {
                throw new RuntimeException("Math is wrong somewhere, we have " + countIn + " vertices in square.  Should be between 0 and 3.");
            }
        }
         */

        Quadrilateral quadrilateral = addSquare(i - 1, i, j - 1, j);
        builder.addAll(quadrilateral.getPolygons());

        // Square Edge Walls.  Extend down to the base.
        if (BaseShape.SQUARE.equals(baseShape)) {
            // One polygon down to the base.  Register of perimeter position to construct the base.
            if (i == 1) {
                addPerimeter(builder, perimeter, quadrilateral.v0, quadrilateral.v3);
            } else if (i == map.getISize() - 1) {
                addPerimeter(builder, perimeter, quadrilateral.v2, quadrilateral.v1);
            }
            if (j == 1) {
                addPerimeter(builder, perimeter, quadrilateral.v1, quadrilateral.v0);
            } else if (j == map.getJSize() - 1) {
                addPerimeter(builder, perimeter, quadrilateral.v3, quadrilateral.v2);
            }
        }
    }

    /** Add perimeter marker.  Also add polygons from bottom of surface to top of base.  Anti-clockwise from two points on bottom of surface. */
    private void addPerimeter(ImmutableList.Builder<Polygon> builder, Set<Coords3d> perimeter, Coords3d v0, Coords3d v1) {
        perimeter.add(v0);
        perimeter.add(v1);
        if (v0.getZ() > baseThickness || v1.getZ() > baseThickness) {
            builder.addAll(Quadrilateral.create(
                    v0,
                    v1,
                    new Coords3d(v1.getX(), v1.getY(), baseThickness),
                    new Coords3d(v0.getX(), v0.getY(), baseThickness)
            ));
        }
    }

    private Quadrilateral addSquare(int i0, int i1, int j0, int j1) {
        return new Quadrilateral(
                new Coords3d(mapX(i0), mapY(j0), mapZ(map.get(i0, j0))),
                new Coords3d(mapX(i1), mapY(j0), mapZ(map.get(i1, j0))),
                new Coords3d(mapX(i1), mapY(j1), mapZ(map.get(i1, j1))),
                new Coords3d(mapX(i0), mapY(j1), mapZ(map.get(i0, j1)))
        );
    }

    //endregion

    //region divide polygon to bounding.

    private Coords3d getBoundaryCrossingLinePoint(Coords3d v0, Coords3d v1) {
        for (int i0 = 0; i0 < boundingCircumference.size(); i0++) {
            int i1 = (i0 + 1) % boundingCircumference.size();
            var vb0 = boundingCircumference.get(i0);
            var vb1 = boundingCircumference.get(i1);
            Optional<Coords3d> cross = Trig.lineSegmentIntersect(v0, v1, vb0, vb1);
            if (cross.isPresent()) {
                return cross.get();
            }
        }
        throw new RuntimeException(String.format("Cannot find where line %s to %s crosses bounding circumference.", v0, v1));
    }

    private int[] getVertexInRadius(Coords3d[] vertexSet, int count) {
        int[] bOut = new int[count];
        for (int i = 0; i < count; i++) {
            bOut[i] = (getRadiusSquare(vertexSet[i]) < circleRadiusSquared) ? 1 : 0;
        }
        return bOut;
    }

    private double getRadiusSquare(Coords3d c) {
        return Math.pow(centre.getX() - c.getX(), 2) + Math.pow(centre.getY() - c.getY(), 2);
    }

    //endregion

    //region Mappers

    private double mapX(double i) {
        return (i / (map.getISize() - 1) * xRange) + xMin;
    }

    private double mapY(double j) {
        return (j / (map.getJSize() - 1) * yRange) + yMin;
    }

    private double mapZ(double k) {
        return (k * zRange) + baseThickness + zMin;
    }

    //endregion

    //region Get the bounding circumference, where not a square.

    private ImmutableList<Coords3d> buildBoundingCircumference() {
        ImmutableList.Builder<Coords3d> builder = new ImmutableList.Builder<>();
        switch (baseShape) {
            case CIRCLE -> getBoundingCircumferenceCircle(builder);
        }
        var out = builder.build();
        logger.info(String.format("Line segments in circumference=%d", out.size()));
        return out;
    }

    static final double DEGREES_CIRCLE_CIRCUMFERENCE = 5.0 / 360.0 * Math.PI * 2; // => 72 lines in circumference.

    private void getBoundingCircumferenceCircle(ImmutableList.Builder<Coords3d> builder) {
        for (double alpha = 0; alpha <= Math.PI * 2; alpha += DEGREES_CIRCLE_CIRCUMFERENCE) {
            double i0 = Math.cos(alpha) * circleRadius + centre.getX();
            double j0 = Math.sin(alpha) * circleRadius + centre.getY();
            builder.add(new Coords3d(i0, j0, 0));
        }
    }

    //endregion

    //region Project model from centre point

    private ImmutableList<Polygon> projectSurfaceFromCentrePoint(ImmutableList<Polygon> surface) {
        return surface.stream()
                .map(this::projectPolygonFromCentrePoint)
                .collect(ImmutableList.toImmutableList());
    }

    private Polygon projectPolygonFromCentrePoint(Polygon polygon) {
        return Polygon.fromPolygons(
                polygon.toFacets()
                        .stream()
                        .map(f -> f.getTriangle().getPoints())
                        .flatMap(Collection::stream)
                        .map(this::projectCoord3dFromCentrePoint)
                        .collect(Collectors.toList()),
                COLOR);
    }

    private Coords3d projectCoord3dFromCentrePoint(Coords3d c) {

        // Project from origin, o, through plane of z = BASE_THICKNESS.

        Coords3d o = projectCentreSphere.orElseThrow();

        // Angle along a vertical projection
        double alpha = Math.atan2(
                - o.getZ() + baseThickness,
                Math.sqrt(Math.pow(c.getX() - o.getX(), 2) + Math.pow(c.getY() - o.getY(), 2))
        );

        // Angle on the horizontal projection
        double theta = Math.atan2(
                c.getY() - o.getY(),
                c.getX() - o.getX()
        );

        // Radius of shadow of point on the plane.
        double r = Math.cos(alpha) * (c.getZ() - baseThickness);

        return new Coords3d(
                Math.cos(theta) * r + c.getX(),
                Math.sin(theta) * r + c.getY(),
                Math.sin(alpha) * (c.getZ() - baseThickness) + baseThickness
        );
    }

    //endregion


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("xMin", xMin)
                .add("xMax", xMax)
                .add("yMin", yMin)
                .add("yMax", yMax)
                .add("zMin", zMin)
                .add("zMax", zMax)
                .add("xRange", xRange)
                .add("yRange", yRange)
                .add("zRange", zRange)
                .add("baseThickness", baseThickness)
                .add("BaseShape", baseShape)
                .add("centre", centre)
                .add("circleRadius", circleRadius)
                .add("projectCentreSphere", projectCentreSphere)
                .toString();
    }
}
