package com.codecritical.lib.model;

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.CircularSorter;
import com.codecritical.lib.mapping.IMapArray;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Complex3dModel;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.models.Empty3dModel;
import eu.printingin3d.javascad.tranzitions.Intersection;
import eu.printingin3d.javascad.tranzitions.Union;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.Polygon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class BuildPrintSurface implements IBuildPrint {
    static final Logger logger = Logger.getLogger("");

    public enum EShape {
        SQUARE, CIRCLE
    }

    private static final Color COLOR = Color.WHITE;
    private final double xMin, xMax, yMin, yMax, zMin, zMax, xRange, yRange, zRange;
    private final double baseThickness;
    private final Coords3dCache coords3dCache = new Coords3dCache();
    private final EShape eShape;
    private final CircularSorter circularSorter;

    // For use when we are in CIRCLE mode
    private final Coords3d centre;
    private final double circleRadiusSquared;
    private final double circleRadius;

    private IMapArray map;

    public BuildPrintSurface(ConfigReader config) {
        this.xMin = config.asDouble(Config.StlPrint.X_MIN);
        this.xMax = config.asDouble(Config.StlPrint.X_MAX);
        this.yMin = config.asDouble(Config.StlPrint.Y_MIN);
        this.yMax = config.asDouble(Config.StlPrint.Y_MAX);
        this.zMin = config.asDouble(Config.StlPrint.Z_MIN);
        this.zMax = config.asDouble(Config.StlPrint.Z_MAX);
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
    }

    @Override
    public ImmutableList<CSG> getPrint(IMapArray map) {
        this.map = map;

        ImmutableList<Polygon> polygons = getPolygons();
        var mandelbrot = new CSG(polygons);

        /*
        var boundary = new Cylinder(zMax - zMin, Radius.fromRadius(circleRadius));

        var csgOut = new Intersection(polygons, boundary).toCSG();
         */

        return ImmutableList.of(mandelbrot);
    }

    private ImmutableList<Polygon> getPolygons() {
        ImmutableList.Builder<Polygon> builder = ImmutableList.builder();

        Set<Coords3d> perimeter = new HashSet<>();

        IntStream.range(1, map.getJSize()).forEach(j ->
                IntStream.range(1, map.getISize()).forEach(i ->
                        builder.addAll(getSurfacePolygons(i, j, perimeter))
                )
        );

        var perimeterSorted = circularSorter.sortAntiClockwiseZPlane(perimeter);

        builder.addAll(getPerimeter(perimeterSorted));

        builder.add(getFloorPolygon(perimeterSorted));

        return builder.build();
    }

    private ImmutableList<Polygon> getPerimeter(ImmutableList<Coords3d> perimeter) {

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
    private Polygon getFloorPolygon(ImmutableList<Coords3d> perimeter) {

        List<Coords3d> baseVertices = new ArrayList<>();

        perimeter.forEach(p -> baseVertices.add(coords3dCache.get(
                p.getX(),
                p.getY(),
                zMin
        )));

        return Polygon.fromPolygons(baseVertices, COLOR);
    }

    //region Get the surface and sides

    private List<Polygon> getSurfacePolygons(int i, int j, Set<Coords3d> perimeter) {

        if (EShape.CIRCLE.equals(eShape)) {
            throw new RuntimeException("CIRCLE not yet supported.");
        }

        // Remember, a correctly defined polygons is ANTI-CLOCKWISE on the surface.
        // (Blender doesn't care, but UltiMaker Cura does.)

        List<Polygon> polygons = new ArrayList<>();

        /*
         *   Vertex and Polygon Diagram
         *
         *   0 v2 +-----------------+ v3
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
        double z2 = map.get(i - 1, j);
        double z3 = map.get(i, j);
        double z4 = (z0 + z1 + z2 + z3) / 4.0;

        // Vertices
        Coords3d[] v = new Coords3d[]{
                coords3dCache.get(mapX(i - 1), mapY(j - 1), mapZ(z0)),
                coords3dCache.get(mapX(i), mapY(j - 1), mapZ(z1)),
                coords3dCache.get(mapX(i - 1), mapY(j), mapZ(z2)),
                coords3dCache.get(mapX(i), mapY(j), mapZ(z3)),
                coords3dCache.get(mapX(i - 0.5), mapY(j - 0.5), mapZ(z4))
        };

        /*

        if (EShape.CIRCLE.equals(eShape)) {
            // Pre-process cut-off on the radius of the circle.
            int[] vInCircle = getVertexInRadius(v, 4);
            int countIn = Arrays.stream(vInCircle).sum();
            if (countIn == 0) {
                return EMPTY_POLYGON_LIST;
            } else if (countIn == 1) {
                List<Coords3d> vTriangle = getTriangle(vInCircle, v, perimeter);
                polygons.add(Polygon.fromPolygons(vTriangle, COLOR));
                return polygons;
            }
        }
         */

        if ((v[0] == v[1] && v[2] == v[3]) || (v[0] == v[2] && v[1] == v[3])) {
            // Coplanar, needs just one polygon.
            List<Coords3d> poly = new ArrayList<>();
            poly.add(v[0]);
            poly.add(v[1]);
            poly.add(v[3]);
            poly.add(v[2]);
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        } else {
            // Complex.  Break up into 4x polygons.

            List<Coords3d> poly1 = new ArrayList<>();
            poly1.add(v[1]);
            poly1.add(v[4]);
            poly1.add(v[0]);
            polygons.add(Polygon.fromPolygons(poly1, COLOR));

            List<Coords3d> poly2 = new ArrayList<>();
            poly2.add(v[3]);
            poly2.add(v[2]);
            poly2.add(v[4]);
            polygons.add(Polygon.fromPolygons(poly2, COLOR));

            List<Coords3d> poly3 = new ArrayList<>();
            poly3.add(v[4]);
            poly3.add(v[2]);
            poly3.add(v[0]);
            polygons.add(Polygon.fromPolygons(poly3, COLOR));

            List<Coords3d> poly4 = new ArrayList<>();
            poly4.add(v[3]);
            poly4.add(v[4]);
            poly4.add(v[1]);
            polygons.add(Polygon.fromPolygons(poly4, COLOR));
        }

        // Square Edge walls

        if (EShape.SQUARE.equals(eShape)) {
            if (i == 1) {
                perimeter.add(v[0]);
                perimeter.add(v[2]);
            } else if (i == map.getISize() - 1) {
                perimeter.add(v[3]);
                perimeter.add(v[1]);
            }
            if (j == 1) {
                perimeter.add(v[1]);
                perimeter.add(v[0]);
            } else if (j == map.getJSize() - 1) {
                perimeter.add(v[2]);
                perimeter.add(v[3]);
            }
        }

        return polygons;
    }

    private List<Coords3d> getTriangle(int[] vInCircle, Coords3d[] vAll, Set<Coords3d> perimeter) {
        throw new RuntimeException("Unsupported");
        /*
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
        }
        vLeft = getRadiusCrossLine(v, vLeft);
        vRight = getRadiusCrossLine(v, vRight);
        perimeter.add(vLeft);
        perimeter.add(vRight);
        return List.of(v, vLeft, vRight);
         */
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
        return (k * (zRange - baseThickness)) + baseThickness + zMin;
    }

    //endregion
}
