package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blockentity.TubeBlockEntity;
import thaumcraft.common.blocks.TubeBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * То, чего нет в модели блока трубы: отвод, удлинённый на 6 пикселей внутрь банки или куба
 * (renderExtendedTube оригинала), и рукоять вентиля, которая проворачивается и утапливается, когда вентиль
 * закрывают (TileTubeValveRenderer оригинала).
 */
public class TubeRenderer implements BlockEntityRenderer<TubeBlockEntity> {
    private static final ResourceLocation PIPE = Thaumcraft.id("textures/block/pipe_1.png");
    private static final ResourceLocation VALVE = Thaumcraft.id("textures/models/valve.png");
    private static final ResourceLocation WHEEL = Thaumcraft.id("textures/block/pipe_valve.png");

    /** Кусок трубы 2x2x6, уходящий от грани блока наружу вдоль +Y. */
    private final ModelPart extension = new ModelPart(List.of(new ModelPart.Cube(0, 0, -1f, 8f, -1f, 2f, 6f, 2f, 0f, 0f, 0f,
            false, 32f, 32f, EnumSet.allOf(Direction.class))), Map.of());
    /** ModelTubeValve оригинала: шток 2x2x2. */
    private final ModelPart rod = new ModelPart(List.of(new ModelPart.Cube(0, 10, -1f, 2f, -1f, 2f, 2f, 2f, 0f, 0f, 0f,
            true, 64f, 32f, EnumSet.allOf(Direction.class))), Map.of());

    public TubeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TubeBlockEntity tube, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        BlockState state = tube.getBlockState();
        if (!(state.getBlock() instanceof TubeBlock block) || tube.getLevel() == null) {
            return;
        }
        VertexConsumer pipe = null;
        for (Direction side : Direction.values()) {
            if (!state.getValue(TubeBlock.ARMS.get(side))) {
                continue;
            }
            // трубы стыкуются грань в грань; сосуды уже блока, и до них отвод надо дотянуть
            if (tube.getLevel().getBlockEntity(tube.getBlockPos().relative(side))
                    instanceof thaumcraft.common.blockentity.EssentiaBlockEntity) {
                if (pipe == null) {
                    pipe = buffer.getBuffer(RenderType.entityCutoutNoCull(PIPE));
                }
                pose.pushPose();
                pose.translate(0.5f, 0.5f, 0.5f);
                pose.mulPose(side.getRotation());
                extension.render(pose, pipe, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        if (block.kind == TubeBlock.Kind.VALVE) {
            renderValve(tube, partialTick, pose, buffer, light);
        }
    }

    private void renderValve(TubeBlockEntity tube, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        Direction facing = tube.getFacing();
        float rotation = tube.getValveRotation(partialTick);
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        if (facing.getStepY() == 0) {
            pose.mulPose(Axis.YP.rotationDegrees(90f));
        } else {
            pose.mulPose(Axis.XN.rotationDegrees(90f));
            pose.mulPose(Axis.XP.rotationDegrees(90f * facing.getStepY()));
        }
        pose.mulPose(Axis.of(new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ())).rotationDegrees(90f));
        pose.mulPose(Axis.YN.rotationDegrees(rotation * 1.5f));
        pose.translate(0f, -(rotation / 360f) * 0.12f, 0f);
        rod.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(VALVE)), light, OverlayTexture.NO_OVERLAY);

        // маховик: плоский значок вентиля толщиной в пиксель, лежит поперёк штока
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        VertexConsumer wheel = buffer.getBuffer(RenderType.entityCutoutNoCull(WHEEL));
        for (float z : new float[]{-0.25f, -0.2f}) {
            Matrix4f m = pose.last().pose();
            Matrix3f n = pose.last().normal();
            float[][] corners = {{-0.25f, -0.25f, 0f, 1f}, {0.25f, -0.25f, 1f, 1f}, {0.25f, 0.25f, 1f, 0f}, {-0.25f, 0.25f, 0f, 0f}};
            for (float[] c : corners) {
                wheel.vertex(m, c[0], c[1], z).color(255, 255, 255, 255).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light).normal(n, 0f, 0f, 1f).endVertex();
            }
        }
        pose.popPose();
    }
}
