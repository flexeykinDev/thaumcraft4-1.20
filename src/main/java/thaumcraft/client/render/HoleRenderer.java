package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import thaumcraft.common.blocks.HoleBlock;
import thaumcraft.common.registry.ModBlocks;

/**
 * Стенки дыры — перенос TileHoleRenderer оригинала: там, где дыра упирается в целый блок, вместо него видна
 * «звёздная» плоскость. Оригинал рисовал её тем же приёмом, что и портал в Край, только со своими текстурами
 * (tunnel.png и particlefield.png); здесь это шейдер портала в Край с теми же текстурами.
 */
public class HoleRenderer implements BlockEntityRenderer<HoleBlock.Hole> {
    public HoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HoleBlock.Hole hole, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Level level = hole.getLevel();
        if (level == null) {
            return;
        }
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.HOLE);
        BlockPos pos = hole.getBlockPos();
        float lo = 0.001f;
        float hi = 0.999f;
        for (Direction side : Direction.values()) {
            BlockPos near = pos.relative(side);
            BlockState state = level.getBlockState(near);
            if (!state.isSolidRender(level, near) || state.is(ModBlocks.HOLE.get())) {
                continue;
            }
            switch (side) {
                case UP -> quad(consumer, matrix, 0, hi, 1, 0, hi, 0, 1, hi, 0, 1, hi, 1);
                case DOWN -> quad(consumer, matrix, 0, lo, 0, 0, lo, 1, 1, lo, 1, 1, lo, 0);
                case NORTH -> quad(consumer, matrix, 0, 0, lo, 1, 0, lo, 1, 1, lo, 0, 1, lo);
                case SOUTH -> quad(consumer, matrix, 0, 0, hi, 0, 1, hi, 1, 1, hi, 1, 0, hi);
                case WEST -> quad(consumer, matrix, lo, 0, 0, lo, 1, 0, lo, 1, 1, lo, 0, 1);
                case EAST -> quad(consumer, matrix, hi, 0, 0, hi, 0, 1, hi, 1, 1, hi, 1, 0);
            }
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, float... p) {
        for (int i = 0; i < 12; i += 3) {
            consumer.vertex(matrix, p[i], p[i + 1], p[i + 2]).endVertex();
        }
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
