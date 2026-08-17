package mypals.ml.wandSystem;

import mypals.ml.config.GlowModeManager;
import mypals.ml.config.GlowMyBlocksConfig;
import mypals.ml.hud.ColorPickerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static mypals.ml.GlowMyBlocks.onConfigUpdated;
import static mypals.ml.config.GlowModeManager.currentGlowRenderMode;
import static mypals.ml.config.GlowModeManager.resolveSelectiveBlockRenderingMode;
import static mypals.ml.config.GlowMyBlocksConfig.*;
import static mypals.ml.config.GlowMyBlocksKeybinds.*;
import static mypals.ml.wandSystem.IntersectionResolver.cutBox;
import static mypals.ml.wandSystem.SelectedManager.isInsideArea;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class WandActionsManager {
    public static BlockPos pos1;
    public static BlockPos pos2;
    public static int SELECT_COOLDOWN = 5;
    public static int selectCoolDown = SELECT_COOLDOWN;
    public static boolean deleteMode = false;
    public static void selectingAction(BlockPos pos, InteractionHand hand, Player player, Boolean isFirstPoint) {
        if (isFirstPoint) {
            pos1 = pos;
            player.swing(hand);
            player.makeSound(SoundEvents.CHAIN_PLACE);
            if(pos1 != null){
                player.makeSound(SoundEvents.CHAIN_PLACE);
            }
        } else{
            pos2 = pos;
            player.swing(hand);
            player.makeSound(SoundEvents.CHAIN_PLACE);
            if(pos2 != null){
                player.makeSound(SoundEvents.CHAIN_PLACE);
            }
        }
    }
    public static void switchRenderMod(boolean increase){
        GlowMyBlocksConfig.CONFIG_HANDLER.instance();
        if(selectCoolDown > 0){return;}
        if (increase) {
            glowBlockMode = glowBlockMode == GlowModeManager.GlowRenderMode.values().length - 1 ? 0 : glowBlockMode + 1;
        } else {
            glowBlockMode = glowBlockMode == 0 ? GlowModeManager.GlowRenderMode.values().length - 1 : glowBlockMode - 1;
        }
        resolveSelectiveBlockRenderingMode(glowBlockMode);
        GlowMyBlocksConfig.CONFIG_HANDLER.save();
        onConfigUpdated();
        selectCoolDown = SELECT_COOLDOWN;
    }
    public static void addAreaAction(BlockPos pos, InteractionHand hand, Player player, Level world) {
        if (pos1 != null && pos2 !=null) {
            GlowMyBlocksConfig.CONFIG_HANDLER.instance();
            GlowMyBlocksConfig.selectedAreasSaved.add(pos1.getX() + "," + pos1.getY() + "," + pos1.getZ() + ":"
                    + pos2.getX() + "," + pos2.getY() + "," + pos2.getZ() + ":" + areaColor.getRGB());
            GlowMyBlocksConfig.CONFIG_HANDLER.save();
            onConfigUpdated();
            pos1 = null;
            pos2 = null;
            player.makeSound(SoundEvents.RESPAWN_ANCHOR_CHARGE);
        }else{
            player.makeSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value());
        }
    }
    public static void cutAreaAction(Player player) {
        if (pos1 != null && pos2 !=null) {
            GlowMyBlocksConfig.CONFIG_HANDLER.instance();
            AreaBox cutBox = new AreaBox(pos1,pos2, Color.RED,0.2f,true);
            List<AreaBox> remainingBoxes = new ArrayList<>();
            AtomicBoolean deletedSomething = new AtomicBoolean(false);

            selectedAreas.forEach(targetArea->{
                remainingBoxes.addAll(cutBox(targetArea, cutBox));
                try {
                    GlowMyBlocksConfig.selectedAreasSaved.remove(selectedAreas.indexOf(targetArea));
                    selectedAreas.remove(targetArea);
                }catch (Exception e){
                    System.out.println("SelectedAreas in config file is not same with current selectedAreas in-game!");
                }
                deletedSomething.set(true);
            });
            if(deletedSomething.get()){
                remainingBoxes.forEach(box->{
                    GlowMyBlocksConfig.selectedAreasSaved.add(box.minPos.getX() + "," + box.minPos.getY() + "," + box.minPos.getZ() + ":"
                            + box.maxPos.getX() + "," + box.maxPos.getY() + "," + box.maxPos.getZ() + ":" + box.color.getRGB());
                });
                GlowMyBlocksConfig.CONFIG_HANDLER.save();
                onConfigUpdated();
            }
            pos1 = null;
            pos2 = null;
            player.makeSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value());
        }else{
            player.makeSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value());
        }
    }
    public static List<AreaBox> getAreasToDelete(BlockPos pos, boolean delete){
        GlowMyBlocksConfig.CONFIG_HANDLER.instance();
        AtomicBoolean deletedSomething = new AtomicBoolean(false);
        List<AreaBox> areas = new ArrayList<>();
        selectedAreas.forEach(area->{
            if(isInsideArea(Vec3.atLowerCornerOf(pos),area)){
                areas.add(area);
                if(delete){
                    try {
                        GlowMyBlocksConfig.selectedAreasSaved.remove(selectedAreas.indexOf(area));
                    }catch (Exception e){
                        System.out.println("SelectedAreas in config file is not same with current selectedAreas in-game!");
                    }
                    deletedSomething.set(true);
                }
            }
        });
        if(deletedSomething.get()){
            GlowMyBlocksConfig.CONFIG_HANDLER.save();
            onConfigUpdated();
        }
        return areas;
    }

    public static BlockHitResult getPlayerLookedBlock(Player player, Level world) {
        Entity camera = Minecraft.getInstance().getCameraEntity();

        Vec3 start = camera.getEyePosition(1.0F);

        Vec3 end = start.add(camera.getViewVector(1.0F).scale(player.isCreative()?5:4));


        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );
        return world.clip(context);
    }
    public static void wandActions(Minecraft client){
        if(selectCoolDown > 0)selectCoolDown--;
        if(client.level == null){return;}
        boolean shouldSelect = client.player.getMainHandItem().getItem() == SelectedManager.wand || (selectInSpectator && client.player.isSpectator());
        deleteMode = false;
        if (Minecraft.getInstance().options.keySprint.isDown() && openColorPickerKey.isDown()) {
            if (client.screen == null &&  client.player.getMainHandItem().getItem() == SelectedManager.wand ) {
                client.setScreen(new ColorPickerScreen(
                        null,
                        GlowMyBlocksConfig.areaColor,
                        color -> {
                            GlowMyBlocksConfig.areaColor = color;
                        }
                ));
            }
        }
        if(selectCoolDown <= 0 && shouldSelect && client.player != null){
            if (deleteOutlineArea.isDown()) {
                deleteMode = true;
                BlockHitResult blockRayCast = getPlayerLookedBlock(client.player, client.level);
                if(client.options.keyUse.isDown()){
                    getAreasToDelete(blockRayCast.getBlockPos(),true);
                    client.player.swing(client.player.getUsedItemHand());
                }
                else if (pos1 != null && pos2 != null && client.options.keyAttack.isDown()) {
                    cutAreaAction(client.player);
                    selectCoolDown = SELECT_COOLDOWN;
                    client.player.swing(client.player.getUsedItemHand());
                }
            } else if (client.options.keyAttack.isDown()) {
                if (client.options.keyShift.isDown()) {
                    clearArea(client.player.blockPosition(), client.player.getUsedItemHand(), client.player, client.level);
                    selectCoolDown = SELECT_COOLDOWN;
                } else if (addOutlineArea.isDown()){
                    addAreaAction(client.player.blockPosition(), client.player.getUsedItemHand(), client.player, client.level);
                    selectCoolDown = SELECT_COOLDOWN;
                }else {
                    BlockHitResult blockBreakingRayCast = getPlayerLookedBlock(client.player, client.level);
                    selectingAction(blockBreakingRayCast.getBlockPos(), client.player.getUsedItemHand(), client.player, true);
                    selectCoolDown = SELECT_COOLDOWN;
                }
            }else if (client.options.keyUse.isDown()) {
                BlockHitResult blockBreakingRayCast = getPlayerLookedBlock(client.player, client.level);
                selectingAction(blockBreakingRayCast.getBlockPos(), client.player.getUsedItemHand(), client.player, false);
                selectCoolDown = SELECT_COOLDOWN;
            }
        }
    }

    public static void clearArea(BlockPos pos, InteractionHand hand, Player player, Level world){
        if((pos1 != null || pos2 != null)) {
            pos1 = null;
            pos2 = null;
            player.makeSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value());
        }
    }
}
