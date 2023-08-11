package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.andreyabli.fpvdrone.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    // Prevents player from being controlled when freecam is enabled.
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (Freecam.isEnabled()) {
            if (Freecam.MC.player != null && Freecam.MC.player.input instanceof KeyboardInput && !Freecam.isPlayerControlEnabled()) {
                Input input = new Input();
                input.sneaking = Freecam.MC.player.input.sneaking; // Makes player continue to sneak after freecam is enabled.
                Freecam.MC.player.input = input;
            }
            Freecam.MC.gameRenderer.setRenderHand(ModConfig.INSTANCE.visual.showHand);

            if (Freecam.disableNextTick()) {
                Freecam.toggle();
                Freecam.setDisableNextTick(false);
            }
        }
    }

    // Prevents attacks when allowInteract is disabled.
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isEnabled() && !Freecam.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            cir.cancel();
        }
    }

    // Prevents item pick when allowInteract is disabled.
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        if (Freecam.isEnabled() && !Freecam.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            ci.cancel();
        }
    }

    // Prevents block breaking when allowInteract is disabled.
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreaking(CallbackInfo ci) {
        if (Freecam.isEnabled() && !Freecam.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            ci.cancel();
        }
    }
}
