package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.EldritchPortalBlock;

/**
 * Портал Элдрича — перенос TileEldritchPortalRenderer оригинала: клубящийся лиловый разрыв два на два блока, всегда
 * обращённый к смотрящему; раскрываясь, он сперва вытягивается по высоте, потом раздаётся вширь.
 */
public class EldritchPortalRenderer implements BlockEntityRenderer<EldritchPortalBlock.Portal> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/misc/eldritch_portal.png");

    public EldritchPortalRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EldritchPortalBlock.Portal portal, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        long time = System.nanoTime() / 50000000L;
        float c = Math.min(30f, portal.opencount + partialTick);
        float e = Math.min(5f, portal.opencount + partialTick);
        float scale = Math.max(0f, e) / 5f;
        float scaley = Math.max(0f, c) / 30f;
        int frame = (int) (time % 16);
        float u0 = frame / 16f;
        float u1 = u0 + 0.0625f;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Quaternionf camera = new Quaternionf(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation())
                .mul(com.mojang.math.Axis.YP.rotationDegrees(180f));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        // развёртка как в оригинале: кадры лежат поперёк, по высоте квадрата
        float[][] corners = {{-1f, -1f, u0, 1f}, {1f, -1f, u0, 0f}, {1f, 1f, u1, 0f}, {-1f, 1f, u1, 1f}};
        for (float[] p : corners) {
            // квадрат в плоскости экрана: по горизонтали тянется scale, по вертикали scaley
            Vector3f v = new Vector3f(p[0] * scale, p[1] * scaley, 0f).rotate(camera);
            consumer.vertex(m, v.x(), v.y(), v.z()).color(1f, 1f, 1f, 1f).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xdc00dc).normal(n, 0f, 0f, -1f).endVertex();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(EldritchPortalBlock.Portal portal) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
