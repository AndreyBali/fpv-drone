package net.xolt.freecam.util;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.TempVars;
import com.mojang.authlib.GameProfile;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import dev.lazurite.toolbox.api.math.VectorHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.xolt.freecam.BetaflightHelper;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.Matrix4fHelper;
import net.xolt.freecam.config.ModConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static net.xolt.freecam.Freecam.MC;

public class FreeCamera extends ClientPlayerEntity implements EntityPhysicsElement {

    private static final ClientPlayNetworkHandler NETWORK_HANDLER = new ClientPlayNetworkHandler(MC, MC.currentScreen, MC.getNetworkHandler().getConnection(), MC.getCurrentServerEntry(), new GameProfile(UUID.randomUUID(), "FreeCamera"), MC.getTelemetryManager().createWorldSession(false, null, null)) {
        @Override
        public void sendPacket(Packet<?> packet) {
        }
    };

    public final int CAMERA_ANGLE = 20;

    public FreeCamera(int id) {
        this(id, FreecamPosition.getSwimmingPosition(MC.player));
    }

    public FreeCamera(int id, FreecamPosition position) {
        super(MC, MC.world, NETWORK_HANDLER, MC.player.getStatHandler(), MC.player.getRecipeBook(), false, false);

        setId(id);
        this.setPosition(position.x, position.y, position.z);
        getAbilities().flying = true;
        input = new KeyboardInput(MC.options);
        this.rigidBody.setBuoyancyType(ElementRigidBody.BuoyancyType.NONE);
        this.rigidBody.setDragType(ElementRigidBody.DragType.SIMPLE);
        this.rigidBody.setMass(0);
        this.rigidBody.setDragCoefficient(getDragCoefficient());
    }

    public void applyPosition(FreecamPosition position) {
//        super.setPose(position.pose);
//        refreshPositionAndAngles(position.x, position.y, position.z, position.yaw, position.pitch);
//        renderPitch = getPitch();
//        renderYaw = getYaw();
//        lastRenderPitch = renderPitch; // Prevents camera from rotating upon entering freecam.
//        lastRenderYaw = renderYaw;
    }

    // Mutate the position and rotation based on perspective
    // If checkCollision is true, move as far as possible without colliding
    public void applyPerspective(ModConfig.Perspective perspective, boolean checkCollision) {
        FreecamPosition position = new FreecamPosition(this);

        switch (perspective) {
            case INSIDE:
                // No-op
                break;
            case FIRST_PERSON:
                // Move just in front of the player's eyes
                moveForwardUntilCollision(position, 0.4, checkCollision);
                break;
            case THIRD_PERSON_MIRROR:
                // Invert the rotation and fallthrough into the THIRD_PERSON case
                position.mirrorRotation();
            case THIRD_PERSON:
                // Move back as per F5 mode
                moveForwardUntilCollision(position, -4.0, checkCollision);
                break;
        }
    }

    // Move FreeCamera forward using FreecamPosition.moveForward.
    // If checkCollision is true, stop moving forward before hitting a collision.
    // Return true if successfully able to move.
    private boolean moveForwardUntilCollision(FreecamPosition position, double distance, boolean checkCollision) {
        if (!checkCollision) {
            position.moveForward(distance);
            applyPosition(position);
            return true;
        }
        return moveForwardUntilCollision(position, distance);
    }

    // Same as above, but always check collision.
    private boolean moveForwardUntilCollision(FreecamPosition position, double maxDistance) {
        boolean negative = maxDistance < 0;
        maxDistance = negative ? -1 * maxDistance : maxDistance;
        double increment = 0.1;

        // Move forward by increment until we reach maxDistance or hit a collision
        for (double distance = 0.0; distance < maxDistance; distance += increment) {
            FreecamPosition oldPosition = new FreecamPosition(this);

            position.moveForward(negative ? -1 * increment : increment);
            applyPosition(position);

            if (!wouldPoseNotCollide(getPose())) {
                // Revert to last non-colliding position and return whether we were unable to move at all
                applyPosition(oldPosition);
                return distance > 0;
            }
        }

        return true;
    }

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

    // Prevents slow down from ladders/vines.
    @Override
    public boolean isClimbing() {
        return false;
    }

    // Prevents slow down from water.
    @Override
    public boolean isTouchingWater() {
        return false;
    }

    // Makes night vision apply to FreeCamera when Iris is enabled.
    @Override
    public StatusEffectInstance getStatusEffect(StatusEffect effect) {
        return MC.player.getStatusEffect(effect);
    }

    // Prevents pistons from moving FreeCamera when collision.ignoreAll is enabled.
    @Override
    public PistonBehavior getPistonBehavior() {
        return ModConfig.INSTANCE.collision.ignoreAll ? PistonBehavior.IGNORE : PistonBehavior.NORMAL;
    }

    // Prevents collision with solid entities (shulkers, boats)
    @Override
    public boolean collidesWith(Entity other) {
        return false;
    }

    // Ensures that the FreeCamera is always in the swimming pose.
    @Override
    public void setPose(EntityPose pose) {
        super.setPose(EntityPose.SWIMMING);
    }

    // Prevents slow down due to being in swimming pose. (Fixes being unable to sprint)
    @Override
    public boolean shouldSlowDown() {
        return false;
    }

    // Prevents water submersion sounds from playing.
    @Override
    protected boolean updateWaterSubmersionState() {
        this.isSubmergedInWater = this.isSubmergedIn(FluidTags.WATER);
        return this.isSubmergedInWater;
    }

    // Prevents water submersion sounds from playing.
    @Override
    protected void onSwimmingStart() {}

    @Override
    public void tickMovement() {
//        if (ModConfig.INSTANCE.movement.flightMode.equals(ModConfig.FlightMode.DEFAULT)) {
//            getAbilities().setFlySpeed(0);
//            Motion.doMotion(this, ModConfig.INSTANCE.movement.horizontalSpeed, ModConfig.INSTANCE.movement.verticalSpeed);
//        } else {
//            getAbilities().setFlySpeed((float) ModConfig.INSTANCE.movement.verticalSpeed / 10);
//        }
        super.tickMovement();
        getAbilities().flying = true;
//        setOnGround(false);
    }

    @Override
    public void tick() {

        super.tick();


        int jId = 0;
        if (!GLFW.glfwJoystickPresent(jId)) return;
//        System.out.println("controller: " + GLFW.glfwGetGamepadName(jId));
//        for (int i = 0; i < 4; i++) {
//            System.out.println("axis " + i + " " + GLFW.glfwGetJoystickAxes(jId).get(i));
//        }
//        System.out.println();
//        var roll = GLFW.glfwGetJoystickAxes(jId).get(3); //DOBE
//        var pitch = GLFW.glfwGetJoystickAxes(jId).get(4);
//        var yaw = GLFW.glfwGetJoystickAxes(jId).get(0);
//        var throttle = GLFW.glfwGetJoystickAxes(jId).get(1);
//
        var throttle = GLFW.glfwGetJoystickAxes(jId).get(0);
        var roll = GLFW.glfwGetJoystickAxes(jId).get(1); //FPV
        var pitch = GLFW.glfwGetJoystickAxes(jId).get(2);
        var yaw = GLFW.glfwGetJoystickAxes(jId).get(3);

        throttle += 1;
        yaw *= -1;


//        System.out.println("r = " + roll);
//        System.out.println("p = " + pitch);
//        System.out.println("y = " + yaw);
//        System.out.println("t = " + throttle);



//        var pitch = player.quadz$getJoystickValue(new ResourceLocation(Quadz.MODID, "pitch"));
//        var yaw = -1 * player.quadz$getJoystickValue(new ResourceLocation(Quadz.MODID, "yaw"));
//        var roll = player.quadz$getJoystickValue(new ResourceLocation(Quadz.MODID, "roll"));
//        var throttle = player.quadz$getJoystickValue(new ResourceLocation(Quadz.MODID, "throttle")) + 1.0f;

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
        return 55;
    }
    private float getThrustCurve() {
        return 0.8f;
    }
    private float getMass() {
        return 1.2f;
    }
    private float getDragCoefficient() {
        return 0.2f;
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

    public Quaternionf getRotationF(float tickDelta) {
        Quaternion q = getRotation(tickDelta);
        return new Quaternionf(q.getX(), -q.getY(), q.getZ(), -q.getW());
    }

    public float[] getAngles(Quaternion droneLastRot) {

        Vector3f droneLook = new Vector3f(0, 0, 1);
        Vector3f droneUp = new Vector3f(0, 1, 0);

        droneLook = droneLastRot.mult(droneLook, new Vector3f());
        droneUp = droneLastRot.mult(droneUp, new Vector3f());
        Vector3f droneLeft = droneUp.cross(droneLook);

        Quaternion cameraRot = (new Quaternion()).fromAngleNormalAxis(
                0,
                droneLeft.normalize()
        );
        Vector3f cameraLook = cameraRot.mult(droneLook, new Vector3f());
        Vector3f cameraUp = cameraRot.mult(droneUp, new Vector3f());

        float[] droneAngles = getWorldEulerAngles(droneLook, droneUp);
        float[] cameraAngles = getWorldEulerAngles(cameraLook, cameraUp);

        float[] angles = new float[6];
        angles[0] = droneAngles[0];
        angles[1] = droneAngles[1];
        angles[2] = droneAngles[2];
        angles[3] = cameraAngles[0];
        angles[4] = cameraAngles[1];
        angles[5] = cameraAngles[2];

        return angles;
    }

    public static float[] getWorldEulerAngles(Vector3f forward, Vector3f up) {
        float worldYaw = 0;
        float worldPitch = 0;
        float worldRoll = 0;

        Vector3f axis = new Vector3f(Float.NaN, Float.NaN, Float.NaN);

        if (forward.x != 0 || forward.z != 0) {
            // Calc yaw
            Vector3f forwardProj = new Vector3f(forward.x, 0, forward.z);
            Quaternion worldYawRot = (new Quaternion());
            worldYawRot = lookAt(forwardProj, Vector3f.UNIT_Y);
            worldYaw = toAngleAxis(worldYawRot, axis);
            worldYaw = worldYaw * axis.y * (float) (180 / Math.PI);

            // Calc pitch
            Quaternion antiYawRot = worldYawRot.inverse();
            forwardProj = antiYawRot.mult(forward, new Vector3f());
            Quaternion worldPitchRot = (new Quaternion());
            worldPitchRot = lookAt(forwardProj, Vector3f.UNIT_Y);
            worldPitch = toAngleAxis(worldPitchRot, axis);
            worldPitch = worldPitch * axis.x * (float) (180 / Math.PI);

            // Calc roll
            Quaternion rollessRot = (new Quaternion()).fromAngles(
                    worldPitch *
                            (float) (Math.PI / 180),
                    worldYaw * (float) (Math.PI / 180),
                    0
            );
            Vector3f rollessUp = rollessRot.mult(Vector3f.UNIT_Y, new Vector3f());
            Vector3f crossUps = rollessUp.cross(up).normalize();
            float lookAngle = angleBetween(crossUps, forward) * (float) (180 / Math.PI);
            float flip = FastMath.abs(lookAngle) > 90 ? -1 : 1;
            worldRoll = angleBetween(rollessUp, up) * (float) (180 / Math.PI) * flip;
        } else if (forward.y > 0) {
            // looking straight up
            worldYaw = 0;
            worldPitch = -90;

            Vector3f upProj = new Vector3f(up.x, 0, up.z);
            Quaternion rot = (new Quaternion());
            rot = lookAt(upProj, Vector3f.UNIT_Y);
            worldRoll = toAngleAxis(rot, axis);
            worldRoll = worldRoll * axis.y * (float) (180 / Math.PI);
        } else if (forward.y < 0) {
            // looking straight down
            worldYaw = 0;
            worldPitch = 90;

            Vector3f upProj = new Vector3f(up.x, 0, up.z);
            Quaternion rot = (new Quaternion());
            rot = lookAt(upProj, Vector3f.UNIT_Y);
            worldRoll = toAngleAxis(rot, axis);
            worldRoll = worldRoll * axis.y * (float) (180 / Math.PI);
        }

        // worldYaw is backwards in minecraft
        return new float[]{-worldYaw, worldPitch, worldRoll};
    }

    public static float angleBetween(Vector3f v1, Vector3f v2) {
        float dotProduct = v1.dot(v2);
        float lengthsMultiplied = v1.length() * v2.length();
        float cosAngle = dotProduct / lengthsMultiplied;
        return (float) Math.acos(Math.min(Math.max(cosAngle, -1.0f), 1.0f));
    }


    public static Quaternion lookAt(Vector3f direction, Vector3f up) {
        // Normalize the direction
        direction = direction.normalize();

        // Compute right vector
        Vector3f right = up.cross(direction).normalize();

        // Compute orthonormal up vector
        Vector3f upOrthonormal = direction.cross(right).normalize();

        // Convert these vectors into quaternion
        Quaternion quaternion = new Quaternion();
        quaternion.fromAxes(right, upOrthonormal, direction);

        return quaternion;
    }



    public static float toAngleAxis(Quaternion q, Vector3f axisStore) {
        float sqrLength = q.getX() * q.getX() + q.getY() * q.getY() + q.getZ() * q.getZ();
        float angle;
        if (sqrLength == 0.0f) {
            angle = 0.0f;
            if (axisStore != null) {
                axisStore.x = 1.0f;
                axisStore.y = 0.0f;
                axisStore.z = 0.0f;
            }
        } else {
            angle = (float) (2.0f * Math.acos(q.getW()));
            if (axisStore != null) {
                float invLength = (1.0f / FastMath.sqrt(sqrLength));
                axisStore.x = q.getX() * invLength;
                axisStore.y = q.getY() * invLength;
                axisStore.z = q.getZ() * invLength;
            }
        }

        return angle;
    }
    



    private final EntityRigidBody rigidBody = new EntityRigidBody(this, MinecraftSpace.get(this.cast().getWorld()), this.createShape());
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

    public float getRoll(float tickDelta) {
        return QuaternionHelper.getRoll(Convert.toMinecraft(getPhysicsRotation(new Quaternion(), tickDelta)));
    }

    @Override
    public void updateVelocity(float speed, Vec3d movementInput) {
        super.updateVelocity(speed, movementInput);
    }
}
