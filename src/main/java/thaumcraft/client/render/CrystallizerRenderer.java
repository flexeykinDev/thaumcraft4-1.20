package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.CrystallizerBlock;

/**
 * Кристаллизатор — перенос TileEssentiaCrystalizerRenderer оригинала: корпус из модели crystalizer.obj
 * и четыре кристалла вокруг него (часть Crystal модели vis_relay.obj). Кристаллы светятся, окрашиваются
 * в цвет перерабатываемого аспекта и раскручиваются, пока идёт работа.
 */
public class CrystallizerRenderer implements BlockEntityRenderer<CrystallizerBlock.Crystallizer> {
    /** Модели грузятся отдельно от блоков (ModelEvent.RegisterAdditional в ClientEvents). */
    public static final ResourceLocation BODY = Thaumcraft.id("block/crystalizer");
    public static final ResourceLocation CRYSTAL = Thaumcraft.id("block/vis_relay_crystal");

    public CrystallizerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrystallizerBlock.Crystallizer tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel body = mc.getModelManager().getModel(BODY);
        BakedModel crystal = mc.getModelManager().getModel(CRYSTAL);
        Direction facing = tile.getBlockState().getValue(CrystallizerBlock.FACING);
        float ticks = mc.player == null ? 0f : mc.player.tickCount + partialTick;

        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        // модель лежит вдоль оси Z основанием к северу; остальные стороны — поворотом (translateFromOrientation оригинала)
        switch (facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90f));
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90f));
            default -> {
            }
        }
        pose.translate(0f, 0f, -0.5f);
        VertexConsumer solid = buffer.getBuffer(Sheets.cutoutBlockSheet());
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), solid, null, body, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);

        float[] color = tile.getColor();
        VertexConsumer glowing = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        for (int q = 0; q < 4; q++) {
            pose.pushPose();
            pose.scale(0.75f, 0.75f, 0.75f);
            // кристаллы мерцают вразнобой: яркость от 190 до 200 из 240
            float glow = Mth.sin((ticks + q * 10) / 2f) * 0.05f + 0.95f;
            int brightness = Mth.clamp((int) ((50 + 150f * glow) / 16f), 0, 15);
            int crystalLight = Math.max(light & 0xffff, brightness << 4) | light & 0xffff0000;
            pose.mulPose(Axis.ZP.rotationDegrees(90f * q));
            pose.translate(0.34f, 0f, 1.2125f);
            pose.mulPose(Axis.ZP.rotationDegrees(tile.getSpin(partialTick)));
            mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), glowing, null, crystal,
                    Math.min(1f, color[0]), Math.min(1f, color[1]), Math.min(1f, color[2]), crystalLight, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
    }
}
