package com.codecritical.parts;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.export.StlBinaryFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExportStl {
    static final Logger logger = Logger.getLogger("");

    public static void export(String fileName, CSG csg) {
        String path = "./output/" + fileName;

        try {
            OutputStream outputStream = new FileOutputStream(path);
            StlBinaryFile stl = new StlBinaryFile(outputStream);
            stl.writeToFile(csg.toFacets());
            outputStream.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}
