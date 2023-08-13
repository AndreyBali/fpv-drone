package net.andreyabli.fpvdrone.util;

import net.andreyabli.fpvdrone.Freecam;
import net.minecraft.client.Mouse;
import net.andreyabli.fpvdrone.config.ModConfig;
import org.lwjgl.glfw.GLFW;

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
    }

    public static void updateControllerAxis() {
        if (System.currentTimeMillis() > nextControllerCheck) {
            updateControllers();
            nextControllerCheck = System.currentTimeMillis() + 500;
        }
        if (config.controls.device.equals("keyboard")) {
            handleKeyboardInput();
            return;
        }

        int jId = controllerIds.get(config.controls.device);
        if(!GLFW.glfwJoystickPresent(jId)){
            updateControllers();
            jId = controllerIds.get(config.controls.device);
        }
        throttle = GLFW.glfwGetJoystickAxes(jId).get(config.controls.controller.throttleChannel) * (config.controls.controller.invertThrottle ? -1 : 1);
        roll = GLFW.glfwGetJoystickAxes(jId).get(config.controls.controller.rollChannel) * (config.controls.controller.invertRoll ? -1 : 1);
        pitch = GLFW.glfwGetJoystickAxes(jId).get(config.controls.controller.pitchChannel) * (config.controls.controller.invertPitch ? -1 : 1);
        yaw = GLFW.glfwGetJoystickAxes(jId).get(config.controls.controller.yawChannel) * (config.controls.controller.invertYaw ? -1 : 1);
    }


    private static float lastMouseX = 0;
    private static float lastMouseY = 0;

    private static void handleKeyboardInput() {
        if(Freecam.MC.options.forwardKey.isPressed()) throttle = config.controls.keyboard.forwardKeyThrottle;
        else if(Freecam.MC.options.backKey.isPressed()) throttle = config.controls.keyboard.backKeyThrottle;
        else throttle = 0;

        if(Freecam.MC.options.rightKey.isPressed()) yaw = config.controls.keyboard.rightKeyYaw;
        else if(Freecam.MC.options.leftKey.isPressed()) yaw = config.controls.keyboard.leftKeyYaw;
        else yaw = 0;

        if(Freecam.MC.currentScreen == null) {
            Mouse mouse = Freecam.MC.mouse;
            float mouseX = (float) mouse.getX();
            float mouseY = (float) mouse.getY();
            float sensitivity = Freecam.MC.options.getMouseSensitivity().getValue().floatValue();

            roll =  (mouseX - lastMouseX) * sensitivity * 0.01f;
            pitch = (mouseY - lastMouseY) * sensitivity * 0.01f * (config.controls.invertMousePitchWhileFlying ? -1 : 1);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    public static String thisOrDefault(String controllerName) {
        updateControllers();
        if(controllers.contains(controllerName)){
            return controllerName;
        }
        return controllers.get(0);
    }
}
