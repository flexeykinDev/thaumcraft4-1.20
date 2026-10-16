package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.AdvancedAlchemyFurnaceBlock;

/**
 * Продвинутая алхимическая печь — перенос TileAlchemyFurnaceAdvancedRenderer оригинала: вся конструкция рисуется
 * от ядра одной моделью (основание и четыре бака). Раскалённая печь меняет текстуру и светится огнём в топках,
 * в окошках баков виден уровень варева.
 */
public class AdvancedAlchemyFurnaceRenderer implements BlockEntityRenderer<AdvancedAlchemyFurnaceBlock.Core> {
    public static final ResourceLocation BASE = Thaumcraft.id("block/adv_furnace_base");
    public static final ResourceLocation BASE_ON = Thaumcraft.id("block/adv_furnace_base_on");
    public static final ResourceLocation TANK = Thaumcraft.id("block/adv_furnace_tank");
    public static final ResourceLocation TANK_ON = Thaumcraft.id("block/adv_furnace_tank_on");
    private static final ResourceLocation GOO = Thaumcraft.id("block/fluxgoo");
    private static final ResourceLocation METAL = Thaumcraft.id("block/metalbase");
    private static final ResourceLocation FIRE = new ResourceLocation("minecraft", "block/fire_0");

    public AdvancedAlchemyFurnaceRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static void model(PoseStack pose, MultiBufferSource buffer, ResourceLocation id, int light) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)),
                null, mc.getModelManager().getModel(id), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
    }

    /** renderQuadCenteredFromIcon оригинала: квадрат 1×1 от высоты width до 1 со значком целиком. */
    private static void quad(PoseStack pose, MultiBufferSource buffer, ResourceLocation sprite, int brightness, float width) {
        TextureAtlasSprite icon = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(sprite);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float[][] v = {{0f, 1f, icon.getU1(), icon.getV1()}, {1f, 1f, icon.getU0(), icon.getV1()}, {1f, width, icon.getU0(), icon.getV0()},
                {0f, width, icon.getU1(), icon.getV0()}};
        for (float[] p : v) {
            consumer.vertex(m, p[0], p[1], 0f).color(1f, 1f, 1f, 1f).uv(p[2], p[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(brightness)
                    .normal(n, 0f, 0f, 1f).endVertex();
        }
    }

    @Override
    public void render(AdvancedAlchemyFurnaceBlock.Core core, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        model(pose, buffer, core.heat <= 100 ? BASE : BASE_ON, light);
        for (int a = 0; a < 4; a++) {
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(90f * a));
            model(pose, buffer, core.vis <= 0 ? TANK : TANK_ON, light);
            pose.popPose();
        }
        if (core.vis > 0) {
            pose.pushPose();
            pose.translate(0.5f, -0.5f, 1.1f);
            pose.mulPose(Axis.YP.rotationDegrees(180f));
            quad(pose, buffer, GOO, 190, 0f);
            pose.popPose();
            float f = 1f - core.vis / (float) AdvancedAlchemyFurnaceBlock.Core.MAX_VIS;
            for (int a = 0; a < 4; a++) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(90f * a));
                pose.mulPose(Axis.XP.rotationDegrees(-90f));
                pose.translate(0.85f, -1.8f, -1.4f);
                pose.scale(0.3f, 0.6f, 1f);
                quad(pose, buffer, METAL, 150, 0f);
                pose.translate(0f, 0f, -0.01f);
                quad(pose, buffer, GOO, 190, f);
                pose.popPose();
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(-90f * a));
                pose.mulPose(Axis.XP.rotationDegrees(90f));
                pose.translate(1.15f, 1.8f, -1.4f);
                pose.scale(-0.3f, -0.6f, -1f);
                quad(pose, buffer, METAL, 150, 0f);
                pose.translate(0f, 0f, 0.01f);
                quad(pose, buffer, GOO, 190, f);
                pose.popPose();
            }
        }
        if (core.heat > 100) {
            pose.pushPose();
            pose.translate(0f, 0f, 1f);
            for (int a = 0; a < 4; a++) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(90f * a));
                pose.mulPose(Axis.XP.rotationDegrees(135f));
                pose.mulPose(Axis.ZP.rotationDegrees(180f));
                pose.translate(-0.5f, 0f, -1f);
                quad(pose, buffer, FIRE, 220, 1f - Math.min(1f, core.heat / (float) AdvancedAlchemyFurnaceBlock.Core.MAX_POWER));
                pose.translate(0f, 0f, 0.05f);
                quad(pose, buffer, METAL, 150, 0f);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(AdvancedAlchemyFurnaceBlock.Core core) {
        return true;
    }
}
