package net.andreyabli.fpvdrone.entity;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.mojang.authlib.GameProfile;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.shape.MinecraftShape;
import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import net.andreyabli.fpvdrone.Freecam;
import net.andreyabli.fpvdrone.util.BetaflightHelper;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.andreyabli.fpvdrone.util.Matrix4fHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Collections;
import java.util.UUID;

import static net.andreyabli.fpvdrone.Freecam.MC;
import static net.andreyabli.fpvdrone.Freecam.debuggerReleaseControl;

public class FreeCamera extends LivingEntity implements EntityPhysicsElement, GeoEntity {

    public int CAMERA_ANGLE = ModConfig.INSTANCE.droneConfig.getCurrentDrone().cameraAngle;
    private final double width = ModConfig.INSTANCE.droneConfig.getCurrentDrone().width;
    private final double height = ModConfig.INSTANCE.droneConfig.getCurrentDrone().height;
    private final EntityRigidBody rigidBody = new EntityRigidBody(this, MinecraftSpace.get(this.cast().getWorld()), getShape());
    private boolean ignoreNextInput = false;

//    private int tickCounter = 0;
//    private long createdAt = 0;
//    private final ReplayModHelper replayModHelper = new ReplayModHelper();


    private static final ClientPlayNetworkHandler NETWORK_HANDLER = new ClientPlayNetworkHandler(MC, MC.currentScreen, MC.getNetworkHandler().getConnection(), MC.getCurrentServerEntry(), new GameProfile(UUID.randomUUID(), "FreeCamera"), MC.getTelemetryManager().createWorldSession(false, null, null)) {
        @Override
        public void sendPacket(Packet<?> packet) {
        }
    };

    public FreeCamera(EntityType<FreeCamera> entityType, World world) {
        super(entityType, world);
        var position = MC.player.getPos();
        setId(-480);
        this.setPosition(position.x, position.y, position.z);
        this.setRotation(0, 0);
//        getAbilities().flying = true;
//        input = new KeyboardInput(MC.options);
        this.rigidBody.setBuoyancyType(ElementRigidBody.BuoyancyType.NONE);
        this.rigidBody.setDragType(ElementRigidBody.DragType.SIMPLE);
        this.rigidBody.setMass(getMass());
        this.rigidBody.setDragCoefficient(getDragCoefficient());
//        createdAt = System.currentTimeMillis();
    }

//    public void spawn() {
//        if (clientWorld != null) {
//            clientWorld.addEntity(getId(), this);
//            ignoreNextInput = true;
//        }
//    }
//
//    public void despawn() {
//        if (clientWorld != null && clientWorld.getEntityById(getId()) != null) {
//            clientWorld.removeEntity(getId(), RemovalReason.DISCARDED);
//        }
////        System.out.println(replayModHelper.getJson());
//    }

    @Override
    public void tick() {
        super.tick();
        if (ModConfig.INSTANCE.utility.pauseOnMenu && MinecraftClient.getInstance().currentScreen != null) {
            rigidBody.setMass(0);
            ignoreNextInput = true;
            return;
        }
        rigidBody.setMass(getMass());
        if(MC.player.getAbilities().allowFlying && ModConfig.INSTANCE.utility.flyAsPlayer) {
            MC.player.copyPositionAndRotation(this);
        }
        if(ignoreNextInput) {
            ControllerManager.updateControllerAxis();
            ignoreNextInput = false;
        }

        ControllerManager.updateControllerAxis();
        float throttle = ControllerManager.throttle + 1;
        float roll = ControllerManager.roll;
        float pitch = ControllerManager.pitch;
        float yaw = ControllerManager.yaw;

        var rate = ModConfig.INSTANCE.controls.rate;
        var superRate = ModConfig.INSTANCE.controls.superRate;
        var expo = ModConfig.INSTANCE.controls.expo;

        this.rotate(
                (float) BetaflightHelper.calculateRates(pitch, rate, expo, superRate, 0.05f),
                (float) BetaflightHelper.calculateRates(yaw, rate, expo, superRate, 0.05f),
                (float) BetaflightHelper.calculateRates(roll, rate, expo, superRate, 0.05f)
        );

        // Decrease angular velocity
        if (throttle > 0.1f) {
            var correction = getRigidBody().getAngularVelocity(new com.jme3.math.Vector3f()).multLocal(0.5f * throttle);

            if (Float.isFinite(correction.lengthSquared())) {
                getRigidBody().setAngularVelocity(correction);
            }
        }

        // Get the thrust unit vector
        // TODO make this into it's own class
        var mat = new Matrix4f();
        Matrix4fHelper.fromQuaternion(mat, QuaternionHelper.rotateX(Convert.toMinecraft(getRigidBody().getPhysicsRotation(new Quaternion())), 90));
        var unit = Convert.toBullet(Matrix4fHelper.matrixToVector(mat));

        // Calculate basic thrust
        var thrust = new com.jme3.math.Vector3f().set(unit).multLocal((float) (getThrust() * (Math.pow(throttle, getThrustCurve()))));

        // Calculate thrust from yaw spin
        var yawThrust = new com.jme3.math.Vector3f().set(unit).multLocal(Math.abs(yaw * getThrust() * 0.002f));

        // Add up the net thrust and apply the force
        if (Float.isFinite(thrust.length())) {
            getRigidBody().applyCentralForce(thrust.add(yawThrust).multLocal(-1));
        } else {
            System.out.println("Infinite thrust force!");
        }

//        if (ModConfig.INSTANCE.droneConfig.replayMod.record) {
//            if (tickCounter++ >= ModConfig.INSTANCE.droneConfig.replayMod.everyXTick) {
//                tickCounter = 0;
//                replayModHelper.add(createdAt, CAMERA_ANGLE, getRotation(0), getPosition(0));
//            }
//        }
    }

    @Override
    public Arm getMainArm() {
        return null;
    }

    private float getThrust() {
        return ModConfig.INSTANCE.droneConfig.getCurrentDrone().thrust;
    }

    private float getThrustCurve() {
        return ModConfig.INSTANCE.droneConfig.getCurrentDrone().thrustCurve;
    }

    private float getMass() {
        return ModConfig.INSTANCE.droneConfig.getCurrentDrone().mass;
    }

    private float getDragCoefficient() {
        return ModConfig.INSTANCE.droneConfig.getCurrentDrone().dragCoefficient;
    }
    private MinecraftShape getShape() {
        return MinecraftShape.convex(new Box(0, 0, 0, width, height, width));
    }

    public void rotate(float x, float y, float z) {
        var rot = new Quaternionf(0, 0, 0, 1);
        QuaternionHelper.rotateX(rot, x);
        QuaternionHelper.rotateY(rot, y);
        QuaternionHelper.rotateZ(rot, z);

        var trans = getRigidBody().getTransform(new Transform());
        trans.getRotation().set(trans.getRotation().mult(Convert.toBullet(rot)));
        getRigidBody().setPhysicsTransform(trans);
    }

    public Quaternion getRotation(float tickDelta) {
        return getPhysicsRotation(new Quaternion(), tickDelta);
    }

    public Vector3f getPosition(float tickDelta) {
        return getPhysicsLocation(new Vector3f(), tickDelta);
    }

    @Override
    public void updateVelocity(float speed, Vec3d movementInput) {}
    @Override
    public @NotNull EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }

    // Prevents fall damage sound when FreeCamera touches ground with noClip disabled.
    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.singleton(ItemStack.EMPTY);
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {

    }

    // Ensures that the FreeCamera is always in the swimming pose.
    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Flying", 5, this::flyAnimController));
    }

    private <T extends GeoAnimatable> PlayState flyAnimController(AnimationState<T> tAnimationState) {
        return PlayState.STOP;
//        if(ControllerManager.throttle > -0.7f) {
//            tAnimationState.getController().setAnimation(RawAnimation.begin().then("animation.fpvdrone.fly", Animation.LoopType.LOOP));
//        }
//        return PlayState.STOP;
    }
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
