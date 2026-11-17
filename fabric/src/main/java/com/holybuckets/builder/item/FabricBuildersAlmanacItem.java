package com.holybuckets.builder.item;

import com.holybuckets.builder.client.render.AlmanacItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FabricBuildersAlmanacItem extends BuildersAlmanacItem {

    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private AlmanacItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new AlmanacItemRenderer();
                return renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

}
