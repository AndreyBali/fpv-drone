package net.andreyabli.fpvdrone.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.minecraft.text.Text;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

@Config(name = "fpvdrone")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static ModConfig INSTANCE;


    public static void init() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        GuiRegistry registry = AutoConfig.getGuiRegistry(ModConfig.class);

        registry.registerAnnotationProvider((i13n, field, config, defaults, guiProvider) -> {
            return Collections.singletonList(
                    ConfigEntryBuilder.create().startSelector(
                                    Text.translatable(i13n),
                                    ControllerManager.getControllers().toArray(),
                                    ControllerManager.thisOrDefault(Utils.getUnsafely(field, config).toString())
                            ).setSaveConsumer((newValue) -> Utils.setUnsafely(field, config, newValue))
                            .build()
            );
        }, SelectableDevice.class);

        registry.registerAnnotationProvider((i13n, field, config, defaults, guiProvider) -> {
            return Collections.singletonList(
                    ConfigEntryBuilder.create().startSelector(
                                    Text.translatable(i13n),
                                    INSTANCE.droneConfig.getDronesNames(),
                                    thisDroneByNameOrDefault(Utils.getUnsafely(field, config)).name
                            ).setSaveConsumer((newValue) -> Utils.setUnsafely(field, config, newValue))
                            .build()
            );
        }, SelectableDrone.class);


        INSTANCE = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    private static DroneConfig.Drone thisDroneByNameOrDefault(String droneName) {
        for(DroneConfig.Drone drone : INSTANCE.droneConfig.drones) {
            if(Objects.equals(drone.name, droneName)) return drone;
        }
        if(INSTANCE.droneConfig.drones.isEmpty()) {
            INSTANCE.droneConfig.drones = new ArrayList<>(Arrays.asList(
                    new DroneConfig.Drone("Pixel", 0.225, 0.125, 0.01, 0.05, 0.4, 1, 5),
                    new DroneConfig.Drone("Voyager", 0.9, 0.2, 1.2, 0.2, 55, 0.8, 20),
                    new DroneConfig.Drone("Voxel racer one", 0.55, 0.3, 0.5, 0.135, 65, 1, 45)
            ));
        }
        return INSTANCE.droneConfig.drones.get(0);
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
        @SelectableDevice
        public String device = "keyboard";
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
            public int throttle = 0;
            public boolean invertThrottle = false;
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int roll = 1;
            public boolean invertRoll = false;
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int pitch = 2;
            public boolean invertPitch = false;
            @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
            public int yaw = 3;
            public boolean invertYaw = true;
        }
    }



    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.CollapsibleObject
    public DroneConfig droneConfig = new DroneConfig();
    public static class DroneConfig {
        public Drone getCurrentDrone(){
            return ModConfig.thisDroneByNameOrDefault(drone);
        }
        public Object[] getDronesNames(){
            if(drones.isEmpty()) {
                drones = new ArrayList<>(Arrays.asList(
                        new DroneConfig.Drone("Pixel", 0.225, 0.125, 0.01, 0.05, 0.4, 1, 5),
                        new DroneConfig.Drone("Voyager", 0.9, 0.2, 1.2, 0.2, 55, 0.8, 20),
                        new DroneConfig.Drone("Voxel racer one", 0.55, 0.3, 0.5, 0.135, 65, 1, 45)
                ));
            }
            List<String> str = new ArrayList<>();
            for(DroneConfig.Drone drone : INSTANCE.droneConfig.drones) {
                str.add(drone.name);
            }
            return str.toArray();
        }

        public List<Drone> drones = new ArrayList<>(Arrays.asList(
                new Drone("Pixel", 0.225, 0.125, 0.01, 0.05, 0.4, 1, 5),
                new Drone("Voyager", 0.9, 0.2, 1.2, 0.2, 55, 0.8, 20),
                new Drone("Voxel racer one", 0.55, 0.3, 0.5, 0.135, 65, 1, 45)
        ));
        @SelectableDrone
        public String drone = "Voyager";

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
            public float width = 0.9f;
            public float height = 0.2f;
            public float mass = 1.2f;
            public float dragCoefficient = 0.2f;
            public float thrust = 55;
            public float thrustCurve = 0.8f;
            public int cameraAngle = 20;

            @Override
            public String toString() {
                return name;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface SelectableDrone {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface SelectableDevice {}



//        @ConfigEntry.Gui.CollapsibleObject
//        public ReplayMod replayMod = new ReplayMod();
//
//        public static class ReplayMod {
//            public boolean record = false;
//            @ConfigEntry.BoundedDiscrete(max = 20, min = 1)
//            public int everyXTick = 3;
//        }
}
