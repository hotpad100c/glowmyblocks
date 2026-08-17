package mypals.ml.hud;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mypals.ml.blockOutline.ChunkDataBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BuildProgressHud {

    private static long completionTime = 0;
    private static boolean shouldFadeOut = false;
    private static long fadeStartTime = 0;
    private static final long DISPLAY_AFTER_COMPLETE_MS = 1000;
    private static final long FADE_DURATION_MS = 2000;
    private static final Minecraft client = Minecraft.getInstance();
    private static final Identifier BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("boss_bar/purple_background");
    private static final Identifier PROGRESS_TEXTURE = Identifier.withDefaultNamespace("boss_bar/purple_progress");
    private static final Identifier NOTCHED_PROGRESS_TEXTURE = Identifier.withDefaultNamespace("boss_bar/notched_20_progress");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    public static void render(GuiGraphics context) {
        ChunkDataBuilder.BuildProgress progress = ChunkDataBuilder.getBuildProgress();
        long currentTime = System.currentTimeMillis();

        boolean isActive = progress.isBuilding();

        if (isActive) {
            shouldFadeOut = false;
            fadeStartTime = 0;
            completionTime = 0;
        } else if (completionTime == 0) {
            completionTime = currentTime;
        } else if (!shouldFadeOut && currentTime - completionTime >= DISPLAY_AFTER_COMPLETE_MS) {
            shouldFadeOut = true;
            fadeStartTime = currentTime;
        }

        float alpha = 1.0f;
        if (shouldFadeOut) {
            long elapsed = currentTime - fadeStartTime;
            if (elapsed >= FADE_DURATION_MS) {
                return;
            }
            alpha = 1.0f - (elapsed / (float)FADE_DURATION_MS);
        } else if (completionTime == 0 && !isActive) {
            return;
        }

        //client.getProfiler().push("glowMyBlocksProgress");

        GlStateManager._enableBlend();
        int screenWidth = context.guiWidth();
        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = 12;

        renderProgressBar(context, x, y, progress.percentage());

        int textAlpha = (int)(alpha * 255) << 24;
        int titleColor = 0x00FFFFFF | textAlpha;
        int detailColor = 0x00AAAAAA | textAlpha;

        Component title = getProgressTitle(progress);
        int titleWidth = client.font.width(title);
        int titleX = screenWidth / 2 - titleWidth / 2;
        context.drawString(client.font, title, titleX, y - 9, titleColor);

        if (progress.isBuilding()) {
            Component details = getProgressDetails(progress);
            int detailsWidth = client.font.width(details);
            int detailsX = screenWidth / 2 - detailsWidth / 2;
            context.drawString(client.font, details, detailsX, y + BAR_HEIGHT + 2, detailColor);
        }
        GlStateManager._disableBlend();

        //client.getProfiler().pop();
    }

    private static void renderProgressBar(GuiGraphics context, int x, int y, float percent) {
        percent = Mth.clamp(percent, 0f, 100f);
        context.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND_TEXTURE,
                BAR_WIDTH, BAR_HEIGHT,
                0, 0,
                x, y,
                BAR_WIDTH, BAR_HEIGHT
        );
        int progressWidth = (int)(BAR_WIDTH * percent / 100f);
        if (progressWidth > 0) {
            context.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    PROGRESS_TEXTURE,
                    BAR_WIDTH, BAR_HEIGHT,
                    0, 0,
                    x, y,
                    progressWidth, BAR_HEIGHT
            );
            context.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    NOTCHED_PROGRESS_TEXTURE,
                    BAR_WIDTH, BAR_HEIGHT,
                    0, 0,
                    x, y,
                    progressWidth, BAR_HEIGHT
            );
        }
    }

    private static Component getProgressTitle(ChunkDataBuilder.BuildProgress progress) {
        if (progress.percentage() >= 99.99f) {
            return Component.translatable("hint.uploading");
        }

        return Component.translatable("hint.building").append(Component.literal(String.format("%.2f%%", progress.percentage())));
    }

    private static Component getProgressDetails(ChunkDataBuilder.BuildProgress progress) {
        return Component.translatable(
                "hint.progress",
                progress.processedBlocks(),
                progress.totalBlocks(),
                progress.blocksPerSecond()
        );
    }
}