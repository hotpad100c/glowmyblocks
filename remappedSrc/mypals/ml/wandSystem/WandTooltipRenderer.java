package mypals.ml.wandSystem;

import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import static mypals.ml.GlowMyBlocks.MOD_ID;
import static mypals.ml.config.GlowModeManager.currentGlowRenderMode;
import static mypals.ml.config.GlowMyBlocksKeybinds.*;
import static mypals.ml.config.GlowMyBlocksConfig.selectInSpectator;
import static mypals.ml.wandSystem.WandActionsManager.pos1;
import static mypals.ml.wandSystem.WandActionsManager.pos2;

public class WandTooltipRenderer {
    private static final List<ToolTipItem> hudItems = new ArrayList<>();
    private static class ToolTipItem {
        String text;
        int color;

        @Nullable
        ResourceLocation icon;
        public ToolTipItem(String text, Color color, ResourceLocation icon) {
            this.text = text;
            this.color = (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
            this.icon = icon;
        }
    }
    public static void addTooltip(String text, Color color, ResourceLocation icon) {
        hudItems.add(new ToolTipItem(text, color, icon));
    }

    public static void generateTooltip() {
        hudItems.clear();

        TriConsumer<String,Color,String> addTooltip = (key, color, icon) ->
                WandTooltipRenderer.addTooltip(Component.translatable(key).getString(), color, ResourceLocation.fromNamespaceAndPath(MOD_ID, icon));

        TriConsumer<KeyMapping,Color ,String> addKeyTooltip = (key,color, icon) ->
                addTooltip.accept(Component.translatable(key.getName()).getString() + "(" + key.getTranslatedKeyMessage().getString() + ")", color, icon);

        if(Minecraft.getInstance().options.keySprint.isDown()){
            addTooltip.accept("config.wand.switchMode", new Color(255, 255, 255, 200), "textures/gui/mouse_middle.png");
        }else if(pos1 == null && pos2 == null)
                addTooltip.accept("config.wand.holdSprint", new Color(200, 200, 200, 150), "textures/gui/hotkey.png");
        if (addOutlineArea.isDown()) {
            addKeyTooltip.accept(addOutlineArea, new Color(200, 255, 200, 200),"textures/gui/hotkey.png");
            if (pos1 != null && pos2 != null) {
                addTooltip.accept("config.wand.addArea", new Color(255, 255, 255, 200), "textures/gui/mouse_left.png");
            }
        } else if (deleteOutlineArea.isDown()) {
            addKeyTooltip.accept(deleteOutlineArea,new Color(200, 255, 200, 200), "textures/gui/hotkey.png");
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
                if (!addOutlineArea.isDown()) {
                    addKeyTooltip.accept(addOutlineArea, new Color(255, 255, 255, 200),"textures/gui/hotkey.png");
                }
                if (!deleteOutlineArea.isDown()) {
                    addKeyTooltip.accept(deleteOutlineArea, new Color(255, 255, 255, 200),"textures/gui/hotkey.png");
                }
            }
        }
    }
    public static void renderWandTooltip(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        boolean shouldSelect = player.getMainHandItem().getItem() == SelectedManager.wand || (selectInSpectator && player.isSpectator());
        if (!shouldSelect || client.options.hideGui) {
            return;
        }
        generateTooltip();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int x = centerX - 50;
        int y = centerY + 5;
        int lineHeight = 10;

        int maxTextWidth = 0;
        for (ToolTipItem item : hudItems) {
            int textWidth = client.font.width(item.text);
            maxTextWidth = Math.max(maxTextWidth, textWidth);
        }

        x = centerX - maxTextWidth / 2;
        for (ToolTipItem item : hudItems) {
            if (item.icon != null) {
                RenderSystem.enableBlend();
                context.blit(item.icon, x, y, 0, 0, 16, 16, 16, 16);
                RenderSystem.disableBlend();
            }

            int textX = x + (item.icon != null ? 20 : 0);
            context.drawString(client.font, item.text, textX, y + 4, item.color, true);

            y += lineHeight;
        }
    }
    public static void renderWandModeIcon(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        boolean shouldSelect = player.getMainHandItem().getItem() == SelectedManager.wand || (selectInSpectator && player.isSpectator());
        if (!shouldSelect) {
            return;
        }

        int screenHeight = client.getWindow().getGuiScaledHeight();
        int centerY = screenHeight;

        int iconWidth = 32;

        int x = 0;
        int y = centerY - 60;
        RenderSystem.enableBlend();
        context.blit(ResourceLocation.fromNamespaceAndPath(MOD_ID, currentGlowRenderMode.getIcon()), x, y, 0, 0, iconWidth, iconWidth, iconWidth, iconWidth);
        RenderSystem.disableBlend();
        context.drawString(client.font, Component.translatable(currentGlowRenderMode.getTranslationKey()), x+iconWidth+2, y+(iconWidth/2), 0xFFFFFFE0, true);

    }
}
