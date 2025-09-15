package mypals.ml.config;

import mypals.ml.blockOutline.OutlineManager;
import mypals.ml.wandSystem.AreaBox;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class GlowModeManager {
    public static Map<Integer, Map<Property,Object>> selectedBlockTypes = new HashMap<>();
    public static GlowRenderMode currentGlowRenderMode = GlowRenderMode.OFF;
    public enum GlowRenderMode {
        OFF("config.blockOutline.render_mode.off","textures/gui/rendering_mode/render_mode_off.png"),
        GLOW_INSIDE_ONLY_SPECIFIC("config.blockOutline.render_mode.render_inside_include","textures/gui/rendering_mode/render_mode_1.png"),
        GLOW_INSIDE_EXCLUDE_SPECIFIC("config.blockOutline.render_mode.render_inside_exclude","textures/gui/rendering_mode/render_mode_3.png"),
        GLOW_INSIDE_ALL("config.blockOutline.render_mode.render_inside_all","textures/gui/rendering_mode/render_mode_7.png");
        private final String translationKey;
        private final String icon;

        GlowRenderMode(String translationKey, String icon) {
            this.translationKey = translationKey;
            this.icon = icon;
        }
        public String getTranslationKey() {
            return translationKey;
        }
        public String getIcon() {
            return icon;
        }
    }
    public static String resolveSelectiveBlockRenderingMode(int index) {
        return resolveSelectiveRenderingMode(index, GlowRenderMode.values(), mode -> currentGlowRenderMode = mode);
    }
    public static String resolveSelectiveRenderingMode(int index, GlowRenderMode[] modes, Consumer<GlowRenderMode> modeSetter) {
        if (index >= 0 && index < modes.length) {
            GlowRenderMode mode = modes[index];
            modeSetter.accept(mode);
            return mode.getTranslationKey();
        }
        return "-";
    }
    public static void resolveSelectedBlockStatesFromString(List<String> blockStrings) {
        selectedBlockTypes.clear();

        GlowMyBlocksConfig.CONFIG_HANDLER.instance();
        blockStrings.forEach(blockString -> {
            try {
                blockString = blockString.replace(" ","");
                String[] parts = blockString.split("\\[", 2);
                String blockIdString = parts[0];
                if (!blockIdString.contains(":")) {
                    blockIdString = "minecraft:" + blockIdString;
                }

                Identifier blockId = Identifier.of(blockIdString);
                Block block = Registries.BLOCK.get(blockId);

                Map<Property,Object> states = new HashMap<>();
                boolean hasState = false;
                if (parts.length > 1) {
                    String propertiesString = parts[1].replace("]", "");
                    String[] properties = propertiesString.split(",");

                    for (String property : properties) {
                        String[] keyValue = property.split("=");
                        if (keyValue.length != 2) continue;

                        String key = keyValue[0];
                        String value = keyValue[1];

                        Property<?> blockProperty = block.getStateManager().getProperty(key);
                        if (blockProperty != null) {
                            states.put(blockProperty,value);
                            hasState = true;
                        }
                    }
                }

                if(hasState) {

                    selectedBlockTypes.put(Registries.BLOCK.getRawId(block), states);
                }else{
                    selectedBlockTypes.put(Registries.BLOCK.getRawId(block), null);
                }

            } catch (Exception e) {
                System.err.println("Failed to parse block state: " + blockString);
            }
        });
    }
    public static boolean isSelectedArea(BlockPos pos){
        for(AreaBox box : selectedAreas){
            if(OutlineManager.isBlockInsideArea(pos, box)){
                return true;
            }
        }
        return false;
    }
    public boolean isGlowModeActive() {
        return currentGlowRenderMode != GlowRenderMode.OFF;
    }
    public static boolean isSelectedTypeAndState(BlockState state, Map<Integer, Map<Property, Object>> selectedTypes) {
        for (Map.Entry<Integer, Map<Property, Object>> entry : selectedTypes.entrySet()) {
            Integer blockId = entry.getKey();
            Map<Property, Object> properties = entry.getValue();

            if (!blockId.equals(Registries.BLOCK.getRawId(state.getBlock()))) {
                continue;
            }
            if(properties == null || properties.isEmpty()){
                if(Registries.BLOCK.getRawId(state.getBlock()) == blockId){
                    return true;
                }
            }

            boolean hasAllProperties = true;
            for (Map.Entry<Property, Object> property : properties.entrySet()) {
                Optional<Object> stateProperty = state.getOrEmpty(property.getKey());

                if (!stateProperty.isPresent()) {
                    hasAllProperties = false;
                    break;
                }

                if (!stateProperty.get().toString().equals(property.getValue())) {
                    hasAllProperties = false;
                    break;
                }
            }

            if (hasAllProperties) {
                return true;
            }
        }
        return false;
    }
    public static boolean shouldGlow(BlockPos pos, BlockState state, @Nullable AreaBox areaBox){
        if (currentGlowRenderMode == GlowRenderMode.OFF) {
            return false;
        }
        boolean isSelected = isSelectedTypeAndState(state, selectedBlockTypes);

        boolean isInArea = areaBox == null? isSelectedArea(pos) : OutlineManager.isBlockInsideArea(pos, areaBox);

        switch (currentGlowRenderMode) {
            case GLOW_INSIDE_ONLY_SPECIFIC:
                return isInArea && isSelected;

            case GLOW_INSIDE_EXCLUDE_SPECIFIC:
                return isInArea && !isSelected;

            case GLOW_INSIDE_ALL:
                return isInArea;

            default:
                return true;
        }
    }
}
