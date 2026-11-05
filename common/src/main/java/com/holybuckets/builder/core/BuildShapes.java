package com.holybuckets.builder.core;

import com.holybuckets.builder.almanac.AlmanacCategory;
import com.holybuckets.builder.almanac.AlmanacEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the block layout for Basic Shapes. Layouts are centered on x/z with the base at y=0,
 * shared by the client preview and server placement. Houses and supports return empty until their .nbt files exist.
 */
public class BuildShapes {

    public static Map<BlockPos, BlockState> getBlocks(AlmanacEntry entry, Rotation rotation) {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        BlockState state = Block.byItem(entry.icon()).defaultBlockState();
        if (entry.category() != AlmanacCategory.BASIC_SHAPES || state.isAir()) return blocks;

        int size = entry.size();
        int c = size / 2;
        double r = size / 2.0;
        for (int x = -c; x <= c; ++x) {
            for (int y = 0; y < size; ++y) {
                for (int z = -c; z <= c; ++z) {
                    if (inShape(entry.name(), x, y, z, c, r))
                        blocks.put(new BlockPos(x, y, z).rotate(rotation), state.rotate(rotation));
                }
            }
        }
        return blocks;
    }

    private static boolean inShape(String shape, int x, int y, int z, int c, double r) {
        int dy = y - c;
        return switch (shape) {
            case "square_plane" -> y == 0;
            case "cube" -> true;
            case "circle_plane" -> y == 0 && x * x + z * z <= r * r;
            case "hollow_cube" -> Math.abs(x) == c || Math.abs(z) == c || y == 0 || y == 2 * c;
            case "sphere" -> inSphere(x, dy, z, r);
            case "half_sphere" -> inSphere(x, y, z, r);
            case "hollow_sphere" -> inSphere(x, dy, z, r) && !(inSphere(x + 1, dy, z, r) && inSphere(x - 1, dy, z, r)
                && inSphere(x, dy + 1, z, r) && inSphere(x, dy - 1, z, r)
                && inSphere(x, dy, z + 1, r) && inSphere(x, dy, z - 1, r));
            default -> false;
        };
    }

    private static boolean inSphere(int x, int y, int z, double r) {
        return x * x + y * y + z * z <= r * r;
    }
}
