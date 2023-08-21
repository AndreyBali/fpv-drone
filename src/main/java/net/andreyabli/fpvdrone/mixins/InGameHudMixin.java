package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Main;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    // Makes HUD correspond to the player rather than the FreeCamera.
    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true)
    private void onGetCameraPlayer(CallbackInfoReturnable<PlayerEntity> cir) {
        if (Main.isEnabled()) {
            cir.setReturnValue(Main.MC.player);
        }
    }
}
