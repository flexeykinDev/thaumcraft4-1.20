package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.entity.Wisp;

/**
 * Висп — перенос RenderWisp оригинала: два светящихся слоя, всегда повёрнутых к камере. Большой — шестнадцать кадров
 * пламени в цвете аспекта (краснеет от удара), малый — белое пульсирующее ядро из листа частиц.
 */
public class WispRenderer extends EntityRenderer<Wisp> {
    private static final ResourceLocation WISP = Thaumcraft.id("textures/misc/wisp.png");
    private static final ResourceLocation PARTICLES = Thaumcraft.id("textures/misc/particles.png");

    public WispRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(Wisp wisp, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        if (wisp.getHealth() <= 0f) {
            return;
        }
        Aspect aspect = wisp.getAspect();
        int color = aspect == null ? 0 : aspect.getColor();
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        if (wisp.hurtTime > 0) {
            r = 1f;
            g = (color >> 8 & 255) / 300f;
            b = (color & 255) / 300f;
        }
        pose.pushPose();
        pose.translate(0f, 0.45f, 0f);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Matrix4f matrix = pose.last().pose();

        int frame = wisp.tickCount % 16;
        float u0 = frame % 4 / 4f;
        float v0 = frame / 4 / 4f;
        quad(buffer.getBuffer(ModRenderTypes.additive(WISP)), matrix, 1f, u0, v0, u0 + 0.2499f, v0 + 0.2499f, r, g, b);

        float core = 0.4f + Mth.sin((wisp.tickCount + partialTick) / 10f) * 0.1f;
        float cu = frame / 16f;
        quad(buffer.getBuffer(ModRenderTypes.additive(PARTICLES)), matrix, core, cu, 5f / 16f, cu + 1f / 16f, 6f / 16f, 1f, 1f, 1f);
        pose.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, float size, float u0, float v0, float u1, float v1, float r, float g, float b) {
        consumer.vertex(matrix, -size, -size, 0f).uv(u1, v1).color(r, g, b, 1f).endVertex();
        consumer.vertex(matrix, -size, size, 0f).uv(u1, v0).color(r, g, b, 1f).endVertex();
        consumer.vertex(matrix, size, size, 0f).uv(u0, v0).color(r, g, b, 1f).endVertex();
        consumer.vertex(matrix, size, -size, 0f).uv(u0, v1).color(r, g, b, 1f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(Wisp wisp) {
        return WISP;
    }
}
