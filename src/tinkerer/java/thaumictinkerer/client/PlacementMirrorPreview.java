package thaumictinkerer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.kami.PlacementMirrorItem;

/** PlacementMirrorPredictionRenderer: пока в руке зеркало с запомненным блоком, видно, куда лягут блоки. */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID, value = Dist.CLIENT)
public final class PlacementMirrorPreview {
    private PlacementMirrorPreview() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PlacementMirrorItem) || PlacementMirrorItem.state(player.level(), stack).isAir()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : PlacementMirrorItem.targets(stack, player)) {
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).deflate(0.02), 0.73f, 0.66f, 1f, 0.8f);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
