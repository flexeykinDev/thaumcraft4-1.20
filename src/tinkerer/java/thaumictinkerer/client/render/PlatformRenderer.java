package thaumictinkerer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import thaumictinkerer.common.block.PlatformBlock;

/** Замаскированная платформа рисует вместо себя выбранный блок (getIcon BlockCamo оригинала). */
public class PlatformRenderer implements BlockEntityRenderer<PlatformBlock.Tile> {
    public PlatformRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PlatformBlock.Tile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        BlockState camo = tile.getCamo();
        if (camo != null) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(camo, pose, buffer, light, OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY, null);
        }
    }
}
