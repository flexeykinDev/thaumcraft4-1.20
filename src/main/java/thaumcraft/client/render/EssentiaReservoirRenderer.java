package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.EssentiaReservoirBlock;

/**
 * Резервуар эссенции — перенос TileEssentiaReservoirRenderer оригинала: внутри стеклянного куба — каркас-цистерна,
 * повёрнутый горловиной к подключённой трубе, и светящаяся жидкость, меняющая цвет от аспекта к аспекту.
 */
public class EssentiaReservoirRenderer implements BlockEntityRenderer<EssentiaReservoirBlock.Reservoir> {
    public static final ResourceLocation FRAME = Thaumcraft.id("block/essentia_reservoir_frame");

    public EssentiaReservoirRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void frame(PoseStack pose, MultiBufferSource buffer, Direction facing, int light) {
        Minecraft mc = Minecraft.getInstance();
        pose.pushPose();
        FluxScrubberRenderer.orient(pose, facing);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                null, mc.getModelManager().getModel(FRAME), 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public void render(EssentiaReservoirBlock.Reservoir reservoir, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        frame(pose, buffer, reservoir.facing, light);
        int size = reservoir.essentia.visSize();
        if (reservoir.displayAspect == null || size == 0) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Thaumcraft.id("block/animatedglow"));
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float x0 = 3f / 16f;
        float x1 = 13f / 16f;
        float y0 = 3f / 16f;
        float y1 = y0 + 10f / 16f * size / EssentiaReservoirBlock.Reservoir.MAX;
        float u0 = sprite.getU(3);
        float u1 = sprite.getU(13);
        float v0 = sprite.getV(3);
        float v1 = sprite.getV(13);
        int glow = Math.max(light & 0xffff, 200) | light & 0xffff0000;
        float[][] quads = {
                {x0, y1, x0, x0, y1, x1, x1, y1, x1, x1, y1, x0, 0, 1, 0},
                {x0, y0, x1, x0, y0, x0, x1, y0, x0, x1, y0, x1, 0, -1, 0},
                {x0, y1, x0, x1, y1, x0, x1, y0, x0, x0, y0, x0, 0, 0, -1},
                {x1, y1, x1, x0, y1, x1, x0, y0, x1, x1, y0, x1, 0, 0, 1},
                {x0, y1, x1, x0, y1, x0, x0, y0, x0, x0, y0, x1, -1, 0, 0},
                {x1, y1, x0, x1, y1, x1, x1, y0, x1, x1, y0, x0, 1, 0, 0},
        };
        float[][] uvs = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
        for (float[] q : quads) {
            for (int i = 0; i < 4; i++) {
                consumer.vertex(m, q[i * 3], q[i * 3 + 1], q[i * 3 + 2]).color(reservoir.cr, reservoir.cg, reservoir.cb, 0.9f)
                        .uv(uvs[i][0], uvs[i][1]).uv2(glow).normal(n, q[12], q[13], q[14]).endVertex();
            }
        }
    }

    /** Резервуар в руке и в интерфейсе: стеклянный куб и каркас горловиной вниз. */
    public static final net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer ITEM =
            new net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels()) {
                @Override
                public void renderByItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext context,
                                         PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
                    Minecraft mc = Minecraft.getInstance();
                    frame(pose, buffer, Direction.DOWN, light);
                    var model = mc.getBlockRenderer().getBlockModel(thaumcraft.common.registry.ModBlocks.ESSENTIA_RESERVOIR.get().defaultBlockState());
                    mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffer.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),
                            null, model, 1f, 1f, 1f, light, overlay);
                }
            };
}
