package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blockentity.CentrifugeBlockEntity;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Центрифуга — перенос ModelCentrifuge и TileCentrifugeRenderer оригинала: неподвижные верх и низ
 * и ротор между ними (ось, перекладина и два груза), который раскручивается, пока идёт работа.
 */
public class CentrifugeRenderer implements BlockEntityRenderer<CentrifugeBlockEntity> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/centrifuge.png");

    private final ModelPart top = part(20, 16, -4f, -8f, -4f, 8f, 4f, 8f);
    private final ModelPart bottom = part(20, 16, -4f, 4f, -4f, 8f, 4f, 8f);
    private final ModelPart crossbar = part(16, 0, -4f, -1f, -1f, 8f, 2f, 2f);
    private final ModelPart weightA = part(0, 16, 4f, -3f, -2f, 4f, 6f, 4f);
    private final ModelPart weightB = part(0, 16, -8f, -3f, -2f, 4f, 6f, 4f);
    private final ModelPart core = part(0, 0, -1.5f, -4f, -1.5f, 3f, 8f, 3f);

    public CentrifugeRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart part(int u, int v, float x, float y, float z, float w, float h, float d) {
        return new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, false, 64f, 32f,
                EnumSet.allOf(Direction.class))), Map.of());
    }

    @Override
    public void render(CentrifugeBlockEntity centrifuge, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        top.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        bottom.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.mulPose(Axis.YP.rotationDegrees(centrifuge.getRotation(partialTick)));
        crossbar.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        weightA.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        weightB.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        core.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
