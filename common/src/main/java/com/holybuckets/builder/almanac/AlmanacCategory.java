package com.holybuckets.builder.almanac;

import com.holybuckets.builder.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum AlmanacCategory {
    BASIC_SHAPES("basic_shapes", new ItemStack(Items.STONE_BRICKS)),
    HOUSES("houses", new ItemStack(Items.OAK_DOOR)),
    SUPPORT_STRUCTURES("support_structures", new ItemStack(Items.SCAFFOLDING));

    public final String key;
    public final ItemStack icon;

    AlmanacCategory(String key, ItemStack icon) {
        this.key = key;
        this.icon = icon;
    }

    public Component getTitle() {
        return Component.translatable("almanac." + Constants.MOD_ID + ".tab." + key);
    }
}
