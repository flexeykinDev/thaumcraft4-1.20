package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blockentity.ResearchTableBlockEntity;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

/**
 * Стол исследований (BlockTable оригинала, мета 2-9): два блока. В главной половине лежат вещи и данные,
 * вторая только продолжает стол. FACING каждой половины смотрит на другую.
 */
public class ResearchTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty MAIN = BooleanProperty.create("main");
    /** Только для вида: на столе стоит чернильница / лежит свиток заметки. */
    public static final BooleanProperty INK = BooleanProperty.create("ink");
    public static final BooleanProperty NOTE = BooleanProperty.create("note");

    private static final VoxelShape SHAPE = Shapes.or(box(0, 12, 0, 16, 16, 16), box(2, 0, 2, 14, 12, 14));

    public ResearchTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MAIN, true)
                .setValue(INK, false).setValue(NOTE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MAIN, INK, NOTE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(MAIN) ? new ResearchTableBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.RESEARCH_TABLE.get(), ResearchTableBlockEntity::serverTick);
    }

    private static BlockPos mainPos(BlockState state, BlockPos pos) {
        return state.getValue(MAIN) ? pos : pos.relative(state.getValue(FACING));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos main = mainPos(state, pos);
        if (!level.isClientSide && level.getBlockEntity(main) instanceof ResearchTableBlockEntity table
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, table, main);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Ломается одна половина — вторая превращается обратно в обычный стол, а вещи выпадают. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
                Containers.dropContents(level, pos, table.getItems());
            }
            BlockPos other = pos.relative(state.getValue(FACING));
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this)) {
                if (level.getBlockEntity(other) instanceof ResearchTableBlockEntity table) {
                    Containers.dropContents(level, other, table.getItems());
                    table.getItems().clear();
                }
                level.setBlock(other, ModBlocks.TABLE.get().defaultBlockState()
                        .setValue(TableBlock.AXIS, state.getValue(FACING).getClockWise().getAxis()), 3);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
