package com.holybuckets.builder;

import com.holybuckets.builder.client.CommonClassClient;
import com.holybuckets.builder.client.IBewlrRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BuildersAlmanacMainForgeClient {


    public static void clientInitializeForge() {
        CommonClassClient.initClient();
        //Item challengeChest = ModBlocks.challengeChest.asItem();
       // setBlockEntityRender( challengeChest, ChallengeItemBlockRenderer.CHEST_RENDERER);
    }

        private static void setBlockEntityRender(Object item, BlockEntityWithoutLevelRenderer renderer) {
            ((IBewlrRenderer) item).setBlockEntityWithoutLevelRenderer(renderer);
        }

}
