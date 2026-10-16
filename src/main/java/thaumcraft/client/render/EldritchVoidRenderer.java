package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import thaumcraft.common.blocks.EldritchVoidBlock;

/**
 * Пустота — перенос TileEldritchNothingRenderer оригинала: на каждой открытой грани видна звёздная бездна (эффект
 * портала Края, с которого оригинал и был списан).
 */
public class EldritchVoidRenderer implements BlockEntityRenderer<EldritchVoidBlock.Void> {
    public EldritchVoidRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EldritchVoidBlock.Void be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        Matrix4f m = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.endPortal());
        for (Direction side : Direction.values()) {
            if (EldritchVoidBlock.faceOpen(be.getLevel(), be.getBlockPos(), side)) {
                face(m, consumer, side);
            }
        }
    }

    /** Грань куба, обращённая наружу (порядок вершин — как у граней моделей блоков). */
    static void face(Matrix4f m, VertexConsumer consumer, Direction side) {
        float[][] c = switch (side) {
            case DOWN -> new float[][]{{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}};
            case UP -> new float[][]{{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}};
            case NORTH -> new float[][]{{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}};
            case SOUTH -> new float[][]{{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}};
            case WEST -> new float[][]{{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}};
            case EAST -> new float[][]{{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}};
        };
        for (float[] v : c) {
            consumer.vertex(m, v[0], v[1], v[2]).endVertex();
        }
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
