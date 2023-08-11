package net.andreyabli.fpvdrone.mixins;

import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import net.andreyabli.fpvdrone.Freecam;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.FreeCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // Disables block outlines when allowInteract is disabled.
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isEnabled() && !Freecam.isPlayerControlEnabled() && !ModConfig.INSTANCE.utility.allowInteract) {
            cir.setReturnValue(false);
        }
    }

    @Shadow private Camera camera;


    @Inject(method = "renderWorld", at = @At(
            value = "INVOKE",
            // Inject before the call to Camera.update()
            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V",
            shift = At.Shift.BEFORE
    ))
    private void PostCameraUpdate(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        if(camera.getFocusedEntity() instanceof FreeCamera freeCamera) {
            var q = freeCamera.getRotation(tickDelta);
            q.set(q.getX(), -q.getY(), q.getZ(), -q.getW());
            q.set(Convert.toBullet(QuaternionHelper.rotateX(Convert.toMinecraft(q), freeCamera.CAMERA_ANGLE)));

            var newMat = Convert.toMinecraft(q).get(new Matrix4f());
            newMat.transpose();
            matrix.multiplyPositionMatrix(newMat);
        }
    }
}
