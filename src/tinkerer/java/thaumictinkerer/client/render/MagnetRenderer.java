package thaumictinkerer.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import thaumcraft.client.render.ModelParts;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.block.MagnetBlock;

import java.util.List;

/**
 * RenderTileMagnet оригинала: подставка с сердечником (красный — тянет, синий — толкает) и над ней кольца молний,
 * тем шире, чем сильнее сигнал красного камня.
 */
public class MagnetRenderer implements BlockEntityRenderer<MagnetBlock.Tile> {
    private static final ResourceLocation PULL = ThaumicTinkerer.id("textures/models/magnet_s.png");
    private static final ResourceLocation PUSH = ThaumicTinkerer.id("textures/models/magnet_n.png");
    private static final ResourceLocation MOB_PULL = ThaumicTinkerer.id("textures/models/mob_magnet_s.png");
    private static final ResourceLocation MOB_PUSH = ThaumicTinkerer.id("textures/models/mob_magnet_n.png");
    private static final ResourceLocation RING = thaumcraft.Thaumcraft.id("textures/item/lightningring.png");

    // ModelMagnet оригинала (tools/convert_model.py)
    private static final List<ModelPart> MODEL = List.of(
            ModelParts.part(0, 0, false, 64f, 64f, -7f, 22f, -7f, 0f, 0f, 0f, 0f, 0f, 0f, 14f, 2f, 14f, 0f),
            ModelParts.part(0, 16, false, 64f, 64f, -2f, 9f, -2f, 0f, 0f, 0f, 0f, 0f, 0f, 4f, 13f, 4f, 0f),
            ModelParts.part(28, 19, false, 64f, 64f, 3f, 10f, -3f, 0f, -1.570796f, 0f, 0f, -2f, 0f, 6f, 14f, 0f, 0f),
            ModelParts.part(28, 33, false, 64f, 64f, -3f, 8f, 3f, 0f, 1.570796f, 0f, 0f, 0f, 0f, 6f, 14f, 0f, 0f),
            ModelParts.part(40, 19, false, 64f, 64f, 3f, 8f, 3f, 0f, 3.141593f, 0f, 0f, 0f, 0f, 6f, 14f, 0f, 0f),
            ModelParts.part(40, 33, false, 64f, 64f, -3f, 8f, -3f, 0f, 0f, 0f, 0f, 0f, 0f, 6f, 14f, 0f, 0f),
            ModelParts.part(28, 49, false, 64f, 64f, -3f, 8f, -3f, 0f, 0f, 0f, 0f, 0f, 0f, 6f, 0f, 6f, 0f));

    public MagnetRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** Предметы магнитов: модель с полной силой, как в оригинале. */
    public static BlockEntityWithoutLevelRenderer item(boolean mobs) {
        return new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()) {
            @Override
            public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffer, int light,
                                     int overlay) {
                draw(pose, buffer, mobs, true, 15, light, overlay, 0);
            }
        };
    }

    static void draw(PoseStack pose, MultiBufferSource buffer, boolean mobs, boolean pull, int power, int light, int overlay, float time) {
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1f, -1f, -1f);
        ResourceLocation texture = mobs ? pull ? MOB_PULL : MOB_PUSH : pull ? PULL : PUSH;
        VertexConsumer model = buffer.getBuffer(RenderType.entityCutout(texture));
        for (ModelPart part : MODEL) {
            part.render(pose, model, light, overlay);
        }
        pose.popPose();
        if (power <= 0) {
            return;
        }
        float size = power / 15f * 0.7f + 0.4f;
        int frame = (int) (time / 2) % 16;
        VertexConsumer ring = buffer.getBuffer(RenderType.entityTranslucentEmissive(RING));
        int r = pull ? 0 : 255;
        int b = pull ? 255 : 0;
        for (int i = 0; i < 3; i++) {
            pose.pushPose();
            pose.translate(0.5, 0.6 + i * 0.15, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * (i + 1) * 3 % 360));
            Matrix4f m = pose.last().pose();
            float half = size / 2;
            float v0 = frame / 16f;
            float v1 = (frame + 1) / 16f;
            ring.vertex(m, -half, 0, half).color(r, 0, b, 230).uv(0, v1).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
            ring.vertex(m, half, 0, half).color(r, 0, b, 230).uv(1, v1).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
            ring.vertex(m, half, 0, -half).color(r, 0, b, 230).uv(1, v0).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
            ring.vertex(m, -half, 0, -half).color(r, 0, b, 230).uv(0, v0).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
            pose.popPose();
        }
    }

    @Override
    public void render(MagnetBlock.Tile tile, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        boolean mobs = ((MagnetBlock) tile.getBlockState().getBlock()).isMobMagnet();
        float time = tile.getLevel() == null ? 0 : tile.getLevel().getGameTime() + partialTick;
        draw(pose, buffer, mobs, tile.getBlockState().getValue(MagnetBlock.PULL), tile.power(), light, overlay, time);
    }
}
