package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Фокус пьедестала (blockStoneDevice:8 оригинала): каменная рамка, которая ставится прямо над пьедесталом подзарядки.
 * С ней пьедестал тянет из узлов и составные аспекты, превращая каждый в один из первичных, из которых он сложен.
 */
public class WandPedestalFocusBlock extends Block {
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 7, 15);

    public WandPedestalFocusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Щелчок по фокусу — это щелчок по пьедесталу под ним: предмет парит как раз внутри рамки. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState pedestal = level.getBlockState(below);
        if (pedestal.getBlock() instanceof WandPedestalBlock) {
            return pedestal.use(level, player, hand, hit.withPosition(below));
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
