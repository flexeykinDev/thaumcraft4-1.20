package thaumcraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.WardedBlock;

import java.util.List;

/**
 * Модель защищённого блока: целиком повторяет модель того блока, который он изображает.
 * В оригинале для этого был отдельный рендерер; здесь блок просто отдаёт чужие грани, и чанк рисует его
 * как обычный блок, без лишней нагрузки.
 */
public class WardedModel implements IDynamicBakedModel {
    private static BlockState mimic(ModelData data) {
        BlockState state = data.get(WardedBlock.MIMIC);
        return state == null ? Blocks.STONE.defaultBlockState() : state;
    }

    private static BakedModel model(BlockState mimic) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(mimic);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        BlockState mimic = mimic(data);
        return model(mimic).getQuads(mimic, side, random, ModelData.EMPTY, renderType);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data) {
        BlockState mimic = mimic(data);
        return model(mimic).getRenderTypes(mimic, random, ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return model(mimic(data)).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return model(Blocks.STONE.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
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
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
