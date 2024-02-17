package com.codecritital;

import com.aspose.threed.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class AdiposeTest {

    Scene scene;
    String testName;

    @BeforeEach
    void beforeEach() {
        scene = new Scene();
    }

    @AfterEach
    void afterEach() {
        save(scene, testName);
    }

    @Test
    void testEmptyScene() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
    }

    @Test
    void testBox() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
        Node cubeNode = new Node("box");
        Mesh box = new Box(1, 1, 1).toMesh();
        cubeNode.setEntity(box);
        scene.getRootNode().getChildNodes().add(cubeNode);
    }

    @Test
    void testCylinder() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
        Node cylinderNode = new Node("cylinder");
        Mesh cylinder = new Cylinder(0.5, 0.5, 1, 40, 1, false).toMesh();
        cylinderNode.setEntity(cylinder);
        scene.getRootNode().getChildNodes().add(cylinderNode);
    }

    @Test
    void testTorus() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
        Node torusNode = new Node("torus");
        Mesh torus = new Torus("torus", 1, 0.2, 40, 40, 2*Math.PI).toMesh();
        torusNode.setEntity(torus);
        scene.getRootNode().getChildNodes().add(torusNode);
    }

    @Test
    void testCone() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
        Node cylinderNode = new Node("cone");
        Mesh cylinder = new Cylinder(0.5, 0, 1, 40, 1, false).toMesh();
        cylinderNode.setEntity(cylinder);
        scene.getRootNode().getChildNodes().add(cylinderNode);
    }

    @Test
    void testSphere() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
        Node sphereNode = new Node();
        Mesh sphere = new Sphere(1, 40, 40).toMesh();
        sphereNode.addEntity(sphere);
        scene.getRootNode().getChildNodes().add(sphereNode);
    }

    @Test
    void testOverlapCube() {
        testName = new Object(){}.getClass().getEnclosingMethod().getName();
        Node cubeNode1 = new Node("box1");
        Node cubeNode2 = new Node("box1");
        Mesh box1 = new Box(2, 2, 1).toMesh();
        Mesh box2 = new Box(1, 1, 1).toMesh();
        Mesh newBox = Mesh.union(box1, box2);
        cubeNode1.setEntity(newBox);
//         cubeNode2.addEntity(box2);
//         cubeNode1.getTransform().setTranslation(0.5, 0.5, 0.5);
        scene.getRootNode().getChildNodes().add(cubeNode1);
//        scene.getRootNode().getChildNodes().add(cubeNode2);
    }

    private void save(Scene scene, String name)  {
        try {
            scene.save(name + ".fbx", FileFormat.FBX7500ASCII);
            scene.save(name + ".stl", FileFormat.STL_BINARY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
