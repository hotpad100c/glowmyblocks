package mypals.ml.config;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import mypals.ml.GlowMyBlocks;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GlowMyBlocksConfig {
    public static ConfigClassHandler<GlowMyBlocksConfig> CONFIG_HANDLER = ConfigClassHandler.createBuilder(GlowMyBlocksConfig.class)
            .id(GlowMyBlocks.id("config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("GlowMyBlocksConfig.json5"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public static List<String> selectedBlockTypes = new ArrayList<>();
    @SerialEntry
    public static List<String> selectedAreasSaved = new ArrayList<>();
    @SerialEntry
    public static Boolean selectInSpectator = true;
    @SerialEntry
    public static String wand = "minecraft:blaze_rod";
    @SerialEntry
    public static Color areaColor = Color.white;
    @SerialEntry
    public static boolean renderSelectionMarker = false;
}
