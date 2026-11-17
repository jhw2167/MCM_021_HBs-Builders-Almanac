package com.holybuckets.builder.networking;

import com.holybuckets.builder.core.BuildRole;
import net.minecraft.world.level.block.Block;

import com.holybuckets.foundation.HBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Codecs {

    //BuildLayout
    public static final FriendlyByteBuf encodeBuildLayout(BuildLayoutMessage object, FriendlyByteBuf buf) {
        buf.writeUtf(object.buildId);
        buf.writeVarInt(object.batchIndex);
        buf.writeBoolean(object.hasMore);
        for (BlockState state : object.previewBlocks) {
            buf.writeId(Block.BLOCK_STATE_REGISTRY, state);
        }

        // State as a registry id keeps properties like stair facing; positions use HBUtil's string format
        buf.writeVarInt(object.blocks.size());
        for (Map.Entry<BlockState, List<BlockPos>> entry : object.blocks.entrySet()) {
            buf.writeId(Block.BLOCK_STATE_REGISTRY, entry.getKey());
            buf.writeUtf(HBUtil.BlockUtil.serializeBlockPos(entry.getValue()));
        }
        return buf;
    }

    public static final BuildLayoutMessage decodeBuildLayout(FriendlyByteBuf buf) {
        String buildId = buf.readUtf();
        int batchIndex = buf.readVarInt();
        boolean hasMore = buf.readBoolean();
        BlockState[] previewBlocks = new BlockState[BuildRole.values().length];
        for (int i = 0; i < previewBlocks.length; ++i) {
            previewBlocks[i] = buf.readById(Block.BLOCK_STATE_REGISTRY);
        }

        int stateCount = buf.readVarInt();
        Map<BlockState, List<BlockPos>> blocks = new LinkedHashMap<>();
        for (int i = 0; i < stateCount; ++i) {
            BlockState state = buf.readById(Block.BLOCK_STATE_REGISTRY);
            blocks.put(state, HBUtil.BlockUtil.deserializeBlockPos(buf.readUtf()));
        }
        return new BuildLayoutMessage(buildId, batchIndex, hasMore, previewBlocks, blocks);
    }

}
