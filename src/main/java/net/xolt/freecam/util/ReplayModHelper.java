//package net.xolt.freecam.util;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.jme3.math.FastMath;
//import com.jme3.math.Quaternion;
//import dev.lazurite.toolbox.api.math.QuaternionHelper;
//import net.minecraft.util.math.Vec3d;
//import org.joml.Quaternionf;
//import org.joml.Vector3f;
//
//import static net.xolt.freecam.Freecam.joinTime;
//
//public class ReplayModHelper {
//    private JsonArray keyframesWorld = new JsonArray();
//    private JsonArray keyframesCamera = new JsonArray();
//    private JsonArray segmentsArray = new JsonArray();
//
//
//    public ReplayModHelper() {
//
//    }
//
//    public String getJson() {
//        if(segmentsArray.isEmpty()) return "{}";
//
//        segmentsArray.remove(0);
//
//        JsonObject worldObj = new JsonObject();
//        worldObj.add("keyframes", keyframesWorld);
//        worldObj.add("segments", segmentsArray);
//
//        //interpolators
//        JsonArray interpolatorsWorldArray = new JsonArray();
//        JsonObject interpolatorsWorld = new JsonObject();
//        interpolatorsWorld.addProperty("type", "linear");
//        JsonArray interpolatorsWorldProperties = new JsonArray();
//        interpolatorsWorldProperties.add("timestamp");
//        interpolatorsWorld.add("properties", interpolatorsWorldProperties);
//        interpolatorsWorldArray.add(interpolatorsWorld);
//        worldObj.add("interpolators", interpolatorsWorldArray);
//
//
//        JsonObject cameraObj = new JsonObject();
//        cameraObj.add("keyframes", keyframesCamera);
//        cameraObj.add("segments", segmentsArray);
//
//        //interpolators
//        JsonArray interpolatorsCameraArray = new JsonArray();
//        JsonObject interpolatorsCamera = new JsonObject();
//        JsonObject interpolatorsCameraType = new JsonObject();
//        interpolatorsCameraType.addProperty("type", "catmull-rom-spline");
//        interpolatorsCameraType.addProperty("alpha", 0.5);
//        interpolatorsCamera.add("type", interpolatorsCameraType);
//        JsonArray interpolatorsCameraProperties = new JsonArray();
//        interpolatorsCameraProperties.add("camera:rotation");
//        interpolatorsCameraProperties.add("camera:position");
//        interpolatorsCamera.add("properties", interpolatorsCameraProperties);
//        interpolatorsCameraArray.add(interpolatorsCamera);
//        cameraObj.add("interpolators", interpolatorsCameraArray);
//
//        JsonArray replayArray = new JsonArray();
//        replayArray.add(worldObj);
//        replayArray.add(cameraObj);
//
//        JsonObject replayObj = new JsonObject();
//        replayObj.add("test", replayArray);
//
//        return replayObj.toString();
//    }
//
//
//    public void add(long createdAt, int cameraAngle, Quaternion rot, Vec3d pos) {
//        long currentTime = System.currentTimeMillis();
//        long timestamp = currentTime - createdAt;
//        if( timestamp > 600_000) return; //10 minutes is max in replay mod
//
//        segmentsArray.add(0);
//
//
//        JsonObject worldKeyFrame = new JsonObject();
//        JsonObject worldKeyFrameProperties = new JsonObject();
//        worldKeyFrameProperties.addProperty("timestamp", currentTime - joinTime);
//        worldKeyFrame.addProperty("time", timestamp);
//        worldKeyFrame.add("properties", worldKeyFrameProperties);
//        keyframesWorld.add(worldKeyFrame);
//
//
//        JsonObject cameraKeyFrame = new JsonObject();
//        cameraKeyFrame.addProperty("time", timestamp);
//        JsonObject cameraKeyFrameProperties = new JsonObject();
//
//
//
//        Vector3f vectorRot = QuaternionHelper.rotateX(new Quaternionf(rot.getX(), -rot.getY(), rot.getZ(), -rot.getW()), cameraAngle).getEulerAnglesXYZ(new Vector3f());
//        JsonArray cameraRotation = new JsonArray();
//        cameraRotation.add(vectorRot.z * FastMath.RAD_TO_DEG);
//        cameraRotation.add(vectorRot.y * FastMath.RAD_TO_DEG);
//        cameraRotation.add(vectorRot.x * FastMath.RAD_TO_DEG);
//        cameraKeyFrameProperties.add("camera:rotation", cameraRotation);
//
//        JsonArray cameraPosition = new JsonArray();
//        cameraPosition.add(pos.x);
//        cameraPosition.add(pos.y);
//        cameraPosition.add(pos.z);
//        cameraKeyFrameProperties.add("camera:position", cameraPosition);
//
//        cameraKeyFrame.add("properties", cameraKeyFrameProperties);
//        keyframesCamera.add(cameraKeyFrame);
//    }
//}
