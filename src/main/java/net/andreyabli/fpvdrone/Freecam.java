package net.andreyabli.fpvdrone;

import dev.lazurite.rayon.impl.bullet.collision.space.MinecraftSpace;
import dev.lazurite.rayon.impl.bullet.collision.space.generator.EntityCollisionGenerator;
import me.shedaniel.autoconfig.AutoConfig;
import net.andreyabli.fpvdrone.entity.DroneRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.andreyabli.fpvdrone.entity.FreeCamera;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import software.bernie.geckolib.GeckoLib;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;

public class Freecam implements ClientModInitializer {

    public static final MinecraftClient MC = MinecraftClient.getInstance();
    public static final String MOD_ID = "fpvdrone";
    private static KeyBinding freecamBind;
    private static KeyBinding configGuiBind;
    private static boolean freecamEnabled = false;
    private static boolean playerControlEnabled = false;
    private static boolean disableNextTick = false;
    private static FreeCamera freeCamera;
    private static Perspective rememberedF5 = null;
//    public static long joinTime = 0;

    @Override
    public void onInitializeClient() {
        ModConfig.init();
        ControllerManager.init(ModConfig.INSTANCE);
        freecamBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpvdrone.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.fpvdrone.freecam"));
        configGuiBind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fpvdrone.configGui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.fpvdrone.freecam"));
//        ClientPlayConnectionEvents.INIT.register((handler, client) -> joinTime = System.currentTimeMillis());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (freecamBind.wasPressed()) {
                toggle();
            }
            while (configGuiBind.wasPressed()) {
                MC.setScreen(AutoConfig.getConfigScreen(ModConfig.class, MC.currentScreen).get());
            }
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if(client.world == null) {
                System.out.println("world is null!");
                return;
            }
            EntityCollisionGenerator.step(MinecraftSpace.get(client.world));
            if(freeCamera != null){
                freeCamera.tick();
            }
        });
        GeckoLib.initialize();
        EntityRendererRegistry.register(DRONE, DroneRenderer::new);
    }

    public static final EntityType<FreeCamera> DRONE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "quadcopter"),
            FabricEntityTypeBuilder.createLiving()
                    .entityFactory(FreeCamera::new)
                    .spawnGroup(SpawnGroup.MISC)
//                    .dimensions(EntityDimensions.scalable(0.5f, 0.2f))
                    .defaultAttributes(LivingEntity::createLivingAttributes)
                    .build());

    public static void toggle() {
        if (freecamEnabled) {
            onDisableFreecam();
        } else {
            onEnableFreecam();
        }
        freecamEnabled = !freecamEnabled;
        if (!freecamEnabled) {
            onDisabled();
        }
    }

    private static void onEnableFreecam() {
        onEnable();
        freeCamera = DRONE.create(MC.world);
        System.out.println("freeCamera = " + freeCamera);
        freeCamera.setPosition(MC.player.getPos());
        MC.setCameraEntity(freeCamera);

        if (ModConfig.INSTANCE.notification.notifyFreecam) {
            MC.player.sendMessage(Text.translatable("msg.freecam.enable"), true);
        }
    }

    public static boolean debuggerReleaseControl() {
        GLFW.glfwSetInputMode(MC.getWindow().getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        return true;
    }

    private static void onDisableFreecam() {
        onDisable();

        if (MC.player != null) {
            if (ModConfig.INSTANCE.notification.notifyFreecam) {
                MC.player.sendMessage(Text.translatable("msg.freecam.disable"), true);
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
        freeCamera.remove(Entity.RemovalReason.DISCARDED);
        freeCamera = null;

        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }
    }

    private static void onDisabled() {
        if (rememberedF5 != null) {
            MC.options.setPerspective(rememberedF5);
        }
    }

    public static FreeCamera getFreeCamera() {
        return freeCamera;
    }

    public static boolean disableNextTick() {
        return disableNextTick;
    }

    public static void setDisableNextTick(boolean damage) {
        disableNextTick = damage;
    }

    public static boolean isEnabled() {
        return freecamEnabled;
    }

    public static boolean isPlayerControlEnabled() {
        return playerControlEnabled;
    }
}
