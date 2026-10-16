package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.EmberEntity;

/**
 * Уголёк — перенос RenderEmber оригинала: светящийся язык пламени из листа частиц, всегда повёрнутый к камере.
 * За жизнь уголька кадр проходит от яркой вспышки до дыма, а сам он растёт.
 */
public class EmberRenderer extends EntityRenderer<EmberEntity> {
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

    public EmberRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EmberEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        float life = Math.min(1f, (entity.tickCount + partialTick) / entity.getDuration());
        int frame = Math.min(8, (int) (8f * life));
        float u0 = (7 + frame) / 16f;
        float u1 = u0 + 0.0625f;
        float v0 = 0.5625f;
        float v1 = v0 + 0.0625f;
        float scale = 0.25f + life;

        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        // лицевая сторона квадрата должна смотреть на камеру, иначе он отсекается
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        // свечение складывается с фоном; прозрачность 0.9 заложена в яркость
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(PARTICLES));
        vertex(consumer, matrix, normal, -0.5f, -0.5f, u1, v1);
        vertex(consumer, matrix, normal, 0.5f, -0.5f, u0, v1);
        vertex(consumer, matrix, normal, 0.5f, 0.5f, u0, v0);
        vertex(consumer, matrix, normal, -0.5f, 0.5f, u1, v0);
        pose.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v) {
        consumer.vertex(matrix, x, y, 0f).color(0.9f, 0.9f, 0.9f, 1f).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xf000f0).normal(normal, 0f, 1f, 0f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EmberEntity entity) {
        return PARTICLES;
    }
}
