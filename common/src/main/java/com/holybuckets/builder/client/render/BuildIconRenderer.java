package com.holybuckets.builder.client.render;

import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.builder.client.render.BuildIconMesher.IconMesh;
import com.holybuckets.builder.item.BuildIconItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BuildIconRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float ICON_SCALE = 0.625F;
    private static final float ICON_PITCH = 30.0F;
    private static final float ICON_YAW = 225.0F;

    public BuildIconRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        AlmanacEntry entry = BuildIconItem.getEntry(stack);
        if (entry == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);

        IconMesh mesh = BuildIconMesher.getMesh(entry);
        if (mesh == null) {
            Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(entry.icon()), context, light, overlay, poseStack, buffers, null, 0);
            poseStack.popPose();
            return;
        }

        Vec3i size = mesh.getSize();
        float scale = ICON_SCALE / Math.max(1, Math.max(size.getX(), Math.max(size.getY(), size.getZ())));
        poseStack.mulPose(Axis.XP.rotationDegrees(ICON_PITCH));
        poseStack.mulPose(Axis.YP.rotationDegrees(ICON_YAW));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-size.getX() / 2.0F, -size.getY() / 2.0F, -size.getZ() / 2.0F);
        mesh.render(poseStack);
        poseStack.popPose();
    }

}
