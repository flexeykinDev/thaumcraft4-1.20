package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.HungryChestBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Голодный сундук — перенос TileChestHungryRenderer оригинала: старая модель сундука со своей текстурой. */
public class HungryChestRenderer implements BlockEntityRenderer<HungryChestBlock.Chest> {
    private static final ResourceLocation TEXTURE = Thaumcraft.id("textures/models/chesthungry.png");

    private final ModelPart lid = part(0, 0, 0f, -5f, -14f, 14, 5, 14, 1f, 7f, 15f);
    private final ModelPart knob = part(0, 0, -1f, -2f, -15f, 2, 4, 1, 8f, 7f, 15f);
    private final ModelPart base = part(0, 19, 0f, 0f, 0f, 14, 10, 14, 1f, 6f, 1f);

    public HungryChestRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, float px, float py, float pz) {
        ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, false, 64f, 64f,
                EnumSet.allOf(Direction.class))), Map.of());
        part.setPos(px, py, pz);
        return part;
    }

    @Override
    public void render(HungryChestBlock.Chest chest, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = chest.getBlockState().getValue(HungryChestBlock.FACING);
        float turn = switch (facing) {
            case NORTH -> 180f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
        pose.pushPose();
        pose.translate(0f, 1f, 1f);
        pose.scale(1f, -1f, -1f);
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        pose.translate(-0.5f, -0.5f, -0.5f);
        float open = 1f - Mth.lerp(partialTick, chest.prevLidAngle, chest.lidAngle);
        open = 1f - open * open * open;
        lid.xRot = -(open * Mth.HALF_PI);
        knob.xRot = lid.xRot;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        lid.render(pose, consumer, light, overlay);
        knob.render(pose, consumer, light, overlay);
        base.render(pose, consumer, light, overlay);
        pose.popPose();
    }
}
