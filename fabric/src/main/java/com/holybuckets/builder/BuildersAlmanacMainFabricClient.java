package com.holybuckets.builder;

import com.holybuckets.builder.client.CommonClassClient;
import com.holybuckets.builder.client.render.BuildIconRenderer;
import com.holybuckets.builder.item.ModItems;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;


public class BuildersAlmanacMainFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient);

        BuildIconRenderer buildIconRenderer = new BuildIconRenderer();
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.buildIcon, buildIconRenderer::renderByItem);
    }

}
