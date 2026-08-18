package mypals.ml.flashback;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * JSON reading and writing for {@link AreaColorKeyframe}.
 *
 * <p>Why this has to be a Gson adapter for the concrete class, and not just an injection into
 * {@code Keyframe.TypeAdapter}: several places in Flashback (notably
 * {@code EditorSceneHistoryAction.SetKeyframe}) write a keyframe through the single-argument
 * {@code context.serialize(keyframe)}, and Gson then picks an adapter by <b>runtime type</b>.
 * Every keyframe class Flashback ships is registered individually in {@code FlashbackGson}, and
 * each of those adapters writes its own {@code "type"}. A class that is not registered falls back
 * to reflection: the fields come out fine but {@code "type"} is missing, and reading it back blows
 * up in {@code Keyframe.TypeAdapter} with "Unable to determine type of keyframe", taking the whole
 * EditorState load with it.
 *
 * <p>So this class is both the adapter registered with Gson and the implementation shared by the
 * {@code Keyframe.class}-typed path in {@code KeyframeTypeAdapterMixin} — one format, written in
 * one place.
 */
public final class AreaColorKeyframeSerializer
        implements JsonSerializer<AreaColorKeyframe>, JsonDeserializer<AreaColorKeyframe> {

    public static final AreaColorKeyframeSerializer INSTANCE = new AreaColorKeyframeSerializer();

    private AreaColorKeyframeSerializer() {}

    public static JsonObject write(AreaColorKeyframe keyframe, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("type", AreaColorKeyframeType.ID);
        json.addProperty("area", keyframe.areaKey);
        json.addProperty("red", keyframe.red);
        json.addProperty("green", keyframe.green);
        json.addProperty("blue", keyframe.blue);
        json.addProperty("alpha", keyframe.alpha);
        json.add("interpolation_type", context.serialize(keyframe.interpolationType()));
        return json;
    }

    public static AreaColorKeyframe read(JsonObject json, JsonDeserializationContext context) {
        String areaKey = json.has("area") ? json.get("area").getAsString() : "";
        float red = json.has("red") ? json.get("red").getAsFloat() : 255.0f;
        float green = json.has("green") ? json.get("green").getAsFloat() : 255.0f;
        float blue = json.has("blue") ? json.get("blue").getAsFloat() : 255.0f;
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 255.0f;
        InterpolationType interpolationType = json.has("interpolation_type")
                ? context.deserialize(json.get("interpolation_type"), InterpolationType.class)
                : InterpolationType.getDefault();

        return new AreaColorKeyframe(areaKey, red, green, blue, alpha, interpolationType);
    }

    /**
     * Recovers keyframes written before the adapter above was registered.
     *
     * <p>Back then Flashback's single-argument {@code context.serialize} dumped our keyframes
     * reflectively: no {@code "type"}, and the field names rather than our JSON names — most
     * tellingly {@code areaKey} instead of {@code area} and {@code interpolationType} instead of
     * {@code interpolation_type}. Such a file now fails the whole EditorState load, so recognise it
     * by those two markers and read it as ours. Returns null when it does not match, leaving
     * Flashback's own error path intact.
     *
     * <p>This only exists for files already on disk; everything written from now on carries
     * {@code "type"}. Safe to delete once those are gone.
     */
    @Nullable
    public static AreaColorKeyframe readLegacyReflective(JsonObject json, JsonDeserializationContext context) {
        if (json.has("type") || !json.has("areaKey") || !json.has("interpolationType")) {
            return null;
        }
        try {
            String areaKey = json.get("areaKey").getAsString();
            float red = json.has("red") ? json.get("red").getAsFloat() : 255.0f;
            float green = json.has("green") ? json.get("green").getAsFloat() : 255.0f;
            float blue = json.has("blue") ? json.get("blue").getAsFloat() : 255.0f;
            float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 255.0f;
            InterpolationType interpolationType =
                    context.deserialize(json.get("interpolationType"), InterpolationType.class);
            return new AreaColorKeyframe(areaKey, red, green, blue, alpha,
                    interpolationType == null ? InterpolationType.getDefault() : interpolationType);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public JsonElement serialize(AreaColorKeyframe keyframe, Type type, JsonSerializationContext context) {
        return write(keyframe, context);
    }

    @Override
    public AreaColorKeyframe deserialize(JsonElement element, Type type, JsonDeserializationContext context)
            throws JsonParseException {
        return read(element.getAsJsonObject(), context);
    }
}
