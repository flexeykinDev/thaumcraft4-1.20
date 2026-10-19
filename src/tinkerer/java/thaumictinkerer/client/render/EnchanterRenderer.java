package thaumictinkerer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumictinkerer.common.block.EnchanterBlock;

/** RenderTileEnchanter оригинала: вещь лежит на столе, рядом над ним покачивается жезл. */
public class EnchanterRenderer implements BlockEntityRenderer<EnchanterBlock.Tile> {
    public EnchanterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EnchanterBlock.Tile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        var renderer = Minecraft.getInstance().getItemRenderer();
        ItemStack tool = tile.tool();
        if (!tool.isEmpty()) {
            pose.pushPose();
            pose.translate(0.42, 0.77, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(30));
            pose.scale(0.55f, 0.55f, 0.55f);
            renderer.renderStatic(tool, ItemDisplayContext.FIXED, light, overlay, pose, buffer, tile.getLevel(), 0);
            pose.popPose();
        }
        ItemStack wand = tile.wand();
        if (!wand.isEmpty()) {
            double time = System.currentTimeMillis();
            pose.pushPose();
            pose.translate(0.75, 0.85 + (Math.cos(time / 1000.0) - 1.2) / 20.0, 0.4);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-70));
            pose.mulPose(Axis.XP.rotationDegrees((float) Math.cos(time / 500.0) * 5f));
            pose.scale(0.5f, 0.5f, 0.5f);
            renderer.renderStatic(wand, ItemDisplayContext.FIXED, light, overlay, pose, buffer, tile.getLevel(), 0);
            pose.popPose();
        }
    }
}
