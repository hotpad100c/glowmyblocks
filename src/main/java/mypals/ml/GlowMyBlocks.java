package mypals.ml;

import mypals.ml.blockOutline.OutlineManager;
import mypals.ml.config.GlowMyBlocksConfig;
import mypals.ml.config.GlowMyBlocksKeybinds;
import mypals.ml.config.GlowMyBlocksScreenGenerator;
import mypals.ml.wandSystem.WandTooltipRenderer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mypals.ml.config.GlowMyBlocksKeybinds.openConfigKey;
import static mypals.ml.wandSystem.SelectedManager.*;
import static mypals.ml.wandSystem.WandActionsManager.wandActions;

public class GlowMyBlocks implements ModInitializer {
	public static final String MOD_ID = "glowmyblocks";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
	@Override
	public void onInitialize() {
		GlowMyBlocksKeybinds.init();
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			WandTooltipRenderer.renderWandTooltip(context);
		});
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			updateConfig();
		});
		WorldRenderEvents.AFTER_ENTITIES.register((context) ->{
			OutlineManager.resolveBlocks();
			OutlineManager.init();
			OutlineManager.onRenderWorldLast(context);

		});
		ClientTickEvents.END_CLIENT_TICK.register(client-> {
			wandActions(client);
			while (openConfigKey.wasPressed()) {
				client.setScreen(GlowMyBlocksScreenGenerator.getConfigScreen(client.currentScreen));
			}
		});
		UseBlockCallback.EVENT.register((player, world, hand, pos) -> {
			if (world.isClient && player.getStackInHand(hand).getItem() == wand && player.isCreative()) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			if (world.isClient && player.getStackInHand(hand).getItem() == wand && player.isCreative()) {
				return ActionResult.FAIL;
			}
			return ActionResult.PASS;
		});
	}
	public static void onConfigUpdated() {
		try {
			updateConfig();
		}catch (Exception e){
			LOGGER.error(e.toString());
			e.printStackTrace();
		}
	}
	public static void updateConfig() {
		var instance = GlowMyBlocksConfig.CONFIG_HANDLER;
		instance.load();
		resolveSettings();
	}
	private static void resolveSettings(){
		resolveSelectedAreasFromString(GlowMyBlocksConfig.selectedAreasSaved);
		resolveSelectedWandFromString(GlowMyBlocksConfig.wand);
	}
}