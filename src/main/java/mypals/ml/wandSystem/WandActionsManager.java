package mypals.ml.wandSystem;

import mypals.ml.config.GlowMyBlocksConfig;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static mypals.ml.GlowMyBlocks.onConfigUpdated;
import static mypals.ml.config.GlowMyBlocksConfig.*;
import static mypals.ml.config.Keybinds.addArea;
import static mypals.ml.config.Keybinds.deleteArea;
import static mypals.ml.wandSystem.IntersectionResolver.cutBox;
import static mypals.ml.wandSystem.SelectedManager.isInsideArea;
import static mypals.ml.wandSystem.SelectedManager.selectedAreas;

public class WandActionsManager {
    public static BlockPos pos1;
    public static BlockPos pos2;
    public static int SELECT_COOLDOWN = 5;
    public static int selectCoolDown = SELECT_COOLDOWN;
    public static boolean deleteMode = false;
    public static void selectingAction(BlockPos pos, Hand hand, PlayerEntity player, Boolean isFirstPoint) {
        if (isFirstPoint) {
            pos1 = pos;
            player.swingHand(hand);
            player.playSound(SoundEvents.BLOCK_CHAIN_PLACE);
            if(pos1 != null){
                player.playSound(SoundEvents.BLOCK_CHAIN_PLACE);
            }
        } else{
            pos2 = pos;
            player.swingHand(hand);
            player.playSound(SoundEvents.BLOCK_CHAIN_PLACE);
            if(pos2 != null){
                player.playSound(SoundEvents.BLOCK_CHAIN_PLACE);
            }
        }
    }
    public static void addAreaAction(BlockPos pos, Hand hand, PlayerEntity player, World world) {
        if (pos1 != null && pos2 !=null) {
            GlowMyBlocksConfig.CONFIG_HANDLER.instance();
            GlowMyBlocksConfig.selectedAreasSaved.add(pos1.getX() + "," + pos1.getY() + "," + pos1.getZ() + ":"
                    + pos2.getX() + "," + pos2.getY() + "," + pos2.getZ() + ":" + areaColor.getRGB());
            GlowMyBlocksConfig.CONFIG_HANDLER.save();
            onConfigUpdated();
            pos1 = null;
            pos2 = null;
            player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE);
        }else{
            //player.sendMessage(Text.literal(Text.translatable("config.lucidity.cant_add_area").getString()), true);
            player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value());
        }
    }
    public static void cutAreaAction(PlayerEntity player) {
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
            player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value());
        }else{
            //player.sendMessage(Text.literal(Text.translatable("config.lucidity.cant_add_area").getString()), true);
            player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value());
        }
    }
    public static List<AreaBox> getAreasToDelete(BlockPos pos, boolean delete){
        GlowMyBlocksConfig.CONFIG_HANDLER.instance();
        AtomicBoolean deletedSomething = new AtomicBoolean(false);
        List<AreaBox> areas = new ArrayList<>();
        selectedAreas.forEach(area->{
            if(isInsideArea(Vec3d.of(pos),area)){
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

    public static BlockHitResult getPlayerLookedBlock(PlayerEntity player, World world) {
        Entity camera = MinecraftClient.getInstance().getCameraEntity();

        Vec3d start = camera.getCameraPosVec(1.0F);

        Vec3d end = start.add(camera.getRotationVec(1.0F).multiply(player.isCreative()?5:4));


        RaycastContext context = new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        );
        return world.raycast(context);
    }
    public static void wandActions(MinecraftClient client){
        if(selectCoolDown > 0)selectCoolDown--;
        if(client.world == null){return;}
        boolean shouldSelect = client.player.getMainHandStack().getItem() == SelectedManager.wand || (selectInSpectator && client.player.isSpectator());
        deleteMode = false;
        if(selectCoolDown <= 0 && shouldSelect && client.player != null){
            if (deleteArea.isPressed()) {
                deleteMode = true;
                BlockHitResult blockRayCast = getPlayerLookedBlock(client.player, client.world);
                if(client.options.useKey.isPressed()){
                    getAreasToDelete(blockRayCast.getBlockPos(),true);
                    client.player.swingHand(client.player.getActiveHand());
                }
                else if (pos1 != null && pos2 != null && client.options.attackKey.isPressed()) {
                    cutAreaAction(client.player);
                    selectCoolDown = SELECT_COOLDOWN;
                    client.player.swingHand(client.player.getActiveHand());
                }
            } else if (client.options.attackKey.isPressed()) {
                if (client.options.sneakKey.isPressed()) {
                    clearArea(client.player.getBlockPos(), client.player.getActiveHand(), client.player, client.world);
                    selectCoolDown = SELECT_COOLDOWN;
                } else if (addArea.isPressed()){
                    addAreaAction(client.player.getBlockPos(), client.player.getActiveHand(), client.player, client.world);
                    selectCoolDown = SELECT_COOLDOWN;
                }else {
                    BlockHitResult blockBreakingRayCast = getPlayerLookedBlock(client.player, client.world);
                    selectingAction(blockBreakingRayCast.getBlockPos(), client.player.getActiveHand(), client.player, true);
                    selectCoolDown = SELECT_COOLDOWN;
                }
            }else if (client.options.useKey.isPressed()) {
                BlockHitResult blockBreakingRayCast = getPlayerLookedBlock(client.player, client.world);
                selectingAction(blockBreakingRayCast.getBlockPos(), client.player.getActiveHand(), client.player, false);
                selectCoolDown = SELECT_COOLDOWN;
            }
        }
    }

    public static void clearArea(BlockPos pos, Hand hand, PlayerEntity player, World world){
        if((pos1 != null || pos2 != null)) {
            pos1 = null;
            pos2 = null;
            //player.sendMessage(Text.literal(Text.translatable("config.lucidity.clear").getString()), true);
            player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value());
        }
    }
}
