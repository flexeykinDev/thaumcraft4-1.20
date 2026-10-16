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
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.StrangeCrystalsBlock;

/**
 * Странные кристаллы — перенос TileEldritchCrystalRenderer оригинала (модель vcrystal.obj): основание из корки и
 * полупрозрачные кристаллы, мерцающие собственным светом. Каждая друза повёрнута по-своему.
 */
public class StrangeCrystalsRenderer implements BlockEntityRenderer<StrangeCrystalsBlock.Crystals> {
    public static final ResourceLocation BASE = Thaumcraft.id("block/strange_crystals_base");
    public static final ResourceLocation CRYSTAL = Thaumcraft.id("block/strange_crystals_crystal");

    public StrangeCrystalsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StrangeCrystalsBlock.Crystals be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // translateFromOrientation оригинала: модель растёт вдоль +Z
        switch (be.getBlockState().getValue(StrangeCrystalsBlock.FACING)) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90f));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(-90f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
            default -> {
            }
        }
        pose.translate(0.0, 0.0, -0.5);
        pose.mulPose(Axis.ZP.rotationDegrees(90f * Math.floorMod(be.getBlockPos().hashCode(), 4)));
        var renderer = mc.getBlockRenderer().getModelRenderer();
        renderer.renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)), null,
                mc.getModelManager().getModel(BASE), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        float ticks = (mc.player == null ? 0 : mc.player.tickCount) + partialTick;
        float shade = Mth.sin(ticks / 6f) * 0.075f + 0.925f;
        int glow = (int) (210f * shade);
        renderer.renderModel(pose.last(), buffer.getBuffer(RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS)), null,
                mc.getModelManager().getModel(CRYSTAL), 1f, 1f, 1f, glow | glow << 16, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
