package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import thaumcraft.common.blocks.DeconstructionTableBlock;
import thaumcraft.common.registry.ModItems;

/**
 * Стол разборки — перенос TileDeconstructionTableRenderer оригинала: на столе стоит таумометр, над ним кружится
 * разбираемый предмет, а добытая точка исследования лежит на столешнице знаком аспекта.
 */
public class DeconstructionTableRenderer implements BlockEntityRenderer<DeconstructionTableBlock.Table> {
    private final ItemRenderer items;
    private final ItemStack thaumometer = new ItemStack(ModItems.THAUMOMETER.get());

    /** Пропускает вершины дальше, убавляя непрозрачность до трёх четвертей. */
    private record Ghost(VertexConsumer inner) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            inner.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            inner.color(red, green, blue, alpha * 3 / 4);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            inner.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            inner.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            inner.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            inner.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            inner.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            inner.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            inner.unsetDefaultColor();
        }
    }

    public DeconstructionTableRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(DeconstructionTableBlock.Table table, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        float ticks = (Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount) + partialTick;
        int lightAbove = table.getLevel() == null ? light
                : net.minecraft.client.renderer.LevelRenderer.getLightColor(table.getLevel(), table.getBlockPos().above());

        pose.pushPose();
        // таумометр лежит плашмя на столешнице
        pose.translate(0.5, 1.2725, 0.5);
        pose.scale(1.4f, 1.4f, 1.4f);
        items.renderStatic(thaumometer, ItemDisplayContext.GROUND, lightAbove, OverlayTexture.NO_OVERLAY, pose, buffer, table.getLevel(), 0);
        pose.popPose();

        ItemStack item = table.getItem(0);
        if (!item.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.15 + Mth.sin(ticks / 10f) * 0.1 + 0.1, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360f));
            // как в оригинале, предмет над столом — полупрозрачный призрак
            MultiBufferSource ghost = type -> new Ghost(buffer.getBuffer(net.minecraft.client.renderer.Sheets.translucentItemSheet()));
            items.renderStatic(item, ItemDisplayContext.GROUND, 0xF000F0, OverlayTexture.NO_OVERLAY, pose, ghost, table.getLevel(), 0);
            pose.popPose();
        }

        if (table.aspect != null) {
            pose.pushPose();
            pose.translate(0.5, 1.081, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.mulPose(Axis.ZP.rotationDegrees(ticks % 360f));
            float half = 8 * 0.024f;
            int color = table.aspect.getColor();
            int r = color >> 16 & 255;
            int g = color >> 8 & 255;
            int b = color & 255;
            Matrix4f matrix = pose.last().pose();
            VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.translucentQuad(table.aspect.getImage()));
            consumer.vertex(matrix, -half, -half, 0f).uv(0f, 0f).color(r, g, b, 204).endVertex();
            consumer.vertex(matrix, -half, half, 0f).uv(0f, 1f).color(r, g, b, 204).endVertex();
            consumer.vertex(matrix, half, half, 0f).uv(1f, 1f).color(r, g, b, 204).endVertex();
            consumer.vertex(matrix, half, -half, 0f).uv(1f, 0f).color(r, g, b, 204).endVertex();
            pose.popPose();
        }
    }
}
