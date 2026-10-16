package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.AncientLockBlock;
import thaumcraft.common.registry.ModItems;

/**
 * Древний замок — перенос TileEldritchLockRenderer оригинала: четыре зубца из колышущихся кубов (ModelCube),
 * которые по одному втягиваются, когда в замок вставлена скрижаль; сама скрижаль торчит из лицевой грани; а весь
 * проём 5×5 вокруг замка затянут звёздной бездной.
 */
public class AncientLockRenderer implements BlockEntityRenderer<AncientLockBlock.Lock> {
    private static final ResourceLocation CUBE = Thaumcraft.id("textures/models/eldritch_cube.png");
    /** ModelCube(0) оригинала: куб в блок с текстурой 64×64. */
    private static final ModelPart MODEL = ModelParts.part(0, 0, true, 64f, 64f, 0f, 0f, 0f, 0f, 0f, 0f, -8f, -8f, -8f, 16f, 16f, 16f, 0f);

    public AncientLockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AncientLockBlock.Lock lock, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float count = (mc.player == null ? 0 : mc.player.tickCount) + partialTick;
        Direction dir = lock.facing();
        VertexConsumer cube = buffer.getBuffer(RenderType.entityCutout(CUBE));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        Vector3f axis = new Vector3f(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        for (int u = 0; u < 4; u++) {
            pose.pushPose();
            pose.mulPose(new org.joml.Quaternionf().rotationAxis(Mth.DEG_TO_RAD * 90f * u, axis));
            int shown = 5 - (Math.max(lock.count, -1) + u * 5) / 20;
            for (int a = 1; a < shown; a++) {
                pose.pushPose();
                pose.translate(0.0, 0.25 + 0.5 * a, 0.0);
                float w = Mth.sin((count + a * 10 + u * 20) / 20f) * 0.1f;
                if (a == 1 || a == 4) {
                    w = w / 2f + 0.2f;
                }
                pose.scale(0.5f + w, 0.5f, 0.5f + w);
                MODEL.render(pose, cube, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();

        if (lock.count >= 0) {
            pose.pushPose();
            pose.translate(0.5 + dir.getStepX() * 0.525, 0.285, 0.5 + dir.getStepZ() * 0.525);
            switch (dir) {
                case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180f));
                case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270f));
                case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90f));
                default -> {
                }
            }
            pose.translate(0.0, 0.215, 0.0);
            pose.scale(0.5f, 0.5f, 0.5f);
            mc.getItemRenderer().renderStatic(new ItemStack(ModItems.RUNED_TABLET.get()), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    pose, buffer, lock.getLevel(), (int) lock.getBlockPos().asLong());
            pose.popPose();
        }

        // проём 5×5 через середину замка, поперёк направления замка
        Matrix4f m = pose.last().pose();
        VertexConsumer stars = buffer.getBuffer(RenderType.endGateway());
        float o = 0.5f;
        float[][] quad = dir.getAxis() == Direction.Axis.Z
                ? new float[][]{{-2, 3, o}, {-2, -2, o}, {3, -2, o}, {3, 3, o}}
                : new float[][]{{o, 3, -2}, {o, 3, 3}, {o, -2, 3}, {o, -2, -2}};
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                float[] v = quad[pass == 0 ? k : 3 - k];
                stars.vertex(m, v[0], v[1], v[2]).endVertex();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AncientLockBlock.Lock lock) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
