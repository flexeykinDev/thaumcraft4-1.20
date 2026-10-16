package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Саженец великого или серебряного дерева. Как в оригинале, растёт без промежуточной стадии:
 * при случайном тике на свету с шансом 1 к growthChance сразу пытается вырасти.
 */
public class MagicalSaplingBlock extends SaplingBlock {
    private final AbstractTreeGrower grower;
    private final int growthChance;

    public MagicalSaplingBlock(AbstractTreeGrower grower, int growthChance, Properties properties) {
        super(grower, properties);
        this.grower = grower;
        this.growthChance = growthChance;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) {
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && random.nextInt(growthChance) == 0) {
            grower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
        }
    }

    /** Костная мука тоже растит сразу, минуя стадию. */
    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        grower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
    }
}
