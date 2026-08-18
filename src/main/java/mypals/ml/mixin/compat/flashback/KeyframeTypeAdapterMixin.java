package mypals.ml.mixin.compat.flashback;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.moulberry.flashback.keyframe.Keyframe;
import mypals.ml.flashback.AreaColorKeyframe;
import mypals.ml.flashback.AreaColorKeyframeSerializer;
import mypals.ml.flashback.AreaColorKeyframeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

/**
 * Handles keyframes read and written explicitly as {@code Keyframe.class}; the runtime-type path is
 * covered by the adapter {@link FlashbackGsonMixin} registers. Both share one implementation so the
 * two formats cannot drift apart.
 */
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
        cir.setReturnValue(AreaColorKeyframeSerializer.write(areaColorKeyframe, context));
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
        if (!json.has("type")) {
            // Written reflectively back when the adapter was missing; recover it if we recognise it.
            AreaColorKeyframe recovered = AreaColorKeyframeSerializer.readLegacyReflective(json, context);
            if (recovered != null) {
                cir.setReturnValue(recovered);
            }
            return;
        }
        if (!AreaColorKeyframeType.ID.equals(json.get("type").getAsString())) {
            return;
        }

        cir.setReturnValue(AreaColorKeyframeSerializer.read(json, context));
    }
}
