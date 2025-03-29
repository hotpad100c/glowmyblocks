package mypals.ml.wandSystem;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static mypals.ml.GlowMyBlocks.MOD_ID;
import static mypals.ml.config.Keybinds.*;
import static mypals.ml.config.GlowMyBlocksConfig.selectInSpectator;
import static mypals.ml.wandSystem.WandActionsManager.pos1;
import static mypals.ml.wandSystem.WandActionsManager.pos2;

public class WandTooltipRenderer {
    private static final List<ToolTipItem> hudItems = new ArrayList<>();
    private static class ToolTipItem {
        String text;
        int color;

        @Nullable
        Identifier icon;
        public ToolTipItem(String text, Color color, Identifier icon) {
            this.text = text;
            this.color = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
            this.icon = icon;
        }
    }
    public static void addTooltip(String text, Color color, Identifier icon) {
        hudItems.add(new ToolTipItem(text, color, icon));
    }

    public static void generateTooltip() {
        hudItems.clear();

        TriConsumer<String,Color,String> addTooltip = (key, color, icon) ->
                WandTooltipRenderer.addTooltip(Text.translatable(key).getString(), color, Identifier.of(MOD_ID, icon));

        TriConsumer<KeyBinding,Color ,String> addKeyTooltip = (key,color, icon) ->
                addTooltip.accept(Text.translatable(key.getTranslationKey()).getString() + "(" + key.getBoundKeyLocalizedText().getString() + ")", color, icon);

        if (addArea.isPressed()) {
            addKeyTooltip.accept(addArea, new Color(200, 255, 200, 200),"textures/gui/hotkey.png");
            if (pos1 != null && pos2 != null) {
                addTooltip.accept("config.wand.addArea", new Color(255, 255, 255, 200), "textures/gui/mouse_left.png");
            }
        } else if (deleteArea.isPressed()) {
            addKeyTooltip.accept(deleteArea,new Color(200, 255, 200, 200), "textures/gui/hotkey.png");
            addTooltip.accept("config.wand.delete", new Color(255, 180, 180, 200), "textures/gui/mouse_right.png");
            if (pos1 != null && pos2 != null) {
                addTooltip.accept("config.wand.cut", new Color(255, 200, 200, 200), "textures/gui/mouse_left.png");
            }
        } else {
            if (pos1 == null) {
                addTooltip.accept("config.wand.selectP1", new Color(255, 255, 255, 200), "textures/gui/mouse_left.png");
            }
            if (pos2 == null) {
                addTooltip.accept("config.wand.selectP2", new Color(255, 255, 255, 200), "textures/gui/mouse_right.png");
            }
            if (pos1 != null && pos2 != null) {
                if (!addArea.isPressed()) {
                    addKeyTooltip.accept(addArea, new Color(255, 255, 255, 200),"textures/gui/hotkey.png");
                }
                if (!deleteArea.isPressed()) {
                    addKeyTooltip.accept(deleteArea, new Color(255, 255, 255, 200),"textures/gui/hotkey.png");
                }
            }
        }
    }
    public static void renderWandTooltip(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        boolean shouldSelect = player.getMainHandStack().getItem() == SelectedManager.wand || (selectInSpectator && player.isSpectator());
        if (!shouldSelect || client.options.hudHidden) {
            return;
        }
        generateTooltip();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int x = centerX - 50;
        int y = centerY + 5;
        int lineHeight = 10;

        int maxTextWidth = 0;
        for (ToolTipItem item : hudItems) {
            int textWidth = client.textRenderer.getWidth(item.text);
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }

        x = centerX - maxTextWidth / 2;
        for (ToolTipItem item : hudItems) {
            if (item.icon != null) {
                RenderSystem.enableBlend();
                context.drawTexture(item.icon, x, y, 0, 0, 16, 16, 16, 16);
                RenderSystem.disableBlend();
            }

            int textX = x + (item.icon != null ? 20 : 0);
            context.drawText(client.textRenderer, item.text, textX, (int) (y + 4), item.color, true);

            y += lineHeight;
        }
    }
}
