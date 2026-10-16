package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import thaumcraft.common.blockentity.CrucibleBlockEntity;

/**
 * Поверхность жидкости в тигле (TileCrucibleRenderer оригинала). Чем больше растворено аспектов,
 * тем сильнее вода уходит из синего в мутно-лиловый.
 */
public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrucibleBlockEntity crucible, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {
        if (crucible.getWater() <= 0 || crucible.getLevel() == null) {
            return;
        }
        float recolor = crucible.aspectTotal() / (float) CrucibleBlockEntity.MAX_ASPECTS;
        if (recolor > 0f) {
            recolor = Math.min(1f, 0.5f + recolor / 2f);
        }
        // текстура воды серая и красится цветом биома; в оригинале она была уже синей
        int water = BiomeColors.getAverageWaterColor(crucible.getLevel(), crucible.getBlockPos());
        float red = (water >> 16 & 255) / 255f * (1f - recolor / 3f);
        float green = (water >> 8 & 255) / 255f * (1f - recolor);
        float blue = (water & 255) / 255f * (1f - recolor / 2f);
        if (recolor > 0f) {
            // без этой добавки густой раствор в синей воде выглядел бы почти чёрным, а не лиловым
            red = Math.min(1f, red + recolor * 0.45f);
            blue = Math.min(1f, blue + recolor * 0.1f);
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(IClientFluidTypeExtensions.of(Fluids.WATER).getStillTexture());
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        // чуть внутри стенок, чтобы края не мерцали на их внутренней стороне
        float min = 0.126f;
        float max = 0.874f;
        float y = Math.min(crucible.getFluidHeight(), 0.999f);
        float u0 = sprite.getU(min * 16f);
        float u1 = sprite.getU(max * 16f);
        float v0 = sprite.getV(min * 16f);
        float v1 = sprite.getV(max * 16f);
        consumer.vertex(matrix, min, y, min).color(red, green, blue, 1f).uv(u0, v0).uv2(light).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(matrix, min, y, max).color(red, green, blue, 1f).uv(u0, v1).uv2(light).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(matrix, max, y, max).color(red, green, blue, 1f).uv(u1, v1).uv2(light).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(matrix, max, y, min).color(red, green, blue, 1f).uv(u1, v0).uv2(light).normal(normal, 0, 1, 0).endVertex();
    }
}
