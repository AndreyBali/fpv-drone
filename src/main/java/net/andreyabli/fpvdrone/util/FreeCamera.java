package net.andreyabli.fpvdrone.util;

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
import dev.lazurite.toolbox.api.math.VectorHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.andreyabli.fpvdrone.config.ModConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.UUID;

import static net.andreyabli.fpvdrone.Freecam.MC;

public class FreeCamera extends ClientPlayerEntity implements EntityPhysicsElement {

    public int CAMERA_ANGLE = ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().cameraAngle;
    private final double width = ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().width;
    private final double height = ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().height;
    private final EntityRigidBody rigidBody = new EntityRigidBody(this, MinecraftSpace.get(this.cast().getWorld()), getShape());

//    private int tickCounter = 0;
//    private long createdAt = 0;
//    private final ReplayModHelper replayModHelper = new ReplayModHelper();


    private static final ClientPlayNetworkHandler NETWORK_HANDLER = new ClientPlayNetworkHandler(MC, MC.currentScreen, MC.getNetworkHandler().getConnection(), MC.getCurrentServerEntry(), new GameProfile(UUID.randomUUID(), "FreeCamera"), MC.getTelemetryManager().createWorldSession(false, null, null)) {
        @Override
        public void sendPacket(Packet<?> packet) {
        }
    };
    public FreeCamera() {
        super(MC, MC.world, NETWORK_HANDLER, MC.player.getStatHandler(), MC.player.getRecipeBook(), false, false);
        var position = MC.player.getPos();
        setId(-480);
        this.setPosition(position.x, position.y, position.z);
        this.setRotation(0, 0);
        getAbilities().flying = true;
        input = new KeyboardInput(MC.options);
        this.rigidBody.setBuoyancyType(ElementRigidBody.BuoyancyType.NONE);
        this.rigidBody.setDragType(ElementRigidBody.DragType.SIMPLE);
        this.rigidBody.setMass(getMass());
        this.rigidBody.setDragCoefficient(getDragCoefficient());
//        createdAt = System.currentTimeMillis();
    }

    public void spawn() {
        if (clientWorld != null) {
            clientWorld.addEntity(getId(), this);
            ControllerManager.updateControllerAxis();
        }
    }

    public void despawn() {
        if (clientWorld != null && clientWorld.getEntityById(getId()) != null) {
            clientWorld.removeEntity(getId(), RemovalReason.DISCARDED);
        }
//        System.out.println(replayModHelper.getJson());
    }

    @Override
    public void tick() {
        super.tick();
        if (ModConfig.INSTANCE.utility.pauseOnMenu && MinecraftClient.getInstance().currentScreen != null) {
            rigidBody.setMass(0);
            return;
        }
        rigidBody.setMass(getMass());
        if(MinecraftClient.getInstance().player.getAbilities().allowFlying && ModConfig.INSTANCE.utility.flyAsPlayer) {
            MC.player.copyPositionAndRotation(this);
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
            FreeCamera.LOGGER.warn("Infinite thrust force!");
        }

//        if (ModConfig.INSTANCE.droneConfig.replayMod.record) {
//            if (tickCounter++ >= ModConfig.INSTANCE.droneConfig.replayMod.everyXTick) {
//                tickCounter = 0;
//                replayModHelper.add(createdAt, CAMERA_ANGLE, getRotation(0), getPosition(0));
//            }
//        }
    }

    private float getThrust() {
        return ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().thrust;
    }

    private float getThrustCurve() {
        return ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().thrustCurve;
    }

    private float getMass() {
        return ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().mass;
    }

    private float getDragCoefficient() {
        return ModConfig.INSTANCE.droneConfig.drone.getSelectedDrone().dragCoefficient;
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

    public Vec3d getPosition(float tickDelta) {
        return VectorHelper.toVec3(Convert.toMinecraft(getPhysicsLocation(new Vector3f(), tickDelta)));
    }

    @Override
    public void updateVelocity(float speed, Vec3d movementInput) {}
    @Override
    public @Nullable EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }

    // Prevents fall damage sound when FreeCamera touches ground with noClip disabled.
    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }
    // Ensures that the FreeCamera is always in the swimming pose.
    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

}
