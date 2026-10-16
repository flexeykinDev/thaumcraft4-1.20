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
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.EldritchOrb;

/**
 * Сфера Элдрича — перенос RenderEldritchOrb оригинала: двенадцать вращающихся лучей, белых у середины и бледно-лиловых
 * к концам (как у гибнущего дракона), вокруг тёмного клубящегося ядра.
 */
public class EldritchOrbRenderer extends EntityRenderer<EldritchOrb> {
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

    public EldritchOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0f;
    }

    @Override
    public void render(EldritchOrb orb, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        float spin = orb.tickCount / 80f;
        float grow = Math.min(orb.tickCount, 10) / 10f / 30f;
        RandomSource random = RandomSource.create(orb.getId());
        VertexConsumer rays = buffer.getBuffer(RenderType.lightning());
        pose.pushPose();
        for (int i = 0; i < 12; i++) {
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
                rays.vertex(m, 0f, 0f, 0f).color(255, 255, 255, 255).endVertex();
                rays.vertex(m, p[0], p[1], p[2]).color(0xee, 0xcc, 0xff, 0).endVertex();
                rays.vertex(m, q[0], q[1], q[2]).color(0xee, 0xcc, 0xff, 0).endVertex();
                rays.vertex(m, 0f, 0f, 0f).color(255, 255, 255, 255).endVertex();
            }
        }
        pose.popPose();

        // тёмное ядро: кадры дыма с листа частиц, всегда лицом к камере
        float u0 = (orb.tickCount % 13) / 16f;
        float u1 = u0 + 0.0624375f;
        float v0 = 0.1875f;
        float v1 = v0 + 0.0624375f;
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer core = buffer.getBuffer(RenderType.entityTranslucent(PARTICLES));
        float[][] corners = {{-0.5f, -0.5f, u0, v1}, {0.5f, -0.5f, u1, v1}, {0.5f, 0.5f, u1, v0}, {-0.5f, 0.5f, u0, v0}};
        for (float[] p : corners) {
            core.vertex(matrix, p[0], p[1], 0f).color(1f, 1f, 1f, 1f).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xf000f0).normal(normal, 0f, 1f, 0f).endVertex();
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EldritchOrb orb) {
        return PARTICLES;
    }
}
