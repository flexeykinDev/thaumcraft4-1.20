package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.NodeStabilizerBlock;
import thaumcraft.common.blocks.NodeTransducerBlock;
import thaumcraft.common.blocks.VisRelayBlock;

/**
 * Отрисовка приборов сети вис — перенос TileNodeStabilizerRenderer, TileNodeConverterRenderer и
 * TileVisRelayRenderer оригинала. Все три собраны из частей моделей node_stabilizer.obj и vis_relay.obj.
 */
public final class VisNetRenderers {
    public static final ResourceLocation STAB_LOCK = Thaumcraft.id("block/node_stabilizer_lock");
    public static final ResourceLocation STAB_PISTON = Thaumcraft.id("block/node_stabilizer_piston");
    public static final ResourceLocation STAB_PISTON_OVER = Thaumcraft.id("block/node_stabilizer_piston_over");
    public static final ResourceLocation CONV_LOCK = Thaumcraft.id("block/node_converter_lock");
    public static final ResourceLocation CONV_LOCK_OVER = Thaumcraft.id("block/node_converter_lock_over");
    public static final ResourceLocation CONV_PISTON = Thaumcraft.id("block/node_converter_piston");
    public static final ResourceLocation CONV_PISTON_OVER = Thaumcraft.id("block/node_converter_piston_over");
    public static final ResourceLocation RELAY_BASE = Thaumcraft.id("block/vis_relay_base");
    public static final ResourceLocation RELAY_FLOAT = Thaumcraft.id("block/vis_relay_float");
    public static final ResourceLocation RELAY_SUPPORT = Thaumcraft.id("block/vis_relay_support");
    /** Все модели, которые надо загрузить отдельно от блоков (ModelEvent.RegisterAdditional). */
    public static final ResourceLocation[] MODELS = {STAB_LOCK, STAB_PISTON, STAB_PISTON_OVER, CONV_LOCK, CONV_LOCK_OVER, CONV_PISTON,
            CONV_PISTON_OVER, RELAY_BASE, RELAY_FLOAT, RELAY_SUPPORT};

    private static final ResourceLocation BUBBLE = Thaumcraft.id("textures/misc/node_bubble.png");
    private static final ResourceLocation RING = Thaumcraft.id("textures/item/lightningringv.png");

    private VisNetRenderers() {
    }

    private static void model(PoseStack pose, VertexConsumer consumer, ResourceLocation id, float r, float g, float b, int light) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(id);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model, r, g, b, light, OverlayTexture.NO_OVERLAY);
    }

    /** Свечение накладки: от тусклого до полного, слегка мерцает. */
    private static int glow(int light, float power, float ticks) {
        float scale = Mth.sin(ticks / 3f) * 0.1f + 0.9f;
        int level = Mth.clamp((int) ((50 + 170f * power * scale) / 16f), 0, 15);
        return Math.max(light & 0xffff, level << 4) | light & 0xffff0000;
    }

    private static float ticks(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 0f : mc.player.tickCount + partialTick;
    }

    /** Светящийся квадрат со стороной size, развёрнутый к камере; кадр frame из ленты в frames кадров по горизонтали. */
    private static void facingQuad(PoseStack pose, MultiBufferSource buffer, ResourceLocation texture, float size, int frames, int frame,
                                   float r, float g, float b, float alpha) {
        pose.pushPose();
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180f));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture));
        float u0 = (float) frame / frames;
        float u1 = (frame + 1f) / frames;
        float h = size / 2f;
        float[][] corners = {{-h, -h, u1, 1f}, {h, -h, u0, 1f}, {h, h, u0, 0f}, {-h, h, u1, 0f}};
        for (float[] c : corners) {
            // цвета складываются с фоном, поэтому прозрачность заложена в яркость
            consumer.vertex(matrix, c[0], c[1], 0f).color(r * alpha, g * alpha, b * alpha, 1f).uv(c[2], c[3])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xf000f0).normal(pose.last().normal(), 0f, 1f, 0f).endVertex();
        }
        pose.popPose();
    }

    /** Стабилизатор: основание и четыре «лапы», которые выдвигаются к узлу и светятся; над ним — пузырь удержания. */
    public static class Stabilizer implements BlockEntityRenderer<NodeStabilizerBlock.Stabilizer> {
        public Stabilizer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(NodeStabilizerBlock.Stabilizer tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            boolean advanced = tile.getLock() == 2;
            float ticks = ticks(partialTick);
            VertexConsumer solid = buffer.getBuffer(Sheets.cutoutBlockSheet());
            pose.pushPose();
            pose.translate(0.5f, 0f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(-90f));
            model(pose, solid, STAB_LOCK, 1f, 1f, 1f, light);
            for (int a = 0; a < 4; a++) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(90f * a));
                pose.mulPose(Axis.YP.rotationDegrees(45f));
                pose.translate(0f, 0f, tile.count / 100f);
                model(pose, solid, STAB_PISTON, 1f, 1f, 1f, light);
                // светящаяся накладка: у улучшенного стабилизатора красная
                pose.scale(1.002f, 1.002f, 1.002f);
                model(pose, solid, STAB_PISTON_OVER, 1f, advanced ? 0.2f : 1f, advanced ? 0.2f : 1f, glow(light, tile.count / 37f, ticks + a * 5));
                pose.popPose();
            }
            pose.popPose();
            if (tile.count > 0) {
                float alpha = (Mth.sin(ticks / 8f) * 0.1f + 0.5f) * tile.count / 37f;
                pose.pushPose();
                pose.translate(0.5f, 1.5f, 0.5f);
                facingQuad(pose, buffer, BUBBLE, 1.8f, 1, 0, 1f, advanced ? 0.27f : 1f, advanced ? 0.27f : 1f, alpha);
                pose.popPose();
            }
        }
    }

    /** Преобразователь: тот же механизм, перевёрнутый над узлом; цвет накладок показывает состояние. */
    public static class Transducer implements BlockEntityRenderer<NodeTransducerBlock.Transducer> {
        public Transducer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(NodeTransducerBlock.Transducer tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            float ticks = ticks(partialTick);
            float v = Math.min(50, Math.max(0, tile.count)) / 137f;
            // зелёный — простаивает, оранжевый — заряжает узел, малиновый — узел заряжен
            float r = tile.status == 0 ? 0.5f : 1f;
            float g = tile.status == 2 ? 0f : tile.status == 1 ? 0.6f : 1f;
            float b = tile.status == 2 ? 0.3f : tile.status == 1 ? 0.1f : 0.5f;
            VertexConsumer solid = buffer.getBuffer(Sheets.cutoutBlockSheet());
            pose.pushPose();
            pose.translate(0.5f, 1f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            model(pose, solid, CONV_LOCK, 1f, 1f, 1f, light);
            pose.pushPose();
            pose.scale(1.002f, 1.002f, 1.002f);
            model(pose, solid, CONV_LOCK_OVER, r, g, b, glow(light, v * 2.5f, ticks));
            pose.popPose();
            for (int a = 0; a < 4; a++) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(90f * a));
                pose.mulPose(Axis.YP.rotationDegrees(45f));
                pose.translate(0f, 0f, v);
                model(pose, solid, CONV_PISTON, 1f, 1f, 1f, light);
                pose.scale(1.002f, 1.002f, 1.002f);
                model(pose, solid, CONV_PISTON_OVER, r, g, b, glow(light, v * 2.5f, ticks + a * 5));
                pose.popPose();
            }
            pose.popPose();
            if (tile.status == 2) {
                // заряженный узел опоясан кольцом молний (TileNodeEnergizedRenderer оригинала)
                int frame = (int) (System.nanoTime() / 40000000L % 16);
                pose.pushPose();
                pose.translate(0.5f, -0.5f, 0.5f);
                facingQuad(pose, buffer, RING, 0.66f, 16, frame, 1f, 1f, 1f, 0.9f);
                pose.popPose();
            }
        }
    }

    /** Реле: кольцо-основание, парящее кольцо и кристалл цвета настройки; кристалл ярче, когда реле в сети. */
    public static class Relay implements BlockEntityRenderer<VisRelayBlock.Relay> {
        public Relay(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(VisRelayBlock.Relay tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
            float ticks = ticks(partialTick);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            switch (tile.getFacing()) {
                case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90f));
                case UP -> pose.mulPose(Axis.XP.rotationDegrees(90f));
                case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
                case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
                case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90f));
                default -> {
                }
            }
            pose.mulPose(Axis.XP.rotationDegrees(180f));
            pose.mulPose(Axis.ZP.rotationDegrees(45f));
            VertexConsumer solid = buffer.getBuffer(Sheets.cutoutBlockSheet());
            pose.pushPose();
            pose.scale(0.75f, 0.75f, 0.75f);
            pose.translate(0f, 0f, -0.16f);
            model(pose, solid, RELAY_BASE, 1f, 1f, 1f, light);
            pose.popPose();
            model(pose, solid, RELAY_FLOAT, 1f, 1f, 1f, light);
            float r = 1f;
            float g = 1f;
            float b = 1f;
            if (tile.getColor() >= 0) {
                int c = VisRelayBlock.Relay.COLORS[tile.getColor()];
                r = Math.min(1f, (c >> 16 & 255) / 200f);
                g = Math.min(1f, (c >> 8 & 255) / 200f);
                b = Math.min(1f, (c & 255) / 200f);
            }
            float scale = Mth.sin(ticks / 2f) * 0.05f + 0.95f;
            int level = Mth.clamp((int) (((tile.isConnected() ? 50 : 0) + 150f * scale) / 16f), 0, 15);
            int crystalLight = Math.max(light & 0xffff, level << 4) | light & 0xffff0000;
            model(pose, buffer.getBuffer(Sheets.translucentCullBlockSheet()), CrystallizerRenderer.CRYSTAL, r, g, b, crystalLight);
            pose.popPose();
        }
    }

    /** TileMagicWorkbenchChargerRenderer оригинала: кольцо с кристаллом висит остриём вниз на четырёх опорах. */
    public static class ChargeRelay implements BlockEntityRenderer<thaumcraft.common.blocks.VisChargeRelayBlock.ChargeRelay> {
        public ChargeRelay(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(thaumcraft.common.blocks.VisChargeRelayBlock.ChargeRelay tile, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int light, int overlay) {
            float ticks = ticks(partialTick);
            pose.pushPose();
            pose.translate(0.5f, 0.5f, 0.5f);
            pose.mulPose(Axis.XP.rotationDegrees(-90f));
            pose.mulPose(Axis.XP.rotationDegrees(180f));
            pose.mulPose(Axis.ZP.rotationDegrees(45f));
            VertexConsumer solid = buffer.getBuffer(Sheets.cutoutBlockSheet());
            model(pose, solid, RELAY_FLOAT, 1f, 1f, 1f, light);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(180f));
            pose.translate(0f, 0f, 0.5f);
            for (int a = 0; a < 4; a++) {
                model(pose, solid, RELAY_SUPPORT, 1f, 1f, 1f, light);
                pose.mulPose(Axis.ZP.rotationDegrees(90f));
            }
            pose.popPose();
            float r = 1f;
            float g = 1f;
            float b = 1f;
            if (tile.getColor() >= 0) {
                int c = VisRelayBlock.Relay.COLORS[tile.getColor()];
                r = Math.min(1f, (c >> 16 & 255) / 200f);
                g = Math.min(1f, (c >> 8 & 255) / 200f);
                b = Math.min(1f, (c & 255) / 200f);
            }
            float scale = Mth.sin(ticks / 2f) * 0.05f + 0.95f;
            int level = Mth.clamp((int) (((tile.isConnected() ? 50 : 0) + 150f * scale) / 16f), 0, 15);
            int crystalLight = Math.max(light & 0xffff, level << 4) | light & 0xffff0000;
            model(pose, buffer.getBuffer(Sheets.translucentCullBlockSheet()), CrystallizerRenderer.CRYSTAL, r, g, b, crystalLight);
            pose.popPose();
        }
    }
}
