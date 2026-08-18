package mypals.ml.flashback;

import com.moulberry.flashback.keyframe.KeyframeRegistry;

final class FlashbackIntegration {

    private FlashbackIntegration() {}

    static void register() {
        KeyframeRegistry.register(AreaColorKeyframeType.INSTANCE);
    }
}
