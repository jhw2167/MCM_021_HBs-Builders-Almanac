package com.holybuckets.builder.mixin;

import com.holybuckets.builder.client.ClientAlmanacAnimator;
import com.holybuckets.builder.item.BuildersAlmanacItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//Allows us to render Builders Almanac in hand like a map,
@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    private static final float BOOK_SCALE = 0.38F;
    private static final float BUILD_SCALE = 0.28F;
    private static final float BOOK_PITCH = 245.0F;
    private static final float BOOK_YAW = 180.0F;
    private static final float BOOK_Y = -0.18F;
    private static final float BUILD_Y = -0.32F;
    private static final float BOOK_Z = 0.0F;
    private static final float HOVER_HEIGHT = 0.08F;
    private static final float BOB_HEIGHT = 0.015F;
    private static final float BOB_SPEED = 0.1F;

    @Shadow protected abstract void renderMapHand(PoseStack poseStack, MultiBufferSource buffer, int light, HumanoidArm arm);
    @Shadow protected abstract float calculateMapTilt(float pitch);
    @Shadow public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, boolean leftHand,
                                            PoseStack poseStack, MultiBufferSource buffer, int light);

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
                                     float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack,
                                     MultiBufferSource buffer, int light, CallbackInfo ci) {

        if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof BuildersAlmanacItem)
            || !player.getOffhandItem().isEmpty()) return;

        // Same arm placement as vanilla renderTwoHandedMap
        poseStack.pushPose();
        float swing = Mth.sqrt(swingProgress);
        poseStack.translate(0.0F, 0.1F * Mth.sin(swingProgress * Mth.PI), -0.4F * Mth.sin(swing * Mth.PI));
        float tilt = calculateMapTilt(pitch);
        poseStack.translate(0.0F, 0.04F + equipProgress * -1.2F + tilt * -0.5F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(tilt * -85.0F));
        if (!player.isInvisible()) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            renderMapHand(poseStack, buffer, light, HumanoidArm.RIGHT);
            renderMapHand(poseStack, buffer, light, HumanoidArm.LEFT);
            poseStack.popPose();
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(swing * Mth.PI) * 20.0F));

        float hover = ClientAlmanacAnimator.getHover(partialTicks);
        float bob = Mth.sin((player.tickCount + partialTicks) * BOB_SPEED) * BOB_HEIGHT;
        float build = ClientAlmanacAnimator.getBuildBlend(partialTicks);
        float scale = Mth.lerp(build, BOOK_SCALE, BUILD_SCALE);
        poseStack.translate(0.0F, Mth.lerp(build, BOOK_Y, BUILD_Y) + (HOVER_HEIGHT + bob) * hover, BOOK_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(BOOK_YAW));
        poseStack.mulPose(Axis.XP.rotationDegrees(BOOK_PITCH));
        poseStack.scale(scale, scale, scale);
        renderItem(player, stack, ItemDisplayContext.NONE, false, poseStack, buffer, light);
        poseStack.popPose();
        ci.cancel();
    }
}
