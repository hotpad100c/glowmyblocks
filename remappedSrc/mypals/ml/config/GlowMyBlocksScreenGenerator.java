package mypals.ml.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import mypals.ml.GlowMyBlocks;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.awt.*;
import java.util.ArrayList;

import static mypals.ml.config.GlowModeManager.resolveSelectiveBlockRenderingMode;

public class GlowMyBlocksScreenGenerator {
    public static Screen getConfigScreen(Screen screen){
        var instance = GlowMyBlocksConfig.CONFIG_HANDLER;
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.title"))
                .category(
                        ConfigCategory.createBuilder()
                                .name(Component.translatable("config.category.blockOutline"))
                                //==================================================
                                .group(ListOption.<String>createBuilder()
                                        .name(Component.translatable("config.category.selectedBlock"))
                                        .description
                                                (OptionDescription.createBuilder()
                                                        .text(Component.translatable("config.description.blockOutline"))

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
                                        .name(Component.translatable("config.blockOutline.render_mode"))
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
                                                .formatValue(val -> Component.translatable(resolveSelectiveBlockRenderingMode(val))))
                                        .build()
                                ).group(ListOption.<String>createBuilder()
                                        .name(Component.translatable("config.option.selectedAreaRender"))
                                        .description
                                                (OptionDescription.createBuilder()
                                                        .text(Component.translatable("config.description.selectiveAreaRender"))
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
                                        .name(Component.translatable("config.category.blockOutline"))
                                        .description(
                                                OptionDescription.createBuilder()
                                                        .text(Component.translatable("config.description.blockOutline"))
                                                        .build()
                                        ).option(
                                                Option.<Boolean>createBuilder()
                                                        .name(Component.translatable("config.option.spectatorSelect"))
                                                        .description(OptionDescription.createBuilder()
                                                                .text(Component.translatable("config.description.spectatorSelect"))
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
                                                        .name(Component.translatable("config.option.renderSelectionMarker"))
                                                        .description(OptionDescription.createBuilder()
                                                                .text(Component.translatable("config.description.renderSelectionMarker"))
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
                                                .name(Component.translatable("config.option.wand"))
                                                .description(OptionDescription.of(Component.translatable("config.description.wand")))
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
                                                        .name(Component.translatable("config.description.area_color"))
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
