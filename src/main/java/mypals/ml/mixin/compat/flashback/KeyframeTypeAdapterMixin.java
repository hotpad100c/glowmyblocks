package mypals.ml.mixin.compat.flashback;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.moulberry.flashback.keyframe.Keyframe;
import com.moulberry.flashback.keyframe.interpolation.InterpolationType;
import mypals.ml.flashback.AreaColorKeyframe;
import mypals.ml.flashback.AreaColorKeyframeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Pseudo
@Mixin(Keyframe.TypeAdapter.class)
public class KeyframeTypeAdapterMixin {

    @Inject(
            method = "serialize(Lcom/moulberry/flashback/keyframe/Keyframe;Ljava/lang/reflect/Type;Lcom/google/gson/JsonSerializationContext;)Lcom/google/gson/JsonElement;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void glowmyblocks$serialize(Keyframe keyframe, Type type, JsonSerializationContext context,
                                        CallbackInfoReturnable<JsonElement> cir) {
        if (!(keyframe instanceof AreaColorKeyframe areaColorKeyframe)) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", AreaColorKeyframeType.ID);
        json.addProperty("area", areaColorKeyframe.areaKey);
        json.addProperty("red", areaColorKeyframe.red);
        json.addProperty("green", areaColorKeyframe.green);
        json.addProperty("blue", areaColorKeyframe.blue);
        json.addProperty("alpha", areaColorKeyframe.alpha);
        json.add("interpolation_type", context.serialize(areaColorKeyframe.interpolationType()));
        cir.setReturnValue(json);
    }

    @Inject(
            method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lcom/moulberry/flashback/keyframe/Keyframe;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void glowmyblocks$deserialize(JsonElement element, Type type, JsonDeserializationContext context,
                                          CallbackInfoReturnable<Keyframe> cir) {
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (!json.has("type") || !AreaColorKeyframeType.ID.equals(json.get("type").getAsString())) {
            return;
        }

        String areaKey = json.has("area") ? json.get("area").getAsString() : "";
        float red = json.has("red") ? json.get("red").getAsFloat() : 255.0f;
        float green = json.has("green") ? json.get("green").getAsFloat() : 255.0f;
        float blue = json.has("blue") ? json.get("blue").getAsFloat() : 255.0f;
        float alpha = json.has("alpha") ? json.get("alpha").getAsFloat() : 255.0f;
        InterpolationType interpolationType = json.has("interpolation_type")
                ? context.deserialize(json.get("interpolation_type"), InterpolationType.class)
                : InterpolationType.getDefault();

        cir.setReturnValue(new AreaColorKeyframe(areaKey, red, green, blue, alpha, interpolationType));
    }
}
