package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Силовое поле зелья терры (BlockForcefield + TileForcefield): невидимая стена на три секунды, искрится. */
public class ForcefieldBlock extends Block {
    public static final int LIFETIME = 60;

    public ForcefieldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        level.scheduleTick(pos, this, LIFETIME);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        thaumictinkerer.client.TTClientHooks.forcefieldSparkle(pos, random);
    }
}
