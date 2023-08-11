package net.andreyabli.fpvdrone.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.gui.registry.api.GuiRegistryAccess;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.*;

@Config(name = "fpvdrone")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static ModConfig INSTANCE;


    public static void init() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        GuiRegistry registry = AutoConfig.getGuiRegistry(ModConfig.class);

        registry.registerTypeProvider((String i13n, Field field, Object config, Object defaults, GuiRegistryAccess guiRegistry) -> {
            return Collections.singletonList(
                    ConfigEntryBuilder.create().startSelector(
                            Text.translatable(i13n),
                            Utils.<SelectableDevice>getUnsafely(field, config).getArray(),
                            Utils.<SelectableDevice>getUnsafely(field, config).getSelectedString()
                    ).setSaveConsumer((newValue) -> Utils.<SelectableDevice>getUnsafely(field, config).select(newValue.toString()))
                    .build()
            );
        }, SelectableDevice.class);

        registry.registerTypeProvider((String i13n, Field field, Object config, Object defaults, GuiRegistryAccess guiRegistry) -> {
            return Collections.singletonList(
                    ConfigEntryBuilder.create().startSelector(
                                    Text.translatable(i13n),
                                    Utils.<SelectableDrone>getUnsafely(field, config).getArray(),
                                    Utils.<SelectableDrone>getUnsafely(field, config).getSelectedDrone()
                            ).setSaveConsumer(newValue -> Utils.<SelectableDrone>getUnsafely(field, config).select((DroneConfig.Drone) newValue))
                            .build()
            );
        }, SelectableDrone.class);


        INSTANCE = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    public VisualConfig visual = new VisualConfig();
    public static class VisualConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean showPlayer = true;

        @ConfigEntry.Gui.Tooltip
        public boolean showHand = false;

        @ConfigEntry.Gui.Tooltip
        public boolean showSubmersion = true;
    }

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    public UtilityConfig utility = new UtilityConfig();
    public static class UtilityConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean disableOnDamage = true;

        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean freezePlayer = false;

        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean allowInteract = false;
        public boolean pauseOnMenu = true;
        public boolean flyAsPlayer = true;
    }

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    public NotificationConfig notification = new NotificationConfig();
    public static class NotificationConfig {
        @ConfigEntry.Gui.Tooltip
        public boolean notifyFreecam = true;
    }


    @ConfigEntry.Gui.CollapsibleObject
    public Controls controls = new Controls();
    public static class Controls {
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SelectableDevice device = new SelectableDevice();
        @ConfigEntry.BoundedDiscrete(min = -1, max = 2) public float rate = 0.7f;
        @ConfigEntry.BoundedDiscrete(min = -1, max = 2) public float superRate = 0.9f;
        @ConfigEntry.BoundedDiscrete(min = -1, max = 2)  public float expo = 0.1f;
        public boolean invertMousePitchWhileFlying = true;
        @ConfigEntry.Gui.CollapsibleObject
        public Keyboard keyboard = new Keyboard();


        public static class Keyboard {
            @ConfigEntry.BoundedDiscrete(min = -1, max = 1)
            public float forwardKeyThrottle = 0.7f;
            @ConfigEntry.BoundedDiscrete(min = -1, max = 1)
            public float backKeyThrottle = -0.7f;
            @ConfigEntry.BoundedDiscrete(min = -1, max = 1)
            public float rightKeyYaw = -0.5f;
            @ConfigEntry.BoundedDiscrete(min = -1, max = 1)
            public float leftKeyYaw = 0.5f;
        }

        @ConfigEntry.Gui.CollapsibleObject
        public Controller controller = new Controller();
        public static class Controller {
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int throttleChannel = 0;
            public boolean invertThrottle = false;
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int rollChannel = 1;
            public boolean invertRoll = false;
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int pitchChannel = 2;
            public boolean invertPitch = false;
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int yawChannel = 3;
            public boolean invertYaw = true;
        }
    }



    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    public DroneConfig droneConfig = new DroneConfig();
    public static class DroneConfig {


        public List<Drone> drones = new ArrayList<>(Arrays.asList(
                new Drone("Pixel", 0.225, 0.125, 0.01, 0.05, 0.4, 1, 5),
                new Drone("Voyager", 0.9, 0.2, 1.2, 0.2, 55, 0.8, 20),
                new Drone("Voxel racer one", 0.55, 0.3, 0.5, 0.135, 65, 1, 45)
        ));

        public SelectableDrone drone = new SelectableDrone(drones);

        public static class Drone {
            public Drone(String name, double width, double height, double mass, double dragCoefficient, double thrust, double thrustCurve, int cameraAngle){
                this.name = name;
                this.width = (float) width;
                this.height = (float) height;
                this.mass = (float) mass;
                this.dragCoefficient = (float) dragCoefficient;
                this.thrust = (float) thrust;
                this.thrustCurve = (float) thrustCurve;
                this.cameraAngle = cameraAngle;
            }
            public Drone(){}
            public String name = "New drone";
            public float width = 0.55f;
            public float height = 0.3f;
            public float mass = 0.5f;
            public float dragCoefficient = 0.135f;
            public float thrust = 65;
            public float thrustCurve = 1;
            public int cameraAngle = 45;

            @Override
            public String toString() {
                return name;
            }
        }


    }
//        @ConfigEntry.Gui.CollapsibleObject
//        public ReplayMod replayMod = new ReplayMod();
//
//        public static class ReplayMod {
//            public boolean record = false;
//            @ConfigEntry.BoundedDiscrete(max = 20, min = 1)
//            public int everyXTick = 3;
//        }
}
