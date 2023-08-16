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
    private enum Stage {
        BEGIN, THROTTLE, YAW, PITCH, ROLL, TEST
    }
    private final Screen parent;
    private ButtonWidget saveButton;
    private ButtonWidget configButton;
    private ButtonWidget nextButton;
    private Consumer<Integer> axisConsumer;

    private FloatBuffer axes;
    private List<Float> previousAxes;
    private Stage stage = Stage.BEGIN;

    public ControllerSetupScreen(Screen parent) {
        super(Text.translatable("quadz.config.controller_setup.title"));
        this.parent = parent;
    }
    @Override
    public void init() {
        var spacing = 10;
        var bWidth = 60;
        var bHeight = 20;

        this.saveButton = ButtonWidget.builder(Text.translatable("quadz.config.controller_setup.save"), this::onSaveButton)
            .position(spacing, height - bHeight - spacing)
            .size(bWidth, bHeight)
            .build();

        this.configButton = ButtonWidget.builder(Text.translatable("config"), this::onConfigButton)
                .position(spacing + bWidth + spacing / 2, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.nextButton = ButtonWidget.builder(Text.translatable("next"), button -> onAxisButton(this::onNextButton))
                .position(width - spacing - bWidth, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(this.configButton);
        this.addDrawableChild(this.nextButton);
    }
    private void onNextButton(int axis) {
        var controllerConfig = ModConfig.INSTANCE.controls.controller;
        boolean invertAxis = false;
        if(axis < 0) {
            invertAxis = true;
            axis *= -1;
        }
        axis -=1;

        switch (stage) {
            case THROTTLE -> {
                controllerConfig.throttle = axis;
                controllerConfig.invertThrottle = invertAxis;
            }
            case YAW -> {
                controllerConfig.yaw = axis;
                controllerConfig.invertYaw = invertAxis;
            }
            case PITCH -> {
                controllerConfig.pitch = axis;
                controllerConfig.invertPitch = invertAxis;
            }
            case ROLL -> {
                controllerConfig.roll = axis;
                controllerConfig.invertRoll = invertAxis;
            }
        }
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
        switch (stage) {
            case BEGIN -> stage = Stage.THROTTLE;
            case THROTTLE -> stage = Stage.YAW;
            case YAW -> stage = Stage.PITCH;
            case PITCH -> stage = Stage.ROLL;
            case ROLL -> stage = Stage.TEST;
        }
        startAnimationTime = System.currentTimeMillis();
        this.axisConsumer = axisConsumer;
        this.saveButton.active = false;
        this.configButton.active = false;
    }

    @Override
    public void tick() {
        while (this.axisConsumer != null && this.axes != null && this.axes.hasRemaining() && this.previousAxes != null) {
            var currentAxis = Math.round(this.axes.get() * 10f) / 10f;
            var previousAxis = Math.round(this.previousAxes.get(this.axes.position() - 1) * 10f) / 10f;

            if (currentAxis != previousAxis) {
                this.axisConsumer.accept(this.axes.position() * (currentAxis > 0 ? 1 : -1));
                this.axisConsumer = null;
                this.saveButton.active = true;
                this.configButton.active = true;
                break;
            }
        }

        if (this.axes != null && this.axes.remaining() > 0) this.previousAxes = toArray(this.axes);
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
    private long startAnimationTime = System.currentTimeMillis();

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        float pitch = 0;
        float yaw = 0;
        float roll = 0;
        float throttle = 0;
        if(stage == Stage.BEGIN || stage == Stage.TEST) {
            ControllerManager.updateControllerAxis();
            pitch = ControllerManager.pitch;
            yaw = ControllerManager.yaw;
            roll = ControllerManager.roll;
            throttle = ControllerManager.throttle;
        } else {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startAnimationTime;
            float progress = 0;
            if (elapsedTime < 700) {
                progress = (float) elapsedTime / 700;
            } else {
                startAnimationTime = currentTime;
            }
            switch (stage) {
                case THROTTLE -> throttle = progress;
                case YAW -> yaw = progress;
                case PITCH -> pitch = progress;
                case ROLL -> roll = progress;
            }
        }

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

        // Draw crosses

        context.fill(leftX, bottomY + 1, leftX + 1, topY, 0xFFFFFFFF);
        context.fill(rightX, bottomY + 1, rightX + 1, topY, 0xFFFFFFFF);
        context.fill(leftX - scale, y, leftX + scale + 1, y + 1, 0xFFFFFFFF);
        context.fill(rightX - scale, y, rightX + scale + 1, y + 1, 0xFFFFFFFF);

        // Draw stick positions
        int dotSize = 3;
        int yawAdjusted = (int) (yaw * scale);
        int throttleAdjusted = (int) (throttle * scale);
        int rollAdjusted = (int) (roll * scale);
        int pitchAdjusted = (int) (pitch * scale);
        context.fill(leftX + yawAdjusted - dotSize, y - throttleAdjusted - dotSize, leftX + yawAdjusted + dotSize, y - throttleAdjusted + dotSize, 0xFFFFFFFF);
        context.fill(rightX + rollAdjusted - dotSize, y - pitchAdjusted - dotSize, rightX + rollAdjusted + dotSize, y - pitchAdjusted + dotSize, 0xFFFFFFFF);

    }
}
