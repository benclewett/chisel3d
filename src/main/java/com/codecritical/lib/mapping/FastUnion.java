package com.codecritical.lib.mapping;

/*
 * Chisel3D, (C) 2024 Ben Clewett & Code Critical Ltd
 */

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import eu.printingin3d.javascad.vrl.CSG;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
public class FastUnion {
    static final Logger logger = Logger.getLogger("");

    private static final int UNION_CHUNK_SIZE = 2;

    // Static Class
    private FastUnion() {}

    /** Unions a large number of parts at some speed. */
    public static CSG fastUnion(ImmutableList<CSG> parts) {

        // Don't forget the mapToCsg() call.
        Verify.verify(parts.size() != 0);

        // The 'Union' class should be able to do this.  However, this slows down exponentially.
        // 10,000 parts can take 10 hours.  This method takes the same number of seconds.

        int i = 0;

        var partList = parts.stream()
                .filter(p -> !p.getPolygons().isEmpty())
                .collect(ImmutableList.toImmutableList());

        do {
            logger.info("Loop " + i + ", outstanding count: " + partList.size() + ", polygons: " +
                    partList.stream().mapToInt(u -> u.getPolygons().size()).sum());
            partList = union(partList);
            i++;
        } while (partList.size() > 1);

        logger.info("Loop " + i + ", outstanding count: " + partList.size() + ", polygons: " +
                partList.stream().mapToInt(u -> u.getPolygons().size()).sum());

        return partList.get(0);
    }

    /** Unions a list of parts into another list of half the size */
    private static ImmutableList<CSG> union(List<CSG> parts) {

        ImmutableList.Builder<CSG> newUnions = ImmutableList.builder();
        @CheckForNull CSG union = null;
        int count = 0;

        for (var part : parts) {
            union = (union == null) ? part : union.union(part);
            count++;
            if (count > 0 && union.getPolygons().isEmpty()) {
                // Bug, sometimes small sized unions break the model.  Just loose them and hope it's not too bad.
                union = part;
                count = 0;
            }

            if (count == UNION_CHUNK_SIZE) {
                newUnions.add(union);
                union = null;
                count = 0;
            }
        }

        if (union != null) {
            newUnions.add(union);
        }

        return newUnions.build();
    }
}
