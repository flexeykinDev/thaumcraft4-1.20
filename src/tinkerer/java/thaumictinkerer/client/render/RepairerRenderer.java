package thaumictinkerer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import thaumcraft.client.render.ModelParts;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.RepairerBlock;

import java.util.List;

/**
 * RenderTileRepairer оригинала: каркас со стеклом, внутри медленно вращается чинимая вещь, а над крышкой
 * кружит значок — яркий, пока вещь чинится, и тусклый, когда работы нет.
 */
public class RepairerRenderer implements BlockEntityRenderer<RepairerBlock.Tile> {
    private static final ResourceLocation TEXTURE = ThaumicTinkerer.id("textures/models/repairer.png");
    private static final ResourceLocation REPAIR = ThaumicTinkerer.id("textures/misc/repair.png");
    private static final ResourceLocation REPAIR_OFF = ThaumicTinkerer.id("textures/misc/repair_off.png");

    // ModelRepairer оригинала (tools/convert_model.py)
    private static final List<ModelPart> FRAME = List.of(
            ModelParts.part(0, 0, false, 64f, 64f, -8f, 23f, -8f, 0f, 0f, 0f, 0f, 0f, 0f, 16f, 1f, 16f, 0f),
            ModelParts.part(0, 17, false, 64f, 64f, -8f, 9f, 6f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 14f, 2f, 0f),
            ModelParts.part(0, 17, false, 64f, 64f, 6f, 9f, 6f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 14f, 2f, 0f),
            ModelParts.part(0, 17, false, 64f, 64f, -8f, 9f, -8f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 14f, 2f, 0f),
            ModelParts.part(0, 17, false, 64f, 64f, 6f, 9f, -8f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 14f, 2f, 0f),
            ModelParts.part(11, 19, false, 64f, 64f, -8f, 8f, -8f, 0f, 0f, 0f, 0f, 0f, 0f, 16f, 1f, 2f, 0f),
            ModelParts.part(11, 19, false, 64f, 64f, -8f, 8f, 6f, 0f, 0f, 0f, 0f, 0f, 0f, 16f, 1f, 2f, 0f),
            ModelParts.part(11, 23, false, 64f, 64f, 6f, 8f, -6f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 1f, 12f, 0f),
            ModelParts.part(11, 23, false, 64f, 64f, -8f, 8f, -6f, 0f, 0f, 0f, 0f, 0f, 0f, 2f, 1f, 12f, 0f),
            ModelParts.part(48, 30, false, 64f, 64f, -2.5f, 14f, 7f, 0f, 0f, 0f, 0f, 0f, 0f, 5f, 1f, 1f, 0f),
            ModelParts.part(48, 30, false, 64f, 64f, -2.5f, 17f, 7f, 0f, 0f, 0f, 0f, 0f, 0f, 5f, 1f, 1f, 0f),
            ModelParts.part(48, 24, false, 64f, 64f, -2f, 14.5f, 7f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 3f, 1f, 0f),
            ModelParts.part(48, 24, false, 64f, 64f, 1f, 14.5f, 7f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 3f, 1f, 0f));
    private static final List<ModelPart> GLASS = List.of(
            ModelParts.part(-11, 37, false, 64f, 64f, -6f, 8.5f, -6f, 0f, 0f, 0f, 0f, 0f, 0f, 12f, 0f, 12f, 0f),
            ModelParts.part(1, 38, false, 64f, 64f, 7.5f, 9f, -6f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 14f, 12f, 0f),
            ModelParts.part(1, 38, false, 64f, 64f, -7.5f, 9f, 6f, 0f, (float) Math.PI, 0f, 0f, 0f, 0f, 0f, 14f, 12f, 0f),
            ModelParts.part(40, 34, false, 64f, 64f, -6f, 9f, -7.5f, 0f, 0f, 0f, 0f, 0f, 0f, 12f, 14f, 0f, 0f),
            ModelParts.part(33, 50, false, 64f, 64f, 6f, 9f, 7.5f, 0f, (float) Math.PI, 0f, 0f, 0f, 0f, 12f, 14f, 0f, 0f));

    public static final BlockEntityWithoutLevelRenderer ITEM = new BlockEntityWithoutLevelRenderer(
            Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light,
                                 int overlay) {
            drawModel(pose, buffer, Direction.SOUTH, light, overlay);
        }
    };

    public RepairerRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Каркас и стекло, повёрнутые лицом в сторону facing (в оригинале — поворот на 0/90/180/270 по метаданным). */
    static void drawModel(PoseStack pose, MultiBufferSource buffer, Direction facing, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1f, -1f, -1f);
        pose.mulPose(Axis.YP.rotationDegrees(yaw(facing)));
        VertexConsumer solid = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        for (ModelPart part : FRAME) {
            part.render(pose, solid, light, overlay);
        }
        VertexConsumer glass = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        for (ModelPart part : GLASS) {
            part.render(pose, glass, light, overlay);
        }
        pose.popPose();
    }

    private static float yaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f;
        };
    }

    @Override
    public void render(RepairerBlock.Tile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        drawModel(pose, buffer, tile.getBlockState().getValue(RepairerBlock.FACING), light, overlay);
        float time = tile.ticks + partialTick;
        ItemStack item = tile.getItem();
        if (!item.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.5 + Mth.sin(time / 10f) * 0.05, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 0.75f % 360f));
            pose.scale(0.5f, 0.5f, 0.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffer, tile.getLevel(), 0);
            pose.popPose();
        }
        // значок над крышкой: шире самого блока и медленно кружится
        pose.pushPose();
        pose.translate(0.5, 0.975, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 0.75f % 360f));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer icon = buffer.getBuffer(RenderType.entityTranslucent(tile.isWorking() ? REPAIR : REPAIR_OFF));
        float half = 0.625f;
        int full = LightTexture.FULL_BRIGHT;
        icon.vertex(matrix, -half, 0, half).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full)
                .normal(0, 1, 0).endVertex();
        icon.vertex(matrix, half, 0, half).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full)
                .normal(0, 1, 0).endVertex();
        icon.vertex(matrix, half, 0, -half).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full)
                .normal(0, 1, 0).endVertex();
        icon.vertex(matrix, -half, 0, -half).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(full)
                .normal(0, 1, 0).endVertex();
        pose.popPose();
    }
}
