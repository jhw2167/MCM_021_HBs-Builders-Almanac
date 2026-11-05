package com.holybuckets.builder;

import com.holybuckets.builder.client.CommonClassClient;
import net.blay09.mods.balm.api.client.BalmClient;
import net.fabricmc.api.ClientModInitializer;


public class BuildersAlmanacMainFabricClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        BalmClient.initialize(Constants.MOD_ID, CommonClassClient::initClient);
    }

}
