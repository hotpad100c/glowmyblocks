package mypals.ml.mixin.compat.flashback;

import com.google.gson.GsonBuilder;
import com.moulberry.flashback.FlashbackGson;
import mypals.ml.flashback.AreaColorKeyframe;
import mypals.ml.flashback.AreaColorKeyframeSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Registers our keyframe class with Flashback's Gson.
 *
 * <p>Flashback writes keyframes in several places through the single-argument
 * {@code context.serialize(keyframe)}, where Gson picks an adapter by runtime type. Every keyframe
 * class it ships is registered here; one that is not falls back to reflection and comes out without
 * a {@code "type"}, which makes reading it back fail with "Unable to determine type of keyframe"
 * and takes down the whole EditorState load.
 *
 * <p>{@code build()} hands back a GsonBuilder that has not been created yet, and it is called once
 * for PRETTY and once for COMPRESSED, so adding the registration on the way out covers both.
 */
@Pseudo
@Mixin(FlashbackGson.class)
public class FlashbackGsonMixin {

    @Inject(method = "build", at = @At("RETURN"), remap = false)
    private static void glowmyblocks$registerKeyframeAdapter(CallbackInfoReturnable<GsonBuilder> cir) {
        GsonBuilder builder = cir.getReturnValue();
        if (builder != null) {
            builder.registerTypeAdapter(AreaColorKeyframe.class, AreaColorKeyframeSerializer.INSTANCE);
        }
    }
}
