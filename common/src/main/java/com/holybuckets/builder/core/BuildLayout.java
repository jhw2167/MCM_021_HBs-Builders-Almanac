package com.holybuckets.builder.core;

import com.holybuckets.builder.mixin.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Block positions and states of a build relative to its origin, centered on x/z with the base at y=0.
 * Built on the server from an .nbt or BuildShapes and sent to the client for the preview.
 */
public record BuildLayout(List<BlockPos> positions, List<BlockState> states) {

    public static final BuildLayout EMPTY = new BuildLayout(List.of(), List.of());

    /** Reads the first palette of a structure template, skipping air */
    public static BuildLayout fromTemplate(StructureTemplate template) {
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).getPalettes();
        if (palettes.isEmpty()) return EMPTY;

        Vec3i size = template.getSize();
        BlockPos center = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
        List<BlockPos> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
            if (info.state().isAir()) continue;
            positions.add(info.pos().subtract(center));
            states.add(info.state());
        }
        return new BuildLayout(positions, states);
    }

    public Map<BlockPos, BlockState> getBlocks(BuildOrientation orientation) {
        List<BlockPos> turned = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) turned.add(orientation.apply(pos));

        BoundingBox box = BoundingBox.encapsulatingPositions(turned).orElse(new BoundingBox(BlockPos.ZERO));
        BlockPos shift = new BlockPos(-(box.minX() + box.maxX()) / 2, -box.minY(), -(box.minZ() + box.maxZ()) / 2);
        Rotation stateRotation = orientation.isUpright() ? orientation.getYRotation() : Rotation.NONE;

        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        for (int i = 0; i < turned.size(); ++i) {
            blocks.put(turned.get(i).offset(shift), states.get(i).rotate(stateRotation));
        }
        return blocks;
    }

    public Map<BlockState, List<BlockPos>> toStateMap() {
        Map<BlockState, List<BlockPos>> map = new LinkedHashMap<>();
        for (int i = 0; i < positions.size(); ++i) {
            map.computeIfAbsent(states.get(i), s -> new ArrayList<>()).add(positions.get(i));
        }
        return map;
    }

    public static BuildLayout fromStateMap(Map<BlockState, List<BlockPos>> map) {
        List<BlockPos> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        map.forEach((state, list) -> {
            for (BlockPos pos : list) {
                positions.add(pos);
                states.add(state);
            }
        });
        return new BuildLayout(positions, states);
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }
}
