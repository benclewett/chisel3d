package com.codecritical.lib.model;

import com.codecritical.lib.config.Config;
import com.codecritical.lib.config.ConfigReader;
import com.codecritical.lib.mapping.CircularSorter;
import com.codecritical.lib.mapping.IMapArray;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.Polygon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        var polygons = getPolygons();
        var csg = new CSG(polygons);

        return ImmutableList.of(csg);
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

    private final static List<Polygon> EMPTY_POLYGON_LIST = new ArrayList<>();

    private List<Polygon> getSurfacePolygons(int i, int j, Set<Coords3d> perimeter) {

        // Remember, a correctly defined polygons is ANTI-CLOCKWISE on the surface.
        // (Blender doesn't care, but UltiCura does.)

        List<Polygon> polygons = new ArrayList<>();

        /*
         *   Vertex and Polygon Diagram
         *
         *   0 v2 +-----------------+ v3
         *        | \     p2      / |
         *        |   \         /   |
         *        |     \     /     |
         *        |       \ /       |
         *   y/j  |  p3   v5    p4  |
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
        double z5 = (z0 + z1 + z2 + z3) / 4.0;

        // Vertices
        var v0 = coords3dCache.get(mapX(i - 1), mapY(j - 1), mapZ(z0));
        var v1 = coords3dCache.get(mapX(i), mapY( j - 1), mapZ(z1));
        var v2 = coords3dCache.get(mapX(i - 1), mapY(j), mapZ(z2));
        var v3 = coords3dCache.get(mapX(i), mapY(j), mapZ(z3));
        var v5 = coords3dCache.get(mapX(i - 0.5), mapY(j - 0.5), mapZ(z5));

        if (EShape.CIRCLE.equals(eShape)) {
            if (getRadiusSquare(v5) > circleRadiusSquared) {
                // Reject, more than half the shape is outside the circle.
                return EMPTY_POLYGON_LIST;
            }
            // Look for edge
            if (getRadiusSquare(v0) > circleRadiusSquared) {
                v0 = coords3dCache.get(trimToRadius(v0));
                perimeter.add(v0);
            }
            if (getRadiusSquare(v1) > circleRadiusSquared) {
                v1 = coords3dCache.get(trimToRadius(v1));
                perimeter.add(v1);
            }
            if (getRadiusSquare(v2) > circleRadiusSquared) {
                v2 = coords3dCache.get(trimToRadius(v2));
                perimeter.add(v2);
            }
            if (getRadiusSquare(v3) > circleRadiusSquared) {
                v3 = coords3dCache.get(trimToRadius(v3));
                perimeter.add(v3);
            }
        }

        if ((v0 == v1 && v2 == v3) || (v0 == v2 && v1 == v3)) {
            // Coplanar, needs just one polygon.
            List<Coords3d> poly = new ArrayList<>();
            poly.add(v0);
            poly.add(v1);
            poly.add(v3);
            poly.add(v2);
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        } else {
            // Complex.  Break up into 4x polygons.

            List<Coords3d> poly1 = new ArrayList<>();
            poly1.add(v1);
            poly1.add(v5);
            poly1.add(v0);
            polygons.add(Polygon.fromPolygons(poly1, COLOR));

            List<Coords3d> poly2 = new ArrayList<>();
            poly2.add(v3);
            poly2.add(v2);
            poly2.add(v5);
            polygons.add(Polygon.fromPolygons(poly2, COLOR));

            List<Coords3d> poly3 = new ArrayList<>();
            poly3.add(v5);
            poly3.add(v2);
            poly3.add(v0);
            polygons.add(Polygon.fromPolygons(poly3, COLOR));

            List<Coords3d> poly4 = new ArrayList<>();
            poly4.add(v3);
            poly4.add(v5);
            poly4.add(v1);
            polygons.add(Polygon.fromPolygons(poly4, COLOR));
        }

        // Square Edge walls

        if (EShape.SQUARE.equals(eShape)) {
            if (i == 1) {
                perimeter.add(v0);
                perimeter.add(v2);
            } else if (i == map.getISize() - 1) {
                perimeter.add(v3);
                perimeter.add(v1);
            }
            if (j == 1) {
                perimeter.add(v1);
                perimeter.add(v0);
            } else if (j == map.getJSize() - 1) {
                perimeter.add(v2);
                perimeter.add(v3);
            }
        }

        return polygons;
    }

    private Coords3d trimToRadius(Coords3d c) {
        double alpha = Math.atan2(c.getY() - centre.getY(), c.getX() - centre.getX());
        return new Coords3d(
                Math.cos(alpha) * circleRadius + centre.getX(),
                Math.sin(alpha) * circleRadius + centre.getY(),
                c.getZ()
        );
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
