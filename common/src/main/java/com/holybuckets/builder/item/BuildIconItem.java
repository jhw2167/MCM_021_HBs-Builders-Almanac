package com.holybuckets.builder.item;

import com.holybuckets.builder.almanac.AlmanacCatalog;
import com.holybuckets.builder.almanac.AlmanacEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class BuildIconItem extends Item {

    public static final String KEY_ENTRY = "entry";

    public BuildIconItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(@Nullable AlmanacEntry entry) {
        if (entry == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ModItems.buildIcon, Math.max(1, entry.x()));
        stack.getOrCreateTag().putString(KEY_ENTRY, entry.getId());
        stack.setHoverName(entry.getDisplayName().copy().withStyle(s -> s.withItalic(false)));
        return stack;
    }

    @Nullable
    public static AlmanacEntry getEntry(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(KEY_ENTRY)) return null;
        return AlmanacCatalog.getEntry(tag.getString(KEY_ENTRY));
    }

}
