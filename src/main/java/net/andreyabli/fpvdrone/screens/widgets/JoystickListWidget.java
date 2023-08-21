package net.andreyabli.fpvdrone.screens.widgets;

import com.google.common.collect.ImmutableList;
import net.andreyabli.fpvdrone.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.util.math.ColorHelper;

import java.util.*;

public class JoystickListWidget extends ElementListWidget<JoystickListWidget.Entry> {
    final Screen parent;
    public final int maxJoysticks;
    public List<CategoryEntry> entries = new ArrayList<>();
    private final String EMPTY_CONTROLLER = "Empty";
    private boolean selectedFromConfig = false;
    private final boolean includeKeyboard;

    public JoystickListWidget(Screen parent, MinecraftClient client, int joystickCount, boolean includeKeyboard) {
        super(client, parent.width + 45, parent.height, 20, parent.height - 32, 20);
        this.parent = parent;
        this.includeKeyboard = includeKeyboard;
        this.maxJoysticks = joystickCount + (includeKeyboard ? 1 : 0);
        for (int i = 0; i < maxJoysticks; i++) {
            CategoryEntry entry = new CategoryEntry(EMPTY_CONTROLLER);
            entries.add(entry);
            addEntry(entry);
        }
    }

    public void update(List<String> joysticks){
        if(!includeKeyboard) {
            joysticks.remove("keyboard");
        }
        int currentJoystickCount = joysticks.size();
        for (int i = 0; i < maxJoysticks; i++) {
            if(i < currentJoystickCount){
                entries.get(i).text = joysticks.get(i);
            } else {
                entries.get(i).text = EMPTY_CONTROLLER;
            }
        }
    }

    public class CategoryEntry extends Entry {
        public String text;
        private final int textWidth;
        public CategoryEntry(String text) {
            this.text = text;
            this.textWidth = JoystickListWidget.this.client.textRenderer.getWidth(this.text);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(text.equals(EMPTY_CONTROLLER)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            if(!JoystickListWidget.this.selectedFromConfig) JoystickListWidget.this.selectedFromConfig = true;
            JoystickListWidget.this.setSelected(this);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            TextRenderer textRenderer = JoystickListWidget.this.client.textRenderer;
            int textX = parent.width / 2 - this.textWidth / 2;
            int textY = y + entryHeight;
            Objects.requireNonNull(textRenderer);

            if(!JoystickListWidget.this.selectedFromConfig && ModConfig.INSTANCE.controls.device.equals(text)) {
                selectedFromConfig = true;
                JoystickListWidget.this.setSelected(this);
            }
            var selectedEntry = JoystickListWidget.this.getSelectedOrNull();
            if(selectedEntry != null && selectedEntry.equals(this)) {
                context.fill(parent.width / 2 - 290 / 2, y+3, parent.width / 2 + 290 / 2, y + 17, ColorHelper.Argb.getArgb(150, 0, 0, 0));
            }
            context.drawCenteredTextWithShadow(textRenderer, this.text, textX, textY - 10, 16777215);
        }
        public List<? extends Element> children() {
            return Collections.emptyList();
        }

        public List<? extends Selectable> selectableChildren() {
            return ImmutableList.of(new Selectable() {
                public Selectable.SelectionType getType() {
                    return Selectable.SelectionType.HOVERED;
                }

                public void appendNarrations(NarrationMessageBuilder builder) {
                    builder.put(NarrationPart.TITLE, text);
                }
            });
        }

        protected void update() {
        }
    }

    public abstract static class Entry extends ElementListWidget.Entry<Entry> {
        abstract void update();
    }
}
