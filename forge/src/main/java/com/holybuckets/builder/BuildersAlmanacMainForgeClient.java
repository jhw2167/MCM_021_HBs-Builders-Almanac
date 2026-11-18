package com.holybuckets.builder;

import com.holybuckets.builder.client.CommonClassClient;
import com.holybuckets.builder.client.IBewlrRenderer;
import com.holybuckets.builder.client.render.BuildIconRenderer;
import com.holybuckets.builder.item.ModItems;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class BuildersAlmanacMainForgeClient {


    public static void clientInitializeForge() {
        CommonClassClient.initClient();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(BuildersAlmanacMainForgeClient::onClientSetup);
        //Item challengeChest = ModBlocks.challengeChest.asItem();
       // setBlockEntityRender( challengeChest, ChallengeItemBlockRenderer.CHEST_RENDERER);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> setBlockEntityRender(ModItems.buildIcon, new BuildIconRenderer()));
    }

        private static void setBlockEntityRender(Object item, BlockEntityWithoutLevelRenderer renderer) {
            ((IBewlrRenderer) item).setBlockEntityWithoutLevelRenderer(renderer);
        }

}
