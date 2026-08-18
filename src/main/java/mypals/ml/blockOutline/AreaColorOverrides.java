package mypals.ml.blockOutline;

import mypals.ml.wandSystem.AreaBox;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AreaColorOverrides {

    private AreaColorOverrides() {
    }

    private static final Map<String, Integer> OVERRIDES = new ConcurrentHashMap<>();

    public static String keyOf(AreaBox area) {
        return keyOf(area.minPos, area.maxPos);
    }

    public static String keyOf(BlockPos min, BlockPos max) {
        return min.getX() + "," + min.getY() + "," + min.getZ()
                + ":" + max.getX() + "," + max.getY() + "," + max.getZ();
    }

    public static void set(String areaKey, int rgb) {
        OVERRIDES.put(areaKey, rgb);
    }

    public static void clear(String areaKey) {
        OVERRIDES.remove(areaKey);
    }

    public static void clearAll() {
        OVERRIDES.clear();
    }

    public static boolean isEmpty() {
        return OVERRIDES.isEmpty();
    }

    @Nullable
    public static Integer get(String areaKey) {
        return OVERRIDES.get(areaKey);
    }

    public static Vector4f modulatorFor(AreaBox area) {
        Integer override = OVERRIDES.get(keyOf(area));
        int rgba = override != null ? override : area.color.getRGB();
        return new Vector4f(
                ((rgba >> 16) & 0xFF) / 255.0F,
                ((rgba >> 8) & 0xFF) / 255.0F,
                (rgba & 0xFF) / 255.0F,
                ((rgba >> 24) & 0xFF) / 255.0F);
    }

    public static Color effectiveColor(AreaBox area) {
        Integer override = OVERRIDES.get(keyOf(area));
        return override != null ? new Color(override, true) : area.color;
    }
}
