package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.EldritchAltarBlock;
import thaumcraft.common.blocks.EldritchObeliskBlock;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.world.outer.OuterLands;

/**
 * Обелиск, колпак и алтарь Элдрича — перенос TileEldritchObeliskRenderer и TileEldritchCapRenderer оригинала.
 * Обелиск — парящий и покачивающийся столб в три блока, сквозь грани которого видна звёздная пустота (тот же
 * эффект, что у портала Края, у оригинала он был списан с него), с резной облицовкой поверх и колпаками сверху и
 * снизу. Колпак и алтарь — тот же колпак из obelisk_cap.obj; в алтарь вставлены очи Элдрича. В Запредельных
 * землях облицовка и колпаки — тёмные (текстуры _2).
 */
public class EldritchObeliskRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public static final ResourceLocation CAP = Thaumcraft.id("block/eldritch_cap");
    public static final ResourceLocation CAP_OUTER = Thaumcraft.id("block/eldritch_cap_2");
    public static final ResourceLocation CAP_ALTAR = Thaumcraft.id("block/eldritch_cap_altar");
    private static final ResourceLocation SIDE = Thaumcraft.id("textures/models/obelisk_side.png");
    private static final ResourceLocation SIDE_OUTER = Thaumcraft.id("textures/models/obelisk_side_2.png");

    public EldritchObeliskRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static void cap(PoseStack pose, MultiBufferSource buffer, ResourceLocation model, int light) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                null, mc.getModelManager().getModel(model), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        boolean outer = be.getLevel() != null && OuterLands.is(be.getLevel());
        if (be instanceof EldritchObeliskBlock.Obelisk) {
            renderObelisk(be, partialTick, pose, buffer, outer);
            return;
        }
        boolean altar = be instanceof EldritchAltarBlock.Altar;
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.XN.rotationDegrees(90f));
        cap(pose, buffer, altar ? CAP_ALTAR : outer ? CAP_OUTER : CAP, light);
        pose.popPose();
        if (altar && ((EldritchAltarBlock.Altar) be).eyes > 0) {
            ItemStack eye = new ItemStack(ModItems.ELDRITCH_EYE.get());
            for (int a = 0; a < ((EldritchAltarBlock.Altar) be).eyes; a++) {
                pose.pushPose();
                pose.translate(0.5, 0.0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(a * 90f));
                pose.translate(0.46, 0.2, 0.0);
                pose.mulPose(Axis.YP.rotationDegrees(90f));
                pose.mulPose(Axis.XN.rotationDegrees(18f));
                pose.scale(0.5f, 0.5f, 0.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(eye, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer,
                        be.getLevel(), (int) be.getBlockPos().asLong() + a);
                pose.popPose();
            }
        }
    }

    private void renderObelisk(T be, float partialTick, PoseStack pose, MultiBufferSource buffer, boolean outer) {
        Minecraft mc = Minecraft.getInstance();
        float count = (mc.player == null ? 0 : mc.player.tickCount) + partialTick;
        float bob = Mth.sin(count / 10f) * 0.1f + 0.1f;
        int light = be.getLevel() == null ? 0xf000f0 : LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above(5));

        // звёздная пустота на четырёх гранях столба
        pose.pushPose();
        pose.translate(0.0, 1.0 + bob, 0.0);
        Matrix4f m = pose.last().pose();
        VertexConsumer stars = buffer.getBuffer(RenderType.endGateway());
        float lo = 0.01f;
        float hi = 0.99f;
        float h = 3f;
        // north (z = lo), south (z = hi), west (x = lo), east (x = hi) — вершины против часовой стрелки снаружи
        float[][] faces = {
                {1, h, lo, 1, 0, lo, 0, 0, lo, 0, h, lo},
                {0, h, hi, 0, 0, hi, 1, 0, hi, 1, h, hi},
                {lo, h, 0, lo, 0, 0, lo, 0, 1, lo, h, 1},
                {hi, h, 1, hi, 0, 1, hi, 0, 0, hi, h, 0},
        };
        for (float[] f : faces) {
            for (int i = 0; i < 4; i++) {
                stars.vertex(m, f[i * 3], f[i * 3 + 1], f[i * 3 + 2]).endVertex();
            }
        }
        pose.popPose();

        // облицовка поверх пустоты
        VertexConsumer side = buffer.getBuffer(RenderType.entityTranslucent(outer ? SIDE_OUTER : SIDE));
        pose.pushPose();
        pose.translate(0.5, 1.0 + bob, 0.5);
        for (int a = 0; a < 4; a++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(a * 90f));
            pose.translate(0.0, 0.0, -0.5);
            Matrix4f sm = pose.last().pose();
            Matrix3f sn = pose.last().normal();
            float[][] quad = {{-0.5f, h, 0f, 1f}, {0.5f, h, 1f, 1f}, {0.5f, 0f, 1f, 0f}, {-0.5f, 0f, 0f, 0f}};
            for (float[] q : quad) {
                side.vertex(sm, q[0], q[1], -0.001f).color(1f, 1f, 1f, 1f).uv(q[2], q[3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light).normal(sn, 0f, 0f, -1f).endVertex();
            }
            pose.popPose();
        }
        pose.popPose();

        // колпаки: снизу перевёрнутый, сверху обычный
        ResourceLocation capModel = outer ? CAP_OUTER : CAP;
        pose.pushPose();
        pose.translate(0.5, 1.0 + bob, 0.5);
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        cap(pose, buffer, capModel, light);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.5, 4.0 + bob, 0.5);
        pose.mulPose(Axis.XN.rotationDegrees(90f));
        cap(pose, buffer, capModel, light);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return be instanceof EldritchObeliskBlock.Obelisk;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
