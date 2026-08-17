package mypals.ml.wandSystem;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import mypals.ml.renderings.BoxShape;
import mypals.ml.renderings.LineShape;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;

public class AreaBox {
    public BlockPos minPos;
    public BlockPos maxPos;

    public Color color;
    public float alpha = 0.2f;
    public boolean seeThrough = false;
    public AreaBox(BlockPos a, BlockPos b, Color color, float alpha, boolean seeThrough){
        this.minPos = new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );
        this.maxPos = new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );
        this.color = color;
        this.alpha = alpha;
        this.seeThrough = seeThrough;
    }
    public void draw(PoseStack matrices,boolean seeThrough) {
        draw(matrices, this.minPos, this.maxPos, this.color, this.alpha, seeThrough);
    }
    public void draw(PoseStack matrices, Color color, float alpha,boolean seeThrough) {
        draw(matrices, this.minPos, this.maxPos, color, alpha, seeThrough);
    }
    public static void draw(PoseStack matrices, BlockPos minPos, BlockPos maxPos, Color color, float alpha, boolean seeThrough) {
        double midX = ((maxPos.getX() + 1f) + (minPos.getX())) / 2.0;
        double midY = ((maxPos.getY() + 1f) + (minPos.getY())) / 2.0;
        double midZ = ((maxPos.getZ() + 1f) + (minPos.getZ())) / 2.0;
        float length = Math.abs(maxPos.getX() - minPos.getX());
        float width = Math.abs(maxPos.getZ() - minPos.getZ());
        float height = Math.abs(maxPos.getY() - minPos.getY());
        Vec3 midpos = new Vec3(midX, midY, midZ);

        Vec3 v1 = new Vec3(minPos.getX(), minPos.getY(), minPos.getZ());
        Vec3 v2 = new Vec3(maxPos.getX()+1, minPos.getY(), minPos.getZ());
        Vec3 v3 = new Vec3(maxPos.getX()+1, minPos.getY(), maxPos.getZ()+1);
        Vec3 v4 = new Vec3(minPos.getX(), minPos.getY(), maxPos.getZ()+1);

        Vec3 v5 = new Vec3(minPos.getX(), maxPos.getY()+1, minPos.getZ());
        Vec3 v6 = new Vec3(maxPos.getX()+1, maxPos.getY()+1, minPos.getZ());
        Vec3 v7 = new Vec3(maxPos.getX()+1, maxPos.getY()+1, maxPos.getZ()+1);
        Vec3 v8 = new Vec3(minPos.getX(), maxPos.getY()+1, maxPos.getZ()+1);

        LineShape.draw(matrices,  v1, v2,  color, 1,true);
        LineShape.draw(matrices,  v2, v3,  color, 1,true);
        LineShape.draw(matrices,  v3, v4,  color, 1,true);
        LineShape.draw(matrices,  v4, v1,  color, 1,true);

        LineShape.draw(matrices,  v5, v6, color, 1,true);
        LineShape.draw(matrices,  v6, v7, color, 1,true);
        LineShape.draw(matrices,  v7, v8, color, 1,true);
        LineShape.draw(matrices,  v8, v5, color, 1,true);

        LineShape.draw(matrices,  v1, v5, color, 1,true);
        LineShape.draw(matrices,  v2, v6, color, 1,true);
        LineShape.draw(matrices,  v3, v7, color, 1,true);
        LineShape.draw(matrices,  v4, v8, color, 1,true);

        BoxShape.draw(matrices,midpos,length+1.01f,width+1.01f,height+1.01f,0,color,alpha,seeThrough);
    }
}
