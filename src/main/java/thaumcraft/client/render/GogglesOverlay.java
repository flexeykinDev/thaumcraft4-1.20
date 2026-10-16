package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.Goggles;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientKnowledge;

/**
 * Значки сквозь очки откровения — перенос RenderEventHandler.blockHighlight оригинала: над сосудом, на который
 * смотрит игрок, всплывают значки аспектов с количеством, а над нотным блоком — номер ноты.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class GogglesOverlay {
    private static final ResourceLocation UNKNOWN = Thaumcraft.id("textures/aspects/_unknown.png");
    /** Размер значков: плавно растёт, пока игрок смотрит на сосуд, и спадает, когда отвёл взгляд. */
    private static float tagScale;

    private GogglesOverlay() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // оригинал менял размер раз в кадр из расчёта на 60 кадров в секунду; здесь шаг привязан ко времени
        float frames = mc.getDeltaFrameTime() * 3f;
        if (tagScale > 0f) {
            tagScale = Math.max(0f, tagScale - 0.005f * frames);
        }
        Player player = mc.player;
        if (player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !Goggles.wornBy(player)) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockEntity tile = mc.level.getBlockEntity(pos);
        BlockState state = mc.level.getBlockState(pos);
        if (tile instanceof AspectContainer container) {
            AspectList aspects = container.getContainedAspects();
            if (aspects != null && !aspects.isEmpty()) {
                if (tagScale < 0.3f) {
                    tagScale = Math.min(0.3f, tagScale + (0.031f - tagScale / 10f) * frames);
                }
                boolean spaceAbove = mc.level.isEmptyBlock(pos.above());
                drawTags(pose, buffer, camera, pos, spaceAbove ? 0.4f : 0f, aspects, container.showAspectAmounts(),
                        spaceAbove ? Direction.UP : hit.getDirection());
            }
        } else if (state.hasProperty(NoteBlock.NOTE)) {
            drawText(pose, buffer, camera, pos.above(), "Note: " + state.getValue(NoteBlock.NOTE));
        }
        buffer.endBatch();
    }

    /** Поворот вокруг вертикали лицом к зрителю, как у значков оригинала. */
    private static void faceViewer(PoseStack pose, Vec3 camera, double x, double z) {
        float yaw = (float) (Math.atan2(camera.x - x, camera.z - z) * 180.0 / Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 180f));
    }

    private static void drawTags(PoseStack pose, MultiBufferSource buffer, Vec3 camera, BlockPos pos, float lift, AspectList tags,
                                 boolean amounts, Direction dir) {
        Font font = Minecraft.getInstance().font;
        int rowSize = 5;
        int current = 0;
        float shiftY = 0f;
        int left = tags.size();
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        for (Aspect tag : tags.getAspects()) {
            int div = Math.min(left, rowSize);
            if (current >= rowSize) {
                current = 0;
                shiftY -= tagScale * 1.05f;
                left -= rowSize;
                if (left < rowSize) {
                    div = left % rowSize;
                }
            }
            float shift = (current - div / 2f + 0.5f) * tagScale * 4f * tagScale;
            pose.pushPose();
            pose.translate(cx - camera.x + tagScale * 2f * dir.getStepX(),
                    pos.getY() + lift - shiftY + 0.5 - camera.y + tagScale * 2f * dir.getStepY(),
                    cz - camera.z + tagScale * 2f * dir.getStepZ());
            faceViewer(pose, camera, cx, cz);
            pose.translate(shift, 0f, 0f);
            pose.mulPose(Axis.ZP.rotationDegrees(180f));
            pose.scale(tagScale, tagScale, tagScale);
            int color = tag.getColor();
            float r = (color >> 16 & 255) / 255f;
            float g = (color >> 8 & 255) / 255f;
            float b = (color & 255) / 255f;
            // аспект, которого игрок ещё не открыл, показан знаком вопроса его цвета
            ResourceLocation image = ClientKnowledge.get().hasDiscovered(tag) ? tag.getImage() : UNKNOWN;
            VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.iconSeeThrough(image));
            Matrix4f matrix = pose.last().pose();
            consumer.vertex(matrix, -0.5f, 0.5f, 0f).uv(0f, 1f).color(r, g, b, 0.75f).endVertex();
            consumer.vertex(matrix, 0.5f, 0.5f, 0f).uv(1f, 1f).color(r, g, b, 0.75f).endVertex();
            consumer.vertex(matrix, 0.5f, -0.5f, 0f).uv(1f, 0f).color(r, g, b, 0.75f).endVertex();
            consumer.vertex(matrix, -0.5f, -0.5f, 0f).uv(0f, 0f).color(r, g, b, 0.75f).endVertex();
            if (amounts) {
                String amount = String.valueOf(tags.getAmount(tag));
                pose.scale(0.04f, 0.04f, 0.04f);
                pose.translate(0f, 6f, -0.1f);
                int width = font.width(amount);
                font.drawInBatch(amount, 14 - width, 1, 0xff111111, false, pose.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0);
                pose.translate(0f, 0f, -0.1f);
                font.drawInBatch(amount, 13 - width, 0, 0xffffffff, false, pose.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, 0xf000f0);
            }
            pose.popPose();
            current++;
        }
    }

    /** drawTextInAir оригинала: надпись в воздухе над блоком. */
    private static void drawText(PoseStack pose, MultiBufferSource buffer, Vec3 camera, BlockPos pos, String text) {
        Font font = Minecraft.getInstance().font;
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        pose.pushPose();
        pose.translate(cx - camera.x, pos.getY() + 0.5 - camera.y, cz - camera.z);
        faceViewer(pose, camera, cx, cz);
        pose.mulPose(Axis.ZP.rotationDegrees(180f));
        pose.scale(0.02f, 0.02f, 0.02f);
        int width = font.width(text);
        font.drawInBatch(text, 1 - width / 2f, 1, 0xff111111, false, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0xf000f0);
        pose.translate(0f, 0f, -0.1f);
        font.drawInBatch(text, -width / 2f, 0, 0xffffffff, false, pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0xf000f0);
        pose.popPose();
    }
}
