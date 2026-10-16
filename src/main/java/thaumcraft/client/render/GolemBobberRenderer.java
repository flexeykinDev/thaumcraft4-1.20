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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.GolemBobber;

/** Поплавок голема-рыбака — перенос RenderGolemBobber оригинала: сам поплавок и провисающая леска до удочки голема. */
public class GolemBobberRenderer extends EntityRenderer<GolemBobber> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/fishing_hook.png");

    public GolemBobberRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GolemBobber bobber) {
        return TEXTURE;
    }

    @Override
    public void render(GolemBobber bobber, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.pushPose();
        pose.scale(0.5f, 0.5f, 0.5f);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        vertex(consumer, matrix, normal, light, 0f, 0, 0, 1);
        vertex(consumer, matrix, normal, light, 1f, 0, 1, 1);
        vertex(consumer, matrix, normal, light, 1f, 1, 1, 0);
        vertex(consumer, matrix, normal, light, 0f, 1, 0, 0);
        pose.popPose();

        Golem fisher = bobber.getFisher();
        if (fisher != null) {
            // конец удочки: чуть впереди и сбоку от голема, на высоте его груди
            float body = Mth.lerp(partialTick, fisher.yBodyRotO, fisher.yBodyRot) * Mth.DEG_TO_RAD;
            double sin = Mth.sin(body);
            double cos = Mth.cos(body);
            Vec3 golem = fisher.getPosition(partialTick);
            double rodX = golem.x - cos * 0.25 - sin * 0.7;
            double rodY = golem.y + fisher.getEyeHeight() - 0.4;
            double rodZ = golem.z - sin * 0.25 + cos * 0.7;
            Vec3 here = bobber.getPosition(partialTick);
            float dx = (float) (rodX - here.x);
            float dy = (float) (rodY - (here.y + 0.25));
            float dz = (float) (rodZ - here.z);
            VertexConsumer line = buffer.getBuffer(RenderType.lineStrip());
            PoseStack.Pose last = pose.last();
            for (int i = 0; i <= 16; i++) {
                string(dx, dy, dz, line, last, i / 16f, (i + 1) / 16f);
            }
        }
        pose.popPose();
        super.render(bobber, yaw, partialTick, pose, buffer, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light, float x, int y, int u, int v) {
        consumer.vertex(matrix, x - 0.5f, y - 0.5f, 0f).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0f, 1f, 0f).endVertex();
    }

    /** Отрезок лески: она провисает по параболе, как у обычной удочки. */
    private static void string(float dx, float dy, float dz, VertexConsumer consumer, PoseStack.Pose pose, float from, float to) {
        float x = dx * from;
        float y = dy * (from * from + from) * 0.5f + 0.25f;
        float z = dz * from;
        float nx = dx * to - x;
        float ny = dy * (to * to + to) * 0.5f + 0.25f - y;
        float nz = dz * to - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0f) {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        consumer.vertex(pose.pose(), x, y, z).color(0, 0, 0, 255).normal(pose.normal(), nx, ny, nz).endVertex();
    }
}
