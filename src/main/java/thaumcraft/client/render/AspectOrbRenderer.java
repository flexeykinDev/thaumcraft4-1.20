package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.AspectOrbEntity;

/**
 * Сфера аспекта — перенос RenderAspectOrb оригинала: мерцающий огонёк из листа частиц в цвете аспекта, всегда
 * повёрнутый к камере. К концу жизни сжимается.
 */
public class AspectOrbRenderer extends EntityRenderer<AspectOrbEntity> {
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

    public AspectOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1f;
        this.shadowStrength = 0.5f;
    }

    @Override
    public void render(AspectOrbEntity orb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        int frame = (int) (System.nanoTime() / 25000000L % 16L);
        float u0 = frame / 16f;
        float u1 = (frame + 1) / 16f;
        float v0 = 0.5f;
        float v1 = 0.5625f;
        float scale = 0.1f + 0.3f * ((float) (orb.orbMaxAge - orb.orbAge) / orb.orbMaxAge);
        int color = orb.getAspect().getColor();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f));
        pose.scale(scale, scale, scale);
        Matrix4f matrix = pose.last().pose();
        // тёмные аспекты рисуются обычным смешиванием, остальные светятся
        VertexConsumer consumer = buffer.getBuffer(orb.getAspect().isAdditiveBlend() ? ModRenderTypes.additive(PARTICLES)
                : ModRenderTypes.translucentQuad(PARTICLES));
        consumer.vertex(matrix, -0.5f, -0.25f, 0f).uv(u0, v1).color(r, g, b, 128).endVertex();
        consumer.vertex(matrix, 0.5f, -0.25f, 0f).uv(u1, v1).color(r, g, b, 128).endVertex();
        consumer.vertex(matrix, 0.5f, 0.75f, 0f).uv(u1, v0).color(r, g, b, 128).endVertex();
        consumer.vertex(matrix, -0.5f, 0.75f, 0f).uv(u0, v0).color(r, g, b, 128).endVertex();
        pose.popPose();
        super.render(orb, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AspectOrbEntity orb) {
        return PARTICLES;
    }
}
