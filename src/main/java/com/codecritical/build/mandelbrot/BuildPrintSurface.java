package com.codecritical.build.mandelbrot;

import com.codecritical.build.lib.config.Config;
import com.codecritical.build.lib.config.ConfigReader;
import com.codecritical.build.lib.mapping.IMapArray;
import com.codecritical.build.lib.model.Coords3dCache;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.Polygon;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@ParametersAreNonnullByDefault
public class BuildPrintSurface implements IBuildPrint {
    static final Logger logger = Logger.getLogger("");

    private static final Color COLOR = Color.WHITE;
    private final double xMin, xMax, yMin, yMax, zMin, zMax, xRange, yRange, zRange;
    private final double baseThickness;
    private IMapArray map;
    private Coords3dCache coords3dCache = new Coords3dCache();

    public BuildPrintSurface(ConfigReader config) {
        this.xMin = config.asDouble(Config.Mandelbrot.Print.X_MIN);
        this.xMax = config.asDouble(Config.Mandelbrot.Print.X_MAX);
        this.yMin = config.asDouble(Config.Mandelbrot.Print.Y_MIN);
        this.yMax = config.asDouble(Config.Mandelbrot.Print.Y_MAX);
        this.zMin = config.asDouble(Config.Mandelbrot.Print.Z_MIN);
        this.zMax = config.asDouble(Config.Mandelbrot.Print.Z_MAX);
        this.xRange = this.xMax - this.xMin;
        this.yRange = this.yMax - this.yMin;
        this.zRange = this.zMax - this.zMin;
        this.baseThickness = config.asInt(Config.Mandelbrot.Print.BASE_THICKNESS);
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

        IntStream.range(1, map.getJSize()).forEach(j ->
                IntStream.range(1, map.getISize()).forEach(i ->
                        builder.addAll(getSurfacePolygons(i, j))
                )
        );

        builder.add(getBasePolygon());

        return builder.build();
    }


    //region Get the base

    /*
     * Add the floor to seal the model
     * (This must be a convex collection of points.  Ie, round the perimeter, in order.)
     */
    private Polygon getBasePolygon() {
        List<Coords3d> baseCoords = new ArrayList<>();
        baseCoords.addAll(
                coords3dCache.stream()
                        .filter(c -> c.getZ() == zMin && c.getY() == yMin)
                        .sorted(this::compareXInOrder)
                        .toList()
        );
        baseCoords.addAll(
                coords3dCache.stream()
                        .filter(c -> c.getZ() == zMin && c.getX() == xMax && c.getY() != yMin)
                        .sorted(this::compareYInOrder)
                        .toList()
        );
        baseCoords.addAll(
                coords3dCache.stream()
                        .filter(c -> c.getZ() == zMin && c.getY() == yMax && c.getX() != xMax)
                        .sorted(this::compareXReverseOrder)
                        .toList()
        );
        baseCoords.addAll(
                coords3dCache.stream()
                        .filter(c -> c.getZ() == zMin && c.getX() == xMin && c.getY() != yMax && c.getY() != yMin)
                        .sorted(this::compareYReverseOrder)
                        .toList()
        );

        return Polygon.fromPolygons(baseCoords, COLOR);
    }

    private int compareXInOrder(Coords3d c0, Coords3d c1) {
        return Double.compare(c0.getX(), c1.getX());
    }

    private int compareYInOrder(Coords3d c0, Coords3d c1) {
        return Double.compare(c0.getY(), c1.getY());
    }

    private int compareXReverseOrder(Coords3d c0, Coords3d c1) {
        return Double.compare(c1.getX(), c0.getX());
    }

    private int compareYReverseOrder(Coords3d c1, Coords3d c0) {
        return Double.compare(c0.getY(), c1.getY());
    }

    //endregion

    //region Get the surface and sides

    private List<Polygon> getSurfacePolygons(int i, int j) {

        List<Polygon> polygons = new ArrayList<>();

        /*
         *   Vertex and Polygon Diagram
         *
         *   0 v2 +-----------------+ v3
         *        | \     p2      / |
         *        |   \         /   |
         *        |     \     /     |
         *        |       \ /       |
         *   y    |  p3   v5    p4  |
         *        |       / \       |
         *        |     /     \     |
         *        |   /         \   |
         *        | /      p1     \ |
         *  -1 v0 +-----------------+ v1
         *       -1        x        0
         */

        // Vertex Z Height
        double v0 = map.get(i - 1, j - 1);
        double v1 = map.get(i, j - 1);
        double v2 = map.get(i - 1, j);
        double v3 = map.get(i, j);

        if ((v0 == v1 && v2 == v3) || (v0 == v2 && v1 == v3)) {
            // Coplanar, needs just one polygon.
            List<Coords3d> poly = new ArrayList<>();
            poly.add(coords3dCache.get(mapX(i - 1), mapY(j - 1), mapZ(v0)));
            poly.add(coords3dCache.get(mapX(i - 1), mapY(j), mapZ(v2)));
            poly.add(coords3dCache.get(mapX(i), mapY(j), mapZ(v3)));
            poly.add(coords3dCache.get(mapX(i), mapY( j - 1), mapZ(v1)));
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        } else {
            // Complex.  Break up into 4x polygons.
            double v5 = (v0 + v1 + v2 + v3) / 4.0;
            double iMid = (double)i - 0.5;
            double jMid = (double)j - 0.5;

            List<Coords3d> poly1 = new ArrayList<>();
            poly1.add(coords3dCache.get(mapX(i - 1), mapY(j - 1), mapZ(v0)));
            poly1.add(coords3dCache.get(mapX(i), mapY(j - 1), mapZ(v1)));
            poly1.add(coords3dCache.get(mapX(iMid), mapY(jMid), mapZ(v5)));
            polygons.add(Polygon.fromPolygons(poly1, COLOR));

            List<Coords3d> poly2 = new ArrayList<>();
            poly2.add(coords3dCache.get(mapX(iMid), mapY(jMid), mapZ(v5)));
            poly2.add(coords3dCache.get(mapX(i - 1), mapY(j), mapZ(v2)));
            poly2.add(coords3dCache.get(mapX(i), mapY(j), mapZ(v3)));
            polygons.add(Polygon.fromPolygons(poly2, COLOR));

            List<Coords3d> poly3 = new ArrayList<>();
            poly3.add(coords3dCache.get(mapX(i - 1), mapY(j - 1), mapZ(v0)));
            poly3.add(coords3dCache.get(mapX(i - 1), mapY(j), mapZ(v2)));
            poly3.add(coords3dCache.get(mapX(iMid), mapY(jMid), mapZ(v5)));
            polygons.add(Polygon.fromPolygons(poly3, COLOR));

            List<Coords3d> poly4 = new ArrayList<>();
            poly4.add(coords3dCache.get(mapX(i), mapY(j - 1), mapZ(v1)));
            poly4.add(coords3dCache.get(mapX(iMid), mapY(jMid), mapZ(v5)));
            poly4.add(coords3dCache.get(mapX(i), mapY(j), mapZ(v3)));
            polygons.add(Polygon.fromPolygons(poly4, COLOR));
        }

        // Edge walls
        
        if (i == 1) {
            List<Coords3d> poly = new ArrayList<>();
            poly.add(coords3dCache.get(xMin, mapY(j), mapZ(v2)));
            poly.add(coords3dCache.get(xMin, mapY(j - 1), mapZ(v0)));
            poly.add(coords3dCache.get(xMin, mapY(j - 1), zMin));
            poly.add(coords3dCache.get(xMin, mapY(j), zMin));
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        }

        if (i == map.getISize() - 1) {
            List<Coords3d> poly = new ArrayList<>();
            poly.add(coords3dCache.get(xMax, mapY(j), mapZ(v3)));
            poly.add(coords3dCache.get(xMax, mapY(j - 1), mapZ(v1)));
            poly.add(coords3dCache.get(xMax, mapY(j - 1), zMin));
            poly.add(coords3dCache.get(xMax, mapY(j), zMin));
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        }

        if (j == 1) {
            List<Coords3d> poly = new ArrayList<>();
            poly.add(coords3dCache.get(mapX(i), yMin, mapZ(v1)));
            poly.add(coords3dCache.get(mapX(i - 1), yMin, mapZ(v0)));
            poly.add(coords3dCache.get(mapX(i - 1), yMin, zMin));
            poly.add(coords3dCache.get(mapX(i), yMin, zMin));
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        }

        if (j == map.getJSize() - 1) {
            List<Coords3d> poly = new ArrayList<>();
            poly.add(coords3dCache.get(mapX(i), yMax, mapZ(v3)));
            poly.add(coords3dCache.get(mapX(i - 1), yMax, mapZ(v2)));
            poly.add(coords3dCache.get(mapX(i - 1), yMax, zMin));
            poly.add(coords3dCache.get(mapX(i), yMax, zMin));
            polygons.add(Polygon.fromPolygons(poly, COLOR));
        }

        return polygons;
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
