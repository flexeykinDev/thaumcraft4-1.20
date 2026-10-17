package thaumictinkerer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumictinkerer.common.block.FunnelBlock;

/** RenderTileFunnel оригинала: вставленная банка стоит на воронке. */
public class FunnelRenderer implements BlockEntityRenderer<FunnelBlock.Tile> {
    public FunnelRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FunnelBlock.Tile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        ItemStack jar = tile.getJar();
        if (jar.isEmpty()) {
            return;
        }
        pose.pushPose();
        // в виде NONE предмет-блок рисуется ровно как поставленный блок; поднимаем его на толщину воронки
        pose.translate(0, 2 / 16.0, 0);
        Minecraft.getInstance().getItemRenderer().renderStatic(jar, ItemDisplayContext.NONE, light, overlay, pose, buffer,
                tile.getLevel(), 0);
        pose.popPose();
    }
}
