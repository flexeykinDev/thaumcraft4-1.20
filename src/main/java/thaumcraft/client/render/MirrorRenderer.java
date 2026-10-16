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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.MirrorBlock;

/**
 * Зеркала — перенос TileMirrorRenderer оригинала. Рамка зеркала — объёмная, как предмет в руке. Несвязанное зеркало
 * матово-серое; в связанном, если смотреть на него спереди, за стеклом клубится бездна (та же, что в портативной
 * дыре), а у раскачанного зеркала стекло дрожит.
 */
public class MirrorRenderer implements BlockEntityRenderer<MirrorBlock.Mirror> {
    public static final ResourceLocation FRAME = Thaumcraft.id("item/mirror_frame");
    public static final ResourceLocation FRAME_ESSENTIA = Thaumcraft.id("item/mirror_frame_essentia");
    private static final ResourceLocation PANE = Thaumcraft.id("textures/block/mirrorpane.png");
    private static final ResourceLocation PANE_OPEN = Thaumcraft.id("textures/block/mirrorpanetrans.png");

    public MirrorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MirrorBlock.Mirror mirror, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = mirror.facing();
        Minecraft mc = Minecraft.getInstance();
        float jitter = mirror.instability > 0 && mirror.getLevel() != null
                ? mirror.getLevel().random.nextFloat() * (mirror.instability / 10000f) : 0f;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // локальная ось +Z смотрит туда же, куда зеркало; стена позади — на z = -0.5
        pose.mulPose(facing.getRotation());
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 toEye = eye.subtract(Vec3.atCenterOf(mirror.getBlockPos()));
        boolean inFront = toEye.dot(Vec3.atLowerCornerOf(facing.getNormal())) > -0.4;
        boolean open = mirror.linked && inFront;
        if (open) {
            // бездна за стеклом
            Matrix4f matrix = pose.last().pose();
            VertexConsumer portal = buffer.getBuffer(ModRenderTypes.HOLE);
            float z = -0.49f;
            portal.vertex(matrix, -0.5f, -0.5f, z).endVertex();
            portal.vertex(matrix, 0.5f, -0.5f, z).endVertex();
            portal.vertex(matrix, 0.5f, 0.5f, z).endVertex();
            portal.vertex(matrix, -0.5f, 0.5f, z).endVertex();
        }
        // стекло: прозрачное у открытого зеркала, матовое у закрытого
        drawPane(pose, buffer.getBuffer(RenderType.entityTranslucent(open ? PANE_OPEN : PANE)), -0.5f + 0.02f + jitter, light);
        // объёмная рамка толщиной в пиксель
        pose.pushPose();
        pose.translate(-0.5, -0.5, -0.5 - 7.5 / 16.0 + 0.5 / 16.0);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                null, mc.getModelManager().getModel(mirror.essentia() ? FRAME_ESSENTIA : FRAME), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.popPose();
    }

    private static void drawPane(PoseStack pose, VertexConsumer consumer, float z, int light) {
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        float[][] corners = {{-0.5f, -0.5f, 0f, 1f}, {0.5f, -0.5f, 1f, 1f}, {0.5f, 0.5f, 1f, 0f}, {-0.5f, 0.5f, 0f, 0f}};
        for (float[] c : corners) {
            consumer.vertex(matrix, c[0], c[1], z).color(255, 255, 255, 255).uv(c[2], c[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(normal, 0f, 0f, 1f).endVertex();
        }
    }
}
