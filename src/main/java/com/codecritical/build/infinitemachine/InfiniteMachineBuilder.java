package com.codecritical.build.infinitemachine;

import com.codecritical.build.lib.config.Config;
import com.codecritical.build.lib.config.ConfigReader;
import com.codecritical.parts.*;
import com.google.common.collect.ImmutableMap;
import eu.printingin3d.javascad.coords.Coords3d;
import eu.printingin3d.javascad.coords.Dims3d;
import eu.printingin3d.javascad.tranform.ITransformation;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.vrl.CSG;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class InfiniteMachineBuilder {
    static final Logger logger = Logger.getLogger("");

    private final ShaftMultiBox root;
    private final Random random;
    private final ImmutableMap<String, List<IParts>> parts;
    private final ConfigReader config;
    private final CSG csg;
    private final int minCount;
    private final int maxCount;
    private final double minScale;
    private final double maxScale;

    final int depth;
    private final double shaftLenMin;
    private final double shaftLenMax;

    public InfiniteMachineBuilder(ConfigReader config, Random random) {

        this.config = config;
        this.depth = config.asInt(Config.InfiniteMachineConfig.DEPTH);
        this.random = random;

        logger.info("depth=" + depth);

        this.root = new ShaftMultiBox(
                config.asDims3d(Config.InfiniteMachineConfig.ROOT_BOX_ORIGIN),
                config.asDims3d(Config.InfiniteMachineConfig.ROOT_BOX_SIZE)
        );
        logger.info("Root ShardMultiBox: " + root);

        this.minCount = config.asInt(Config.InfiniteMachineConfig.ShaftBoxBranch.MIN_BRANCH_COUNT);
        this.maxCount = config.asInt(Config.InfiniteMachineConfig.ShaftBoxBranch.MAX_BRANCH_COUNT);

        this.minScale = config.asDouble(Config.InfiniteMachineConfig.ShaftBoxBranch.SIZE_SCALE_MIN);
        this.maxScale = config.asDouble(Config.InfiniteMachineConfig.ShaftBoxBranch.SIZE_SCALE_MAX);

        this.shaftLenMin = config.asDouble(Config.InfiniteMachineConfig.ShaftBoxBranch.SHAFT_LENGTH_MIN);
        this.shaftLenMax = config.asDouble(Config.InfiniteMachineConfig.ShaftBoxBranch.SHAFT_LENGTH_MAX);

        parts = buildAll();

        var transform = getNormalise(parts);

        csg = buildCsgFast(transform);

        ExportStl.export(config.asString(Config.InfiniteMachineConfig.OUTPUT_FILENAME), csg);
    }


    private ImmutableMap<String, List<IParts>> buildAll() {

        Map<String, List<IParts>> items = new HashMap<>();

        addPart(items, root);

        int branchDepth = 0;

        addBranches(root, branchDepth, items);

        logger.info("Part count: " + items.values().stream().flatMap(Collection::stream).count());

        return ImmutableMap.copyOf(items);
    }

    private void addBranches(ShaftMultiBox parent, int branchDepth, Map<String, List<IParts>> items) {
        if (depth <= branchDepth) {
            return;
        }

        int count = getRandomFromRange(minCount, maxCount);
        IntStream.range(0, count)
                .forEach(i -> addSingleBranch(parent, items, branchDepth));
    }

    private void addSingleBranch(ShaftMultiBox parent, Map<String, List<IParts>> items, int branchDepth) {
        ShaftMultiBox nextBox;
        Shaft nextShaft;
        int attempt = 0;
        do {
            if (attempt++ >= 3) {
                return;
            } else {
                nextBox = buildNextShaftMultiBox(parent);
                nextShaft = buildNextShaft(parent, nextBox);
            }
        } while (anyIntersection(items.values(), nextBox));
            // anyIntersection(items.values(), nextShaft));

        addPart(items, nextBox);
        addPart(items, nextShaft);
        addBranches(nextBox, branchDepth + 1, items);
    }

    private boolean anyIntersection(Collection<List<IParts>> values, IParts part) {
        return anyIntersection(values, part.getCsg());
    }
    private boolean anyIntersection(Collection<List<IParts>> values, CSG csg) {
        return values.stream()
                .flatMap(Collection::stream)
                .anyMatch(part -> 0 != part.getCsg().intersect(csg).getPolygons().size());
    }

    private Shaft buildNextShaft(ShaftMultiBox parent, ShaftMultiBox nextBox) {
        var o0 = parent.getOrigin();
        var o1 = nextBox.getOrigin();
        if (o0.getX() < o1.getX()) {
            double x0 = o0.getX() + (parent.getSize().getX() / 2.0);
            double x1 = o1.getX() - (nextBox.getSize().getX() / 2.0);
            var newOrigin = new Dims3d((x0 + x1) / 2.0, o0.getY(), o0.getZ());
            return new Shaft(newOrigin, Math.abs(x0 - x1), nextBox.getSize().getY() / 5.0, Axis.X_PLUS);
      } else if (o0.getX() > o1.getX()) {
            double x0 = o0.getX() - (parent.getSize().getX() / 2.0);
            double x1 = o1.getX() + (nextBox.getSize().getX() / 2.0);
            var newOrigin = new Dims3d((x0 + x1) / 2.0, o0.getY(), o0.getZ());
            return new Shaft(newOrigin, Math.abs(x0 - x1), nextBox.getSize().getY() / 5.0, Axis.X_MINUS);
        } else if (o0.getY() < o1.getY()) {
            double y0 = o0.getY() + (parent.getSize().getY() / 2.0);
            double y1 = o1.getY() - (nextBox.getSize().getY() / 2.0);
            var newOrigin = new Dims3d(o0.getX(), (y0 + y1) / 2.0, o0.getZ());
            return new Shaft(newOrigin, Math.abs(y0 - y1), nextBox.getSize().getY() / 5.0, Axis.Y_PLUS);
        } else if (o0.getY() > o1.getY()) {
            double y0 = o0.getY() - (parent.getSize().getY() / 2.0);
            double y1 = o1.getY() + (nextBox.getSize().getY() / 2.0);
            var newOrigin = new Dims3d(o0.getX(), (y0 + y1) / 2.0, o0.getZ());
            return new Shaft(newOrigin, Math.abs(y0 - y1), nextBox.getSize().getY() / 5.0, Axis.Y_MINUS);
        } else if (o0.getZ() < o1.getZ()) {
            double z0 = o0.getZ() + (parent.getSize().getZ() / 2.0);
            double z1 = o1.getZ() - (nextBox.getSize().getZ() / 2.0);
            var newOrigin = new Dims3d(o0.getX(), o0.getY(), (z0 + z1) / 2.0);
            return new Shaft(newOrigin, Math.abs(z0 - z1), nextBox.getSize().getZ() / 5.0, Axis.Z_PLUS);
        } else {
            double z0 = o0.getZ() - (parent.getSize().getZ() / 2.0);
            double z1 = o1.getZ() + (nextBox.getSize().getZ() / 2.0);
            var newOrigin = new Dims3d(o0.getX(), o0.getY(), (z0 + z1) / 2.0);
            return new Shaft(newOrigin, Math.abs(z0 - z1), nextBox.getSize().getZ() / 5.0, Axis.Z_MINUS);
        }
    }

    private ShaftMultiBox buildNextShaftMultiBox(ShaftMultiBox parent) {
        var axis = Axis.getRandomAxis(random);
        var scale = new Dims3d(getRandomFromRange(minScale, maxScale), getRandomFromRange(minScale, maxScale), getRandomFromRange(minScale, maxScale));
        double lengthScale = getRandomFromRange(this.shaftLenMin, this.shaftLenMax);
        var newOrigin = extendOrigin(parent, axis, parent.getSize().mul(lengthScale));
        var newSize = parent.getSize().mul(scale);
        return new ShaftMultiBox(newOrigin, newSize);
    }

    private void addPart(Map<String, List<IParts>> items, IParts part) {
        var clazz = part.getClass().toString();
        clazz = clazz.substring(clazz.lastIndexOf(".") + 1);
        if (items.containsKey(clazz)) {
            items.get(clazz).add(part);
        } else {
            List<IParts> l = new ArrayList<>();
            l.add(part);
            items.put(clazz, l);
        }
    }

    private static Dims3d extendOrigin(IParts source, Axis axis, Dims3d length) {
        var origin = source.getOrigin();
        return switch (axis) {
            case X_PLUS -> new Dims3d(origin.getX() + length.getX(), origin.getY(), origin.getZ());
            case X_MINUS -> new Dims3d(origin.getX() - length.getX(), origin.getY(), origin.getZ());
            case Y_PLUS -> new Dims3d(origin.getX(), origin.getY() + length.getY(), origin.getZ());
            case Y_MINUS -> new Dims3d(origin.getX(), origin.getY() - length.getY(), origin.getZ());
            case Z_PLUS -> new Dims3d(origin.getX(), origin.getY(), origin.getZ() + length.getZ());
            case Z_MINUS -> new Dims3d(origin.getX(), origin.getY(), origin.getZ() - length.getZ());
        };
    }

    private int getRandomFromRange(int i0, int i1) {
        if (i0 == i1) {
            return i0;
        } else {
            return random.nextInt(i1 - i0 + 1) + i0;
        }
    }

    private double getRandomFromRange(double d0, double d1) {
        if (d0 == d1) {
            return d0;
        } else {
            return random.nextDouble() * (d1 - d0) + d0;
        }
    }

    public CSG getCsg() {
        return csg;
    }

    private ITransformation getNormalise(ImmutableMap<String, List<IParts>> parts) {

        AtomicReference<Dims3d> min = new AtomicReference<>();

        parts.values().stream().flatMap(Collection::stream)
                .forEach(p -> {
                    Dims3d partStart = getPartMin(p.getOrigin(), p.getSize());
                    if (min.get() == null) {
                        min.set(partStart);
                    } else {
                        if (partStart.getX() < min.get().getX()) {
                            min.set(new Dims3d(partStart.getX(), min.get().getY(), min.get().getZ()));
                        }
                        if (partStart.getY() < min.get().getY()) {
                            min.set(new Dims3d(min.get().getX(), partStart.getY(), min.get().getZ()));
                        }
                        if (partStart.getZ() < min.get().getZ()) {
                            min.set(new Dims3d(min.get().getX(), min.get().getY(), partStart.getZ()));
                        }
                    }
                });

        logger.info("Min coordinates: " + min.get());

        return TransformationFactory.getTranlationMatrix(new Coords3d(
                -min.get().getX(),
                -min.get().getY(),
                -min.get().getZ()));

    }

    private Dims3d getPartMin(Dims3d origin, Dims3d size) {
        double x = origin.getX() - size.getX() / 2.0;
        double y = origin.getY() - size.getY() / 2.0;
        double z = origin.getZ() - size.getZ() / 2.0;
        return new Dims3d(x, y, z);
    }

    private CSG buildCsg(ITransformation transform) {
        CSG union = null;
        int i = 0;
        var partList = parts.values().stream()
                .flatMap(Collection::stream)
                .filter(p -> !p.getCsg().getPolygons().isEmpty())
                .toList();
        for (var part : partList) {
            var nextUnion = (union == null)
                    ? part.getCsg().transformed(transform)
                    : union.union(part.getCsg().transformed(transform));
            union = (nextUnion.getPolygons().isEmpty())     // Bug, sometimes small sized unions break the model.
                    ? union
                    : nextUnion;
            if (i % 20 == 0) {
                logger.info("Part " + i + " of " + partList.size() + ", Union polygon count: " + union.getPolygons().size());
            }
            i++;
        }
        logger.info("Part " + partList.size() + " of " + partList.size() + ", Union polygon count: " + union.getPolygons().size());
        return union;
    }

    private CSG buildCsgFast(ITransformation transform) {
        CSG union = null;
        int i = 0;
        var partList = parts.values().stream()
                .flatMap(Collection::stream)
                .filter(p -> !p.getCsg().getPolygons().isEmpty())
                .toList();

        List<CSG> unions = partList.stream()
                .map(part -> part.getCsg().transformed(transform))
                .toList();

        do {
            logger.info("Loop " + i + ", union count: " + unions.size() + ", polygons: " +
                    unions.stream().mapToInt(u -> u.getPolygons().size()).sum());
            unions = unionPairs(unions);
            i++;
        } while (unions.size() > 1);

        logger.info("Loop " + i + ", union count: " + unions.size() + ", polygons: " +
                unions.stream().mapToInt(u -> u.getPolygons().size()).sum());

        return unions.get(0);
    }

    private List<CSG> unionPairs(List<CSG> parts) {

        List<CSG> newUnions = new ArrayList<>();
        CSG union = null;
        int count = 0;

        for (var part : parts) {
            union = (union == null) ? part : union.union(part);
            count++;
            if (count > 0 && union.getPolygons().isEmpty()) {
                // Bug, sometimes small sized unions break the model.
                union = part;
                count = 0;
            }

            if (count == 2) {
                newUnions.add(union);
                union = null;
                count = 0;
            }
        }

        if (union != null) {
            newUnions.add(union);
        }

        return newUnions;
    }

}
