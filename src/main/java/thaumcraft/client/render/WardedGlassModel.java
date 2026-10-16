package thaumcraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModBlocks;

import java.util.List;

/**
 * Модель защищённого стекла — «соединённые» текстуры оригинала (connectedTextureRefByID): на каждой грани — та из
 * 47 текстур рамки, что подходит к соседним стёклам, так что сплошная стена стёкол обведена одной рамкой.
 */
public class WardedGlassModel implements IDynamicBakedModel {
    private static final ModelProperty<int[]> FACES = new ModelProperty<>();
    private static final ChunkRenderTypeSet TYPES = ChunkRenderTypeSet.of(RenderType.translucent());

    private static TextureAtlasSprite sprite(int index) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(Thaumcraft.id("block/warded_glass_" + (index + 1)));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state,
                                           @NotNull ModelData data) {
        int[] faces = new int[6];
        for (Direction side : Direction.values()) {
            faces[side.ordinal()] = WardedRenderer.connectedTexture(pos, side, other -> level.getBlockState(other).is(ModBlocks.WARDED_GLASS.get()));
        }
        return ModelData.builder().with(FACES, faces).build();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        int[] faces = data.get(FACES);
        return List.of(quad(side, sprite(faces == null ? 0 : faces[side.ordinal()])));
    }

    /** Квадрат на всю грань: верхний левый угол текстуры — там, где у грани «влево» и «вверх». */
    private static BakedQuad quad(Direction side, TextureAtlasSprite sprite) {
        BakedQuad[] out = new BakedQuad[1];
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer(quad -> out[0] = quad);
        builder.setDirection(side);
        builder.setSprite(sprite);
        builder.setShade(true);
        builder.setTintIndex(-1);
        Vec3i right = WardedRenderer.rightOf(side);
        Vec3i up = WardedRenderer.upOf(side);
        float cx = 0.5f + side.getStepX() * 0.5f;
        float cy = 0.5f + side.getStepY() * 0.5f;
        float cz = 0.5f + side.getStepZ() * 0.5f;
        int[][] corners = {{-1, 1}, {-1, -1}, {1, -1}, {1, 1}};
        float[][] uvs = {{0f, 0f}, {0f, 1f}, {1f, 1f}, {1f, 0f}};
        for (int i = 0; i < 4; i++) {
            int u = corners[i][0];
            int v = corners[i][1];
            builder.vertex(cx + (right.getX() * u + up.getX() * v) * 0.5f, cy + (right.getY() * u + up.getY() * v) * 0.5f,
                            cz + (right.getZ() * u + up.getZ() * v) * 0.5f)
                    .color(1f, 1f, 1f, 1f)
                    .uv(sprite.getU(uvs[i][0] * 16f), sprite.getV(uvs[i][1] * 16f))
                    .uv2(0, 0)
                    .normal(side.getStepX(), side.getStepY(), side.getStepZ())
                    .endVertex();
        }
        return out[0];
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data) {
        return TYPES;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return sprite(0);
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
