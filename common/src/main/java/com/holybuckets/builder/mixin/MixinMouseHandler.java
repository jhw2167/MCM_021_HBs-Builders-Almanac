package com.holybuckets.builder.mixin;

import com.holybuckets.builder.client.ClientBuilderManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (ClientBuilderManager.onMouseScroll(yOffset)) ci.cancel();
    }
}
