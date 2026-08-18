package mypals.ml.hud;


import java.awt.Color;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import static mypals.ml.config.GlowMyBlocksKeybinds.openColorPickerKey;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final Consumer<Color> onColorSelected;
    private final Color initialColor;
    private float hue = 0f;
    private float saturation = 1f;
    private float value = 1f;

    private int pickerX, pickerY;
    private int pickerSize = 100;
    private int hueBarX, hueBarY;
    private int hueBarWidth = 10;
    private int hueBarHeight = 100;
    private boolean draggingSV = false;
    private boolean draggingHue = false;

    public ColorPickerScreen(Screen parent, Color initialColor, Consumer<Color> onColorSelected) {
        super(Component.literal(""));
        this.parent = parent;
        this.initialColor = initialColor;
        this.onColorSelected = onColorSelected;

        if (initialColor != null) {
            float[] hsv = Color.RGBtoHSB(
                    initialColor.getRed(),
                    initialColor.getGreen(),
                    initialColor.getBlue(),
                    null
            );
            this.hue = hsv[0] * 360f;
            this.saturation = hsv[1];
            this.value = hsv[2];
        }
    }

    @Override
    protected void init() {
        super.init();
        pickerX = this.width / 2 - pickerSize / 2 - 40;
        pickerY = this.height / 2 - pickerSize / 2;
        hueBarX = pickerX + pickerSize + 20;
        hueBarY = pickerY;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Panel styling matches AreaManagerScreen, which is where this screen is opened from.
        context.fill(0, 0, this.width, this.height, 0xB4000000);

        int panelX = pickerX - 20;
        int panelY = pickerY - 44;
        int panelW = (hueBarX + hueBarWidth + 20) - panelX;
        int panelH = (pickerY + pickerSize + 46) - panelY;

        context.fill(panelX + 1, panelY, panelX + panelW - 1, panelY + panelH, 0xF01A1A20);
        context.fill(panelX, panelY + 1, panelX + 1, panelY + panelH - 1, 0xF01A1A20);
        context.fill(panelX + panelW - 1, panelY + 1, panelX + panelW, panelY + panelH - 1, 0xF01A1A20);
        context.renderOutline(panelX, panelY, panelW, panelH, 0x30FFFFFF);
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 26, 0x24FFFFFF);

        context.drawString(this.font, Component.translatable("gui.glowmyblocks.color.title"),
                panelX + 12, panelY + 9, 0xFFF0F0F5, false);

        renderSVPicker(context);
        renderHueBar(context);

        // Live preview of the picked colour plus its hex, so the value is readable, not just visible.
        Color current = getCurrentColor();
        int swatchY = pickerY + pickerSize + 10;
        context.fill(pickerX, swatchY, pickerX + 22, swatchY + 16, 0xFF000000 | current.getRGB());
        context.renderOutline(pickerX - 1, swatchY - 1, 24, 18, 0x60FFFFFF);
        context.drawString(this.font,
                String.format("#%06X", current.getRGB() & 0xFFFFFF),
                pickerX + 30, swatchY + 4, 0xFF9A9AA8, false);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSVPicker(GuiGraphics context) {
        for (int y = 0; y < pickerSize; y+=2) {
            for (int x = 0; x < pickerSize; x+=2) {
                float s = x / (float) pickerSize;
                float v = 1f - (y / (float) pickerSize);
                int rgb = Color.HSBtoRGB(hue / 360f, s, v);
                context.fill(pickerX + x, pickerY + y, pickerX + x + 2, pickerY + y + 2,
                        rgb);
            }
        }
        context.renderOutline(pickerX - 1, pickerY - 1, pickerSize + 2, pickerSize + 2, 0xFFFFFFFF);
        int circleX = pickerX + (int)(saturation * pickerSize);
        int circleY = pickerY + (int)((1f - value) * pickerSize);
        drawCircle(context, circleX, circleY, 5, 0xFFFFFFFF);
        drawCircle(context, circleX, circleY, 4, 0xFF000000);
    }

    private void renderHueBar(GuiGraphics context) {
        for (int y = 0; y < hueBarHeight; y+=2) {
            float h = y / (float) hueBarHeight;
            int rgb = Color.HSBtoRGB(h, 1f, 1f);
            int rgbNext = Color.HSBtoRGB(h+2, 1f, 1f);
            context.fillGradient(hueBarX, hueBarY + y, hueBarX + hueBarWidth, hueBarY + y + 2, rgb,rgbNext);
        }

        context.renderOutline(hueBarX - 1, hueBarY - 1, hueBarWidth + 2, hueBarHeight + 2, 0xFFFFFFFF);

        int indicatorY = hueBarY + (int)((hue / 360f) * hueBarHeight);
        context.fill(hueBarX - 2, indicatorY - 1, hueBarX + hueBarWidth + 2, indicatorY + 2, 0xFFFFFFFF);
    }

    // 1.21.11 folds the x/y/button triple into a MouseButtonEvent record.
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0) {
            if (mouseX >= pickerX && mouseX <= pickerX + pickerSize &&
                    mouseY >= pickerY && mouseY <= pickerY + pickerSize) {
                draggingSV = true;
                updateSVFromMouse(mouseX, mouseY);
                return true;
            }

            if (mouseX >= hueBarX && mouseX <= hueBarX + hueBarWidth &&
                    mouseY >= hueBarY && mouseY <= hueBarY + hueBarHeight) {
                draggingHue = true;
                updateHueFromMouse(mouseY);
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingSV) {
            updateSVFromMouse(event.x(), event.y());
            return true;
        }

        if (draggingHue) {
            updateHueFromMouse(event.y());
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            // Commit once the drag ends. Applying per-frame while dragging would kick off a full
            // outline mesh rebuild on every mouse move.
            boolean wasDragging = draggingSV || draggingHue;
            draggingSV = false;
            draggingHue = false;
            if (wasDragging) {
                onColorSelected.accept(getCurrentColor());
            }
        }
        return super.mouseReleased(event);
    }

    private void updateSVFromMouse(double mouseX, double mouseY) {
        saturation = Mth.clamp((float)(mouseX - pickerX) / pickerSize, 0f, 1f);
        value = 1f - Mth.clamp((float)(mouseY - pickerY) / pickerSize, 0f, 1f);
    }

    private void updateHueFromMouse(double mouseY) {
        float h = Mth.clamp((float)(mouseY - hueBarY) / hueBarHeight, 0f, 1f);
        hue = h * 360f;
    }


    private Color getCurrentColor() {
        int rgb = Color.HSBtoRGB(hue / 360f, saturation, value);
        return new Color(rgb);
    }

    private void drawCircle(GuiGraphics context, int x, int y, int radius, int color) {
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            int x1 = x + (int)(Math.cos(angle) * radius);
            int y1 = y + (int)(Math.sin(angle) * radius);
            context.fill(x1, y1, x1 + 1, y1 + 1, color);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}