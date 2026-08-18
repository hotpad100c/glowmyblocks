package mypals.ml.flashback;

import mypals.ml.GlowMyBlocks;
import net.fabricmc.loader.api.FabricLoader;

public final class FlashbackCompat {

    public static final String FLASHBACK_MOD_ID = "flashback";

    private static boolean registered = false;

    private FlashbackCompat() {}

    public static boolean isFlashbackLoaded() {
        return FabricLoader.getInstance().isModLoaded(FLASHBACK_MOD_ID);
    }

    public static boolean isRegistered() {
        return registered;
    }

    public static void init() {
        if (!isFlashbackLoaded()) {
            return;
        }
        try {
            FlashbackIntegration.register();
            registered = true;
            GlowMyBlocks.LOGGER.info("Flashback detected: registered the area colour keyframe");
        } catch (Throwable throwable) {
            GlowMyBlocks.LOGGER.warn("Failed to register the area colour keyframe with Flashback", throwable);
        }
    }
}
