package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.WandPedestalBlock;

/** Жезл или амулет над пьедесталом подзарядки (TileWandPedestalRenderer оригинала): парит, покачиваясь, и вращается. */
public class WandPedestalRenderer implements BlockEntityRenderer<WandPedestalBlock.Charger> {
    private final ItemRenderer itemRenderer;

    public WandPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(WandPedestalBlock.Charger pedestal, float partialTick, PoseStack pose, MultiBufferSource buffer, int light,
                       int overlay) {
        ItemStack stack = pedestal.getItem();
        Minecraft mc = Minecraft.getInstance();
        if (stack.isEmpty() || pedestal.getLevel() == null || mc.player == null) {
            return;
        }
        float ticks = mc.player.tickCount + partialTick;
        float bob = Mth.sin(ticks % 32767f / 16f) * 0.05f;
        pose.pushPose();
        pose.translate(0.5f, 1.15f + bob, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360f));
        // свет берётся над пьедесталом: сам блок непрозрачен и дал бы черноту
        int above = LevelRenderer.getLightColor(pedestal.getLevel(), pedestal.getBlockPos().above());
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, above, OverlayTexture.NO_OVERLAY, pose, buffer, pedestal.getLevel(), 0);
        pose.popPose();
    }
}
