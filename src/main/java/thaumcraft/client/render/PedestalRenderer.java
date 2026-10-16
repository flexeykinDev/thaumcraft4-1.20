package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blockentity.PedestalBlockEntity;

/** Предмет, парящий над пьедесталом и медленно вращающийся (TilePedestalRenderer оригинала). */
public class PedestalRenderer implements BlockEntityRenderer<PedestalBlockEntity> {
    private final ItemRenderer itemRenderer;

    public PedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(PedestalBlockEntity pedestal, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        ItemStack stack = pedestal.getItem();
        if (stack.isEmpty() || pedestal.getLevel() == null) {
            return;
        }
        float time = pedestal.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5f, 1.25f + Mth.sin(time / 10f) * 0.03f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(time % 360f));
        pose.scale(0.75f, 0.75f, 0.75f);
        // свет берётся над пьедесталом: сам блок непрозрачен и дал бы черноту
        int above = LevelRenderer.getLightColor(pedestal.getLevel(), pedestal.getBlockPos().above());
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, above, OverlayTexture.NO_OVERLAY, pose, buffer,
                pedestal.getLevel(), 0);
        pose.popPose();
    }
}
