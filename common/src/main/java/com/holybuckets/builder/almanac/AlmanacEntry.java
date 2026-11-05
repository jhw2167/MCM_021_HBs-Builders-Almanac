package com.holybuckets.builder.almanac;

import com.holybuckets.builder.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One prefab build. Size is 0 for builds without a dimension (houses, supports).
 */
public record AlmanacEntry(AlmanacCategory category, String name, int size, Item icon) {

    /** Unique id such as basic_shapes/cube_5 */
    public String getId() {
        String file = (size > 0) ? name + "_" + size : name;
        return category.key + "/" + file;
    }

    /** Resolves to data/hbs_builderalmanac/structures/almanac/[id].nbt */
    public ResourceLocation getStructureId() {
        return new ResourceLocation(Constants.MOD_ID, "almanac/" + getId());
    }

    public Component getDisplayName() {
        String key = "almanac." + Constants.MOD_ID + "." + category.key + "." + name;
        return (size > 0) ? Component.translatable(key, size) : Component.translatable(key);
    }

    public ItemStack createIcon() {
        ItemStack stack = new ItemStack(icon, Math.max(1, size));
        stack.setHoverName(getDisplayName().copy().withStyle(s -> s.withItalic(false)));
        return stack;
    }
}
