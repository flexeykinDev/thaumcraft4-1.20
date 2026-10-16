package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.items.ThaumometerItem;

import java.util.List;

/**
 * Отрисовка узла ауры — перенос TileNodeRenderer оригинала. Без таумометра узел виден как слабый призрак;
 * с таумометром в руке — целиком и сквозь стены: по вращающемуся слою на каждый аспект плюс ядро по типу узла.
 */
public class NodeRenderer implements BlockEntityRenderer<NodeBlockEntity> {
    /** Текстура узлов: 32 кадра анимации по горизонтали, ряды по вертикали. */
    static final int FRAMES = 32;
    /**
     * Яркость узла, на который смотрят без таумометра. В оригинале 0.1. Свойство JVM thaumcraft.nodeGhostAlpha
     * позволяет подменить её при отладке, не трогая значение по умолчанию.
     */
    private static final float GHOST_ALPHA = Float.parseFloat(System.getProperty("thaumcraft.nodeGhostAlpha", "0.1"));

    public NodeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NodeBlockEntity node, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        Player viewer = mc.player;
        if (viewer == null) {
            return;
        }
        // очки откровения показывают узлы с 64 блоков, таумометр в руке — с 48
        boolean goggles = thaumcraft.api.nodes.Revealer.wornBy(viewer);
        boolean revealed = goggles || viewer.getMainHandItem().getItem() instanceof ThaumometerItem;
        double viewDistance = goggles ? 64.0 : 48.0;
        Vec3 center = Vec3.atCenterOf(node.getBlockPos());
        double distance = mc.gameRenderer.getMainCamera().getPosition().distanceTo(center);

        long nanos = System.nanoTime();
        int frame = (int) Math.floorMod(nanos / 40_000_000L + node.getBlockPos().getX(), FRAMES);
        float ticks = viewer.tickCount + partialTick;

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // слои всегда повёрнуты лицом к камере
        pose.mulPose(mc.gameRenderer.getMainCamera().rotation());

        List<Aspect> aspects = node.getAspects();
        if (aspects.isEmpty() || !revealed) {
            drawLayer(pose, buffer.getBuffer(ModRenderTypes.NODE_GLOW), 0f, 0.5f, GHOST_ALPHA, 1, frame, 0xffffff);
            pose.popPose();
            return;
        }
        if (distance > viewDistance) {
            pose.popPose();
            return;
        }

        float alpha = (float) ((viewDistance - distance) / viewDistance);
        NodeModifier modifier = node.getNodeModifier();
        if (modifier == NodeModifier.BRIGHT) {
            alpha *= 1.5f;
        } else if (modifier == NodeModifier.PALE) {
            alpha *= 0.66f;
        } else if (modifier == NodeModifier.FADING) {
            alpha *= Mth.sin(viewer.tickCount / 3f) * 0.25f + 0.33f;
        }

        RenderType glow = ModRenderTypes.NODE_GLOW_SEE_THROUGH;
        RenderType dark = ModRenderTypes.NODE_DARK_SEE_THROUGH;
        long time = nanos / 5_000_000L;
        float average = 0f;
        int index = 0;
        for (Aspect aspect : aspects) {
            int amount = node.getAmount(aspect);
            average += amount;
            float layerAlpha = aspect.isAdditiveBlend() ? alpha : alpha * 1.5f;
            float scale = Mth.sin(ticks / (14f - index)) * 0.25f + 0.5f;
            scale = 0.2f + scale * (amount / 50f);
            int period = 5000 + 500 * index;
            float angle = (float) (time % period) / period * Mth.TWO_PI;
            drawLayer(pose, buffer.getBuffer(aspect.isAdditiveBlend() ? glow : dark), angle, scale,
                    layerAlpha / Math.max(1f, aspects.size() / 2f), 0, frame, aspect.getColor());
            index++;
        }
        average /= aspects.size();

        float coreScale = 0.1f + average / 150f;
        float coreAngle = aspects.size() > 0
                ? (float) (time % (5000 + 500 * (index - 1))) / (5000 + 500 * (index - 1)) * Mth.TWO_PI : 0f;
        int strip = 1;
        boolean additive = true;
        NodeType type = node.getNodeType();
        switch (type) {
            case UNSTABLE -> {
                strip = 6;
                coreAngle = 0f;
            }
            case DARK -> {
                strip = 2;
                additive = false;
            }
            case TAINTED -> {
                strip = 5;
                additive = false;
            }
            case PURE -> strip = 4;
            case HUNGRY -> {
                strip = 3;
                coreScale *= 0.75f;
            }
            default -> {
            }
        }
        drawLayer(pose, buffer.getBuffer(additive ? glow : dark), coreAngle, coreScale, alpha, strip, frame, 0xffffff);
        pose.popPose();
    }

    /** Один квадрат из текстуры узлов: кадр frame в ряду strip, повёрнутый на angle вокруг взгляда. */
    static void drawLayer(PoseStack pose, VertexConsumer consumer, float angle, float scale, float alpha,
                                  int strip, int frame, int color) {
        pose.pushPose();
        if (angle != 0f) {
            pose.mulPose(Axis.ZP.rotation(angle));
        }
        Matrix4f matrix = pose.last().pose();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int a = Mth.clamp((int) (alpha * 255f), 0, 255);
        float u0 = (float) frame / FRAMES;
        float u1 = (float) (frame + 1) / FRAMES;
        float v0 = (float) strip / FRAMES;
        float v1 = (float) (strip + 1) / FRAMES;
        consumer.vertex(matrix, -scale, -scale, 0f).uv(u1, v1).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, -scale, scale, 0f).uv(u1, v0).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, scale, scale, 0f).uv(u0, v0).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, scale, -scale, 0f).uv(u0, v1).color(r, g, b, a).endVertex();
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(NodeBlockEntity node) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
