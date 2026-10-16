package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.EssentiaBlockEntity;

/**
 * Банка и перегонный куб (TileJarRenderer и TileAlembicRenderer оригинала): эссенция внутри банки столбиком
 * в цвете аспекта и ярлык со значком аспекта на стенке сосуда.
 */
public class JarRenderer implements BlockEntityRenderer<EssentiaBlockEntity> {
    private static final ResourceLocation LABEL = Thaumcraft.id("textures/models/label.png");

    public JarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EssentiaBlockEntity vessel, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (vessel.isJar() && vessel.getAspect() != null) {
            renderEssentia(vessel, pose, buffer, light);
        }
        Aspect filter = vessel.getFilter();
        if (filter != null) {
            renderLabel(vessel, filter, pose, buffer, light);
        }
    }

    private static void renderEssentia(EssentiaBlockEntity jar, PoseStack pose, MultiBufferSource buffer, int light) {
        int color = jar.getAspect().getColor();
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(Thaumcraft.id("block/animatedglow"));
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        // банка занимает 3..13 по ширине и 0..12 по высоте; жидкость чуть внутри стекла
        float x0 = 4.1f / 16f;
        float x1 = 11.9f / 16f;
        float y0 = 1.05f / 16f;
        float y1 = y0 + 9.9f / 16f * jar.getAmount() / jar.getCapacity();
        float u0 = sprite.getU(4);
        float u1 = sprite.getU(12);
        float v0 = sprite.getV(4);
        float v1 = sprite.getV(12);
        // как в оригинале, эссенция светится: яркость не ниже 200 из 240
        int glow = Math.max(light & 0xffff, 200) | light & 0xffff0000;
        float[][] quads = {
                {x0, y1, x0, x0, y1, x1, x1, y1, x1, x1, y1, x0, 0, 1, 0},
                {x0, y0, x1, x0, y0, x0, x1, y0, x0, x1, y0, x1, 0, -1, 0},
                {x0, y1, x0, x1, y1, x0, x1, y0, x0, x0, y0, x0, 0, 0, -1},
                {x1, y1, x1, x0, y1, x1, x0, y0, x1, x1, y0, x1, 0, 0, 1},
                {x0, y1, x1, x0, y1, x0, x0, y0, x0, x0, y0, x1, -1, 0, 0},
                {x1, y1, x0, x1, y1, x1, x1, y0, x1, x1, y0, x0, 1, 0, 0},
        };
        float[][] uvs = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (float[] q : quads) {
            // каждая грань в обе стороны: изнутри банки жидкость тоже видна
            for (int pass = 0; pass < 2; pass++) {
                for (int k = 0; k < 4; k++) {
                    int i = pass == 0 ? k : 3 - k;
                    consumer.vertex(m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).color(r, g, b, 0.9f).uv(uvs[i][0], uvs[i][1])
                            .uv2(glow).normal(n, q[12], q[13], q[14]).endVertex();
                }
            }
        }
    }

    /** Бумажный ярлык на стенке и значок аспекта на нём; ярлык наклеен чуть криво, как в оригинале. */
    private static void renderLabel(EssentiaBlockEntity vessel, Aspect aspect, PoseStack pose, MultiBufferSource buffer, int light) {
        boolean jar = vessel.isJar();
        pose.pushPose();
        pose.translate(0.5f, jar ? 0.4f : 0.468f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-vessel.getLabelSide().toYRot()));
        // стенка банки в 5/16 от середины, панель куба — в 0.409
        pose.translate(0f, 0f, jar ? 0.3155f : 0.411f);
        float crooked = Math.floorMod(aspect.getTag().hashCode() + vessel.getBlockPos().getX() + vessel.getLabelSide().get3DDataValue(), 4) - 2;
        pose.mulPose(Axis.ZP.rotationDegrees(crooked));
        float size = jar ? 0.25f : 0.135f;
        quad(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(LABEL)), size, 0f, 0xffffff, light);
        quad(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(aspect.getImage())), size * 0.68f, 0.001f, aspect.getColor(), light);
        pose.popPose();
    }

    private static void quad(PoseStack pose, VertexConsumer consumer, float half, float z, int color, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        float[][] corners = {{-half, -half, 0f, 1f}, {half, -half, 1f, 1f}, {half, half, 1f, 0f}, {-half, half, 0f, 0f}};
        for (float[] c : corners) {
            consumer.vertex(m, c[0], c[1], z).color(r, g, b, 255).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(n, 0f, 0f, 1f).endVertex();
        }
    }
}
