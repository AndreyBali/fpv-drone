package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Freecam;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    // Makes mouse input rotate the FreeCamera.
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double x, double y, CallbackInfo ci) {
        if (Freecam.isEnabled() && this.equals(Freecam.MC.player) && !Freecam.isPlayerControlEnabled()) {
            Freecam.getFreeCamera().changeLookDirection(x, y);
            ci.cancel();
        }
    }

    // Prevents FreeCamera from pushing/getting pushed by entities.
    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void onPushAwayFrom(Entity entity, CallbackInfo ci) {
        if (Freecam.isEnabled() && (entity.equals(Freecam.getFreeCamera()) || this.equals(Freecam.getFreeCamera()))) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "setVelocity", at = @At("HEAD"), cancellable = true)
    private void onSetVelocity(CallbackInfo ci) {
        if (Freecam.isEnabled() && ModConfig.INSTANCE.utility.freezePlayer && !Freecam.isPlayerControlEnabled() && this.equals(Freecam.MC.player)) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "updateVelocity", at = @At("HEAD"), cancellable = true)
    private void onUpdateVelocity(CallbackInfo ci) {
        if (Freecam.isEnabled() && ModConfig.INSTANCE.utility.freezePlayer && !Freecam.isPlayerControlEnabled() && this.equals(Freecam.MC.player)) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "setPosition", at = @At("HEAD"), cancellable = true)
    private void onSetPosition(CallbackInfo ci) {
        if (Freecam.isEnabled() && ModConfig.INSTANCE.utility.freezePlayer && !Freecam.isPlayerControlEnabled() && this.equals(Freecam.MC.player)) {
            ci.cancel();
        }
    }

    // Freezes the player's position if freezePlayer is enabled.
    @Inject(method = "setPos", at = @At("HEAD"), cancellable = true)
    private void onSetPos(CallbackInfo ci) {
        if (Freecam.isEnabled() && ModConfig.INSTANCE.utility.freezePlayer && !Freecam.isPlayerControlEnabled() && this.equals(Freecam.MC.player)) {
            ci.cancel();
        }
    }
}
