package thaumictinkerer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.AnimationTabletBlock;

/**
 * RenderTileAnimationTablet оригинала: на табличке светятся знаки (середина, левая или правая «кнопка», стрелка
 * направления), а над ней покачивается вещь и машет в сторону цели.
 */
public class AnimationTabletRenderer implements BlockEntityRenderer<AnimationTabletBlock.Tile> {
    private static final ResourceLocation CENTER = ThaumicTinkerer.id("textures/misc/at_center.png");
    private static final ResourceLocation LEFT = ThaumicTinkerer.id("textures/misc/at_left.png");
    private static final ResourceLocation RIGHT = ThaumicTinkerer.id("textures/misc/at_right.png");
    private static final ResourceLocation INDENT = ThaumicTinkerer.id("textures/misc/at_facing_indent.png");

    public AnimationTabletRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static void overlay(PoseStack pose, MultiBufferSource buffer, ResourceLocation texture, double size, float height, float yaw) {
        pose.pushPose();
        pose.translate(0.5, height, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        Matrix4f m = pose.last().pose();
        float h = (float) size / 2;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture));
        vc.vertex(m, -h, 0, h).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 1, 0).endVertex();
        vc.vertex(m, h, 0, h).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 1, 0).endVertex();
        vc.vertex(m, h, 0, -h).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 1, 0).endVertex();
        vc.vertex(m, -h, 0, -h).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 1, 0).endVertex();
        pose.popPose();
    }

    @Override
    public void render(AnimationTabletBlock.Tile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = tile.getBlockState().getValue(AnimationTabletBlock.FACING);
        float yaw = 180f - facing.toYRot();
        overlay(pose, buffer, CENTER, 0.65, 0.13f, 0);
        overlay(pose, buffer, tile.leftClick ? LEFT : RIGHT, 1.0, 0.131f, 0);
        overlay(pose, buffer, INDENT, 0.5, 0.132f, yaw + 90);
        ItemStack stack = tile.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        double time = System.currentTimeMillis();
        pose.pushPose();
        pose.translate(0.5, 0.45 + Math.cos(time / 600.0) / 18.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        // взмах в сторону цели (вперёд по оси z таблички)
        pose.translate(0, 0, 0.1);
        pose.mulPose(Axis.XP.rotationDegrees(tile.swing));
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.cos(time / 400.0) * 5f));
        float scale = stack.getItem() instanceof BlockItem ? 0.5f : 0.6f;
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffer,
                tile.getLevel(), 0);
        pose.popPose();
    }
}
