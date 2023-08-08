package net.xolt.freecam.util;

import net.xolt.freecam.Freecam;
import net.xolt.freecam.config.ModConfig;
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
        if (!controllers.contains(config.droneConfig.device)) {
            config.droneConfig.device = controllers.get(0);
        }
        if (config.droneConfig.device.equals("keyboard")) {
            //TODO keyboard handling
            throttle = 0;
            roll = 0;
            pitch = 0;
            yaw = 0;
            return;
        }

        int jId = controllerIds.get(config.droneConfig.device);
        throttle = GLFW.glfwGetJoystickAxes(jId).get(0);
        roll = GLFW.glfwGetJoystickAxes(jId).get(1);
        pitch = GLFW.glfwGetJoystickAxes(jId).get(2);
        yaw = GLFW.glfwGetJoystickAxes(jId).get(3);
    }
}
