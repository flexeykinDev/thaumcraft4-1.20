package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.FocusOrbEntity;

/**
 * Шары улучшенных фокусов — перенос RenderExplosiveOrb и RenderElectricOrb оригинала: кадр из листа частиц,
 * всегда повёрнутый к камере. Огненный шар — четыре кадра пламени, шар разряда — шесть кадров искр и пульсация.
 */
public class FocusOrbRenderer extends EntityRenderer<FocusOrbEntity> {
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");
    private static final ResourceLocation PARTICLES2 = Thaumcraft.id("textures/misc/particles2.png");

    public FocusOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FocusOrbEntity orb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        boolean shock = orb.isShock();
        float u0;
        float v0;
        float cell;
        float scale;
        if (shock) {
            cell = 0.125f;
            u0 = (1 + orb.tickCount % 6) / 8f;
            v0 = 0.875f;
            scale = 1f + Mth.sin((orb.tickCount + partialTick) / 5f) * 0.2f + 0.2f;
        } else {
            cell = 0.0625f;
            u0 = orb.tickCount % 4 / 16f;
            v0 = 0.8125f;
            scale = 0.7f;
        }
        pose.pushPose();
        pose.scale(scale, scale, scale);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        // разряд светится и складывается с фоном, огненный шар рисуется обычной полупрозрачностью
        VertexConsumer consumer = buffer.getBuffer(shock ? RenderType.eyes(PARTICLES) : RenderType.entityTranslucentEmissive(PARTICLES2));
        float c = 0.8f;
        float a = shock ? 1f : 0.8f;
        float rgb = shock ? c : 1f;
        float[][] corners = {{-0.5f, -0.5f, u0 + cell, v0 + cell}, {0.5f, -0.5f, u0, v0 + cell}, {0.5f, 0.5f, u0, v0}, {-0.5f, 0.5f, u0 + cell, v0}};
        for (float[] p : corners) {
            consumer.vertex(matrix, p[0], p[1], 0f).color(rgb, rgb, rgb, a).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xf000f0).normal(normal, 0f, 1f, 0f).endVertex();
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(FocusOrbEntity orb) {
        return orb.isShock() ? PARTICLES : PARTICLES2;
    }
}
