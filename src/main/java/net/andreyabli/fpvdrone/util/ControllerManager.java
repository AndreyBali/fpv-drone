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
        if (config.droneConfig.device.getSelectedString().equals("keyboard")) {
            handleKeyboardInput();
            return;
        }

        int jId = controllerIds.get(config.droneConfig.device.getSelectedString());
        throttle = GLFW.glfwGetJoystickAxes(jId).get(0);
        roll = GLFW.glfwGetJoystickAxes(jId).get(1);
        pitch = GLFW.glfwGetJoystickAxes(jId).get(2);
        yaw = GLFW.glfwGetJoystickAxes(jId).get(3);
    }


    private static float lastMouseX = 0;
    private static float lastMouseY = 0;

    private static void handleKeyboardInput() {
        if(Freecam.MC.options.forwardKey.isPressed()) throttle = 0.7f;
        else if(Freecam.MC.options.backKey.isPressed()) throttle = -0.7f;
        else throttle = 0;

        if(Freecam.MC.options.rightKey.isPressed()) yaw = 0.5f;
        else if(Freecam.MC.options.leftKey.isPressed()) yaw = -0.5f;
        else yaw = 0;

        if(Freecam.MC.currentScreen == null) {
            Mouse mouse = Freecam.MC.mouse;
            float mouseX = (float) mouse.getX();
            float mouseY = (float) mouse.getY();
            float sensitivity = Freecam.MC.options.getMouseSensitivity().getValue().floatValue();

            roll =  (mouseX - lastMouseX) * sensitivity * 0.01f;
            pitch = (mouseY - lastMouseY) * sensitivity * 0.01f * (config.droneConfig.invertMousePitchWhileFlying ? -1 : 1);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }
}
