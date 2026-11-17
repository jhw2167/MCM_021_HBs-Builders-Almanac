package com.holybuckets.builder.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

/**
 * A build's orientation as a row-major 3x3 matrix of quarter turns, so turns about Y and about the player's axis combine in any order.
 */
public record BuildOrientation(int[] matrix) {

    public static final BuildOrientation IDENTITY = new BuildOrientation(new int[]{1, 0, 0, 0, 1, 0, 0, 0, 1});

    /** One clockwise quarter turn about a world axis, matching BlockPos.rotate for Y */
    public BuildOrientation turn(Direction.Axis axis) {
        int[] turn = switch (axis) {
            case X -> new int[]{1, 0, 0, 0, 0, -1, 0, 1, 0};
            case Y -> new int[]{0, 0, -1, 0, 1, 0, 1, 0, 0};
            case Z -> new int[]{0, -1, 0, 1, 0, 0, 0, 0, 1};
        };
        int[] result = new int[9];
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                for (int k = 0; k < 3; k++)
                    result[r * 3 + c] += turn[r * 3 + k] * matrix[k * 3 + c];
        return new BuildOrientation(result);
    }

    public BlockPos apply(BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new BlockPos(
            matrix[0] * x + matrix[1] * y + matrix[2] * z,
            matrix[3] * x + matrix[4] * y + matrix[5] * z,
            matrix[6] * x + matrix[7] * y + matrix[8] * z);
    }

    /** True when the build has only turned about Y, so block states can turn with it */
    public boolean isUpright() {
        return matrix[4] == 1;
    }

    /** The turn about Y for block states, read from where +x ends up */
    public Rotation getYRotation() {
        if (matrix[6] == 1) return Rotation.CLOCKWISE_90;
        if (matrix[0] == -1) return Rotation.CLOCKWISE_180;
        if (matrix[6] == -1) return Rotation.COUNTERCLOCKWISE_90;
        return Rotation.NONE;
    }

    public JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (int value : matrix) array.add(value);
        return array;
    }

    /** Falls back to IDENTITY unless the array is a signed permutation, so a client cannot stretch or skew a build */
    public static BuildOrientation fromJson(JsonElement json) {
        if (json == null || !json.isJsonArray() || json.getAsJsonArray().size() != 9) return IDENTITY;
        int[] m = new int[9];
        for (int i = 0; i < 9; i++) m[i] = json.getAsJsonArray().get(i).getAsInt();

        for (int i = 0; i < 3; i++) {
            int rowSum = 0;
            int colSum = 0;
            for (int j = 0; j < 3; j++) {
                if (Math.abs(m[i * 3 + j]) > 1) return IDENTITY;
                rowSum += Math.abs(m[i * 3 + j]);
                colSum += Math.abs(m[j * 3 + i]);
            }
            if (rowSum != 1 || colSum != 1) return IDENTITY;
        }
        return new BuildOrientation(m);
    }
}
