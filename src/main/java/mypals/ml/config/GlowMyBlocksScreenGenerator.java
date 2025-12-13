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
                                                    instance.instance();
                                                    if (GlowMyBlocksConfig.selectedBlockTypes == null) {
                                                        GlowMyBlocksConfig.selectedBlockTypes = new ArrayList<>();
                                                    }
                                                    instance.instance();
                                                    return GlowMyBlocksConfig.selectedBlockTypes;
                                                },
                                                list -> {
                                                    instance.instance();
                                                    GlowMyBlocksConfig.selectedBlockTypes = list;
                                                }
                                        )
                                        .controller(StringControllerBuilder::create)
                                        .initial("")
                                        .build()
                                ).option(Option.<Integer>createBuilder()
                                        .name(Text.translatable("config.lucidity.render_mode.rendering_mode_block"))
                                        .description(OptionDescription.of(Text.translatable("config.lucidity.render_mode.rendering_mode_block")))
                                        .binding(0, () -> {
                                            instance.instance();
                                            return GlowMyBlocksConfig.glowBlockMode;
                                        }, v -> {
                                            instance.instance();
                                            GlowMyBlocksConfig.glowBlockMode = v;
                                        })
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
                                                    instance.instance();
                                                    if (GlowMyBlocksConfig.selectedAreasSaved == null) {
                                                        GlowMyBlocksConfig.selectedAreasSaved = new ArrayList<>();
                                                    }
                                                    instance.instance();
                                                    return GlowMyBlocksConfig.selectedAreasSaved;
                                                },
                                                list -> {
                                                    instance.instance();
                                                    GlowMyBlocksConfig.selectedAreasSaved = list;
                                                }
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
                                                        .binding(true, () -> {
                                                            instance.instance();
                                                            return GlowMyBlocksConfig.selectInSpectator;
                                                        }, bool -> {
                                                            instance.instance();
                                                            GlowMyBlocksConfig.selectInSpectator = bool;
                                                        })
                                                        .controller(BooleanControllerBuilder::create)
                                                        .build()
                                        ).option(
                                                Option.<Boolean>createBuilder()
                                                        .name(Text.translatable("config.renderSelectionMarker"))
                                                        .description(OptionDescription.createBuilder()
                                                                .text(Text.translatable("config.description.renderSelectionMarker"))
                                                                .build()
                                                        )
                                                        .binding(false, () -> {
                                                            instance.instance();
                                                            return GlowMyBlocksConfig.renderSelectionMarker;
                                                        }, bool -> {
                                                            instance.instance();
                                                            GlowMyBlocksConfig.renderSelectionMarker = bool;
                                                        })
                                                        .controller(BooleanControllerBuilder::create)
                                                        .build()
                                        ).option(Option.<String>createBuilder()
                                                .name(Text.translatable("config.option.wand"))
                                                .description(OptionDescription.of(Text.translatable("config.description.wand")))
                                                .binding("minecraft:breeze_rod", () -> {
                                                    instance.instance();
                                                    return GlowMyBlocksConfig.wand;
                                                }, s -> {
                                                    instance.instance();
                                                    GlowMyBlocksConfig.wand = s;
                                                })
                                                .controller(opt -> StringControllerBuilder.create(opt))
                                                .build()
                                        ).option(
                                                Option.<Color>createBuilder()
                                                        .name(Text.translatable("config.description.area_color"))
                                                        .binding(Color.white, () -> {
                                                            instance.instance();
                                                            return GlowMyBlocksConfig.areaColor;
                                                        }, color -> {
                                                            instance.instance();
                                                            GlowMyBlocksConfig.areaColor = color;
                                                        })
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
