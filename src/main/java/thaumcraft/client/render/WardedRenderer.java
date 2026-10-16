package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import thaumcraft.common.blocks.WardedBlock;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.items.wands.foci.WardingFocus;

import java.util.Objects;
import java.util.UUID;

/**
 * Рунная подсветка защищённых блоков — перенос TileWardedRenderer оригинала. Видна только тому, кто держит жезл
 * с фокусом «Защита»: свои блоки светятся золотом, чужие — тускло-красным. Рамка из рун обводит всю защищённую
 * область целиком: соседние блоки одного хозяина сливаются (47 «соединённых» текстур warded_glass).
 */
public class WardedRenderer implements BlockEntityRenderer<WardedBlock.Warded> {
    /** connectedTextureRefByID оригинала: по восьми соседям в плоскости грани — номер текстуры рамки. */
    private static final int[] CONNECTED = {0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31,
            2, 2, 27, 14, 0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 4, 4, 5,
            5, 4, 4, 5, 5, 17, 17, 22, 26, 17, 17, 22, 26, 16, 16, 20, 20, 16, 16, 28, 28, 21, 21, 46, 42, 21, 21, 43, 38, 4, 4, 5, 5, 4, 4,
            5, 5, 9, 9, 30, 12, 9, 9, 30, 12, 16, 16, 20, 20, 16, 16, 28, 28, 25, 25, 45, 37, 25, 25, 40, 32, 0, 0, 6, 6, 0, 0, 6, 6, 3, 3,
            19, 15, 3, 3, 19, 15, 1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 0, 0, 6, 6, 0, 0, 6, 6, 3, 3, 19, 15, 3, 3, 19, 15,
            1, 1, 18, 18, 1, 1, 13, 13, 2, 2, 23, 31, 2, 2, 27, 14, 4, 4, 5, 5, 4, 4, 5, 5, 17, 17, 22, 26, 17, 17, 22, 26, 7, 7, 24, 24, 7,
            7, 10, 10, 29, 29, 44, 41, 29, 29, 39, 33, 4, 4, 5, 5, 4, 4, 5, 5, 9, 9, 30, 12, 9, 9, 30, 12, 7, 7, 24, 24, 7, 7, 10, 10, 8, 8,
            36, 35, 8, 8, 34, 11};

    public WardedRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** «Вправо» и «вверх» на грани, если смотреть на неё снаружи: в этих осях заданы соседи и текстура. */
    private static Vec3i right(Direction side) {
        return switch (side) {
            case UP, DOWN, SOUTH -> new Vec3i(1, 0, 0);
            case NORTH -> new Vec3i(-1, 0, 0);
            case WEST -> new Vec3i(0, 0, 1);
            case EAST -> new Vec3i(0, 0, -1);
        };
    }

    private static Vec3i up(Direction side) {
        return side.getAxis() == Direction.Axis.Y ? new Vec3i(0, 0, -1) : new Vec3i(0, 1, 0);
    }

    private static boolean connected(Level level, BlockPos pos, UUID owner) {
        return level.getBlockEntity(pos) instanceof WardedBlock.Warded other && Objects.equals(other.getOwner(), owner);
    }

    @Override
    public void render(WardedBlock.Warded tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Player player = Minecraft.getInstance().player;
        Level level = tile.getLevel();
        if (player == null || level == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof WandItem) || !(WandItem.getFocusStack(held).getItem() instanceof WardingFocus)) {
            return;
        }
        BlockPos pos = tile.getBlockPos();
        boolean own = player.getUUID().equals(tile.getOwner());
        float ticks = player.tickCount + partialTick;
        float r = Mth.sin(ticks / 2f + pos.getX()) * 0.2f + 0.8f;
        float g = Mth.sin(ticks / 3f + pos.getY()) * 0.2f + (own ? 0.7f : 0.28f);
        float b = Mth.sin(ticks / 4f + pos.getZ()) * 0.2f + 0.28f;
        float alpha = own ? 0.5f : 0.25f;
        UUID owner = tile.getOwner();
        drawFrame(pose.last().pose(), buffer, pos, other -> connected(level, other, owner), r, g, b, alpha, true);
    }

    /**
     * Рамка вокруг одного блока области: на каждой наружной грани — та из 47 текстур, что соответствует
     * восьми соседям в плоскости грани, так что рамки соседних блоков сливаются в общий контур.
     *
     * @param matrix матрица, в которой блок занимает куб от 0 до 1
     * @param inArea входит ли блок с такими координатами в ту же область
     * @param depthTest false — рамка видна сквозь блоки
     */
    /** Номер (от 0 до 46) «соединённой» текстуры грани: по восьми соседям в плоскости грани, которые входят в ту же область. */
    public static int connectedTexture(BlockPos pos, Direction side, java.util.function.Predicate<BlockPos> inArea) {
        Vec3i rt = right(side);
        Vec3i upv = up(side);
        // восемь соседей в плоскости грани: слева сверху, сверху, справа сверху, слева, справа и три снизу
        int id = 0;
        int bit = 1;
        for (int v = 1; v >= -1; v--) {
            for (int u = -1; u <= 1; u++) {
                if (u == 0 && v == 0) {
                    continue;
                }
                if (inArea.test(pos.offset(rt.getX() * u + upv.getX() * v, rt.getY() * u + upv.getY() * v, rt.getZ() * u + upv.getZ() * v))) {
                    id |= bit;
                }
                bit <<= 1;
            }
        }
        return CONNECTED[id];
    }

    /** «Вправо» на грани, если смотреть на неё снаружи. */
    public static Vec3i rightOf(Direction side) {
        return right(side);
    }

    /** «Вверх» на грани, если смотреть на неё снаружи. */
    public static Vec3i upOf(Direction side) {
        return up(side);
    }

    public static void drawFrame(Matrix4f matrix, MultiBufferSource buffer, BlockPos pos, java.util.function.Predicate<BlockPos> inArea,
                                 float r, float g, float b, float alpha, boolean depthTest) {
        for (Direction side : Direction.values()) {
            if (inArea.test(pos.relative(side))) {
                continue;
            }
            Vec3i rt = right(side);
            Vec3i upv = up(side);
            VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.wardOverlay(connectedTexture(pos, side, inArea) + 1, depthTest));
            float cx = 0.5f + side.getStepX() * 0.502f;
            float cy = 0.5f + side.getStepY() * 0.502f;
            float cz = 0.5f + side.getStepZ() * 0.502f;
            corner(consumer, matrix, cx, cy, cz, rt, upv, -1, 1, 0f, 0f, r, g, b, alpha);
            corner(consumer, matrix, cx, cy, cz, rt, upv, -1, -1, 0f, 1f, r, g, b, alpha);
            corner(consumer, matrix, cx, cy, cz, rt, upv, 1, -1, 1f, 1f, r, g, b, alpha);
            corner(consumer, matrix, cx, cy, cz, rt, upv, 1, 1, 1f, 0f, r, g, b, alpha);
        }
    }

    private static void corner(VertexConsumer consumer, Matrix4f matrix, float cx, float cy, float cz, Vec3i rt, Vec3i up, int u, int v,
                               float tu, float tv, float r, float g, float b, float a) {
        consumer.vertex(matrix, cx + (rt.getX() * u + up.getX() * v) * 0.5f, cy + (rt.getY() * u + up.getY() * v) * 0.5f,
                cz + (rt.getZ() * u + up.getZ() * v) * 0.5f).uv(tu, tv).color(r, g, b, a).endVertex();
    }
}
