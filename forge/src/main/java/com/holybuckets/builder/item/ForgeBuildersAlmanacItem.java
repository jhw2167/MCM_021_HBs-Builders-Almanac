package com.holybuckets.builder.item;

import com.holybuckets.builder.client.render.AlmanacItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ForgeBuildersAlmanacItem extends BuildersAlmanacItem {

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private AlmanacItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new AlmanacItemRenderer();
                return renderer;
            }
        });
    }

}
