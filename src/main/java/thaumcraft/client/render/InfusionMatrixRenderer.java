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
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blockentity.InfusionMatrixBlockEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Руническая матрица — перенос TileRunicMatrixRenderer оригинала. Матрица состоит из восьми кубиков с рунами.
 * Спящая стоит ровно; пробуждённая поворачивается на угол и вращается, по рунам пробегает лиловое свечение.
 * Во время инфузии кубики дрожат тем сильнее, чем выше нестабильность, а из середины бьют лучи света.
 */
public class InfusionMatrixRenderer implements BlockEntityRenderer<InfusionMatrixBlockEntity> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/infuser.png");

    /** ModelCube оригинала: куб 16x16x16, развёртка в левом верхнем углу листа 64x64; shift 32 — слой свечения. */
    private final ModelPart cube = cube(0);
    private final ModelPart glow = cube(32);

    public InfusionMatrixRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart cube(int shift) {
        ModelPart.Cube box = new ModelPart.Cube(0, shift, -8f, -8f, -8f, 16f, 16f, 16f, 0f, 0f, 0f, true, 64f, 64f,
                EnumSet.allOf(Direction.class));
        return new ModelPart(List.of(box), Map.of());
    }

    @Override
    public void render(InfusionMatrixBlockEntity matrix, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {
        float ticks = Minecraft.getInstance().player.tickCount + partialTick;
        float startUp = matrix.getStartUp(partialTick);
        float craft = Math.min(matrix.getCraftCount(), 50) / 50f;
        float instability = Math.min(6f, 1f + matrix.getInstability() * 0.66f * craft);

        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360f * startUp));
        pose.mulPose(Axis.XP.rotationDegrees(35f * startUp));
        pose.mulPose(Axis.ZP.rotationDegrees(45f * startUp));

        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int pass = 0; pass < (matrix.isActive() || startUp > 0f ? 2 : 1); pass++) {
            VertexConsumer consumer = pass == 0 ? solid : buffer.getBuffer(RenderType.eyes(TEXTURE));
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    for (int c = 0; c < 2; c++) {
                        float b1 = 0f;
                        float b2 = 0f;
                        float b3 = 0f;
                        if (matrix.isActive()) {
                            b1 = Mth.sin((ticks + a * 10) / (15f - instability / 2f)) * 0.01f * startUp * instability;
                            b2 = Mth.sin((ticks + b * 10) / (14f - instability / 2f)) * 0.01f * startUp * instability;
                            b3 = Mth.sin((ticks + c * 10) / (13f - instability / 2f)) * 0.01f * startUp * instability;
                        }
                        pose.pushPose();
                        pose.translate(b1 + (a == 0 ? -0.25f : 0.25f), b2 + (b == 0 ? -0.25f : 0.25f), b3 + (c == 0 ? -0.25f : 0.25f));
                        if (a > 0) {
                            pose.mulPose(Axis.XP.rotationDegrees(90f));
                        }
                        if (b > 0) {
                            pose.mulPose(Axis.YP.rotationDegrees(90f));
                        }
                        if (c > 0) {
                            pose.mulPose(Axis.ZP.rotationDegrees(90f));
                        }
                        pose.scale(0.45f, 0.45f, 0.45f);
                        if (pass == 0) {
                            cube.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
                        } else {
                            // лиловое свечение рун мерцает у каждого кубика в своём ритме
                            float alpha = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4f) * 0.1f + 0.2f) * startUp;
                            // слой складывается с картинкой, поэтому яркость задаётся цветом (0.8, 0.1, 1.0), умноженным на силу
                            glow.render(pose, consumer, 0xf000f0, OverlayTexture.NO_OVERLAY, 0.8f * alpha, 0.1f * alpha, alpha, 1f);
                        }
                        pose.popPose();
                    }
                }
            }
        }
        pose.popPose();

        if (matrix.isCrafting()) {
            drawHalo(pose, buffer, matrix.getCraftCount() + partialTick);
        }
    }

    /** drawHalo оригинала: расходящиеся лучи, как у гибнущего дракона, белые в середине и лиловые к краям. */
    private static void drawHalo(PoseStack pose, MultiBufferSource buffer, float count) {
        boolean fancy = Minecraft.useFancyGraphics();
        float spin = count / 500f;
        float grow = Math.min(count, 50f) / 50f / 20f;
        RandomSource random = RandomSource.create(245L);
        VertexConsumer consumer = buffer.getBuffer(RenderType.lightning());
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        for (int i = 0; i < (fancy ? 20 : 10); i++) {
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360f));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360f));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360f));
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360f));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360f));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360f + spin * 360f));
            float length = (random.nextFloat() * 20f + 5f) * grow;
            float width = (random.nextFloat() * 2f + 1f) * grow;
            Matrix4f m = pose.last().pose();
            float[][] rim = {{-0.866f * width, length, -0.5f * width}, {0.866f * width, length, -0.5f * width}, {0f, length, width}};
            for (int side = 0; side < 3; side++) {
                float[] p = rim[side];
                float[] q = rim[(side + 1) % 3];
                // треугольник, записанный вырожденным четырёхугольником
                consumer.vertex(m, 0f, 0f, 0f).color(255, 255, 255, 255).endVertex();
                consumer.vertex(m, p[0], p[1], p[2]).color(204, 0, 255, 0).endVertex();
                consumer.vertex(m, q[0], q[1], q[2]).color(204, 0, 255, 0).endVertex();
                consumer.vertex(m, 0f, 0f, 0f).color(255, 255, 255, 255).endVertex();
            }
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(InfusionMatrixBlockEntity matrix) {
        return true;
    }
}
