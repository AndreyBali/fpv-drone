package net.xolt.freecam.mixins;

import com.jme3.math.Quaternion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.config.ModConfig;
import net.xolt.freecam.util.FreeCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Entity focusedEntity;

    @Shadow
    private float lastCameraY;

    @Shadow
    private float cameraY;
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);


    // When toggling freecam, update the camera's eye height instantly without any transition.
    @Inject(method = "update", at = @At("HEAD"))
    public void onUpdate(BlockView area, Entity newFocusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        // You'll need to provide newPosX, newPosY, newPosZ, newYaw, and newPitch
        if(!(focusedEntity instanceof FreeCamera freeCamera)) return;
        Vec3d vec = freeCamera.getPosition(tickDelta);
        Quaternion qRot = freeCamera.getRotation(tickDelta);
        float[] eulerAngles = toEulerAngles(qRot);

        this.setPos(vec.x, vec.y, vec.z);
        this.setRotation(eulerAngles[2], eulerAngles[1]);

    }

    @Unique
    private static float[] toEulerAngles(Quaternion q) {
        double ysqr = q.getY() * q.getY();

        // roll (x-axis rotation)
        double t0 = +2.0 * (q.getW() * q.getX() + q.getY() * q.getZ());
        double t1 = +1.0 - 2.0 * (q.getX() * q.getX() + ysqr);
        double roll = Math.atan2(t0, t1);

        // pitch (y-axis rotation)
        double t2 = +2.0 * (q.getW() * q.getY() - q.getZ() * q.getX());
        t2 = t2 > +1.0 ? +1.0 : t2;
        t2 = t2 < -1.0 ? -1.0 : t2;
        double pitch = Math.asin(t2);

        // yaw (z-axis rotation)
        double t3 = +2.0 * (q.getW() * q.getZ() + q.getX() * q.getY());
        double t4 = +1.0 - 2.0 * (ysqr + q.getZ() * q.getZ());
        double yaw = Math.atan2(t3, t4);

        return new float[]{(float) Math.toDegrees(roll), (float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw)};
    }


    // Removes the submersion overlay when underwater, in lava, or powdered snow.
    @Inject(method = "getSubmersionType", at = @At("HEAD"), cancellable = true)
    public void onGetSubmersionType(CallbackInfoReturnable<CameraSubmersionType> cir) {
        if (Freecam.isEnabled() && !ModConfig.INSTANCE.visual.showSubmersion) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
