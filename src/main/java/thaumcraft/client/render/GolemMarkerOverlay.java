package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.TallowCandleBlock;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.entity.golem.GolemMarker;
import thaumcraft.common.items.golem.GolemBellItem;

import java.util.UUID;

/**
 * Отметки голема, видимые с колокольчиком в руке, — перенос renderMarkedBlocks оригинала: значок дома на блоке, у
 * которого голем поставлен, метка на каждой отмеченной стороне блока и светящаяся нить письмен от голема к метке.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class GolemMarkerOverlay {
    private static final ResourceLocation MARK = Thaumcraft.id("textures/misc/mark.png");
    private static final ResourceLocation HOME = Thaumcraft.id("textures/misc/home.png");
    private static final ResourceLocation SCRIPT = Thaumcraft.id("textures/misc/script.png");
    private static final ResourceLocation EMPTY = Thaumcraft.id("textures/block/empty.png");
    /** golemLinkQuality оригинала: сколько отрезков нити приходится на блок. */
    private static final int LINK_QUALITY = 16;

    private GolemMarkerOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || player == null || mc.level == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GolemBellItem) || !held.hasTag() || !held.getTag().contains("markers")) {
            return;
        }
        UUID id = GolemBellItem.getGolemId(held);
        Golem golem = null;
        if (id != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof Golem candidate && entity.getUUID().equals(id)) {
                    golem = candidate;
                    break;
                }
            }
        }
        if (golem == null) {
            return;
        }
        float time = System.nanoTime() / 30000000L % 32767f;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);

        BlockPos home = GolemBellItem.getGolemHome(held);
        int face = GolemBellItem.getGolemHomeFace(held);
        if (home != null && face > -1 && player.distanceToSqr(home.getX(), home.getY(), home.getZ()) < 4096.0) {
            float[] rgb = shimmer(time, face, 0);
            facePlate(pose, buffer, HOME, home, Direction.from3DDataValue(face), 0.65f, 0.01f, rgb);
        }

        String dimension = mc.level.dimension().location().toString();
        for (GolemMarker marker : GolemBellItem.getMarkers(held)) {
            Direction side = Direction.from3DDataValue(marker.side());
            BlockPos front = marker.pos().relative(side);
            if (!marker.dimension().equals(dimension) || player.distanceToSqr(front.getX(), front.getY(), front.getZ()) >= 4096.0) {
                continue;
            }
            float[] rgb = marker.color() == -1 ? shimmer(time, marker.side(), 0) : dye(marker.color());
            facePlate(pose, buffer, MARK, front, side, 0.4f, 0.01f, rgb);
            // отметка на пустом месте: показываем сам «блок» воздуха
            if (mc.level.isEmptyBlock(marker.pos())) {
                for (Direction around : Direction.values()) {
                    float[] glow = marker.color() == -1 ? shimmer(time, around.get3DDataValue(), 0) : rgb;
                    facePlate(pose, buffer, EMPTY, marker.pos().relative(around), around, 0.98f, -0.01f, glow);
                }
            }
            link(pose, buffer, golem, marker, side, event.getPartialTick(), time);
        }
        pose.popPose();
        buffer.endBatch();
    }

    /** Переливчатый цвет отметки без цвета. */
    private static float[] shimmer(float time, int side, int step) {
        return new float[]{Mth.sin(time / 12f + side + step) * 0.2f + 0.8f, Mth.sin(time / 14f + side + step) * 0.2f + 0.8f,
                Mth.sin(time / 16f + side + step) * 0.2f + 0.8f};
    }

    private static float[] dye(int color) {
        int rgb = TallowCandleBlock.COLORS[Mth.clamp(color, 0, 15)];
        return new float[]{(rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f};
    }

    /**
     * Светящийся значок на грани: лежит в блоке pos вплотную к его стороне, противоположной side
     * (то есть на отмеченной стороне соседнего блока), сдвинутый от неё на offset.
     */
    private static void facePlate(PoseStack pose, MultiBufferSource buffer, ResourceLocation texture, BlockPos pos, Direction side,
                                  float scale, float offset, float[] rgb) {
        pose.pushPose();
        pose.translate(pos.getX() + 0.5 + side.getStepX() * (offset - 0.5), pos.getY() + 0.5 + side.getStepY() * (offset - 0.5),
                pos.getZ() + 0.5 + side.getStepZ() * (offset - 0.5));
        pose.mulPose(side.getRotation());
        pose.mulPose(Axis.XP.rotationDegrees(90f));
        float half = scale / 2f;
        int r = (int) (rgb[0] * 255f);
        int g = (int) (rgb[1] * 255f);
        int b = (int) (rgb[2] * 255f);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.additive(texture));
        consumer.vertex(matrix, -half, -half, 0f).uv(0f, 1f).color(r, g, b, 255).endVertex();
        consumer.vertex(matrix, half, -half, 0f).uv(1f, 1f).color(r, g, b, 255).endVertex();
        consumer.vertex(matrix, half, half, 0f).uv(1f, 0f).color(r, g, b, 255).endVertex();
        consumer.vertex(matrix, -half, half, 0f).uv(0f, 0f).color(r, g, b, 255).endVertex();
        pose.popPose();
    }

    /** drawMarkerLine оригинала: лента письмен, которая вьётся от макушки голема к отмеченной стороне блока. */
    private static void link(PoseStack pose, MultiBufferSource buffer, Golem golem, GolemMarker marker, Direction side, float partialTick,
                             float time) {
        Vec3 from = golem.getPosition(partialTick).add(0, golem.getBbHeight(), 0);
        double x = marker.pos().getX();
        double y = marker.pos().getY();
        double z = marker.pos().getZ();
        // конец нити — середина отмеченной грани, а перед ним — точка на полблока снаружи, к которой нить сходится
        Vec3 outer = new Vec3(x + 0.5 + side.getStepX(), y + 0.5 + side.getStepY(), z + 0.5 + side.getStepZ()).subtract(from);
        Vec3 inner = new Vec3(x + 0.5 + side.getStepX() * 0.5, y + 0.5 + side.getStepY() * 0.5, z + 0.5 + side.getStepZ() * 0.5).subtract(from);
        float dist = (float) Math.sqrt(golem.distanceToSqr(x + side.getStepX() * 0.5, y + side.getStepY() * 0.5, z + side.getStepZ() * 0.5));
        float length = Math.max(1, Math.round(dist)) * LINK_QUALITY;
        int s = marker.side();
        float[] fixed = marker.color() == -1 ? null : dye(marker.color());

        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.additive(SCRIPT));
        float[] prev = null;
        for (int i = 0; i <= length; i++) {
            float f2 = i / length;
            float alpha = Math.min(0.75f, i * 1.5f / length);
            float sway = 1f - Math.abs(i - length / 2f) / (length / 2f);
            float fade = 0f;
            double phase = s * 20 + dist * (1f - f2) * LINK_QUALITY - time / 5f;
            double dx = outer.x + Mth.sin((float) ((phase + z % 16.0) / 4.0)) * 0.5f * sway;
            double dy = outer.y + Mth.sin((float) ((phase + x % 16.0) / 3.0)) * 0.5f * sway;
            double dz = outer.z + Mth.sin((float) ((phase + y % 16.0) / 2.0)) * 0.5f * sway;
            if (i > length - LINK_QUALITY / 2f) {
                double dx2 = inner.x + Mth.sin((float) ((phase + z % 16.0) / 4.0)) * 0.5f * sway;
                double dy2 = inner.y + Mth.sin((float) ((phase + x % 16.0) / 3.0)) * 0.5f * sway;
                double dz2 = inner.z + Mth.sin((float) ((phase + y % 16.0) / 2.0)) * 0.5f * sway;
                float keep = (length - i) / (LINK_QUALITY / 2f);
                fade = 1f - keep;
                dx = dx * keep + dx2 * fade;
                dy = dy * keep + dy2 * fade;
                dz = dz * keep + dz2 * fade;
            }
            float[] rgb = fixed != null ? fixed : shimmer(time, s, i);
            float u = (1f - f2) * dist - time * 0.005f;
            float[] now = {(float) (dx * f2), (float) (dy * f2), (float) (dz * f2), u, rgb[0], rgb[1], rgb[2], alpha * (1f - fade)};
            if (prev != null) {
                // лента шириной в десятую блока: по четырёхугольнику на каждый отрезок
                ribbon(consumer, matrix, prev, -0.05f, 1f);
                ribbon(consumer, matrix, prev, 0.05f, 0f);
                ribbon(consumer, matrix, now, 0.05f, 0f);
                ribbon(consumer, matrix, now, -0.05f, 1f);
            }
            prev = now;
        }
        pose.popPose();
    }

    private static void ribbon(VertexConsumer consumer, Matrix4f matrix, float[] point, float lift, float v) {
        consumer.vertex(matrix, point[0], point[1] + lift, point[2]).uv(point[3], v)
                .color(point[4], point[5], point[6], point[7]).endVertex();
    }
}
