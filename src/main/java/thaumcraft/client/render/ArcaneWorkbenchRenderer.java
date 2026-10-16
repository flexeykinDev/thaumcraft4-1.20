package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity;

/** Жезл, лежащий на магическом верстаке (сам стол — обычная модель блока). */
public class ArcaneWorkbenchRenderer implements BlockEntityRenderer<ArcaneWorkbenchBlockEntity> {
    private final ItemRenderer itemRenderer;

    public ArcaneWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ArcaneWorkbenchBlockEntity workbench, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {
        ItemStack wand = workbench.getWand();
        if (wand.isEmpty()) {
            return;
        }
        pose.pushPose();
        // Жезл лежит плашмя посередине столешницы, чуть наискосок. Модель жезла длиной 1.375 блока,
        // поэтому она ужата вдвое, чтобы целиком помещаться на столе.
        pose.translate(0.5f, 1.04f, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        pose.mulPose(Axis.ZP.rotationDegrees(20f));
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(wand, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer,
                workbench.getLevel(), 0);
        pose.popPose();
    }
}
