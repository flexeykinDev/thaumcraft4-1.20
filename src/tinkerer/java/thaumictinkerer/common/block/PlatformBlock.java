package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumictinkerer.common.registry.TTBlockEntities;

/**
 * Эфирная платформа (BlockPlatform оригинала): на неё можно встать, но снизу сквозь неё проходят насквозь, а с Shift
 * с неё спрыгивают вниз. Маскируется под любой полный блок (см. {@link CamoBlockEntity}).
 */
public class PlatformBlock extends BaseEntityBlock {
    public static final BooleanProperty CAMO = CamoBlockEntity.CAMO;

    public PlatformBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CAMO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAMO);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isDescending() || !context.isAbove(Shapes.block(), pos, true)) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    /** Под маскировку годится только обычный полный блок без своего содержимого (validRenderTypes оригинала). */
    public static boolean canDisguiseAs(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.isAir() && state.getRenderShape() == RenderShape.MODEL && !(state.getBlock() instanceof EntityBlock)
                && state.isCollisionShapeFullBlock(level, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return CamoBlockEntity.useCamo(state, level, pos, player, hand);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    public static class Tile extends CamoBlockEntity {
        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.PLATFORM.get(), pos, state);
        }
    }
}
