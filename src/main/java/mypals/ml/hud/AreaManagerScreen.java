package mypals.ml.hud;

import mypals.ml.GlowMyBlocks;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.List;

import static mypals.ml.wandSystem.SelectedManager.persistAreas;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;


public class AreaManagerScreen extends Screen {

    private static final int PANEL_W = 400;
    private static final int ROW_H = 36;
    private static final int ROW_GAP = 3;
    private static final int PAD = 12;
    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 36;

    private static final int SCRIM = 0xB4000000;
    private static final int PANEL_BG = 0xF01A1A20;
    private static final int PANEL_EDGE = 0x30FFFFFF;
    private static final int HEADER_BG = 0x24FFFFFF;
    private static final int ROW_BG = 0x18FFFFFF;
    private static final int ROW_BG_HOVER = 0x30FFFFFF;
    private static final int TEXT = 0xFFF0F0F5;
    private static final int TEXT_DIM = 0xFF9A9AA8;
    private static final int DANGER = 0xFFFF6B6B;

    private static final String ARROW = " → ";
    private static final String TIMES = " × ";
    private static final String CROSS = "✕";

    private final Screen parent;

    private int panelX;
    private int panelY;
    private int panelH;
    private int listTop;
    private int listBottom;
    private double scroll;

    private int pendingDelete = -1;

    public AreaManagerScreen(Screen parent) {
        super(Component.translatable("gui.glowmyblocks.areas.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelH = Mth.clamp(
                140 + Math.min(selectedAreas.size(), 8) * (ROW_H + ROW_GAP),
                160, Math.max(160, this.height - 40));
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.listTop = this.panelY + HEADER_H;
        this.listBottom = this.panelY + this.panelH - FOOTER_H;

        int btnW = 110;
        int btnY = this.panelY + this.panelH - FOOTER_H + 7;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.glowmyblocks.areas.clear_all"),
                b -> {
                    selectedAreas.clear();
                    persistAreas();
                    GlowMyBlocks.onConfigUpdated();
                    this.pendingDelete = -1;
                    this.rebuildWidgets();
                }).bounds(this.panelX + PAD, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> this.onClose()
        ).bounds(this.panelX + PANEL_W - PAD - btnW, btnY, btnW, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, SCRIM);

        panel(g, this.panelX, this.panelY, PANEL_W, this.panelH, PANEL_BG, PANEL_EDGE);
        g.fill(this.panelX + 1, this.panelY + 1, this.panelX + PANEL_W - 1, this.panelY + HEADER_H, HEADER_BG);

        g.drawString(this.font, this.title, this.panelX + PAD, this.panelY + 12, TEXT, false);
        Component count = Component.translatable("gui.glowmyblocks.areas.count", selectedAreas.size());
        g.drawString(this.font, count,
                this.panelX + PANEL_W - PAD - this.font.width(count), this.panelY + 12, TEXT_DIM, false);

        if (selectedAreas.isEmpty()) {
            Component empty = Component.translatable("gui.glowmyblocks.areas.empty");
            Component hint = Component.translatable("gui.glowmyblocks.areas.empty_hint");
            int cy = (this.listTop + this.listBottom) / 2;
            g.drawString(this.font, empty, this.panelX + (PANEL_W - this.font.width(empty)) / 2, cy - 10, TEXT_DIM, false);
            g.drawString(this.font, hint, this.panelX + (PANEL_W - this.font.width(hint)) / 2, cy + 4, TEXT_DIM, false);
        } else {
            renderList(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, delta);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int rowW = PANEL_W - PAD * 2;
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll());

        g.enableScissor(this.panelX, this.listTop, this.panelX + PANEL_W, this.listBottom);

        List<AreaBox> areas = selectedAreas;
        for (int i = 0; i < areas.size(); i++) {
            int y = rowY(i);
            if (y + ROW_H < this.listTop || y > this.listBottom) continue;
            renderRow(g, areas.get(i), i, this.panelX + PAD, y, rowW, mouseX, mouseY);
        }

        g.disableScissor();
        renderScrollbar(g);
    }

    private void renderRow(GuiGraphics g, AreaBox area, int index, int x, int y, int w, int mouseX, int mouseY) {
        boolean hovered = mouseY >= this.listTop && mouseY < this.listBottom
                && inside(mouseX, mouseY, x, y, w, ROW_H);

        g.fill(x, y, x + w, y + ROW_H, hovered ? ROW_BG_HOVER : ROW_BG);

        // Colour spine down the left edge, so areas stay tellable apart at a glance.
        int rgb = 0xFF000000 | (area.color.getRGB() & 0xFFFFFF);
        g.fill(x, y, x + 3, y + ROW_H, rgb);

        int swatchX = x + 10;
        int swatchY = y + (ROW_H - 18) / 2;
        boolean swatchHovered = hovered && inside(mouseX, mouseY, swatchX, swatchY, 18, 18);
        g.fill(swatchX - 1, swatchY - 1, swatchX + 19, swatchY + 19, swatchHovered ? 0xFFFFFFFF : 0x60FFFFFF);
        g.fill(swatchX, swatchY, swatchX + 18, swatchY + 18, rgb);

        BlockPos min = area.minPos;
        BlockPos max = area.maxPos;
        int dx = max.getX() - min.getX() + 1;
        int dy = max.getY() - min.getY() + 1;
        int dz = max.getZ() - min.getZ() + 1;

        String extent = min.getX() + ", " + min.getY() + ", " + min.getZ()
                + ARROW + max.getX() + ", " + max.getY() + ", " + max.getZ();
        String size = dx + TIMES + dy + TIMES + dz
                + "   (" + formatVolume((long) dx * dy * dz) + ")";

        int textX = swatchX + 26;
        g.drawString(this.font, extent, textX, y + 7, TEXT, false);
        g.drawString(this.font, size, textX, y + 20, TEXT_DIM, false);

        int delW = 18;
        int delX = x + w - 8 - delW;
        int delY = y + (ROW_H - 18) / 2;
        boolean delHovered = hovered && inside(mouseX, mouseY, delX, delY, delW, 18);
        boolean armed = this.pendingDelete == index;

        g.fill(delX, delY, delX + delW, delY + 18, armed ? 0x50FF6B6B : (delHovered ? 0x40FFFFFF : 0x20FFFFFF));
        Component mark = Component.literal(armed ? "!" : CROSS);
        g.drawString(this.font, mark,
                delX + (delW - this.font.width(mark)) / 2, delY + 5,
                armed || delHovered ? DANGER : TEXT_DIM, false);

        if (armed) {
            Component confirm = Component.translatable("gui.glowmyblocks.areas.confirm_delete");
            g.drawString(this.font, confirm, delX - 6 - this.font.width(confirm), y + 14, DANGER, false);
        }
    }

    private void renderScrollbar(GuiGraphics g) {
        int max = maxScroll();
        if (max <= 0) return;

        int trackX = this.panelX + PANEL_W - 6;
        int trackH = this.listBottom - this.listTop;
        int contentH = trackH + max;
        int thumbH = Math.max(20, trackH * trackH / contentH);
        int thumbY = this.listTop + (int) ((trackH - thumbH) * (this.scroll / max));

        g.fill(trackX, this.listTop, trackX + 3, this.listBottom, 0x20FFFFFF);
        g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0x80FFFFFF);
    }

    /** Filled rect with the corner pixels dropped, which reads as a soft rounded panel. */
    private static void panel(GuiGraphics g, int x, int y, int w, int h, int fill, int edge) {
        g.fill(x + 1, y, x + w - 1, y + h, fill);
        g.fill(x, y + 1, x + 1, y + h - 1, fill);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, fill);

        g.fill(x + 1, y, x + w - 1, y + 1, edge);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        g.fill(x, y + 1, x + 1, y + h - 1, edge);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static String formatVolume(long blocks) {
        if (blocks >= 1000000L) return String.format("%.1fM", blocks / 1000000.0);
        if (blocks >= 1000L) return String.format("%.1fk", blocks / 1000.0);
        return Long.toString(blocks);
    }

    private int rowY(int index) {
        return this.listTop + 4 + index * (ROW_H + ROW_GAP) - (int) this.scroll;
    }

    private int maxScroll() {
        int contentH = selectedAreas.size() * (ROW_H + ROW_GAP) + 8;
        return Math.max(0, contentH - (this.listBottom - this.listTop));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (event.button() == 0 && mouseY >= this.listTop && mouseY < this.listBottom) {
            int rowW = PANEL_W - PAD * 2;
            int x = this.panelX + PAD;

            for (int i = 0; i < selectedAreas.size(); i++) {
                int y = rowY(i);
                if (y + ROW_H < this.listTop || y > this.listBottom) continue;

                int delX = x + rowW - 8 - 18;
                int delY = y + (ROW_H - 18) / 2;
                if (inside(mouseX, mouseY, delX, delY, 18, 18)) {
                    if (this.pendingDelete == i) {
                        selectedAreas.remove(i);
                        persistAreas();
                        GlowMyBlocks.onConfigUpdated();
                        this.pendingDelete = -1;
                        this.rebuildWidgets();
                    } else {
                        this.pendingDelete = i;
                    }
                    return true;
                }

                int swatchX = x + 10;
                int swatchY = y + (ROW_H - 18) / 2;
                if (inside(mouseX, mouseY, swatchX, swatchY, 18, 18)) {
                    int areaIndex = i;
                    Color initial = selectedAreas.get(areaIndex).color;
                    this.minecraft.setScreen(new ColorPickerScreen(this, initial, picked -> {
                        // onConfigUpdated() reparses the config and replaces every AreaBox
                        // instance, so resolve the target by index on each callback rather than
                        // capturing the object -- a captured one goes stale after the first apply.
                        if (areaIndex < selectedAreas.size()) {
                            selectedAreas.get(areaIndex).color = picked;
                            persistAreas();
                            GlowMyBlocks.onConfigUpdated();
                        }
                    }));
                    return true;
                }
            }
            this.pendingDelete = -1;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= this.listTop && mouseY < this.listBottom) {
            this.scroll = Mth.clamp(this.scroll - scrollY * (ROW_H + ROW_GAP), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
