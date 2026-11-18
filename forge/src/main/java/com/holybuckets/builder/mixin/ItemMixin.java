package com.holybuckets.builder.mixin;

import com.holybuckets.builder.client.IBewlrRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Item.class)
public class ItemMixin implements IBewlrRenderer {

    @Shadow(remap = false)
    private Object renderProperties;

    @Override
    public void setBlockEntityWithoutLevelRenderer(BlockEntityWithoutLevelRenderer bewlr) {
        if (this.renderProperties != null)
            throw new IllegalStateException("Cannot set BlockEntityWithoutLevelRenderer because render properties already exist");

        this.renderProperties = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return bewlr;
            }
        };
    }

}
