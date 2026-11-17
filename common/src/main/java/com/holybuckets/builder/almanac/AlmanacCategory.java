package com.holybuckets.builder.almanac;

import com.holybuckets.builder.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum AlmanacCategory {
    BASIC_SHAPES("basic_shapes", new ItemStack(Items.SMOOTH_STONE_SLAB), true, true),
    BASIC_3D("basic_3d", new ItemStack(Items.STONE_BRICKS), true, true),
    FRAME_3D("frame_3d", new ItemStack(Items.IRON_BARS), true, true),
    WALLS("walls", new ItemStack(Items.COBBLESTONE_WALL), true, true),
    HOUSES("houses", new ItemStack(Items.OAK_DOOR), false, false),
    SUPPORT_STRUCTURES("support_structures", new ItemStack(Items.SCAFFOLDING), false, false),
    ROOFS("roofs", new ItemStack(Items.DARK_OAK_STAIRS), false, false),
    SIMPLE_HOUSES("simple_houses", new ItemStack(Items.OAK_PLANKS), false, false),
    MEDIEVAL_HOUSES("medieval_houses", new ItemStack(Items.SPRUCE_LOG), false, false),
    MODERN_HOUSES("modern_houses", new ItemStack(Items.WHITE_CONCRETE), false, false);

    public final String key;
    public final ItemStack icon;
    public final boolean generated;
    public final boolean tips;

    // tips: E may tip the build over the horizontal axis the player faces
    AlmanacCategory(String key, ItemStack icon, boolean generated, boolean tips) {
        this.key = key;
        this.icon = icon;
        this.generated = generated;
        this.tips = tips;
    }

    public Component getTitle() {
        return Component.translatable("almanac." + Constants.MOD_ID + ".tab." + key);
    }
}
