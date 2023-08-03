package net.xolt.freecam.util;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.mojang.authlib.GameProfile;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import dev.lazurite.toolbox.api.math.VectorHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
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
        getAbilities().flying = true;
        input = new KeyboardInput(MC.options);
        this.rigidBody.setBuoyancyType(ElementRigidBody.BuoyancyType.NONE);
        this.rigidBody.setDragType(ElementRigidBody.DragType.SIMPLE);
        this.rigidBody.setMass(1f);

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
        System.out.println("controller: " + GLFW.glfwGetGamepadName(jId));
//        for (int i = 0; i < 4; i++) {
//            System.out.println("axis " + i + " " + GLFW.glfwGetJoystickAxes(jId).get(i));
//        }
        System.out.println();
//        var roll = GLFW.glfwGetJoystickAxes(jId).get(3); //DOBE
//        var pitch = GLFW.glfwGetJoystickAxes(jId).get(4);
//        var yaw = GLFW.glfwGetJoystickAxes(jId).get(0);
//        var throttle = GLFW.glfwGetJoystickAxes(jId).get(1);

        var roll = GLFW.glfwGetJoystickAxes(jId).get(0); //FPV
        var pitch = GLFW.glfwGetJoystickAxes(jId).get(3);
        var yaw = GLFW.glfwGetJoystickAxes(jId).get(2);
        var throttle = GLFW.glfwGetJoystickAxes(jId).get(1);

        throttle += 1;

        System.out.println("r = " + roll);
        System.out.println("p = " + pitch);
        System.out.println("y = " + yaw);
        System.out.println("t = " + throttle);



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
        return 65;
    }

    private float getThrustCurve() {
        return 1f;
    }


//    @Override
//    public void tick() {
//        super.tick();
//        int jId = 0;
//        if (!GLFW.glfwJoystickPresent(jId)) return;
//        System.out.println("controller: " + GLFW.glfwGetGamepadName(jId));
////        for (int i = 0; i < 4; i++) {
////            System.out.println("axis " + i + " " + GLFW.glfwGetJoystickAxes(jId).get(i));
////        }
//        System.out.println();
//        rollInput = GLFW.glfwGetJoystickAxes(jId).get(3);
//        pitchInput = GLFW.glfwGetJoystickAxes(jId).get(4);
//        yawInput = GLFW.glfwGetJoystickAxes(jId).get(0);
//        throttleInput = GLFW.glfwGetJoystickAxes(jId).get(1);
//        throttleInput += 1;
////        yawInput *= -1;
//
//        System.out.println("r = " + rollInput);
//        System.out.println("p = " + pitchInput);
//        System.out.println("y = " + yawInput);
//        System.out.println("t = " + throttleInput);
//
//        direction.x += pitchInput * multiplier;
//        direction.y += rollInput * multiplier;
//        direction.z += yawInput * multiplier;
//
//
//
//
////        Vec3d currentRotation = MC.player.getRotationVecClient();
////        MC.player.setRotationYawHead((float) ((currentRotation.y + 90) % 360));
////        MC.roll
////        MC.player.applyRotation(BlockRotation.)
//        setDirection();
//    }

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
        var rot = getRotation(tickDelta);
        return new Quaternionf(rot.getX(), rot.getY(), rot.getZ(), rot.getW());
    }


    private final EntityRigidBody rigidBody = new EntityRigidBody(this);
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

    public void updateFastPosition(float tickDelta) {
        this.setPosition(getPosition(tickDelta));
    }
}
