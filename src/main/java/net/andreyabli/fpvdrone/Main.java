package net.andreyabli.fpvdrone;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.andreyabli.fpvdrone.util.DroneEntity;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;

public class Main implements ClientModInitializer {

    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static final String MOD_ID = "fpvdrone";
    private static KeyBinding droneBind;
    private static KeyBinding configGuiBind;
    private static boolean droneEnabled = false;
    private static boolean playerControlEnabled = false;
    private static boolean disableNextTick = false;
    private static DroneEntity droneEntity;
    private static Perspective rememberedF5 = null;
//    public static long joinTime = 0;

    @Override
    public void onInitializeClient() {
        ModConfig.init();
        ControllerManager.init(ModConfig.INSTANCE);
        droneBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpvdrone.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.fpvdrone.freecam"));
        configGuiBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpvdrone.configGui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.fpvdrone.freecam"));
//        ClientPlayConnectionEvents.INIT.register((handler, client) -> joinTime = System.currentTimeMillis());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (droneBind.wasPressed()) {
                toggle();
            }
            while (configGuiBind.wasPressed()) {
                MC.setScreen(AutoConfig.getConfigScreen(ModConfig.class, MC.currentScreen).get());
            }
        });
    }

    public static void toggle() {
        if (droneEnabled) {
            onDisableDrone();
        } else {
            onEnableDrone();
        }
        droneEnabled = !droneEnabled;
        if (!droneEnabled) {
            onDisabled();
        }
    }

    private static void onEnableDrone() {
        onEnable();
        droneEntity = new DroneEntity();
        droneEntity.spawn();
        MC.setCameraEntity(droneEntity);

        if (ModConfig.INSTANCE.notification.notifyFreecam) {
            MC.player.sendMessage(Text.translatable("msg.fpvdrone.enable"), true);
        }
    }

    public static boolean debuggerReleaseControl() {
        GLFW.glfwSetInputMode(MC.getWindow().getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        return true;
    }

    private static void onDisableDrone() {
        onDisable();

        if (MC.player != null) {
            if (ModConfig.INSTANCE.notification.notifyFreecam) {
                MC.player.sendMessage(Text.translatable("msg.fpvdrone.disable"), true);
            }
        }
    }

    private static void onEnable() {
        MC.chunkCullingEnabled = false;
        MC.gameRenderer.setRenderHand(false);

        rememberedF5 = MC.options.getPerspective();
        if (MC.gameRenderer.getCamera().isThirdPerson()) {
            MC.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    private static void onDisable() {
        MC.chunkCullingEnabled = true;
        MC.gameRenderer.setRenderHand(true);
        MC.setCameraEntity(MC.player);
        playerControlEnabled = false;
        droneEntity.despawn();
        droneEntity.input = new Input();
        droneEntity = null;

        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }
    }

    private static void onDisabled() {
        if (rememberedF5 != null) {
            MC.options.setPerspective(rememberedF5);
        }
    }

    public static DroneEntity getDrone() {
        return droneEntity;
    }

    public static boolean disableNextTick() {
        return disableNextTick;
    }

    public static void setDisableNextTick(boolean damage) {
        disableNextTick = damage;
    }

    public static boolean isEnabled() {
        return droneEnabled;
    }

    public static boolean isPlayerControlEnabled() {
        return playerControlEnabled;
    }
}
