package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entity.PrimalArrow;

/**
 * Первозданная стрела — перенос RenderPrimalArrow оригинала: обычная стрела, окутанная огоньком цвета своей стихии
 * (у энтропийной огонёк тёмный). Воткнувшись, огонёк понемногу гаснет.
 */
public class PrimalArrowRenderer extends ArrowRenderer<PrimalArrow> {
    private static final ResourceLocation ARROW = new ResourceLocation("textures/entity/projectiles/arrow.png");
    private static final ResourceLocation WISP = Thaumcraft.id("textures/misc/wisp.png");
    /** Цвета осколков (BlockCustomOreItem.colors[1..6] оригинала). */
    private static final int[] COLORS = {0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};

    public PrimalArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PrimalArrow arrow) {
        return ARROW;
    }

    @Override
    public void render(PrimalArrow arrow, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        super.render(arrow, yaw, partialTick, pose, buffer, light);
        int type = Math.min(arrow.getArrowType(), 5);
        int color = COLORS[type];
        float alpha = Math.max(0f, (100f - arrow.groundTime()) / 100f);
        int frame = arrow.tickCount % 16;
        float u0 = frame % 4 / 4f;
        float v0 = frame / 4 / 4f;
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(type < 5 ? ModRenderTypes.additive(WISP) : ModRenderTypes.translucentQuad(WISP));
        float size = 0.5f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        consumer.vertex(matrix, -size, -size, 0f).uv(u0 + 0.2499f, v0 + 0.2499f).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, -size, size, 0f).uv(u0 + 0.2499f, v0).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, size, size, 0f).uv(u0, v0).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, size, -size, 0f).uv(u0, v0 + 0.2499f).color(r, g, b, alpha).endVertex();
        pose.popPose();
    }
}
