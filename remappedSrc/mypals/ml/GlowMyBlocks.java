package mypals.ml;

import mypals.ml.blockOutline.ChunkDataBuilder;
import mypals.ml.blockOutline.OutlineManager;
import mypals.ml.config.GlowMyBlocksConfig;
import mypals.ml.config.GlowMyBlocksKeybinds;
import mypals.ml.config.GlowMyBlocksScreenGenerator;
import mypals.ml.hud.BuildProgressHud;
import mypals.ml.hud.ColorPickerScreen;
import mypals.ml.wandSystem.SelectedManager;
import mypals.ml.wandSystem.WandActionsManager;
import mypals.ml.wandSystem.WandTooltipRenderer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static mypals.ml.blockOutline.OutlineManager.areaVbos;
import static mypals.ml.config.GlowModeManager.resolveSelectedBlockStatesFromString;
import static mypals.ml.config.GlowMyBlocksKeybinds.openColorPickerKey;
import static mypals.ml.config.GlowMyBlocksKeybinds.openConfigKey;
import static mypals.ml.wandSystem.SelectedManager.*;
import static mypals.ml.wandSystem.WandActionsManager.wandActions;

import com.mojang.blaze3d.vertex.PoseStack;

public class GlowMyBlocks implements ModInitializer {
	public static final String MOD_ID = "glowmyblocks";
	public static boolean needRebuildOutlineMesh = false;
	public static boolean finishedLoadingWorld = false;
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
	public static void renderBlockOutlines(PoseStack stack, DeltaTracker counter, Matrix4f projectionMatrix) {
		if(needRebuildOutlineMesh) {
			OutlineManager.buildMeshes(counter);
		}

		//OutlineManager.renderBlocks(stack,counter, projectionMatrix);
	}
	public static void renderBlockEntitiesOutlines(PoseStack stack, DeltaTracker counter, Matrix4f projectionMatrix) {
		if(needRebuildOutlineMesh) {
			OutlineManager.buildMeshes(counter);
		}
		OutlineManager.renderBlocks(stack,counter, projectionMatrix);

		OutlineManager.renderBlockEntities(stack,counter, projectionMatrix);
	}
	@Override
	public void onInitialize() {
		GlowMyBlocksKeybinds.init();
		needRebuildOutlineMesh = true;
		HudRenderCallback.EVENT.register((context, tickDelta) -> {
			WandTooltipRenderer.renderWandTooltip(context);
			WandTooltipRenderer.renderWandModeIcon(context);
		});
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			updateConfig();
		});
		WorldRenderEvents.AFTER_ENTITIES.register((context) ->{
			renderBlockEntitiesOutlines(context.matrixStack(), context.tickCounter(),context.projectionMatrix());
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			areaVbos.values().forEach(data -> {
				data.sectionData.values().forEach(chunk -> {
					if (chunk.vbo != null) chunk.vbo.close();
				});
			});
			areaVbos.clear();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ChunkDataBuilder.shutdown();
		});
		HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
			BuildProgressHud.render(ctx);
		});
		ClientTickEvents.END_CLIENT_TICK.register(client-> {
			wandActions(client);
			while (openConfigKey.consumeClick()) {
				client.setScreen(GlowMyBlocksScreenGenerator.getConfigScreen(client.screen));
			}
			if(!finishedLoadingWorld){
				if(client.getConnection() != null && client.getConnection().levelLoadStatusManager != null){
					if(client.getConnection().levelLoadStatusManager.levelReady()){
						finishedLoadingWorld = true;
						resolveSettings();
						needRebuildOutlineMesh = true;
					}
				}
			}
		});
		UseBlockCallback.EVENT.register((player, world, hand, pos) -> {
			if (world.isClientSide && player.getItemInHand(hand).getItem() == wand && player.isCreative()) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			if (world.isClientSide && player.getItemInHand(hand).getItem() == wand && player.isCreative()) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
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
		resolveSelectedBlockStatesFromString(GlowMyBlocksConfig.selectedBlockTypes);
		resolveSelectedAreasFromString(GlowMyBlocksConfig.selectedAreasSaved);
		resolveSelectedWandFromString(GlowMyBlocksConfig.wand);
		OutlineManager.resolveBlocks();
	}
}