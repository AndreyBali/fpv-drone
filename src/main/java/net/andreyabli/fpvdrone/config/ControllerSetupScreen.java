package net.andreyabli.fpvdrone.config;

import me.shedaniel.autoconfig.AutoConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ControllerSetupScreen extends Screen {
    private final Screen parent;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private ButtonWidget configButton;
    private ButtonWidget pitchButton;
    private ButtonWidget yawButton;
    private ButtonWidget rollButton;
    private ButtonWidget throttleButton;
    private ButtonWidget invertPitchButton;
    private ButtonWidget invertYawButton;
    private ButtonWidget invertRollButton;
    private ButtonWidget invertThrottleButton;
    private Consumer<Integer> axisConsumer;

    private FloatBuffer axes;
    private List<Float> previousAxes;

    public ControllerSetupScreen(Screen parent) {
        super(Text.translatable("quadz.config.controller_setup.title"));
        this.parent = parent;
    }

    @Override
    public void init() {
        var controllerConfig = ModConfig.INSTANCE.controls.controller; 
        
        var spacing = 10;
        var bWidth = 60;
        var bHeight = 20;

        this.saveButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.save"), this::onSaveButton)
            .position(spacing, height - bHeight - spacing)
            .size(bWidth, bHeight)
            .build();

        this.cancelButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.cancel"), this::onCancelButton)
                .position(spacing + bWidth + spacing / 2, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.configButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.config"), this::onConfigButton)
                .position(width - spacing - bWidth, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.pitchButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.pitch"), button -> onAxisButton(axis -> controllerConfig.pitch = axis))
            .position(width / 2 + bWidth + spacing, bHeight + spacing)
            .size(bWidth, bHeight)
            .build();

        this.yawButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.yaw"), button -> onAxisButton(axis -> controllerConfig.yaw = axis))
            .position(width / 2 - bWidth - spacing / 3, bHeight + spacing)
            .size(bWidth, bHeight)
            .build();

        this.rollButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.roll"), button -> onAxisButton(axis -> controllerConfig.roll = axis))
            .position(width / 2 + spacing / 3, bHeight + spacing)
            .size(bWidth, bHeight)
            .build();

        this.throttleButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.throttle"), button -> onAxisButton(axis -> controllerConfig.throttle = axis))
            .position(width / 2 - bWidth * 2 - spacing, bHeight + spacing)
            .size(bWidth, bHeight)
            .build();

        this.invertPitchButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.invert"), button -> controllerConfig.invertPitch = !controllerConfig.invertPitch)
                .position(width / 2 + bWidth + spacing, bHeight * 2 + spacing)
                .size(bWidth, bHeight)
                .build();

        this.invertYawButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.invert"), button -> controllerConfig.invertYaw = !controllerConfig.invertYaw)
                .position(width / 2 - bWidth - spacing / 3, bHeight * 2 + spacing)
                .size(bWidth, bHeight)
                .build();

        this.invertRollButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.invert"), button -> controllerConfig.invertRoll = !controllerConfig.invertRoll)
                .position(width / 2 + spacing / 3, bHeight * 2 + spacing)
                .size(bWidth, bHeight)
                .build();

        this.invertThrottleButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.invert"), button -> controllerConfig.invertThrottle = !controllerConfig.invertThrottle)
                .position(width / 2 - bWidth * 2 - spacing, bHeight * 2 + spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(this.cancelButton);
        this.addDrawableChild(this.configButton);
        this.addDrawableChild(this.pitchButton);
        this.addDrawableChild(this.yawButton);
        this.addDrawableChild(this.rollButton);
        this.addDrawableChild(this.throttleButton);
        this.addDrawableChild(this.invertPitchButton);
        this.addDrawableChild(this.invertYawButton);
        this.addDrawableChild(this.invertRollButton);
        this.addDrawableChild(this.invertThrottleButton);
    }

    private void onSaveButton(ButtonWidget button) {
        AutoConfig.getConfigHolder(ModConfig.class).save();
        this.client.setScreen(this.parent);
    }

    private void onCancelButton(ButtonWidget button) {
        this.client.setScreen(this.parent);
    }

    private void onConfigButton(ButtonWidget button) {
        this.client.setScreen(AutoConfig.getConfigScreen(ModConfig.class, parent).get());
    }

    private void onAxisButton(Consumer<Integer> axisConsumer) {
        this.axisConsumer = axisConsumer;
        this.pitchButton.active = false;
        this.yawButton.active = false;
        this.rollButton.active = false;
        this.throttleButton.active = false;
        this.saveButton.active = false;
        this.invertPitchButton.active = false;
        this.invertYawButton.active = false;
        this.invertRollButton.active = false;
        this.invertThrottleButton.active = false;
    }

    @Override
    public void tick() {
        while (this.axisConsumer != null && this.axes != null && this.axes.hasRemaining() && this.previousAxes != null) {
            var currentAxis = Math.round(this.axes.get() * 10f) / 10f;
            var previousAxis = Math.round(this.previousAxes.get(this.axes.position() - 1) * 10f) / 10f;

            if (currentAxis != previousAxis) {
                this.axisConsumer.accept(this.axes.position() - 1);
                this.axisConsumer = null;
                this.pitchButton.active = true;
                this.yawButton.active = true;
                this.rollButton.active = true;
                this.throttleButton.active = true;
                this.saveButton.active = true;
                this.invertPitchButton.active = true;
                this.invertYawButton.active = true;
                this.invertRollButton.active = true;
                this.invertThrottleButton.active = true;
                break;
            }
        }

        if (this.axes != null && this.axes.remaining() > 0)
            this.previousAxes = toArray(this.axes);
        this.axes = ControllerManager.getAllAxisValues();
    }
    static List<Float> toArray(FloatBuffer floatBuffer) {
        var out = new ArrayList<Float>();
        floatBuffer.rewind();

        while (floatBuffer.hasRemaining()) {
            out.add(floatBuffer.get());
        }

        return out;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        ControllerManager.updateControllerAxis();
        var pitch = ControllerManager.pitch;
        var yaw = ControllerManager.yaw;
        var roll = ControllerManager.roll;
        var throttle = ControllerManager.throttle;
        renderSticks(context, delta, width / 2, height / 2 + 20, 40, 10, pitch, yaw, roll, throttle);

        // An axis has been selected. Time to listen...
        if (this.axisConsumer != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("quadz.config.controller_setup.prompt"), this.width / 2, this.height / 2 - 50, 16777215);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    public static void renderSticks(DrawContext context, float tickDelta, int x, int y, int scale, int spacing, float pitch, float yaw, float roll, float throttle) {
        var leftX = x - scale - spacing;
        var rightX = x + scale + spacing;
        var topY = y + scale;
        var bottomY = y - scale;

        // Draw crosses1

        context.fill(leftX, bottomY + 1, leftX + 1, topY, 0xFFFFFFFF);
        context.fill(rightX, bottomY + 1, rightX + 1, topY, 0xFFFFFFFF);
        context.fill(leftX - scale, y, leftX + scale + 1, y + 1, 0xFFFFFFFF);
        context.fill(rightX - scale, y, rightX + scale + 1, y + 1, 0xFFFFFFFF);

        // Draw stick positions
        int dotSize = 2;
        int yawAdjusted = (int) (yaw * scale);
        int throttleAdjusted = (int) (throttle * scale);
        int rollAdjusted = (int) (roll * scale);
        int pitchAdjusted = (int) (pitch * scale);
        context.fill(leftX + yawAdjusted - dotSize, y - throttleAdjusted - dotSize, leftX + yawAdjusted + dotSize, y - throttleAdjusted + dotSize, 0xFFFFFFFF);
        context.fill(rightX + rollAdjusted - dotSize, y - pitchAdjusted - dotSize, rightX + rollAdjusted + dotSize, y - pitchAdjusted + dotSize, 0xFFFFFFFF);

    }
}
