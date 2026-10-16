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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.CultistPortal;

/**
 * Багровый портал — перенос RenderCultistPortal оригинала: алый разрыв, всегда обращённый к смотрящему. Удар
 * сплющивает его, каждая волна культистов заставляет вспыхнуть, а израненный портал дрожит и меркнет.
 */
public class CultistPortalRenderer extends EntityRenderer<CultistPortal> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/misc/cultist_portal.png");

    public CultistPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.1f;
        shadowStrength = 0.5f;
    }

    @Override
    public void render(CultistPortal portal, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        long time = System.nanoTime() / 50000000L;
        float scaley = 1.5f;
        float e = Math.min(50f, portal.tickCount + partialTick);
        if (portal.hurtTime > 0) {
            double d = Math.sin(portal.hurtTime * 72 * Math.PI / 180.0);
            scaley -= (float) (d / 4.0);
            e += (float) (6.0 * d);
        }
        if (portal.pulse > 0) {
            double d = Math.sin(portal.pulse * 36 * Math.PI / 180.0);
            scaley += (float) (d / 4.0);
            e += (float) (12.0 * d);
        }
        float scale = (int) e / 50f * 1.3f;
        float m = (1f - portal.getHealth() / portal.getMaxHealth()) / 3f;
        float bob = Mth.sin(portal.tickCount / (5f - 12f * m)) * m + m;
        float bob2 = Mth.sin(portal.tickCount / (6f - 15f * m)) * m + m;
        float alpha = 1f - bob;
        scaley -= bob / 4f;
        scale -= bob2 / 3f;
        int frame = 15 - (int) (time % 16);
        float u0 = frame / 16f;
        float u1 = u0 + 0.0625f;
        pose.pushPose();
        pose.translate(0.0, portal.getBbHeight() / 2f, 0.0);
        Quaternionf camera = new Quaternionf(entityRenderDispatcher.cameraOrientation()).mul(Axis.YP.rotationDegrees(180f));
        Matrix4f mat = pose.last().pose();
        Matrix3f n = pose.last().normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        // развёртка как в оригинале: кадры идут по ширине, низ квадрата — верх кадра
        float[][] corners = {{-1f, -1f, u1, 0f}, {1f, -1f, u0, 0f}, {1f, 1f, u0, 1f}, {-1f, 1f, u1, 1f}};
        for (float[] p : corners) {
            Vector3f v = new Vector3f(p[0] * scale, p[1] * scaley, 0f).rotate(camera);
            consumer.vertex(mat, v.x(), v.y(), v.z()).color(1f, 1f, 1f, alpha).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xdc00dc).normal(n, 0f, 0f, -1f).endVertex();
        }
        pose.popPose();
        super.render(portal, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CultistPortal portal) {
        return TEXTURE;
    }
}
