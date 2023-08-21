package net.andreyabli.fpvdrone.screens;

import net.andreyabli.fpvdrone.config.ModConfig;
import net.andreyabli.fpvdrone.util.ControllerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class SelectJoystickScreen extends Screen {
    private final Screen parent;
    private final Consumer<SelectJoystickScreen> nextScreen;
    private JoystickListWidget controlsList;
    private List<String> controllers = null;
    private ButtonWidget backButton;
    private ButtonWidget doneButton;
    private final boolean includeKeyboard;

    public SelectJoystickScreen(Screen parent, boolean includeKeyboard, Consumer<SelectJoystickScreen> nextScreen) {
        super(Text.translatable("fpvdrone.screen.select_joystick"));
        this.parent = parent;
        this.nextScreen = nextScreen;
        this.includeKeyboard = includeKeyboard;
    }

    @Override
    public void init() {
        controlsList = new JoystickListWidget(this, MinecraftClient.getInstance(), 16, includeKeyboard);
        this.addSelectableChild(this.controlsList);

        var spacing = 10;
        var bWidth = 80;
        var bHeight = 20;

        this.backButton = ButtonWidget.builder(ScreenTexts.CANCEL,
                        button -> this.client.setScreen(parent))
                .position(spacing, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.doneButton = ButtonWidget.builder(ScreenTexts.OK,
                        button -> {
                            var selectedDevice = controlsList.getSelectedOrNull();
                            if(selectedDevice != null){
                                ModConfig.INSTANCE.controls.device = ((JoystickListWidget.CategoryEntry) selectedDevice).text;
                            }
                            if(nextScreen != null){
                                nextScreen.accept(this);
                            } else {
                                this.client.setScreen(parent);
                            }
                        })
                .position(width - spacing - bWidth, height - bHeight - spacing)
                .size(bWidth, bHeight)
                .build();

        this.addDrawableChild(this.backButton);
        this.addDrawableChild(this.doneButton);
    }
    @Override
    public void tick() {
        List<String> currentControllers = ControllerManager.getControllers();
        if(controllers == null || controllers.equals(currentControllers)) {
            controllers = currentControllers;
            controlsList.update(controllers);
        }
        doneButton.active = controlsList.getSelectedOrNull() != null;
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 16777215);
        this.controlsList.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
    @Override
    public void close() {
        client.setScreen(parent);
    }
}
