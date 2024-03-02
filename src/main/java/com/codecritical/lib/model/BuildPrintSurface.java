package com.codecritical.lib.model;

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.CircularSorter;
import com.codecritical.lib.mapping.IMapArray;
import com.codecritical.lib.mapping.Trig;
import com.codecritical.lib.mapping.VertexCache;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.Polygon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class BuildPrintSurface implements IBuildPrint {
    static final Logger logger = Logger.getLogger("");
    private static final List<Polygon> EMPTY_POLYGON_LIST = new ArrayList<>() {};

    public enum EShape {
        SQUARE, CIRCLE
    }

    private static final Color COLOR = Color.WHITE;
    private final double xMin, xMax, yMin, yMax, zMin, zMax, xRange, yRange, zRange;
    private final double baseThickness;
    private final VertexCache vertexCache = new VertexCache();
    private final EShape eShape;
    private final CircularSorter circularSorter;

    // For use when we are in CIRCLE mode
    private final Coords3d centre;
    private final double circleRadiusSquared;
    private final double circleRadius;
    private final ImmutableList<Coords3d> boundingCircumference;

    private IMapArray map;

    public BuildPrintSurface(ConfigReader config) {
        this.xMin = 0.0;
        this.xMax = config.asDouble(Config.StlPrint.X_SIZE);
        this.yMin = 0.0;
        this.yMax = config.asDouble(Config.StlPrint.Y_SIZE);
        this.zMin = 0.0;
        this.zMax = config.asDouble(Config.StlPrint.Z_SIZE);
        this.xRange = this.xMax - this.xMin;
        this.yRange = this.yMax - this.yMin;
        this.zRange = this.zMax - this.zMin;
        this.baseThickness = config.asInt(Config.StlPrint.BASE_THICKNESS);
        this.eShape = (EShape)config.asEnum(EShape.class, Config.StlPrint.SHAPE);
        this.centre = new Coords3d(
                (xMin + xMax) / 2,
                (yMin + yMax) / 2,
                (zMin + zMax) / 2
        );
        this.circularSorter = new CircularSorter(this.centre);
        this.circleRadius = (xMax - xMin) / 2;
        this.circleRadiusSquared = this.circleRadius * this.circleRadius;
        this.boundingCircumference = buindBoundingCircumference();

        logger.info(this.toString());
    }

    @Override
    public ImmutableList<CSG> getPrint(IMapArray map) {
        this.map = map;

        ImmutableList<Polygon> polygons = buildAllPolygons();
        var mandelbrot = new CSG(polygons);

        return ImmutableList.of(mandelbrot);
    }

    private ImmutableList<Polygon> buildAllPolygons() {
        ImmutableList.Builder<Polygon> builder = ImmutableList.builder();

        Set<Coords3d> perimeter = new HashSet<>();

        IntStream.range(1, map.getJSize()).forEach(j ->
                IntStream.range(1, map.getISize()).forEach(i ->
                        builder.addAll(buildSurfacePolygons(i, j, perimeter))
                )
        );

        var perimeterSorted = circularSorter.sortAntiClockwiseZPlane(perimeter);

        builder.addAll(buildPerimeterPolygons(perimeterSorted));

        builder.add(buildFloorPolygon(perimeterSorted));

        return builder.build();
    }

    private ImmutableList<Polygon> buildPerimeterPolygons(ImmutableList<Coords3d> perimeter) {

        if (perimeter.size() == 0) {
            logger.severe("We don't seem to have a perimeter.");
        }

        ImmutableList.Builder<Polygon> builder = ImmutableList.builder();

        var prevVertex = perimeter.get(perimeter.size() - 1);
        for (var p : perimeter) {
            List<Coords3d> poly = new ArrayList<>();
            poly.add(p);
            poly.add(prevVertex);
            poly.add(new Coords3d(prevVertex.getX(), prevVertex.getY(), zMin));
            poly.add(new Coords3d(p.getX(), p.getY(), zMin));
            builder.add(Polygon.fromPolygons(poly, COLOR));
            prevVertex = p;
        }

        return builder.build();
    }

    /*
     * Add the floor to seal the model.  Point must have been ordered anti-clockwise.
     */
    private Polygon buildFloorPolygon(ImmutableList<Coords3d> perimeter) {

        List<Coords3d> baseVertices = new ArrayList<>();

        perimeter.forEach(p -> baseVertices.add(vertexCache.get(
                p.getX(),
                p.getY(),
                zMin
        )));

        return Polygon.fromPolygons(baseVertices, COLOR);
    }

    //region Get the surface and sides

    private List<Polygon> buildSurfacePolygons(int i, int j, Set<Coords3d> perimeter) {

        if (EShape.CIRCLE.equals(eShape)) {
            // throw new RuntimeException("CIRCLE not yet supported.");
        }

        // Remember, a correctly defined polygons is ANTI-CLOCKWISE on the surface.
        // (Blender doesn't care, but UltiMaker Cura does.)

        List<Polygon> polygons = new ArrayList<>();

        /*
         *   Vertex and Polygon Diagram
         *
         *   0 v3 +-----------------+ v2
         *        | \     p2      / |
         *        |   \         /   |
         *        |     \     /     |
         *        |       \ /       |
         *   y/j  |  p3   v4    p4  |
         *        |       / \       |
         *        |     /     \     |
         *        |   /         \   |
         *        | /      p1     \ |
         *  -1 v0 +-----------------+ v1
         *       -1        x/i      0
         */

        // Vertex Z Height
        double z0 = map.get(i - 1, j - 1);
        double z1 = map.get(i, j - 1);
        double z2 = map.get(i, j);
        double z3 = map.get(i - 1, j);
        double z4 = (z0 + z1 + z2 + z3) / 4.0;

        // Vertices
        Coords3d[] vs = new Coords3d[]{
                vertexCache.get(mapX(i - 1), mapY(j - 1), mapZ(z0)),
                vertexCache.get(mapX(i), mapY(j - 1), mapZ(z1)),
                vertexCache.get(mapX(i), mapY(j), mapZ(z2)),
                vertexCache.get(mapX(i - 1), mapY(j), mapZ(z3)),
                vertexCache.get(mapX(i - 0.5), mapY(j - 0.5), mapZ(z4))
        };

        if (EShape.CIRCLE.equals(eShape)) {
            throw new RuntimeException("CIRCLE not supported.");
            /*
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
             */
        }

        if ((vs[0] == vs[1] && vs[2] == vs[3]) || (vs[0] == vs[2] && vs[1] == vs[3])) {
            // Coplanar, needs just one polygon.
            List<Coords3d> poly = new ArrayList<>();
            poly.add(vs[0]);
            poly.add(vs[1]);
            poly.add(vs[2]);
            poly.add(vs[3]);
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        } else {
            // Complex.  Break up into 4x polygons.

            List<Coords3d> poly1 = new ArrayList<>();
            poly1.add(vs[1]);
            poly1.add(vs[4]);
            poly1.add(vs[0]);
            polygons.add(Polygon.fromPolygons(poly1, COLOR));

            List<Coords3d> poly2 = new ArrayList<>();
            poly2.add(vs[2]);
            poly2.add(vs[3]);
            poly2.add(vs[4]);
            polygons.add(Polygon.fromPolygons(poly2, COLOR));

            List<Coords3d> poly3 = new ArrayList<>();
            poly3.add(vs[4]);
            poly3.add(vs[3]);
            poly3.add(vs[0]);
            polygons.add(Polygon.fromPolygons(poly3, COLOR));

            List<Coords3d> poly4 = new ArrayList<>();
            poly4.add(vs[2]);
            poly4.add(vs[4]);
            poly4.add(vs[1]);
            polygons.add(Polygon.fromPolygons(poly4, COLOR));
        }

        // Square Edge walls

        if (EShape.SQUARE.equals(eShape)) {
            if (i == 1) {
                perimeter.add(vs[0]);
                perimeter.add(vs[3]);
            } else if (i == map.getISize() - 1) {
                perimeter.add(vs[2]);
                perimeter.add(vs[1]);
            }
            if (j == 1) {
                perimeter.add(vs[1]);
                perimeter.add(vs[0]);
            } else if (j == map.getJSize() - 1) {
                perimeter.add(vs[3]);
                perimeter.add(vs[2]);
            }
        }

        return polygons;
    }

    //endregion

    //region divide polygon to bounding.

    /**
     * A square of vertices divided in two, half in and half out of the boundary.
     * @param vs Vertices in anti-clockwise order.
     * @return Two triangles representing the square divided by the boundary.
     */
    private Coords3d[][] mapToSquare(int[] vsInRadius, Coords3d[] vs, Set<Coords3d> perimeter) {
        Coords3d[] vsTrimmed = new Coords3d[] {vs[0], vs[1], vs[2], vs[3]};
        for (int i0 = 0; i0 < 4; i0++) {
            int i1 = (i0 + 1) % 4;
            if (vsInRadius[i0] == 1 && vsInRadius[i1] == 0) {
                vsTrimmed[i1] = getBoundaryCrossingLinePoint(vs[0], vs[1]);
                perimeter.add(vsTrimmed[i1]);
            } else if (vsInRadius[i0] == 0 && vsInRadius[i1] == 1) {
                vsTrimmed[i0] = getBoundaryCrossingLinePoint(vs[0], vs[1]);
                perimeter.add(vsTrimmed[i0]);
            }
        }
        Coords3d[] vs1 = new Coords3d[] {vsTrimmed[0], vsTrimmed[1], vsTrimmed[2]};
        Coords3d[] vs2 = new Coords3d[] {vsTrimmed[2], vsTrimmed[3], vsTrimmed[0]};
        return new Coords3d[][] { vs1, vs2 };
    }

    private List<Coords3d> mapToTriangle(int[] vInCircle, Coords3d[] vAll, Set<Coords3d> perimeter) {
        // Find the valid triangle of structures list of vertices.
        Coords3d v, vLeft, vRight;
        if (vInCircle[0] == 1) {
            v = vAll[0];
            vLeft = vAll[2];
            vRight = vAll[1];
        } else if (vInCircle[1] == 1) {
            v = vAll[1];
            vLeft = vAll[0];
            vRight = vAll[3];
        } else if (vInCircle[2] == 1) {
            v = vAll[2];
            vLeft = vAll[3];
            vRight = vAll[0];
        } else if (vInCircle[3] == 1) {
            v = vAll[3];
            vLeft = vAll[1];
            vRight = vAll[2];
        } else {
            throw new RuntimeException("OOps, these are not the droids we are looking for.");
        }
        vLeft = getBoundaryCrossingLinePoint(v, vLeft);
        vRight = getBoundaryCrossingLinePoint(v, vRight);
        perimeter.add(vLeft);
        perimeter.add(vRight);
        return List.of(v, vLeft, vRight);
    }

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

    private ImmutableList<Coords3d> buindBoundingCircumference() {
        ImmutableList.Builder<Coords3d> builder = new ImmutableList.Builder<>();
        switch (eShape) {
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
            var vertex = vertexCache.get(i0, j0, 0);
            builder.add(vertex);
        }
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
                .add("eShape", eShape)
                .add("centre", centre)
                .add("circleRadius", circleRadius)
                .toString();
    }
}
