package net.andreyabli.fpvdrone.screens;

import me.shedaniel.autoconfig.AutoConfig;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class WelcomeScreen extends Screen {
    private final Screen parent;

    public WelcomeScreen(Screen parent) {
        super(Text.translatable("fpvdrone.screen.welcome"));
        this.parent = parent;
    }
    private ButtonWidget wizardButton;
    private ButtonWidget doneButton;
    private ButtonWidget configButton;
    private ButtonWidget droneButton;
    private ButtonWidget deviceButton;
    private ButtonWidget testDeviceButton;
    @Override
    public void init() {
        var spacing = 10;
        var bHeight = 20;
        addFooterButtons(spacing, bHeight);
        addCenterButtons(spacing, bHeight);
    }

    @Override
    public void tick() {
        ControllerManager.updateControllers();
        super.tick();
    }

    private void addCenterButtons(int spacing, int bHeight){
        int bWidth = 120;

        this.deviceButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.deviceButton"),
                        btn -> client.setScreen(new SelectJoystickScreen(this, true, null)))
                .position((width - bWidth)/2 - 75, height/2 + spacing)
                .size(bWidth, bHeight)
                .build();

        this.testDeviceButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.testButton"), this::onTestDeviceButton)
                .position((width - bWidth)/2 - 75, height/2 + spacing + bHeight)
                .size(bWidth, bHeight)
                .build();

        this.droneButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.droneButton"), this::onDroneButton)
                .position((width - bWidth)/2 + 75, height/2 + spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.deviceButton);
        this.addDrawableChild(this.testDeviceButton);
        this.addDrawableChild(this.droneButton);
    }

    private void onTestDeviceButton(ButtonWidget button){
        ControllerManager.updateControllers();

        if(ModConfig.INSTANCE.controls.device.equals("keyboard")){
            client.setScreen(new SelectJoystickScreen(this, false, sjs -> {
                client.setScreen(new ControllerTestScreen(sjs));
            }));
        } else {
            client.setScreen(new ControllerTestScreen(this));
        }
    }

    private void onDroneButton(ButtonWidget button){
        boolean chooseNext = false;
        for (var drone : ModConfig.INSTANCE.droneConfig.drones) {
            if(drone.name.equals(ModConfig.INSTANCE.droneConfig.drone)) {
                chooseNext = true;
                continue;
            }
            if(chooseNext){
                ModConfig.INSTANCE.droneConfig.drone = drone.name;
                return;
            }
        }
        ModConfig.INSTANCE.droneConfig.drone = ModConfig.INSTANCE.droneConfig.drones.get(0).name;
    }

    private void addFooterButtons(int spacing, int bHeight){

        this.wizardButton = ButtonWidget.builder(Text.translatable("fpvdrone.config.wizard"),
                        this::onWizardButton)
                .position(spacing, height - bHeight - spacing)
                .size(120, bHeight)
                .build();

        this.configButton = ButtonWidget.builder(Text.translatable("fpvdrone.config"),
                        button -> this.client.setScreen(AutoConfig.getConfigScreen(ModConfig.class, parent).get()))
                .position(width / 2 - 100 / 2, height - bHeight - spacing)
                .size(100, bHeight)
                .build();

        this.doneButton = ButtonWidget.builder(ScreenTexts.DONE,
                        button -> this.client.setScreen(parent))
                .position(width - spacing - 80, height - bHeight - spacing)
                .size(80, bHeight)
                .build();

        this.addDrawableChild(this.wizardButton);
        this.addDrawableChild(this.doneButton);
        this.addDrawableChild(this.configButton);
    }

    private void onWizardButton(ButtonWidget button){
        this.client.setScreen(new SelectJoystickScreen(this, false,
                sjs -> this.client.setScreen(new ControllerSetupScreen(sjs))
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 16777215);

        context.drawCenteredTextWithShadow(this.textRenderer, "Current controller: ",        width/2 - 75, height/2 - 30, 16777215);
        context.drawCenteredTextWithShadow(this.textRenderer, ModConfig.INSTANCE.controls.device, width/2 - 75, height/2 - 10, 16777215);

        context.drawCenteredTextWithShadow(this.textRenderer, "Current drone: ",               width/2 + 75, height/2 - 30, 16777215);
        context.drawCenteredTextWithShadow(this.textRenderer, ModConfig.INSTANCE.droneConfig.drone, width/2 + 75, height/2 - 10, 16777215);

        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    public void close() {
        client.setScreen(parent);
    }
}
