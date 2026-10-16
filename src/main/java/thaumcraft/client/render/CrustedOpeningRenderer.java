package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.CrustedOpeningBlock;

/** Отдушина на покрытом коркой отверстии — перенос TileEldritchCrabSpawnerRenderer оригинала (модель crabvent.obj). */
public class CrustedOpeningRenderer implements BlockEntityRenderer<CrustedOpeningBlock.Spawner> {
    public static final ResourceLocation VENT = Thaumcraft.id("block/crab_vent");

    public CrustedOpeningRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrustedOpeningBlock.Spawner spawner, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // translateFromOrientation оригинала: модель смотрит на юг (+Z)
        switch (spawner.facing()) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90f));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
            default -> {
            }
        }
        Minecraft mc = Minecraft.getInstance();
        // свет берём с той стороны, куда смотрит отдушина: сам блок непрозрачен
        int lit = spawner.getLevel() == null ? light
                : net.minecraft.client.renderer.LevelRenderer.getLightColor(spawner.getLevel(), spawner.getBlockPos().relative(spawner.facing()));
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                null, mc.getModelManager().getModel(VENT), 1f, 1f, 1f, lit, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
