package net.xolt.freecam.mixins;

import com.jme3.math.Quaternion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.config.ModConfig;
import net.xolt.freecam.util.FreeCamera;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.xolt.freecam.Freecam.MC;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Entity focusedEntity;

    @Shadow
    private float lastCameraY;

    @Shadow
    private float cameraY;
    @Shadow
    private boolean ready;
    @Shadow
    private BlockView area;
    @Shadow
    private boolean thirdPerson;
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;
    private float roll;
    @Mutable
    @Shadow
    @Final
    private Quaternionf rotation;
    @Shadow
    @Final
    private Vector3f horizontalPlane;
    @Shadow
    @Final
    private Vector3f verticalPlane;
    @Shadow
    @Final
    private Vector3f diagonalPlane;


    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);


    // When toggling freecam, update the camera's eye height instantly without any transition.
//    @Inject(method = "update", at = @At("HEAD"))
//    public void onUpdate(BlockView area, Entity newFocusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
////        MC.player.
//        // You'll need to provide newPosX, newPosY, newPosZ, newYaw, and newPitch
//        if(!(focusedEntity instanceof FreeCamera freeCamera)) return;
//        Vec3d vec = freeCamera.getPosition(tickDelta);
//        Quaternion qRot = freeCamera.getRotation(tickDelta);
//        float[] eulerAngles = toEulerAngles(qRot);
//
//        this.setPos(vec.x, vec.y, vec.z);
//        this.setRotation(eulerAngles[2], eulerAngles[1]);
//
//    }

//    @Redirect(method = "update", at = @At(
//            value = "INVOKE",
//            // Inject before the call to Camera.update()
//            target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"
//    ))

    @Shadow
    protected abstract void moveBy(double x, double y, double z);

    @Shadow
    protected abstract double clipToSpace(double desiredCameraDistance);


    /**
     * @author
     * @reason
     */
    @Overwrite
    public void update(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
        if (!(focusedEntity instanceof FreeCamera freeCamera)) { //do it as normal
            this.ready = true;
            this.area = area;
            this.focusedEntity = focusedEntity;
            this.thirdPerson = thirdPerson;
            this.setRotation(focusedEntity.getYaw(tickDelta), focusedEntity.getPitch(tickDelta));
            this.setPos(MathHelper.lerp((double) tickDelta, focusedEntity.prevX, focusedEntity.getX()), MathHelper.lerp((double) tickDelta, focusedEntity.prevY, focusedEntity.getY()) + (double) MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY), MathHelper.lerp((double) tickDelta, focusedEntity.prevZ, focusedEntity.getZ()));
            if (thirdPerson) {
                if (inverseView) {
                    this.setRotation(this.yaw + 180.0F, -this.pitch);
                }

                this.moveBy(-this.clipToSpace(4.0), 0.0, 0.0);
            } else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
                Direction direction = ((LivingEntity) focusedEntity).getSleepingDirection();
                this.setRotation(direction != null ? direction.asRotation() - 180.0F : 0.0F, 0.0F);
                this.moveBy(0.0, 0.3, 0.0);
            }
            return;
        }
        this.ready = true;
        this.area = area;
        this.focusedEntity = focusedEntity;
        this.thirdPerson = thirdPerson;
        { //set rotation
            yaw = freeCamera.getYaw(tickDelta);
            pitch = freeCamera.getPitch(tickDelta);
//            roll = freeCamera.getRoll(tickDelta);
            this.rotation = freeCamera.getRotationF(tickDelta);
//            this.rotation.rotationYXZ(-yaw * 0.017453292F, pitch * 0.017453292F, roll * 0.017453292F);
            this.horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
            this.verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
            this.diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);

        }
//        this.setPos(MathHelper.lerp((double) tickDelta, focusedEntity.prevX, focusedEntity.getX()), MathHelper.lerp((double) tickDelta, focusedEntity.prevY, focusedEntity.getY()) + (double) MathHelper.lerp(tickDelta, this.lastCameraY, this.cameraY), MathHelper.lerp((double) tickDelta, focusedEntity.prevZ, focusedEntity.getZ()));
        var pos = freeCamera.getPosition(tickDelta);
        this.setPos(pos.x, pos.y, pos.z);
        if (thirdPerson) {
            if (inverseView) {
                this.setRotation(this.yaw + 180.0F, -this.pitch);
            }

            this.moveBy(-this.clipToSpace(4.0), 0.0, 0.0);
        } else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
            Direction direction = ((LivingEntity) focusedEntity).getSleepingDirection();
            this.setRotation(direction != null ? direction.asRotation() - 180.0F : 0.0F, 0.0F);
            this.moveBy(0.0, 0.3, 0.0);
        }


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
