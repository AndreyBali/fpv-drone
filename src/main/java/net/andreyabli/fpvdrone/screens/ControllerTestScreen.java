package net.andreyabli.fpvdrone.screens;

import me.shedaniel.autoconfig.AutoConfig;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.andreyabli.fpvdrone.util.GuiUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ControllerTestScreen extends Screen {

    private final Screen parent;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private ButtonWidget configButton;
    public ControllerTestScreen(Screen parent) {
        super(Text.translatable("fpvdrone.config.controller_setup.title"));
        this.parent = parent;
    }

    @Override
    public void init() {
        var spacing = 10;
        var bWidth = 60;
        var bHeight = 20;

        this.saveButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.controller_setup.save"), this::onSaveButton)
                .position(spacing, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.cancelButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.controller_setup.cancel"), this::onCancelButton)
                .position(spacing + bWidth + spacing / 2, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.configButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.controller_setup.config"), this::onConfigButton)
                .position(width - spacing - bWidth, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.saveButton);
        this.addDrawableChild(this.cancelButton);
        this.addDrawableChild(this.configButton);
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        ControllerManager.updateControllerAxis();
        var pitch = ControllerManager.pitch;
        var yaw = ControllerManager.yaw;
        var roll = ControllerManager.roll;
        var throttle = ControllerManager.throttle;
        GuiUtils.renderSticks(context, delta, width / 2, height / 2 + 20, 40, 10, pitch, yaw, roll, throttle);
        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    public void close() {
        client.setScreen(parent);
    }
}