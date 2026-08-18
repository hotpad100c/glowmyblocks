package mypals.ml.flashback;

import com.moulberry.flashback.editor.ui.ReplayUI;
import com.moulberry.flashback.keyframe.KeyframeType;
import com.moulberry.flashback.keyframe.change.KeyframeChange;
import com.moulberry.flashback.keyframe.handler.KeyframeHandler;
import com.moulberry.flashback.keyframe.handler.MinecraftKeyframeHandler;
import imgui.moulberry90.ImGui;
import mypals.ml.blockOutline.AreaColorOverrides;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.client.resources.language.I18n;

import java.util.List;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class AreaColorKeyframeType implements KeyframeType<AreaColorKeyframe> {

    public static final String ID = "glowmyblocks_area_color";

    public static final AreaColorKeyframeType INSTANCE = new AreaColorKeyframeType();

    private AreaColorKeyframeType() {}

    @Override
    public Class<? extends KeyframeChange> keyframeChangeType() {
        return AreaColorKeyframeChange.class;
    }

    @Override
    public boolean allowApplyingDuplicateKeyframeChanges(){
        return true;
    }

    @Override
    public boolean supportsHandler(KeyframeHandler keyframeHandler) {
        return keyframeHandler instanceof MinecraftKeyframeHandler;
    }

    @Override
    public String icon() {
        return "C";
    }

    @Override
    public String name() {
        return I18n.get("glowmyblocks.flashback.keyframe.area_color");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public AreaColorKeyframe createDirect() {
        List<AreaBox> areas = selectedAreas;
        if (areas.isEmpty()) {
            return new AreaColorKeyframe("", 255.0f, 255.0f, 255.0f, 255.0f);
        }
        AreaBox first = areas.getFirst();
        java.awt.Color color = AreaColorOverrides.effectiveColor(first);
        return new AreaColorKeyframe(AreaColorOverrides.keyOf(first),
                color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    @Override
    public KeyframeCreatePopup<AreaColorKeyframe> createPopup() {
        List<AreaBox> areas = selectedAreas;

        // Seeded from the first area so the popup opens on something sensible; the combo below
        // lets you pick a different one before adding.
        String[] areaKey = new String[]{ areas.isEmpty() ? "" : AreaColorOverrides.keyOf(areas.get(0)) };
        java.awt.Color seed = areas.isEmpty() ? java.awt.Color.WHITE : AreaColorOverrides.effectiveColor(areas.get(0));
        float[] rgba = new float[]{
                seed.getRed() / 255.0f  ,
                seed.getGreen() / 255.0f,
                seed.getBlue() / 255.0f ,
                seed.getAlpha() / 255.0f
        };

        return () -> {
            String picked = AreaColorKeyframe.renderAreaSelector(areaKey[0]);
            if (picked != null) {
                areaKey[0] = picked;
            }
            ImGui.setNextItemWidth(200);
            ImGui.colorEdit4(I18n.get("glowmyblocks.flashback.area_color"), rgba);

            if (ImGui.button(I18n.get("flashback.add")) || ReplayUI.consumeConfirm()) {
                return new AreaColorKeyframe(areaKey[0],
                        rgba[0] * 255.0f, rgba[1] * 255.0f, rgba[2] * 255.0f, rgba[3] * 255.0f);
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.get("gui.cancel")) || ReplayUI.consumeCancel()) {
                ImGui.closeCurrentPopup();
            }
            return null;
        };
    }
}
