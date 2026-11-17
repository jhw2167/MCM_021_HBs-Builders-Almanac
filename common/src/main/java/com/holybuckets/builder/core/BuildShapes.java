package com.holybuckets.builder.core;

import com.holybuckets.builder.almanac.AlmanacCatalog;
import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.foundation.AAA.ShapeGen;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates the shape tabs in the PRIMARY role block through Foundation's ShapeGen,
 * centered on x/z with the base at y=0.
 */
public class BuildShapes {

    public static BuildLayout getLayout(AlmanacEntry entry) {
        boolean hollow = entry.name().startsWith(AlmanacCatalog.HOLLOW);
        String shape = hollow ? entry.name().substring(AlmanacCatalog.HOLLOW.length()) : entry.name();
        List<BlockPos> positions = switch (entry.category()) {
            case BASIC_SHAPES -> hollow ? ShapeGen.outline(plane(shape, entry)) : plane(shape, entry);
            case BASIC_3D -> hollow ? ShapeGen.shell(solid(shape, entry)) : solid(shape, entry);
            case FRAME_3D -> frame(entry);
            case WALLS -> wall(entry);
            default -> List.of();
        };
        return new BuildLayout(positions, Collections.nCopies(positions.size(), BuildRole.PRIMARY.block.defaultBlockState()));
    }

    private static List<BlockPos> plane(String shape, AlmanacEntry e) {
        return switch (shape) {
            case "circle" -> ShapeGen.circle(BlockPos.ZERO, e.x());
            case "ellipse" -> ShapeGen.ellipse(BlockPos.ZERO, e.x(), e.z());
            case "semicircle" -> ShapeGen.semicircle(BlockPos.ZERO, e.x());
            case "square" -> ShapeGen.square(BlockPos.ZERO, e.x(), e.x());
            case "rectangle" -> ShapeGen.square(BlockPos.ZERO, e.x(), e.z());
            case "triangle" -> ShapeGen.triangle(BlockPos.ZERO, e.x(), e.z());
            case "right_triangle" -> ShapeGen.rightTriangle(BlockPos.ZERO, e.x(), e.z());
            case "trapezoid" -> ShapeGen.trapezoid(BlockPos.ZERO, e.x(), topWidth(e.x()), e.z());
            default -> List.of();
        };
    }

    private static List<BlockPos> solid(String shape, AlmanacEntry e) {
        return switch (shape) {
            case "sphere" -> ShapeGen.sphere(BlockPos.ZERO, e.x());
            case "half_sphere" -> ShapeGen.halfSphere(BlockPos.ZERO, e.x());
            case "cube" -> ShapeGen.cuboid(BlockPos.ZERO, e.x(), e.y(), e.x());
            case "prism" -> ShapeGen.prism(BlockPos.ZERO, e.x(), e.x(), e.y());
            case "cylinder" -> ShapeGen.cylinder(BlockPos.ZERO, e.x(), e.y());
            case "cone" -> ShapeGen.cone(BlockPos.ZERO, e.x(), e.y());
            case "truncated_cone" -> ShapeGen.frustum(BlockPos.ZERO, e.x(), topWidth(e.x()), e.y());
            default -> List.of();
        };
    }

    private static List<BlockPos> frame(AlmanacEntry e) {
        return switch (e.name()) {
            case "sphere" -> ShapeGen.sphereFrame(BlockPos.ZERO, e.x());
            case "half_sphere" -> ShapeGen.halfSphereFrame(BlockPos.ZERO, e.x());
            case "cube" -> ShapeGen.cuboidFrame(BlockPos.ZERO, e.x(), e.y(), e.x());
            case "prism" -> ShapeGen.prismFrame(BlockPos.ZERO, e.x(), e.x(), e.y());
            case "cylinder" -> ShapeGen.cylinderFrame(BlockPos.ZERO, e.x(), e.y());
            case "cone" -> ShapeGen.frustumFrame(BlockPos.ZERO, e.x(), 1, e.y());
            case "truncated_cone" -> ShapeGen.frustumFrame(BlockPos.ZERO, e.x(), topWidth(e.x()), e.y());
            default -> List.of();
        };
    }

    // Walls run along x, so Q/E turning about Y lines them up with other directions
    private static List<BlockPos> wall(AlmanacEntry e) {
        int length = e.x();
        int height = e.y();
        BlockPos o = BlockPos.ZERO;
        return switch (e.name()) {
            case "single_wall" -> thickWall(length, height, 1);
            case "double_wall" -> thickWall(length, height, 2);
            case "triple_wall" -> thickWall(length, height, 3);
            case "crenellated_wall" -> crenellate(ShapeGen.cuboid(o, length, height, 1), height);
            case "corner_wall" -> ShapeGen.extrude(cornerFootprint(length), height);
            case "square_enclosure" -> ShapeGen.extrude(ShapeGen.outline(ShapeGen.square(o, length, length)), height);
            case "round_enclosure" -> ShapeGen.extrude(ShapeGen.outline(ShapeGen.circle(o, length)), height);
            default -> List.of();
        };
    }

    private static List<BlockPos> thickWall(int length, int height, int thickness) {
        List<BlockPos> positions = new ArrayList<>();
        for (int t = 0; t < thickness; t++) {
            positions.addAll(ShapeGen.cuboid(new BlockPos(0, 0, t - (thickness - 1) / 2), length, height, 1));
        }
        return positions;
    }

    private static List<BlockPos> crenellate(List<BlockPos> wall, int height) {
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : wall) {
            if (pos.getY() < height - 1 || Math.floorMod(pos.getX(), 2) == 0) positions.add(pos);
        }
        return positions;
    }

    private static List<BlockPos> cornerFootprint(int length) {
        int start = -(length - 1) / 2;
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : ShapeGen.square(BlockPos.ZERO, length, length)) {
            if (pos.getX() == start || pos.getZ() == start) positions.add(pos);
        }
        return positions;
    }

    private static int topWidth(int base) {
        int top = base / 2;
        return ((base - top) % 2 == 0) ? top : top + 1;
    }
}
