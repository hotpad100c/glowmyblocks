package mypals.ml.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import mypals.ml.blockOutline.ChunkDataBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class BuildProgressHud {

    private static long completionTime = 0;
    private static boolean shouldFadeOut = false;
    private static long fadeStartTime = 0;
    private static final long DISPLAY_AFTER_COMPLETE_MS = 1000;
    private static final long FADE_DURATION_MS = 2000;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("boss_bar/purple_background");
    private static final Identifier PROGRESS_TEXTURE = Identifier.ofVanilla("boss_bar/purple_progress");
    private static final Identifier NOTCHED_PROGRESS_TEXTURE = Identifier.ofVanilla("boss_bar/notched_20_progress");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    public static void render(DrawContext context) {
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

        client.getProfiler().push("glowMyBlocksProgress");

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        int screenWidth = context.getScaledWindowWidth();
        int x = screenWidth / 2 - BAR_WIDTH / 2;
        int y = 12;

        renderProgressBar(context, x, y, progress.percentage());

        int textAlpha = (int)(alpha * 255) << 24;
        int titleColor = 0x00FFFFFF | textAlpha;
        int detailColor = 0x00AAAAAA | textAlpha;

        Text title = getProgressTitle(progress);
        int titleWidth = client.textRenderer.getWidth(title);
        int titleX = screenWidth / 2 - titleWidth / 2;
        context.drawTextWithShadow(client.textRenderer, title, titleX, y - 9, titleColor);

        if (progress.isBuilding()) {
            Text details = getProgressDetails(progress);
            int detailsWidth = client.textRenderer.getWidth(details);
            int detailsX = screenWidth / 2 - detailsWidth / 2;
            context.drawTextWithShadow(client.textRenderer, details, detailsX, y + BAR_HEIGHT + 2, detailColor);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

        client.getProfiler().pop();
    }

    private static void renderProgressBar(DrawContext context, int x, int y, float percent) {
        percent = MathHelper.clamp(percent, 0f, 100f);
        context.drawGuiTexture(
                BACKGROUND_TEXTURE,
                BAR_WIDTH, BAR_HEIGHT,
                0, 0,
                x, y,
                BAR_WIDTH, BAR_HEIGHT
        );
        int progressWidth = (int)(BAR_WIDTH * percent / 100f);
        if (progressWidth > 0) {
            context.drawGuiTexture(
                    PROGRESS_TEXTURE,
                    BAR_WIDTH, BAR_HEIGHT,
                    0, 0,
                    x, y,
                    progressWidth, BAR_HEIGHT
            );
            context.drawGuiTexture(
                    NOTCHED_PROGRESS_TEXTURE,
                    BAR_WIDTH, BAR_HEIGHT,
                    0, 0,
                    x, y,
                    progressWidth, BAR_HEIGHT
            );
        }
    }

    private static Text getProgressTitle(ChunkDataBuilder.BuildProgress progress) {
        if (progress.percentage() >= 99.99f) {
            return Text.translatable("hint.uploading");
        }

        return Text.translatable("hint.building").append(Text.literal(String.format("%.2f%%", progress.percentage())));
    }

    private static Text getProgressDetails(ChunkDataBuilder.BuildProgress progress) {
        return Text.translatable(
                "hint.progress",
                progress.processedBlocks(),
                progress.totalBlocks(),
                progress.blocksPerSecond()
        );
    }
}