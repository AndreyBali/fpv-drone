package net.andreyabli.fpvdrone.util;

import net.andreyabli.fpvdrone.Main;
import net.minecraft.client.Mouse;
import net.andreyabli.fpvdrone.config.ModConfig;
import org.lwjgl.glfw.GLFW;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControllerManager {

    public static void init(ModConfig config) {
        ControllerManager.config = config;
    }

    private static ModConfig config;
    private static List<String> controllers = new ArrayList<>();

    public static List<String> getControllers() {
        updateControllers();
        return controllers;
    }

    public static float throttle = 0;
    public static float roll = 0;
    public static float pitch = 0;
    public static float yaw = 0;

    private static long nextControllerCheck;
    private static HashMap<String, Integer> controllerIds = new HashMap<>();

    public static void updateControllers() {
        controllers.clear();
        controllers.add("keyboard");
        for (int i = 0; i < 16; i++) {
            if (GLFW.glfwJoystickPresent(i)) {
                String jName = GLFW.glfwGetJoystickName(i);
                controllerIds.put(jName, i);
                controllers.add(jName);
            }
        }
        if(!controllers.contains(config.controls.device)) config.controls.device = controllers.get(0);
    }

    public static void updateControllerAxis() {
        if (System.currentTimeMillis() > nextControllerCheck) {
            updateControllers();
            nextControllerCheck = System.currentTimeMillis() + 500;
        }
        Integer jId = controllerIds.get(config.controls.device);
        if(jId == null || !GLFW.glfwJoystickPresent(jId)) {
            updateControllers();
            jId = controllerIds.get(config.controls.device);
        }
        if (config.controls.device.equals("keyboard")) {
            handleKeyboardInput();
            return;
        }

        FloatBuffer axes = GLFW.glfwGetJoystickAxes(jId);
        int len = axes.remaining();

        throttle = axes.get(config.controls.controller.throttle > len ? len-1 : config.controls.controller.throttle) * (config.controls.controller.invertThrottle ? -1 : 1);
        roll =     axes.get(config.controls.controller.roll     > len ? len-1 : config.controls.controller.roll)     * (config.controls.controller.invertRoll ? -1 : 1);
        pitch =    axes.get(config.controls.controller.pitch    > len ? len-1 : config.controls.controller.pitch)    * (config.controls.controller.invertPitch ? -1 : 1);
        yaw =      axes.get(config.controls.controller.yaw      > len ? len-1 : config.controls.controller.yaw)      * (config.controls.controller.invertYaw ? -1 : 1);
    }


    private static float lastMouseX = 0;
    private static float lastMouseY = 0;

    private static void handleKeyboardInput() {
        if(Main.MC.options.forwardKey.isPressed()) throttle = config.controls.keyboard.forwardKeyThrottle;
        else if(Main.MC.options.backKey.isPressed()) throttle = config.controls.keyboard.backKeyThrottle;
        else throttle = 0;

        if(Main.MC.options.rightKey.isPressed()) yaw = config.controls.keyboard.rightKeyYaw;
        else if(Main.MC.options.leftKey.isPressed()) yaw = config.controls.keyboard.leftKeyYaw;
        else yaw = 0;

        if(Main.MC.currentScreen == null) {
            Mouse mouse = Main.MC.mouse;
            float mouseX = (float) mouse.getX();
            float mouseY = (float) mouse.getY();
            float sensitivity = Main.MC.options.getMouseSensitivity().getValue().floatValue();

            roll =  (mouseX - lastMouseX) * sensitivity * 0.01f;
            pitch = (mouseY - lastMouseY) * sensitivity * 0.01f * (config.controls.invertMousePitchWhileFlying ? -1 : 1);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    public static FloatBuffer getAllAxisValues() {
        updateControllers();
        if (config.controls.device.equals("keyboard")) return null;
        int jId = controllerIds.get(config.controls.device);
        if(!GLFW.glfwJoystickPresent(jId)) return null;
        return GLFW.glfwGetJoystickAxes(jId).duplicate();
    }

    public static String thisOrDefault(String controllerName) {
        updateControllers();
        if(controllers.contains(controllerName)){
            return controllerName;
        }
        return controllers.get(0);
    }
}
