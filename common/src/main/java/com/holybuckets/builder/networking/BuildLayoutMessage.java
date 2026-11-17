package com.holybuckets.builder.networking;

import com.holybuckets.builder.core.BuildLayout;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server to client message. Sends blockstates to client in batches for previewing builds.
 */
public class BuildLayoutMessage {

    public static final String LOCATION = "build_layout";

    // Keeps each serialized position string under FriendlyByteBuf's 32767 character limit
    public static final int MAX_BATCH_BLOCKS = 2000;

    final String buildId;
    final int batchIndex;
    final boolean hasMore;
    final BlockState[] previewBlocks;
    final Map<BlockState, List<BlockPos>> blocks;

    BuildLayoutMessage(String buildId, int batchIndex, boolean hasMore, BlockState[] previewBlocks, Map<BlockState, List<BlockPos>> blocks) {
        this.buildId = buildId;
        this.batchIndex = batchIndex;
        this.hasMore = hasMore;
        this.previewBlocks = previewBlocks;
        this.blocks = blocks;
    }

    public static void createAndFire(ServerPlayer player, String buildId, BuildLayout layout, BlockState[] previewBlocks) {
        List<Map<BlockState, List<BlockPos>>> batches = new ArrayList<>();
        Map<BlockState, List<BlockPos>> batch = new LinkedHashMap<>();
        int count = 0;

        for (Map.Entry<BlockState, List<BlockPos>> entry : layout.toStateMap().entrySet()) {
            for (BlockPos pos : entry.getValue()) {
                if (count == MAX_BATCH_BLOCKS) {
                    batches.add(batch);
                    batch = new LinkedHashMap<>();
                    count = 0;
                }
                batch.computeIfAbsent(entry.getKey(), s -> new ArrayList<>()).add(pos);
                count++;
            }
        }
        batches.add(batch);

        for (int i = 0; i < batches.size(); ++i) {
            boolean hasMore = i < batches.size() - 1;
            Balm.getNetworking().sendTo(player, new BuildLayoutMessage(buildId, i, hasMore, previewBlocks, batches.get(i)));
        }
    }

    public String getBuildId() { return buildId; }
    public int getBatchIndex() { return batchIndex; }
    public boolean hasMore() { return hasMore; }
    public BlockState[] getPreviewBlocks() { return previewBlocks; }
    public Map<BlockState, List<BlockPos>> getBlocks() { return blocks; }
}
