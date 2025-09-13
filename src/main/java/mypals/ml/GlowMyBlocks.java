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
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mypals.ml.config.GlowMyBlocksKeybinds.openConfigKey;
import static mypals.ml.wandSystem.SelectedManager.*;
import static mypals.ml.wandSystem.WandActionsManager.wandActions;

public class GlowMyBlocks implements ModInitializer {
	public static final String MOD_ID = "glowmyblocks";
	public static boolean needRebuildOutlineMesh = false;
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
	public static void renderBlockOutlines(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
		if(needRebuildOutlineMesh) {
			OutlineManager.buildMeshes(counter);
		}
		OutlineManager.renderBlockEntities(stack,counter, projectionMatrix);
		OutlineManager.renderBlocks(stack,counter, projectionMatrix);
	}
	public static void renderBlockEntitiesOutlines(MatrixStack stack, RenderTickCounter counter, Matrix4f projectionMatrix) {
		if(needRebuildOutlineMesh) {
			OutlineManager.buildMeshes(counter);
		}
		//OutlineManager.renderBlockEntities(stack,counter, projectionMatrix);
	}
	@Override
	public void onInitialize() {
		GlowMyBlocksKeybinds.init();
		needRebuildOutlineMesh = true;
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			WandTooltipRenderer.renderWandTooltip(context);
		});
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			updateConfig();
		});
		WorldRenderEvents.AFTER_ENTITIES.register((context) ->{
			renderBlockOutlines(context.matrixStack(), context.tickCounter(),context.projectionMatrix());
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
		needRebuildOutlineMesh = true;
	}
	private static void resolveSettings(){
		OutlineManager.resolveBlocks();
		resolveSelectedAreasFromString(GlowMyBlocksConfig.selectedAreasSaved);
		resolveSelectedWandFromString(GlowMyBlocksConfig.wand);
	}
}