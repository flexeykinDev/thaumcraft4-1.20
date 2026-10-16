package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.FocalManipulatorBlock;

/**
 * Фокус над фокальным манипулятором — как в TileFocalManipulatorRenderer оригинала: предмет парит над столом,
 * медленно вращаясь и покачиваясь. Сам стол — обычная модель блока.
 */
public class FocalManipulatorRenderer implements BlockEntityRenderer<FocalManipulatorBlock.Manipulator> {
    public FocalManipulatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FocalManipulatorBlock.Manipulator table, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        ItemStack focus = table.getItem(0);
        Minecraft mc = Minecraft.getInstance();
        if (focus.isEmpty() || mc.player == null) {
            return;
        }
        float ticks = mc.player.tickCount + partialTick;
        pose.pushPose();
        pose.translate(0.5f, 1.2f + Mth.sin(ticks / 14f) * 0.05f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360f));
        pose.scale(0.6f, 0.6f, 0.6f);
        mc.getItemRenderer().renderStatic(focus, ItemDisplayContext.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, pose, buffer, table.getLevel(), 0);
        pose.popPose();
    }
}
