package mypals.ml.flashback;

import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import mypals.ml.blockOutline.AreaColorOverrides;

public record AreaColorKeyframeChange(String areaKey, float red, float green, float blue, float alpha) implements KeyframeChange {

    @Override
    public void apply(KeyframeHandler keyframeHandler) {
        if (this.areaKey == null || this.areaKey.isEmpty()) {
            return;
        }
        int r = clampChannel(this.red);
        int g = clampChannel(this.green);
        int b = clampChannel(this.blue);
        int a = clampChannel(this.alpha);
        AreaColorOverrides.set(this.areaKey, (a << 24) | (r << 16) | (g << 8) | b);
    }

    private static int clampChannel(float value) {
        return Math.round(Math.clamp(value, 0.0f, 255.0f));
    }

    @Override
    public KeyframeChange interpolate(KeyframeChange other, double amount) {
        if (!(other instanceof AreaColorKeyframeChange target)) {
            return this;
        }
        return new AreaColorKeyframeChange(
                this.areaKey,
                (float) (this.red + (target.red - this.red) * amount),
                (float) (this.green + (target.green - this.green) * amount),
                (float) (this.blue + (target.blue - this.blue) * amount),
                (float) (this.alpha + (target.alpha - this.alpha) * amount));
    }
}
