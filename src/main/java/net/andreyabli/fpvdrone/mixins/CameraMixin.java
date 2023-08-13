package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Freecam;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.FreeCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (focusedEntity instanceof FreeCamera freeCamera) {
            var pos = freeCamera.getPosition(tickDelta);
            this.setPos(pos.x, pos.y, pos.z);
            this.setRotation(0,0);
        }
    }

    // Removes the submersion overlay when underwater, in lava, or powdered snow.
    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    public void onGetSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (Freecam.isEnabled() && !ModConfig.INSTANCE.visual.showSubmersion) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
