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
import thaumcraft.common.blocks.BellowsBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Мехи — перенос ModelBellows и TileBellowsRenderer оригинала: три дощечки, кожаный мешок между ними и сопло.
 * Мешок сжимается и раздувается, крайние дощечки ходят вместе с ним.
 */
public class BellowsRenderer implements BlockEntityRenderer<BellowsBlock.Bellows> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/bellows.png");

    private final ModelPart bottomPlank = part(0, 0, -6f, 0f, -6f, 12f, 2f, 12f, 0f, 22f, 0f);
    private final ModelPart middlePlank = part(0, 0, -6f, -1f, -6f, 12f, 2f, 12f, 0f, 16f, 0f);
    private final ModelPart topPlank = part(0, 0, -6f, 0f, -6f, 12f, 2f, 12f, 0f, 8f, 0f);
    private final ModelPart bag = part(48, 0, -10f, -12.03333f, -10f, 20f, 24f, 20f, 0f, 0.5f, 0f);
    private final ModelPart nozzle = part(0, 36, -2f, -2f, 0f, 4f, 4f, 2f, 0f, 16f, 6f);

    public BellowsRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart part(int u, int v, float x, float y, float z, float w, float h, float d, float px, float py, float pz) {
        ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, false, 128f, 64f,
                EnumSet.allOf(Direction.class))), Map.of());
        part.setPos(px, py, pz);
        return part;
    }

    @Override
    public void render(BellowsBlock.Bellows bellows, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        float scale = bellows.getInflation(partialTick);
        float travel = 0.125f + scale * 0.875f;
        Direction facing = bellows.getBlockState().getValue(BellowsBlock.FACING);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        pose.pushPose();
        pose.translate(0.5f, -0.5f, 0.5f);
        // модель смотрит соплом на юг; остальные направления — поворотом, как translateFromOrientation оригинала
        switch (facing) {
            case DOWN -> {
                pose.translate(0f, 1f, -1f);
                pose.mulPose(Axis.XP.rotationDegrees(90f));
            }
            case UP -> {
                pose.translate(0f, 1f, 1f);
                pose.mulPose(Axis.XP.rotationDegrees(270f));
            }
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
            default -> {
            }
        }
        pose.translate(0f, 1f, 0f);
        pose.pushPose();
        pose.scale(0.5f, (scale + 0.1f) / 2f, 0.5f);
        bag.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.translate(0f, -1f, 0f);

        pose.pushPose();
        pose.translate(0f, -travel / 2f + 0.5f, 0f);
        topPlank.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.translate(0f, travel / 2f - 0.5f, 0f);
        bottomPlank.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        middlePlank.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        nozzle.render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
