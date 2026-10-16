package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.EtherealBloomBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Эфирный цветок — перенос TileEtherealBloomRenderer оригинала: стебель вытягивается из земли, следом раскрываются
 * два яруса листьев, а на макушке загорается кристалл в сияющем ореоле.
 */
public class EtherealBloomRenderer implements BlockEntityRenderer<EtherealBloomBlock.Bloom> {
    private static final ResourceLocation LEAVES = Thaumcraft.id("textures/block/purifier_leaves.png");
    private static final ResourceLocation STALK = Thaumcraft.id("textures/block/purifier_stalk.png");
    private static final ResourceLocation CRYSTAL = Thaumcraft.id("textures/models/crystalcapacitor.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    /** ModelCube оригинала: куб 16×16×16 с развёрткой 64×32. */
    private final ModelPart cube = new ModelPart(List.of(new ModelPart.Cube(0, 0, -8f, -8f, -8f, 16, 16, 16, 0f, 0f, 0f, true, 64f, 32f,
            EnumSet.allOf(Direction.class))), Map.of());

    public EtherealBloomRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EtherealBloomBlock.Bloom bloom, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        float age = bloom.growthCounter + partialTick;
        float scale1 = Math.min(age, 100f) / 100f;
        float scale2 = Math.min(age, 50f) / 60f + 0.1666666f;
        float scale3 = Mth.clamp(age - 33f, 0f, 33f) / 33f;
        float scale4 = Mth.clamp(age - 66f, 0f, 33f) / 33f * 0.7f;

        // стебель растёт от земли
        cross(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(STALK)), scale1 / 2f, scale2, scale1, 0f);
        // нижние листья
        cross(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(LEAVES)), 0.25f, scale3, scale1, 0f);
        // верхние листья, повёрнутые на полоборота
        cross(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(LEAVES)), 0.6f, scale4, scale1 * 0.7f, 45f);

        // кристалл на макушке
        if (scale4 > 0f) {
            pose.pushPose();
            pose.translate(0.5, scale1, 0.5);
            pose.scale(scale4 / 4f, scale4 / 3f, scale4 / 4f);
            // как в оригинале, кристалл светится: его серая текстура складывается с фоном, а не закрывает его
            cube.render(pose, buffer.getBuffer(RenderType.eyes(CRYSTAL)), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }

        // ореол, всегда лицом к смотрящему
        pose.pushPose();
        pose.translate(0.5, scale1, 0.5);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        NodeRenderer.drawLayer(pose, buffer.getBuffer(ModRenderTypes.NODE_GLOW), 0f, scale1, 1f, 6, bloom.counter % 32, 0xAADDFF);
        pose.popPose();
    }

    /** Две скрещённые плоскости шириной width и высотой height с центром на высоте centre. */
    private static void cross(PoseStack pose, VertexConsumer consumer, float centre, float width, float height, float turn) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, centre, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        pose.scale(width, height, width);
        for (int plane = 0; plane < 2; plane++) {
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            vertex(consumer, matrix, normal, -0.5f, -0.5f, 0f, 1f);
            vertex(consumer, matrix, normal, 0.5f, -0.5f, 1f, 1f);
            vertex(consumer, matrix, normal, 0.5f, 0.5f, 1f, 0f);
            vertex(consumer, matrix, normal, -0.5f, 0.5f, 0f, 0f);
            pose.mulPose(Axis.YP.rotationDegrees(90f));
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v) {
        consumer.vertex(matrix, x, y, 0f).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT)
                .normal(normal, 0f, 1f, 0f).endVertex();
    }
}
