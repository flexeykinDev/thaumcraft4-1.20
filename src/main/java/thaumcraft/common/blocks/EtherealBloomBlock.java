package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.taint.Taint;
import thaumcraft.common.world.ModBiomes;

/**
 * Эфирный цветок (BlockCustomPlant оригинала, мета 4): раз в секунду возвращает случайному месту в девяти блоках
 * вокруг тот биом, который был там от рождения мира, — так он вычищает порчу, жуткие земли и наведённый волшебный лес.
 * Растёт где угодно на твёрдой поверхности, в том числе в пещерах, и ярко светится.
 */
public class EtherealBloomBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(4, 0, 4, 12, 14, 12);

    public EtherealBloomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Сам цветок рисует EtherealBloomRenderer; модель блока пустая и нужна только ради частиц. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
                                  BlockPos neighbourPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Bloom(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ETHEREAL_BLOOM.get(), Bloom::tick);
    }

    /** TileEtherealBloom оригинала. */
    public static class Bloom extends BlockEntity {
        public int counter;
        /** Сколько тиков цветок на глазах у игрока: за сотню он вырастает из семечка. */
        public int growthCounter;

        public Bloom(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ETHEREAL_BLOOM.get(), pos, state);
        }

        public static void tick(Level level, BlockPos pos, BlockState state, Bloom bloom) {
            if (bloom.counter == 0) {
                bloom.counter = level.random.nextInt(100);
            }
            bloom.counter++;
            if (level instanceof ServerLevel server && bloom.counter % 20 == 0) {
                bloom.cleanse(server, pos);
            }
            if (level.isClientSide && bloom.growthCounter == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.ROOTS.get(), SoundSource.BLOCKS,
                        1f, 0.6f, false);
            }
            bloom.growthCounter++;
        }

        private void cleanse(ServerLevel level, BlockPos pos) {
            int dx = level.random.nextInt(8) - level.random.nextInt(8);
            int dz = level.random.nextInt(8) - level.random.nextInt(8);
            BlockPos target = pos.offset(dx, 0, dz);
            if (dx * dx + dz * dz > 81 || !(ModBiomes.is(level, target, Taint.BIOME) || ModBiomes.is(level, target, ModBiomes.EERIE)
                    || ModBiomes.is(level, target, ModBiomes.MAGICAL_FOREST))) {
                return;
            }
            // биом, который генератор положил сюда изначально; природная порча становится равниной
            Holder<Biome> natural = level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(target.getX()),
                    QuartPos.fromBlock(target.getY()), QuartPos.fromBlock(target.getZ()), level.getChunkSource().randomState().sampler());
            ResourceKey<Biome> key = natural.unwrapKey().orElse(Biomes.PLAINS);
            if (key == Taint.BIOME) {
                key = Biomes.PLAINS;
            }
            if (!ModBiomes.is(level, target, key)) {
                ModBiomes.set(level, target, key);
            }
        }

        /** Цветок с ореолом выше самого блока. */
        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(1.0);
        }
    }
}
