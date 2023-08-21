package net.andreyabli.fpvdrone.screens;

import com.jme3.math.FastMath;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.andreyabli.fpvdrone.util.GuiUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ControllerSetupScreen extends Screen {
    private enum Stage {
        BEGIN, THROTTLE, YAW, PITCH, ROLL
    }
    private final Screen parent;
    private ButtonWidget backButton;
    private ButtonWidget nextButton;
    private Consumer<Integer> axisConsumer;
    private FloatBuffer axes;
    private List<Float> previousAxes;
    private Stage stage = Stage.BEGIN;
    private long startAnimationTime = System.currentTimeMillis();
    private boolean centerSticksStage = true;
    private final int STICK_ANIMATION_TIME = 1000;
    private final int CENTER_ANIMATION_TIME = 2000;

    public ControllerSetupScreen(Screen parent) {
        super(Text.translatable("fpvdrone.config.controller_setup.title"));
        this.parent = parent;
    }
    @Override
    public void init() {
        var spacing = 10;
        var bWidth = 80;
        var bHeight = 20;

        this.backButton = ButtonWidget.builder(ScreenTexts.BACK, this::onBackButton)
                .position(spacing, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.nextButton = ButtonWidget.builder(ScreenTexts.PROCEED, button -> onAxisButton())
                .position(width - spacing - bWidth, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.backButton);
        this.addDrawableChild(this.nextButton);
    }

    private void onAxisConsumer(int axis) {
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

    private void onBackButton(ButtonWidget button) {
        this.client.setScreen(this.parent);
    }

    private void onAxisButton() {
        centerSticksStage = false;
        switch (stage) {
            case BEGIN -> stage = Stage.THROTTLE;
            case THROTTLE -> stage = Stage.YAW;
            case YAW -> stage = Stage.PITCH;
            case PITCH -> stage = Stage.ROLL;
            case ROLL -> this.client.setScreen(new ControllerTestScreen(this));
        }
        startAnimationTime = System.currentTimeMillis();
        this.axisConsumer = this::onAxisConsumer;
        this.backButton.active = false;
        this.nextButton.active = false;
    }
    @Override
    public void tick() {
        while (this.axisConsumer != null && this.axes != null && this.axes.hasRemaining() && this.previousAxes != null) {
            var currentAxis = Math.round(this.axes.get() * 10f) / 10f;
            var previousAxis = Math.round(this.previousAxes.get(this.axes.position() - 1) * 10f) / 10f;

            if (currentAxis != previousAxis) {
                this.axisConsumer.accept(this.axes.position() * (currentAxis > 0 ? 1 : -1));
                this.axisConsumer = null;
                this.backButton.active = true;
                this.nextButton.active = true;
                if(stage != Stage.ROLL) centerSticksStage = true; //dont show center sticks animation on the last step
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
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        float throttle = 0;
        float yaw = 0;
        float pitch = 0;
        float roll = 0;
        float progress = 0;
        int animationTime = centerSticksStage ? CENTER_ANIMATION_TIME : STICK_ANIMATION_TIME;
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startAnimationTime;
        if (elapsedTime < animationTime) {
            progress = (float) elapsedTime / animationTime;
        } else {
            startAnimationTime = currentTime;
        }
        if(centerSticksStage){
            progress -= 1;
            float theta = progress * 3 * FastMath.PI;
            float x = progress * FastMath.sin(theta);
            float y = progress * FastMath.cos(theta);

            throttle = y;
            yaw = x;
            pitch = y;
            roll = -x;
        } else {
            switch (stage) {
                case THROTTLE -> throttle = progress;
                case YAW -> yaw = progress;
                case PITCH -> pitch = progress;
                case ROLL -> roll = progress;
            }
        }

        GuiUtils.renderSticks(context, delta, width / 2, height / 2 + 20, 40, 10, pitch, yaw, roll, throttle);

        // An axis has been selected. Time to listen...
        if (this.axisConsumer != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("fpvdrone.config.controller_setup.move_stick"), this.width / 2, this.height / 2 - 50, 16777215);
        } else if(centerSticksStage) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("fpvdrone.config.controller_setup.center_sticks"), this.width / 2, this.height / 2 - 50, 16777215);
        }
        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    public void close() {
        client.setScreen(parent);
    }
}
