package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.ThaumBannerBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Знамя — перенос TileBannerRenderer и ModelBanner оригинала: древко, перекладина с ушками, колышущееся на ветру
 * полотнище своего цвета и знак аспекта на нём.
 */
public class ThaumBannerRenderer implements BlockEntityRenderer<ThaumBannerBlock.Banner> {
    private static final ResourceLocation BLANK = Thaumcraft.id("textures/models/banner_blank.png");
    private static final ResourceLocation CULT = Thaumcraft.id("textures/models/banner_cultist.png");
    private static final ModelPart TAB_L = part(0, 29, -5f, -7.5f, -1.5f, 2, 3, 3, 0f, 0f, 0f);
    private static final ModelPart TAB_R = part(0, 29, 3f, -7.5f, -1.5f, 2, 3, 3, 0f, 0f, 0f);
    private static final ModelPart BEAM = part(30, 0, -7f, -7f, -1f, 14, 2, 2, 0f, 0f, 0f);
    private static final ModelPart CLOTH = part(0, 0, -7f, 0f, -0.5f, 14, 28, 1, 0f, -5f, 0f);
    private static final ModelPart POLE = part(62, 0, 0f, 0f, -1f, 2, 31, 2, -1f, -7f, -2f);

    public ThaumBannerRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, false, 128f, 64f,
                EnumSet.allOf(Direction.class))), Map.of());
        part.setPos(px, py, pz);
        return part;
    }

    /** Знамя целиком; rotation — один из 16 поворотов, time — время для колыхания (или 0 в руке). */
    public static void draw(PoseStack pose, MultiBufferSource buffer, int light, int color, Aspect aspect, boolean wall, int rotation,
                            float time) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        pose.mulPose(Axis.YP.rotationDegrees(rotation * 360f / 16f));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(color < 0 && aspect == null ? CULT : BLANK));
        if (!wall) {
            POLE.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        } else {
            pose.translate(0.0, 0.0, -0.4125);
        }
        BEAM.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        float r = 1f;
        float g = 1f;
        float b = 1f;
        if (color >= 0) {
            int rgb = ThaumBannerBlock.COLORS[color & 15];
            r = (rgb >> 16 & 255) / 255f;
            g = (rgb >> 8 & 255) / 255f;
            b = (rgb & 255) / 255f;
        }
        TAB_L.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
        TAB_R.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
        float sway = (0.005f + 0.005f * Mth.cos(time * Mth.PI * 0.02f)) * Mth.PI;
        CLOTH.xRot = sway;
        CLOTH.render(pose, consumer, light, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
        if (aspect != null) {
            pose.pushPose();
            pose.translate(0f, 0f, 0.05001f);
            pose.scale(0.0375f, 0.0375f, 0.0375f);
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            pose.mulPose(Axis.XP.rotationDegrees(-sway * Mth.RAD_TO_DEG * 2f));
            Matrix4f matrix = pose.last().pose();
            Matrix3f normal = pose.last().normal();
            VertexConsumer icon = buffer.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
            int c = aspect.getColor();
            int cr = c >> 16 & 255;
            int cg = c >> 8 & 255;
            int cb = c & 255;
            float x = -8f;
            float y = 4f;
            float[][] corners = {{x, y + 16f, 0f, 1f}, {x + 16f, y + 16f, 1f, 1f}, {x + 16f, y, 1f, 0f}, {x, y, 0f, 0f}};
            for (float[] corner : corners) {
                icon.vertex(matrix, corner[0], corner[1], 0f).color(cr, cg, cb, 191).uv(corner[2], corner[3])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0f, 0f, 1f).endVertex();
            }
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public void render(ThaumBannerBlock.Banner banner, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        var pos = banner.getBlockPos();
        long time = banner.getLevel() == null ? 0L : banner.getLevel().getGameTime();
        float t = pos.getX() * 7 + pos.getY() * 9 + pos.getZ() * 13 + time + partialTick;
        var state = banner.getBlockState();
        draw(pose, buffer, light, banner.color, banner.aspect, state.getValue(ThaumBannerBlock.WALL), state.getValue(ThaumBannerBlock.ROTATION), t);
    }

    /** Знамя в руке и в интерфейсе. */
    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                @Override
                public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                                         PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
                    Aspect aspect = stack.hasTag() ? Aspect.byTag(stack.getTag().getString("aspect")) : null;
                    draw(pose, buffer, light, ThaumBannerBlock.getColor(stack), aspect, false, 8, 0f);
                }
            };
}
