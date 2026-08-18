package mypals.ml.flashback;

import com.google.common.collect.Maps;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import com.moulberry.flashback.spline.CatmullRom;
import com.moulberry.flashback.spline.Hermite;
import imgui.moulberry90.ImGui;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;


public class AreaColorKeyframe extends Keyframe {

    public String areaKey;

    public float red;
    public float green;
    public float blue;
    public float alpha;

    public AreaColorKeyframe(String areaKey, float red, float green, float blue, float alpha) {
        this.areaKey = areaKey == null ? "" : areaKey;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public AreaColorKeyframe(String areaKey, float red, float green, float blue, float alpha,
                             InterpolationType interpolationType) {
        this(areaKey, red, green, blue, alpha);
        this.interpolationType(interpolationType);
    }

    @Override
    public KeyframeType<?> keyframeType() {
        return AreaColorKeyframeType.INSTANCE;
    }

    @Override
    public Keyframe copy() {
        return new AreaColorKeyframe(this.areaKey, this.red, this.green, this.blue, this.alpha, this.interpolationType());
    }

    @Override
    public void renderEditKeyframe(Consumer<Consumer<Keyframe>> update) {
        String picked = renderAreaSelector(this.areaKey);
        if (picked != null) {
            String chosen = picked;
            update.accept(keyframe -> ((AreaColorKeyframe) keyframe).areaKey = chosen);
        }

        float[] rgba = new float[]{ this.red / 255.0f, this.green / 255.0f, this.blue / 255.0f, this.alpha / 255.0f };
        ImGui.setNextItemWidth(200);
        if (ImGui.colorEdit4(I18n.get("glowmyblocks.flashback.area_color"), rgba)) {
            update.accept(keyframe -> {
                AreaColorKeyframe target = (AreaColorKeyframe) keyframe;
                target.red = rgba[0] * 255.0f;
                target.green = rgba[1] * 255.0f;
                target.blue = rgba[2] * 255.0f;
                target.alpha = rgba[3] * 255.0f;
            });
        }
    }

    @Nullable
    static String renderAreaSelector(String currentKey) {
        List<AreaBox> areas = selectedAreas;
        if (areas.isEmpty()) {
            ImGui.text(I18n.get("glowmyblocks.flashback.no_areas"));
            return null;
        }

        String result = null;
        ImGui.setNextItemWidth(260);
        if (ImGui.beginCombo(I18n.get("glowmyblocks.flashback.area"), labelForKey(currentKey))) {
            for (AreaBox area : areas) {
                String key = mypals.ml.blockOutline.AreaColorOverrides.keyOf(area);
                if (ImGui.selectable(describe(area), key.equals(currentKey))) {
                    result = key;
                }
            }
            ImGui.endCombo();
        }
        return result;
    }

    static String describe(AreaBox area) {
        return area.minPos.getX() + "," + area.minPos.getY() + "," + area.minPos.getZ()
                + " -> " + area.maxPos.getX() + "," + area.maxPos.getY() + "," + area.maxPos.getZ();
    }

    private static String labelForKey(String key) {
        if (key == null || key.isEmpty()) {
            return I18n.get("glowmyblocks.flashback.pick_area");
        }
        for (AreaBox area : selectedAreas) {
            if (mypals.ml.blockOutline.AreaColorOverrides.keyOf(area).equals(key)) {
                return describe(area);
            }
        }
        // The area the keyframe was authored against is gone; show the raw key rather than
        // silently pretending it is unbound.
        return key + " (?)";
    }

    @Override
    public @Nullable KeyframeChange createChange() {
        return new AreaColorKeyframeChange(this.areaKey, this.red, this.green, this.blue, this.alpha);
    }

    @Override
    public @Nullable KeyframeChange createSmoothInterpolatedChange(Keyframe p1, Keyframe p2, Keyframe p3,
                                                                   float t0, float t1, float t2, float t3,
                                                                   float amount) {
        AreaColorKeyframe k1 = (AreaColorKeyframe) p1;
        AreaColorKeyframe k2 = (AreaColorKeyframe) p2;
        AreaColorKeyframe k3 = (AreaColorKeyframe) p3;

        float r = CatmullRom.value(this.red, k1.red, k2.red, k3.red, t1 - t0, t2 - t0, t3 - t0, amount);
        float g = CatmullRom.value(this.green, k1.green, k2.green, k3.green, t1 - t0, t2 - t0, t3 - t0, amount);
        float b = CatmullRom.value(this.blue, k1.blue, k2.blue, k3.blue, t1 - t0, t2 - t0, t3 - t0, amount);
        float a = CatmullRom.value(this.alpha, k1.alpha, k2.alpha, k3.alpha, t1 - t0, t2 - t0, t3 - t0, amount);

        return new AreaColorKeyframeChange(this.areaKey, r, g, b, a);
    }

    @Override
    public @Nullable KeyframeChange createHermiteInterpolatedChange(Map<Float, Keyframe> keyframes, float amount) {
        double r = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((AreaColorKeyframe) k).red), amount);
        double g = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((AreaColorKeyframe) k).green), amount);
        double b = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((AreaColorKeyframe) k).blue), amount);
        double a = Hermite.value(Maps.transformValues(keyframes, k -> (double) ((AreaColorKeyframe) k).alpha), amount);

        return new AreaColorKeyframeChange(this.areaKey, (float) r, (float) g, (float) b, (float) a);
    }
}
