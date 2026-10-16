package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.BrainJarBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Мозг в банке — перенос TileJarRenderer.renderBrain и ModelBrain оригинала: мозг покачивается в мутном рассоле. */
public class BrainJarRenderer implements BlockEntityRenderer<BrainJarBlock.Brain> {
    private static final ResourceLocation BRAIN = Thaumcraft.id("textures/models/brain2.png");
    private static final ResourceLocation BRINE = Thaumcraft.id("textures/models/jarbrine.png");
    private static final ModelPart BRAIN_MODEL = new ModelPart(List.of(), Map.of(
            "hemispheres", part(0, 0, 0f, 0f, 0f, 12, 10, 16, 128, 64, -6f, 8f, -8f, 0f),
            "cerebellum", part(64, 0, 0f, 0f, 0f, 8, 3, 7, 128, 64, -4f, 18f, 0f, 0f),
            "stem", part(0, 32, 0f, 0f, 0f, 2, 6, 2, 128, 64, -1f, 18f, -2f, 0.4089647f)));
    private static final ModelPart BRINE_MODEL = part(0, 0, -4f, -11f, -4f, 8, 10, 8, 64, 32, 0f, 0f, 0f, 0f);

    public BrainJarRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelPart part(int u, int v, float x, float y, float z, int w, int h, int d, int texW, int texH, float px, float py,
                                  float pz, float xRot) {
        ModelPart part = new ModelPart(List.of(new ModelPart.Cube(u, v, x, y, z, w, h, d, 0f, 0f, 0f, true, texW, texH,
                EnumSet.allOf(Direction.class))), Map.of());
        part.setPos(px, py, pz);
        part.xRot = xRot;
        return part;
    }

    /** Мозг и рассол; используется и для банки в руке. */
    public static void draw(PoseStack pose, MultiBufferSource buffer, int light, float rotation, float bob) {
        pose.pushPose();
        pose.translate(0.5, 0.01, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        pose.pushPose();
        pose.translate(0f, -0.8f + bob, 0f);
        pose.mulPose(Axis.YP.rotation(rotation));
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.scale(0.4f, 0.4f, 0.4f);
        BRAIN_MODEL.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(BRAIN)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        BRINE_MODEL.render(pose, buffer.getBuffer(RenderType.entityTranslucent(BRINE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public void render(BrainJarBlock.Brain brain, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float ticks = mc.player == null ? 0f : mc.player.tickCount + partialTick;
        float bob = Mth.sin(ticks / 14f) * 0.03f + 0.03f;
        float turn = Mth.wrapDegrees((brain.rota - brain.rotb) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        draw(pose, buffer, light, brain.rotb + turn * partialTick, bob);
    }

    /** Банка в руке и в интерфейсе: стекло с крышкой и неподвижный мозг в рассоле. */
    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                @Override
                public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                                         PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
                    draw(pose, buffer, light, 0f, 0.03f);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                            thaumcraft.common.registry.ModBlocks.WARDED_JAR.get().defaultBlockState(), pose, buffer, light, overlay,
                            net.minecraftforge.client.model.data.ModelData.EMPTY, null);
                }
            };
}
