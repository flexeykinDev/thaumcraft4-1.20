package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.FluxScrubberBlock;

/** Очиститель скверны — перенос TileFluxScrubberRenderer оригинала: колпак обелиска с покачивающимся наконечником. */
public class FluxScrubberRenderer implements BlockEntityRenderer<FluxScrubberBlock.Scrubber> {
    public static final ResourceLocation CAP = Thaumcraft.id("block/flux_scrubber_cap");
    public static final ResourceLocation TIP = Thaumcraft.id("block/flux_scrubber_tip");

    public FluxScrubberRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** translateFromOrientation оригинала. */
    static void orient(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90f));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90f));
            default -> {
            }
        }
        pose.translate(0.0, 0.0, -0.5);
    }

    @Override
    public void render(FluxScrubberBlock.Scrubber scrubber, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        orient(pose, scrubber.facing());
        var consumer = buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        var renderer = mc.getBlockRenderer().getModelRenderer();
        renderer.renderModel(pose.last(), consumer, null, mc.getModelManager().getModel(CAP), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        float ticks = (mc.player == null ? 0 : mc.player.tickCount) + partialTick + scrubber.phase;
        float bob = Mth.sin(ticks / 8f) * 0.075f + 0.075f;
        pose.translate(0.0, 0.0, -bob);
        renderer.renderModel(pose.last(), consumer, null, mc.getModelManager().getModel(TIP), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** Очиститель в руке и в интерфейсе: колпак наконечником вверх. */
    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                @Override
                public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                                         PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
                    Minecraft mc = Minecraft.getInstance();
                    pose.pushPose();
                    orient(pose, Direction.DOWN);
                    var consumer = buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
                    var renderer = mc.getBlockRenderer().getModelRenderer();
                    renderer.renderModel(pose.last(), consumer, null, mc.getModelManager().getModel(CAP), 1f, 1f, 1f, light, overlay);
                    renderer.renderModel(pose.last(), consumer, null, mc.getModelManager().getModel(TIP), 1f, 1f, 1f, light, overlay);
                    pose.popPose();
                }
            };
}
