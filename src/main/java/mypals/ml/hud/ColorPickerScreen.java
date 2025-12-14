package mypals.ml.hud;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.function.Consumer;

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
        super(Text.literal(""));
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderSVPicker(context);
        renderHueBar(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 20, 0xFFFFFF);
    }

    private void renderSVPicker(DrawContext context) {
        for (int y = 0; y < pickerSize; y+=2) {
            for (int x = 0; x < pickerSize; x+=2) {
                float s = x / (float) pickerSize;
                float v = 1f - (y / (float) pickerSize);
                int rgb = Color.HSBtoRGB(hue / 360f, s, v);
                context.fill(pickerX + x, pickerY + y, pickerX + x + 2, pickerY + y + 2,
                        rgb);
            }
        }
        context.drawBorder(pickerX - 1, pickerY - 1, pickerSize + 2, pickerSize + 2, 0xFFFFFFFF);
        int circleX = pickerX + (int)(saturation * pickerSize);
        int circleY = pickerY + (int)((1f - value) * pickerSize);
        drawCircle(context, circleX, circleY, 5, 0xFFFFFFFF);
        drawCircle(context, circleX, circleY, 4, 0xFF000000);
    }

    private void renderHueBar(DrawContext context) {
        for (int y = 0; y < hueBarHeight; y+=2) {
            float h = y / (float) hueBarHeight;
            int rgb = Color.HSBtoRGB(h, 1f, 1f);
            int rgbNext = Color.HSBtoRGB(h+2, 1f, 1f);
            context.fillGradient(hueBarX, hueBarY + y, hueBarX + hueBarWidth, hueBarY + y + 2, rgb,rgbNext);
        }

        context.drawBorder(hueBarX - 1, hueBarY - 1, hueBarWidth + 2, hueBarHeight + 2, 0xFFFFFFFF);

        int indicatorY = hueBarY + (int)((hue / 360f) * hueBarHeight);
        context.fill(hueBarX - 2, indicatorY - 1, hueBarX + hueBarWidth + 2, indicatorY + 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
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
        onColorSelected.accept(getCurrentColor());

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSV) {
            updateSVFromMouse(mouseX, mouseY);
            return true;
        }

        if (draggingHue) {
            updateHueFromMouse(mouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSV = false;
            draggingHue = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSVFromMouse(double mouseX, double mouseY) {
        saturation = MathHelper.clamp((float)(mouseX - pickerX) / pickerSize, 0f, 1f);
        value = 1f - MathHelper.clamp((float)(mouseY - pickerY) / pickerSize, 0f, 1f);
    }

    private void updateHueFromMouse(double mouseY) {
        float h = MathHelper.clamp((float)(mouseY - hueBarY) / hueBarHeight, 0f, 1f);
        hue = h * 360f;
    }


    private Color getCurrentColor() {
        int rgb = Color.HSBtoRGB(hue / 360f, saturation, value);
        return new Color(rgb);
    }

    private void drawCircle(DrawContext context, int x, int y, int radius, int color) {
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            int x1 = x + (int)(Math.cos(angle) * radius);
            int y1 = y + (int)(Math.sin(angle) * radius);
            context.fill(x1, y1, x1 + 1, y1 + 1, color);
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}