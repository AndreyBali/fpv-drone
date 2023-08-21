package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Main;
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
        if (Main.isEnabled()) {
            if (Main.MC.player != null && Main.MC.player.input instanceof KeyboardInput && !Main.isPlayerControlEnabled()) {
                Input input = new Input();
                input.sneaking = Main.MC.player.input.sneaking; // Makes player continue to sneak after freecam is enabled.
                Main.MC.player.input = input;
            }
            Main.MC.gameRenderer.setRenderHand(ModConfig.INSTANCE.visual.showHand);

            if (Main.disableNextTick()) {
                Main.toggle();
                Main.setDisableNextTick(false);
            }
        }
    }

    // Prevents attacks when allowInteract is disabled.
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Main.isEnabled() && !Main.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            cir.cancel();
        }
    }

    // Prevents item pick when allowInteract is disabled.
    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        if (Main.isEnabled() && !Main.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            ci.cancel();
        }
    }

    // Prevents block breaking when allowInteract is disabled.
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void onHandleBlockBreaking(CallbackInfo ci) {
        if (Main.isEnabled() && !Main.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            ci.cancel();
        }
    }
}
