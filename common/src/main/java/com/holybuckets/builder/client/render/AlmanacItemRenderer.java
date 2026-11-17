package com.holybuckets.builder.client.render;

import com.holybuckets.builder.Constants;
import com.holybuckets.builder.item.BuildersAlmanacItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class AlmanacItemRenderer extends GeoItemRenderer<BuildersAlmanacItem> {

    public AlmanacItemRenderer() {
        super(new DefaultedItemGeoModel<>(new ResourceLocation(Constants.MOD_ID, "builders_almanac")));
    }

}
