package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.ClientItemAspects;

/**
 * Чутьё кирки ядра — перенос RenderEventHandler.startScan и showScannedBlocks оригинала. На пять секунд сквозь камень
 * видны руды (огоньком тем крупнее, чем богаче руда аспектами), вода (синим) и лава (рыжим) в восьми блоках
 * от места удара.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class OreScanOverlay {
    private static final ResourceLocation GLASS = Thaumcraft.id("textures/block/wardedglass.png");
    private static final ResourceLocation NODES = Thaumcraft.id("textures/misc/nodes.png");
    private static final int[][][] SCANNED = new int[17][17][17];
    private static BlockPos centre = BlockPos.ZERO;
    private static long until;

    private OreScanOverlay() {
    }

    /** Сколько клеток последнего осмотра отмечено (руды, вода и лава) — для самопроверки. */
    public static int marked() {
        int count = 0;
        for (int[][] plane : SCANNED) {
            for (int[] row : plane) {
                for (int value : row) {
                    if (value != -1) {
                        count++;
                    }
                }
            }
        }
        return System.currentTimeMillis() < until ? count : 0;
    }

    /** Значения отмеченных клеток последнего осмотра — для самопроверки. */
    public static String values() {
        StringBuilder out = new StringBuilder();
        for (int[][] plane : SCANNED) {
            for (int[] row : plane) {
                for (int value : row) {
                    if (value != -1) {
                        out.append(value).append(' ');
                    }
                }
            }
        }
        return out.toString().trim();
    }

    public static void start(Level level, BlockPos pos, long duration, int range) {
        centre = pos.immutable();
        until = System.currentTimeMillis() + duration;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int xx = -8; xx <= 8; xx++) {
            for (int yy = -8; yy <= 8; yy++) {
                for (int zz = -8; zz <= 8; zz++) {
                    int value = -1;
                    if (Math.abs(xx) <= range && Math.abs(yy) <= range && Math.abs(zz) <= range) {
                        BlockState state = level.getBlockState(cursor.set(pos.getX() + xx, pos.getY() + yy, pos.getZ() + zz));
                        if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                            if (state.getFluidState().is(FluidTags.LAVA)) {
                                value = -10;
                            } else if (state.getFluidState().is(FluidTags.WATER)) {
                                value = -5;
                            } else if (state.is(Tags.Blocks.ORES)) {
                                value = ClientItemAspects.get(state.getBlock().asItem()).visSize();
                            }
                        }
                    }
                    SCANNED[xx + 8][yy + 8][zz + 8] = value;
                }
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        long time = System.currentTimeMillis();
        long left = until - time;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || left <= 0) {
            return;
        }
        float fade = 1f;
        if (left > 4750L) {
            fade = Math.max(0f, 1f - (left - 4750L) / 5f);
        }
        if (left < 1500L) {
            fade = left / 1500f;
        }
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        int frame = (int) (time / 50L % 32L);
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (int xx = -8; xx <= 8; xx++) {
            for (int yy = -8; yy <= 8; yy++) {
                for (int zz = -8; zz <= 8; zz++) {
                    int value = SCANNED[xx + 8][yy + 8][zz + 8];
                    float alpha = fade * (1f - (xx * xx + yy * yy + zz * zz) / 64f);
                    if (value == -1 || alpha <= 0f) {
                        continue;
                    }
                    pose.pushPose();
                    pose.translate(centre.getX() + xx + 0.5, centre.getY() + yy + 0.5, centre.getZ() + zz + 0.5);
                    if (value < 0) {
                        box(pose.last().pose(), buffer.getBuffer(ModRenderTypes.additiveSeeThrough(GLASS)),
                                value == -5 ? 0x3CD4FC : 0xFF5A01, alpha);
                    } else {
                        float size = value / 7f;
                        pose.mulPose(event.getCamera().rotation());
                        // свой вид отрисовки: он сам выключает проверку глубины, иначе огоньки прячутся под землёй
                        VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.additiveSeeThrough(NODES));
                        NodeRenderer.drawLayer(pose, consumer, 0f, 0.2f * size, alpha, 0, frame, 0xAAAA11);
                        NodeRenderer.drawLayer(pose, consumer, 0f, 0.5f * size, alpha, 0, frame, 0xAA1122);
                    }
                    pose.popPose();
                }
            }
        }
        pose.popPose();
        buffer.endBatch();
    }

    /** Шесть граней блока, затянутые цветным «стеклом». */
    private static void box(Matrix4f matrix, VertexConsumer consumer, int color, float alpha) {
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int a = Mth.clamp((int) (alpha * 255f), 0, 255);
        for (Direction side : Direction.values()) {
            // две оси, перпендикулярные грани
            Direction.Axis axis = side.getAxis();
            float[] u = axis == Direction.Axis.X ? new float[]{0, 0, 1} : new float[]{1, 0, 0};
            float[] v = axis == Direction.Axis.Y ? new float[]{0, 0, 1} : new float[]{0, 1, 0};
            float nx = side.getStepX() * 0.5f;
            float ny = side.getStepY() * 0.5f;
            float nz = side.getStepZ() * 0.5f;
            float[][] corners = {{-0.5f, -0.5f, 0f, 1f}, {-0.5f, 0.5f, 0f, 0f}, {0.5f, 0.5f, 1f, 0f}, {0.5f, -0.5f, 1f, 1f}};
            for (float[] c : corners) {
                consumer.vertex(matrix, nx + u[0] * c[0] + v[0] * c[1], ny + u[1] * c[0] + v[1] * c[1], nz + u[2] * c[0] + v[2] * c[1])
                        .uv(c[2], c[3]).color(r, g, b, a).endVertex();
            }
        }
    }
}
