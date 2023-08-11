package net.andreyabli.fpvdrone.mixins;

import net.andreyabli.fpvdrone.Freecam;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.FreeCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    private boolean ready;
    @Shadow
    private BlockView area;
    @Shadow
    private boolean thirdPerson;
    @Shadow
    private float yaw;
    @Shadow
    private float pitch;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

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
        var pos = freeCamera.getPosition(tickDelta);
        this.setPos(pos.x, pos.y, pos.z);
        this.setRotation(0,0);
        if (thirdPerson) {
            if (inverseView) {
//                this.setRotation(this.yaw + 180.0F, -this.pitch);
            }

            this.moveBy(-this.clipToSpace(4.0), 0.0, 0.0);
        } else if (focusedEntity instanceof LivingEntity && ((LivingEntity) focusedEntity).isSleeping()) {
            Direction direction = ((LivingEntity) focusedEntity).getSleepingDirection();
//            this.setRotation(direction != null ? direction.asRotation() - 180.0F : 0.0F, 0.0F);
            this.moveBy(0.0, 0.3, 0.0);
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
