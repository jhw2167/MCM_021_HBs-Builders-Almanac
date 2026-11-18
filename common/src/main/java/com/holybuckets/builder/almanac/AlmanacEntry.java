package com.holybuckets.builder.almanac;

import com.holybuckets.builder.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One prefab build. x is width, z is depth and y is height;
 * unused dimensions are 0, all are 0 for houses and supports.
 */
public record AlmanacEntry(AlmanacCategory category, String name, int x, int y, int z, Item icon) {

    public String getId() {
        List<Integer> dims = getDimensions();
        if (dims.isEmpty()) return category.key + "/" + name;
        return category.key + "/" + name + "_" + String.join("x", dims.stream().map(String::valueOf).toList());
    }


    public ResourceLocation getStructureId() {
        return new ResourceLocation(Constants.MOD_ID, "almanac/" + getId());
    }

    public Component getDisplayName() {
        String key = "almanac." + Constants.MOD_ID + "." + category.key + "." + name;
        return Component.translatable(key, getDimensions().toArray());
    }

    // Ordered x, z, y to match the lang arguments
    private List<Integer> getDimensions() {
        List<Integer> dims = new ArrayList<>(3);
        if (x > 0) dims.add(x);
        if (z > 0) dims.add(z);
        if (y > 0) dims.add(y);
        return dims;
    }
}
