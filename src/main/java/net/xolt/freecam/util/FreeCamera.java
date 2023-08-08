package net.xolt.freecam.util;

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
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.xolt.freecam.config.ModConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.UUID;

import static net.xolt.freecam.Freecam.MC;

public class FreeCamera extends ClientPlayerEntity implements EntityPhysicsElement {

    private static final ClientPlayNetworkHandler NETWORK_HANDLER = new ClientPlayNetworkHandler(MC, MC.currentScreen, MC.getNetworkHandler().getConnection(), MC.getCurrentServerEntry(), new GameProfile(UUID.randomUUID(), "FreeCamera"), MC.getTelemetryManager().createWorldSession(false, null, null)) {
        @Override
        public void sendPacket(Packet<?> packet) {
        }
    };

    public FreeCamera(int id) {
        this(id, FreecamPosition.getSwimmingPosition(MC.player));
    }

    public FreeCamera(int id, FreecamPosition position) {
        super(MC, MC.world, NETWORK_HANDLER, MC.player.getStatHandler(), MC.player.getRecipeBook(), false, false);

        setId(id);
        this.setPosition(position.x, position.y, position.z);
        this.setRotation(0,0);
        getAbilities().flying = true;
        input = new KeyboardInput(MC.options);
        this.rigidBody.setBuoyancyType(ElementRigidBody.BuoyancyType.NONE);
        this.rigidBody.setDragType(ElementRigidBody.DragType.SIMPLE);
        this.rigidBody.setMass(getMass());
        this.rigidBody.setDragCoefficient(getDragCoefficient());
    }


    // Mutate the position and rotation based on perspective
    // If checkCollision is true, move as far as possible without colliding
//    public void applyPerspective(ModConfig.Perspective perspective, boolean checkCollision) {}

    public void spawn() {
        if (clientWorld != null) {
            clientWorld.addEntity(getId(), this);
        }
    }

    public void despawn() {
        if (clientWorld != null && clientWorld.getEntityById(getId()) != null) {
            clientWorld.removeEntity(getId(), RemovalReason.DISCARDED);
        }
    }

    // Prevents fall damage sound when FreeCamera touches ground with noClip disabled.
    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    // Needed for hand swings to be shown in freecam since the player is replaced by FreeCamera in HeldItemRenderer.renderItem()
    @Override
    public float getHandSwingProgress(float tickDelta) {
        return MC.player.getHandSwingProgress(tickDelta);
    }

    // Needed for item use animations to be shown in freecam since the player is replaced by FreeCamera in HeldItemRenderer.renderItem()
    @Override
    public int getItemUseTimeLeft() {
        return MC.player.getItemUseTimeLeft();
    }

    // Also needed for item use animations to be shown in freecam.
    @Override
    public boolean isUsingItem() {
        return MC.player.isUsingItem();
    }

    // Makes night vision apply to FreeCamera when Iris is enabled.
    @Override
    public StatusEffectInstance getStatusEffect(StatusEffect effect) {
        return MC.player.getStatusEffect(effect);
    }

    // Prevents pistons from moving FreeCamera when collision.ignoreAll is enabled.
    @Override
    public PistonBehavior getPistonBehavior() {
        return /*ModConfig.INSTANCE.collision.ignoreAll ? PistonBehavior.IGNORE :*/ PistonBehavior.NORMAL;
    }

    // Ensures that the FreeCamera is always in the swimming pose.
    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

    @Override
    public void tick() {
        super.tick();

        ControllerManager.updateControllerAxis();
        float throttle = ControllerManager.throttle + 1;
        float roll = ControllerManager.roll;
        float pitch = ControllerManager.pitch;
        float yaw = ControllerManager.yaw * -1;

        var rate = 0.7f;
        var superRate = 0.8f;
        var expo = 0.0f;

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
    }


    private float getThrust() {
        return ModConfig.INSTANCE.droneConfig.drone.thrust;
    }
    private float getThrustCurve() {
        return ModConfig.INSTANCE.droneConfig.drone.thrustCurve;
    }
    private float getMass() {
        return ModConfig.INSTANCE.droneConfig.drone.mass;
    }
    private float getDragCoefficient() {
        return ModConfig.INSTANCE.droneConfig.drone.dragCoefficient;
    }
    public int CAMERA_ANGLE = ModConfig.INSTANCE.droneConfig.drone.cameraAngle;
    private final double width = ModConfig.INSTANCE.droneConfig.drone.width;
    private final double height = ModConfig.INSTANCE.droneConfig.drone.height;

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

    private final EntityRigidBody rigidBody = new EntityRigidBody(this, MinecraftSpace.get(this.cast().getWorld()), getShape());

   private MinecraftShape getShape(){
       return MinecraftShape.convex(new Box(0,0,0,  width,height,width));
   }
    @Override
    public @Nullable EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }

    public Vec3d getPosition(float tickDelta) {
        return VectorHelper.toVec3(Convert.toMinecraft(getPhysicsLocation(new Vector3f(), tickDelta)));
    }

    @Override
    public float getPitch(float tickDelta) {
        return QuaternionHelper.getPitch(Convert.toMinecraft(getPhysicsRotation(new Quaternion(), tickDelta)));
    }

    @Override
    public float getYaw(float tickDelta) {
        return QuaternionHelper.getYaw(Convert.toMinecraft(getPhysicsRotation(new Quaternion(), tickDelta)));
    }

    @Override
    public void updateVelocity(float speed, Vec3d movementInput) {
        super.updateVelocity(speed, movementInput);
    }
}
