package mypals.ml.wandSystem;

import mypals.ml.config.GlowMyBlocksConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;


public class SelectedManager {
    public static Map<Integer, Map<Property,Object>> selectedBlockTypes = new HashMap<>();
    public static List<AreaBox> selectedAreas = new CopyOnWriteArrayList<>();
    public static Item wand;

    public static void resolveSelectedWandFromString(String name){
        Item last_wind = wand;
        if (!name.contains(":")) {
            name = "minecraft:" + name;
        }
        Identifier id = Identifier.of(name);
        Item nweWand = Registries.ITEM.get(id);
        if (nweWand == null) {
            wand = last_wind;
        }
        else{
            wand = nweWand;
        }
    }
    public static void resolveSelectedAreasFromString(List<String> areaStrings){
        selectedAreas.clear();

        areaStrings.forEach(areaString -> {
            try {
                AreaBox area = parseAABB(areaString);
                selectedAreas.add(area);
            } catch (IllegalArgumentException e) {
                System.err.println("Failed to parse area: " + areaString + " -> " + e.getMessage());
            }
        });
    }
    private static AreaBox parseAABB(String areaString) throws IllegalArgumentException {
        String[] parts = areaString.split(":");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid format. Expected x1,y1,z1:x2,y2,z2:color");
        }

        String[] startCoords = parts[0].split(",");
        String[] endCoords = parts[1].split(",");
        String colorString = parts[2];

        Color color = Color.white;
        try{
            int c = Integer.decode(colorString);
            color = new Color(c);
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse color: " + colorString);
        }

        if (startCoords.length != 3 || endCoords.length != 3) {
            System.err.println("Invalid coordinates. Expected x1,y1,z1:x2,y2,z2:color");
        }

        try {
            int x1 = Integer.parseInt(startCoords[0].trim());
            int y1 = Integer.parseInt(startCoords[1].trim());
            int z1 = Integer.parseInt(startCoords[2].trim());

            int x2 = Integer.parseInt(endCoords[0].trim());
            int y2 = Integer.parseInt(endCoords[1].trim());
            int z2 = Integer.parseInt(endCoords[2].trim());

            return new AreaBox(
                    new BlockPos(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
                    new BlockPos(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2))
                    ,color,0.2f,false
            );
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in input: " + areaString);
            return new AreaBox(
                    new BlockPos(0, 0, 0),
                    new BlockPos(0, 0, 0)
                    ,Color.white,0.2f,false
            );
        }
    }
    public static boolean isInsideArea(Vec3d pos, AreaBox areaBox){
        if (areaBox.minPos.getX() <= pos.getX() && pos.getX() <= areaBox.maxPos.getX() &&
                areaBox.minPos.getY() <= pos.getY() && pos.getY() <= areaBox.maxPos.getY() &&
                areaBox.minPos.getZ() <= pos.getZ() && pos.getZ() <= areaBox.maxPos.getZ()) {
            return true;
        }
        return false;
    }
}
