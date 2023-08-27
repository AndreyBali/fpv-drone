package net.andreyabli.fpvdrone.screens;

import me.shedaniel.autoconfig.AutoConfig;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.andreyabli.fpvdrone.util.GuiUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ControllerTestScreen extends Screen {

    private final Screen parent;
    private ButtonWidget saveButton;
    private ButtonWidget backButton;
    private ButtonWidget calibrateButton;

    public ControllerTestScreen(Screen parent) {
        super(Text.translatable("fpvdrone.config.controller_setup.title"));
        this.parent = parent;
        ControllerManager.updateControllers();
    }

    @Override
    public void init() {
        var spacing = 10;
        var bWidth = 80;
        var bHeight = 20;

        this.backButton = ButtonWidget.builder(ScreenTexts.BACK, button -> this.client.setScreen(this.parent))
                .position(spacing, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.calibrateButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.wizard"), this::onCalibrateButton)
                .position(spacing*2 + bWidth, height - bHeight - spacing)
                .size(120, bHeight)
                .build();

        this.saveButton = ButtonWidget.builder(ScreenTexts.DONE, this::onSaveButton)
                .position(width - spacing - bWidth, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(this.calibrateButton);
        this.addDrawableChild(this.backButton);
    }

    private void onSaveButton(ButtonWidget button) {
        AutoConfig.getConfigHolder(ModConfig.class).save();
        if (parent instanceof ControllerSetupScreen css){
            if (css.getParent() instanceof SelectJoystickScreen sjs){
                this.client.setScreen(sjs.getParent());
                return;
            }
        }
        this.client.setScreen(new WelcomeScreen(null));
    }

    private void onCalibrateButton(ButtonWidget button) {
        if (parent instanceof ControllerSetupScreen css){
            if (css.getParent() instanceof SelectJoystickScreen sjs){
                this.client.setScreen(sjs);
                return;
            }
        }
        this.client.setScreen(new SelectJoystickScreen(null, false,
                sjs -> this.client.setScreen(new ControllerSetupScreen(sjs))));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        ControllerManager.updateControllerAxis();
        var pitch = ControllerManager.pitch;
        var yaw = ControllerManager.yaw;
        var roll = ControllerManager.roll;
        var throttle = ControllerManager.throttle;
        GuiUtils.renderSticks(context, delta, width / 2, height / 2 + 20, 40, 10, pitch, yaw, roll, throttle);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("fpvdrone.config.controller_setup.is_working"), this.width / 2, this.height / 2 - 50, 16777215);

        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    public void close() {
        client.setScreen(parent);
    }
}