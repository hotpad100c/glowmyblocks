package mypals.ml.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import mypals.ml.GlowMyBlocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;

import static mypals.ml.config.GlowModeManager.resolveSelectiveBlockRenderingMode;

public class GlowMyBlocksScreenGenerator {
    public static Screen getConfigScreen(Screen screen){
        var instance = GlowMyBlocksConfig.CONFIG_HANDLER;
        return YetAnotherConfigLib.createBuilder()
                .title(Text.translatable("config.title"))
                .category(
                        ConfigCategory.createBuilder()
                                .name(Text.translatable("config.category.blockOutline"))
                                //==================================================
                                .group(ListOption.<String>createBuilder()
                                        .name(Text.translatable("config.category.selectedBlock"))
                                        .description
                                                (OptionDescription.createBuilder()
                                                        .text(Text.translatable("config.description.blockOutline"))

                                                        .build()
                                                )
                                        .binding(
                                                new ArrayList<>(),
                                                () -> {
                                                    if (instance.instance().selectedBlockTypes == null) {
                                                        GlowMyBlocksConfig.selectedBlockTypes = new ArrayList<>();
                                                    }
                                                    return instance.instance().selectedBlockTypes;
                                                },
                                                list -> instance.instance().selectedBlockTypes = list
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .initial("")
                                        .build()
                                ).option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("config.lucidity.render_mode.rendering_mode_block"))
                                        .description(OptionDescription.of(Text.translatable("config.lucidity.render_mode.rendering_mode_block")))
                                        .binding(0, () -> instance.instance().glowBlockMode, v -> instance.instance().glowBlockMode = v)
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, GlowModeManager.GlowRenderMode.values().length-1)
                                                .step(1)
                                                .formatValue(val -> Text.translatable(resolveSelectiveBlockRenderingMode(val))))
                                        .build()
                                ).group(ListOption.<String>createBuilder()
                                        .name(Text.translatable("config.option.selectedAreaRender"))
                                        .description
                                                (OptionDescription.createBuilder()
                                                        .text(Text.translatable("config.description.selectiveAreaRender"))
                                                        .build()
                                                )
                                        .binding(
                                                new ArrayList<>(),
                                                () -> {
                                                    if (instance.instance().selectedAreasSaved == null) {
                                                        GlowMyBlocksConfig.selectedAreasSaved = new ArrayList<>();
                                                    }
                                                    return instance.instance().selectedAreasSaved;
                                                },
                                                list -> instance.instance().selectedAreasSaved = list
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .initial("")
                                        .build()
                                )
                                //==================================================
                                .group(OptionGroup.createBuilder()
                                        .name(Text.translatable("config.category.blockOutline"))
                                        .description(
                                                OptionDescription.createBuilder()
                                                        .text(Text.translatable("config.description.blockOutline"))
                                                        .build()
                                        ).option(
                                                Option.<Boolean>createBuilder()
                                                        .name(Text.translatable("config.option.spectator_select"))
                                                        .description(OptionDescription.createBuilder()
                                                                .text(Text.translatable("config.description.spectator_select"))
                                                                .build()
                                                        )
                                                        .binding(true, () -> instance.instance().selectInSpectator, bool -> instance.instance().selectInSpectator = bool)
                                                        .controller(BooleanControllerBuilder::create)
                                                        .build()
                                        ).option(
                                                Option.<Boolean>createBuilder()
                                                        .name(Text.translatable("config.renderSelectionMarker"))
                                                        .description(OptionDescription.createBuilder()
                                                                .text(Text.translatable("config.description.renderSelectionMarker"))
                                                                .build()
                                                        )
                                                        .binding(false, () -> instance.instance().renderSelectionMarker, bool -> instance.instance().renderSelectionMarker = bool)
                                                        .controller(BooleanControllerBuilder::create)
                                                        .build()
                                        ).option(Option.<String>createBuilder()
                                                .name(Text.translatable("config.option.wand"))
                                                .description(OptionDescription.of(Text.translatable("config.description.wand")))
                                                .binding("minecraft:breeze_rod", () -> instance.instance().wand, s -> instance.instance().wand = s)
                                                .controller(opt -> StringControllerBuilder.create(opt))
                                                .build()
                                        ).option(
                                                Option.<Color>createBuilder()
                                                        .name(Text.translatable("config.description.area_color"))
                                                        .binding(Color.white, () -> instance.instance().areaColor, color -> instance.instance().areaColor = color)
                                                        .controller(opt -> ColorControllerBuilder.create(opt)
                                                                .allowAlpha(false))
                                                        .build()
                                        ).build()
                                )

                                .build()
                ).save(() -> {
                    instance.save();
                    GlowMyBlocks.onConfigUpdated();
                })
                .build()
                .generateScreen(screen);
    }
}
