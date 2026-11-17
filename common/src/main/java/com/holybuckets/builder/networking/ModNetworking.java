package com.holybuckets.builder.networking;

import com.holybuckets.builder.Constants;
import com.holybuckets.builder.client.ClientBuilderManager;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ModNetworking {

    public static String CLASS_ID = "014";

    public static void initialize() {
        BalmNetworking bn = Balm.getNetworking();
        bn.registerClientboundPacket(id(BuildLayoutMessage.LOCATION), BuildLayoutMessage.class, Codecs::encodeBuildLayout, Codecs::decodeBuildLayout, ModNetworking::handleBuildLayout);
    }

    private static ResourceLocation id(String location) {
        return new ResourceLocation(Constants.MOD_ID, location);
    }

    public static void handleBuildLayout(Player p, BuildLayoutMessage m) {
        ClientBuilderManager.onBuildLayout(m);
    }
}
