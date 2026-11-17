package com.holybuckets.builder.mixin;

import com.holybuckets.builder.client.ClientBuilderManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Don't love mixins here, but it is just the simplest way to do this
@Mixin(Minecraft.class)
public class MixinMinecraftAttack {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ClientBuilderManager.onAttack()) cir.setReturnValue(false);
    }

    // Stops held left click from mining while in build mode
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void onContinueAttack(boolean leftClick, CallbackInfo ci) {
        if (ClientBuilderManager.isActive()) ci.cancel();
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void onPickBlock(CallbackInfo ci) {
        if (ClientBuilderManager.onMiddleClick()) ci.cancel();
    }
}
