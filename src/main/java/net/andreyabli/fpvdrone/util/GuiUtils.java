package net.andreyabli.fpvdrone.util;

import net.minecraft.client.gui.DrawContext;

public class GuiUtils {
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
        int yawAdjusted = (int) (yaw * -1 * scale);
        int throttleAdjusted = (int) (throttle * scale);
        int rollAdjusted = (int) (roll * scale);
        int pitchAdjusted = (int) (pitch * scale);
        context.fill(leftX + yawAdjusted - dotSize, y - throttleAdjusted - dotSize, leftX + yawAdjusted + dotSize, y - throttleAdjusted + dotSize, 0xFFFFFFFF);
        context.fill(rightX + rollAdjusted - dotSize, y - pitchAdjusted - dotSize, rightX + rollAdjusted + dotSize, y - pitchAdjusted + dotSize, 0xFFFFFFFF);
    }
}
