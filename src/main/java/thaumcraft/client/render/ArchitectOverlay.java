package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.wands.Architect;
import thaumcraft.common.items.wands.WandItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Разметка области для фокусов с улучшением «Архитектор» — перенос REHWandHandler.handleArchitectOverlay оригинала.
 * Блоки, на которые подействует фокус, обведены общей мерцающей рамкой, а стрелки у блока под прицелом
 * показывают, размер по какой оси сейчас меняет клавиша.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ArchitectOverlay {
    private static final ResourceLocation ARROWS = Thaumcraft.id("textures/misc/architect_arrows.png");
    /** Добавка яркости вместо сложения цвета с текстурой в оригинале. */
    private static final float GLOW = 0.55f;
    private static int lastHash;
    private static Set<BlockPos> blocks = Set.of();

    private ArchitectOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || player == null || mc.level == null
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ItemStack wand = player.getMainHandItem();
        // лопата землекопа размечает свою кладку так же, только без стрелок
        thaumcraft.common.items.tools.ElementalShovelItem shovel =
                wand.getItem() instanceof thaumcraft.common.items.tools.ElementalShovelItem item ? item : null;
        if (shovel == null && !Architect.isActive(wand)) {
            return;
        }
        Architect architect = shovel != null ? null : (Architect) WandItem.getFocusStack(wand).getItem();
        // область пересчитывается четыре раза в секунду или когда прицел перешёл на другой блок
        int hash = java.util.Objects.hash(hit.getBlockPos(), hit.getDirection(), player.tickCount / 5, wand.getTag(),
                player.isShiftKeyDown());
        if (hash != lastHash) {
            lastHash = hash;
            List<BlockPos> found = shovel != null
                    ? shovel.getArchitectBlocks(wand, mc.level, hit.getBlockPos(), hit.getDirection(), player)
                    : architect.getArchitectBlocks(wand, mc.level, hit.getBlockPos(), hit.getDirection(), player);
            blocks = new HashSet<>(found);
        }
        if (blocks.isEmpty()) {
            return;
        }
        float ticks = player.tickCount + event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : blocks) {
            // в оригинале цвет не умножается на текстуру, а прибавляется к ней (GL_ADD), поэтому рамка яркая,
            // почти белая с синим отливом; она рисуется без проверки глубины и видна сквозь блоки
            float r = Math.min(1f, Mth.sin(ticks / 2f + pos.getX()) * 0.2f + 0.3f + GLOW);
            float g = Math.min(1f, Mth.sin(ticks / 3f + pos.getY()) * 0.2f + 0.3f + GLOW);
            float b = Math.min(1f, Mth.sin(ticks / 4f + pos.getZ()) * 0.2f + 0.8f + GLOW);
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            WardedRenderer.drawFrame(pose.last().pose(), buffer, pos, blocks::contains, r, g, b, 0.45f, false);
            pose.popPose();
        }
        // стрелки вдоль осей, которые сейчас меняет клавиша
        BlockPos at = hit.getBlockPos();
        float r = Mth.sin(ticks / 4f + at.getX()) * 0.2f + 0.3f;
        float g = Mth.sin(ticks / 3f + at.getY()) * 0.2f + 0.3f;
        float b = Mth.sin(ticks / 2f + at.getZ()) * 0.2f + 0.8f;
        pose.translate(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.additiveSeeThrough(ARROWS));
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (architect == null || !architect.showAxis(wand, hit.getDirection(), axis)) {
                continue;
            }
            Vec3i along = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).getNormal();
            // две скрещённые плоскости, проходящие через ось
            for (Direction.Axis other : Direction.Axis.values()) {
                if (other == axis) {
                    continue;
                }
                Vec3i across = Direction.fromAxisAndDirection(other, Direction.AxisDirection.POSITIVE).getNormal();
                float[][] corners = {{-1, -1, 0f, 0f}, {1, -1, 1f, 0f}, {1, 1, 1f, 1f}, {-1, 1, 0f, 1f}};
                for (float[] c : corners) {
                    consumer.vertex(matrix, (along.getX() * c[0] + across.getX() * c[1]) * 0.5f,
                            (along.getY() * c[0] + across.getY() * c[1]) * 0.5f,
                            (along.getZ() * c[0] + across.getZ() * c[1]) * 0.5f).uv(c[2], c[3]).color(r, g, b, 1f).endVertex();
                }
            }
        }
        pose.popPose();
        buffer.endBatch();
    }
}
