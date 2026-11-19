package com.holybuckets.builder.core;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Placeholder blocks every almanac .nbt is authored with, each swapped for the player's preview block of the same slotType.
 */
public enum BuildRole {
    PRIMARY(Blocks.COBBLESTONE, "block"),
    SECONDARY(Blocks.OAK_PLANKS, "block"),
    TERTIARY(Blocks.COBBLESTONE_SLAB, "slab"),
    QUATERNARY(Blocks.COBBLESTONE_STAIRS, "stairs");

    public final Block block;
    public final String slotType;

    BuildRole(Block block, String slotType) {
        this.block = block;
        this.slotType = slotType;
    }
}
