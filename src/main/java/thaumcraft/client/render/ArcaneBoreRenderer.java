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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.ArcaneBoreBaseBlock;
import thaumcraft.common.blocks.ArcaneBoreBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Чародейский бур и его основание — перенос TileArcaneBoreRenderer, TileArcaneBoreBaseRenderer и моделей ModelBore,
 * ModelBoreBase, ModelBoreEmit оригинала: головка поворачивается к стороне раскопок, вертушка на ней крутится,
 * в стеклянной колбе вихрится воронка.
 */
public final class ArcaneBoreRenderer {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/bore.png");
    private static final ResourceLocation JAR = Thaumcraft.id("textures/models/jar.png");
    private static final ResourceLocation VORTEX = Thaumcraft.id("textures/misc/vortex.png");
    // ModelBore
    private static final ModelPart[] BASE = {box(0, 32, -6, 0, -6, 12, 2, 12, 0, 0, 0), box(0, 0, -2, 2, -5.5f, 4, 8, 1, 0, 0, 0),
            box(0, 0, -2, 2, 4.5f, 4, 8, 1, 0, 0, 0), box(0, 48, -1, -1, -6, 2, 2, 12, 0, 8, 0)};
    private static final ModelPart[] NOZZLE = {box(30, 14, 4, -2.5f, -2.5f, 4, 5, 5, 0, 8, 0), box(0, 14, -2, -4, -4, 6, 8, 8, 0, 8, 0)};
    // ModelBoreEmit
    private static final ModelPart KNOB = box(66, 0, -2, 12, -2, 4, 4, 4, 0, 0, 0);
    private static final ModelPart[] EMIT = {box(56, 16, -2, 0, -2, 4, 1, 4, 0, 8, 0), box(56, 16, -2, 0, -2, 4, 1, 4, 0, 0, 0),
            box(56, 24, -3, 4, -3, 6, 1, 6, 0, 0, 0), box(56, 0, -1, 1, -1, 2, 11, 2, 0, 0, 0)};
    // ModelBoreBase
    private static final ModelPart[] STAND = {box(64, 24, -8, 0, -8, 16, 2, 16, 0, 0, 0), box(64, 24, -8, 0, -8, 16, 2, 16, 0, 14, 0),
            box(84, 42, -2.5f, 0, -2.5f, 5, 12, 5, 0, 2, 0), box(64, 42, -2, 0, -2, 4, 12, 4, -5, 2, -5), box(64, 42, -2, 0, -2, 4, 12, 4, -5, 2, 5),
            box(64, 42, -2, 0, -2, 4, 12, 4, 5, 2, 5), box(64, 42, -2, 0, -2, 4, 12, 4, 5, 2, -5)};
    private static final ModelPart[] STAND_NOZZLE = {box(106, 42, 2.5f, -2, -2, 5, 4, 4, 0, 8, 0), box(106, 51, 7, -2.5f, -2.5f, 1, 5, 5, 0, 8, 0)};
    private static final ModelPart JAR_CORE = new ModelPart(List.of(new ModelPart.Cube(0, 0, -5f, -12f, -5f, 10f, 12f, 10f, 0f, 0f, 0f, true,
            64f, 32f, EnumSet.allOf(Direction.class))), Map.of());

    private ArcaneBoreRenderer() {
    }

    private static ModelPart box(int u, int v, float x, float y, float z, float w, float h, float d, float px, float py, float pz) {
        ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, true, 128f, 64f,
                EnumSet.allOf(Direction.class))), Map.of());
        part.setPos(px, py, pz);
        return part;
    }

    private static void render(ModelPart[] parts, PoseStack pose, VertexConsumer consumer, int light) {
        for (ModelPart part : parts) {
            part.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        }
    }

    /** Головка бура. rotX/rotZ — повороты в градусах, top — вертушка, ticks — время для воронки. */
    static void drawBore(PoseStack pose, MultiBufferSource buffer, int light, float rotX, float rotZ, float top, boolean focus, boolean hanging,
                         float ticks) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(rotX));
        pose.pushPose();
        if (hanging) {
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
        }
        pose.translate(0f, -0.5f, 0f);
        render(BASE, pose, consumer, light);
        pose.popPose();
        pose.mulPose(Axis.ZP.rotationDegrees(rotZ));
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(90f));
        pose.translate(0f, -0.5f, 0f);
        render(NOZZLE, pose, consumer, light);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(top));
        pose.translate(0f, 0.5f, 0f);
        if (focus) {
            KNOB.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        }
        render(EMIT, pose, consumer, light);
        pose.popPose();
        // три воронки в колбе
        float rotation = ticks % 45f;
        vortex(pose, buffer, -0.17f, -(rotation * 8f), 10f, 0.4f, 1f, light);
        vortex(pose, buffer, -0.21f, rotation * 8f, 10f, 0.3f, 0.8f, light);
        vortex(pose, buffer, -0.25f, -(rotation * 8f), -10f, 0.2f, 0.8f, light);
        // колба
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(180f));
        pose.translate(0f, 0.3f, 0f);
        pose.scale(0.6f, 0.6f, 0.6f);
        JAR_CORE.render(pose, buffer.getBuffer(RenderType.entityTranslucent(JAR)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.popPose();
    }

    private static void vortex(PoseStack pose, MultiBufferSource buffer, float y, float spin, float tilt, float scale, float alpha, int light) {
        pose.pushPose();
        pose.translate(0f, y, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        pose.mulPose(Axis.YP.rotationDegrees(tilt));
        pose.scale(scale, scale, scale);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(VORTEX));
        int a = (int) (alpha * 255f);
        float[][] corners = {{-0.5f, 0.5f, 0f, 1f}, {0.5f, 0.5f, 1f, 1f}, {0.5f, -0.5f, 1f, 0f}, {-0.5f, -0.5f, 0f, 0f}};
        for (float[] c : corners) {
            consumer.vertex(matrix, c[0], c[1], 0f).color(255, 255, 255, a).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(0xF000F0).normal(normal, 0f, 0f, 1f).endVertex();
        }
        pose.popPose();
    }

    /** Основание: стойка и носик в сторону выдачи. */
    static void drawBase(PoseStack pose, MultiBufferSource buffer, int light, Direction nozzle) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        render(STAND, pose, consumer, light);
        float turn = switch (nozzle) {
            case NORTH -> 90f;
            case SOUTH -> 270f;
            case WEST -> 180f;
            default -> 0f;
        };
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        render(STAND_NOZZLE, pose, consumer, light);
        pose.popPose();
    }

    public static class Bore implements BlockEntityRenderer<ArcaneBoreBlock.Bore> {
        public Bore(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(ArcaneBoreBlock.Bore bore, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            Minecraft mc = Minecraft.getInstance();
            float ticks = (mc.player == null ? 0 : mc.player.tickCount) + partialTick;
            drawBore(pose, buffer, light, bore.rotX - bore.vRadX + partialTick * bore.speedX, bore.rotZ - bore.vRadZ + partialTick * bore.speedZ,
                    bore.topRotation, bore.hasFocus, bore.hanging(), ticks);
        }
    }

    public static class Base implements BlockEntityRenderer<ArcaneBoreBaseBlock.Base> {
        public Base(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(ArcaneBoreBaseBlock.Base base, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            drawBase(pose, buffer, light, base.facing());
        }
    }

    /** Бур и основание в руке и в интерфейсе. */
    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                @Override
                public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                                         PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
                    if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof ArcaneBoreBaseBlock) {
                        drawBase(pose, buffer, light, Direction.EAST);
                    } else {
                        drawBore(pose, buffer, light, 0f, 90f, 0f, false, false, 0f);
                    }
                }
            };
}
