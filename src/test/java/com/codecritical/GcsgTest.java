package com.codecritical;

import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.models.Sphere;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.export.StlBinaryFile;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GcsgTest {

    static final Logger logger = Logger.getLogger("");

    @Test
    void testIntersectHit() {
        CSG cube1 = new Cube(100).toCSG();
        CSG cube2 = new Cube(100).toCSG().transformed(TransformationFactory.getTranlationMatrix(Coords3d.xOnly(50)));

        CSG cubeIntersect = cube1.intersect(cube2);

        Assert.assertEquals(cubeIntersect.getPolygons().size(), 1);
    }

    @Test
    void testIntersectMiss() {
        CSG cube1 = new Cube(100).toCSG();
        CSG cube2 = new Cube(100).toCSG().transformed(TransformationFactory.getTranlationMatrix(Coords3d.xOnly(150)));

        CSG cubeIntersect = cube1.intersect(cube2);

        Assert.assertEquals(cubeIntersect.getPolygons().size(), 0);
    }

    @Test
    void testBasic() {
        // we use cube and sphere as base geometries
        CSG cube = new Cube(20).toCSG();
        CSG sphere = new Sphere(Radius.fromRadius(12.5)).toCSG();
        CSG cylinder = new Cylinder(20, Radius.fromRadius(12.5)).toCSG();

        // perform union, difference and intersection
        CSG cubePlusSphere = cube.union(sphere);
        CSG cubeMinusSphere = cube.difference(sphere);
        CSG cubeIntersectSphere = cube.intersect(sphere);

        var x30 = TransformationFactory.getTranlationMatrix(Coords3d.xOnly(30));
        var x60 = TransformationFactory.getTranlationMatrix(Coords3d.xOnly(60));
        var x90 = TransformationFactory.getTranlationMatrix(Coords3d.xOnly(90));
        var x120 = TransformationFactory.getTranlationMatrix(Coords3d.xOnly(120));
        var x150 = TransformationFactory.getTranlationMatrix(Coords3d.xOnly(150));

        CSG union = cube.
                union(sphere.transformed(x30)).
                union(cubePlusSphere.transformed(x60)).
                union(cubeMinusSphere.transformed(x90)).
                union(cubeIntersectSphere.transformed(x120)).
                union(cylinder.transformed(x150));

        // save union as stl
        try {
            OutputStream outputStream = new FileOutputStream("test.stl");
            StlBinaryFile stl = new StlBinaryFile(outputStream);
            stl.writeToFile(union.toFacets());
            outputStream.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        Assert.assertTrue(true);
    }
}
