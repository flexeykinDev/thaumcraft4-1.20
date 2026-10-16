package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Магические мехи (BlockWoodenDevice оригинала, мета 0). Соплом смотрят на блок, к которому приставлены:
 * тигель от каждых мехов греется втрое быстрее, алхимическая печь плавит на восьмую быстрее, буфер эссенции
 * начинает тянуть с силой 32, а обычная печь готовит в полтора раза быстрее. Сигнал красного камня их останавливает.
 */
public class BellowsBlock extends BaseEntityBlock {
    /** Куда направлено сопло. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = box(2, 2, 2, 14, 14, 14);

    public BellowsBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Мехи приставляются соплом к блоку, по которому кликнули. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Bellows(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BELLOWS.get(), level.isClientSide ? Bellows::clientTick : Bellows::serverTick);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    /** TileBellows.getBellows оригинала: сколько работающих мехов дует на этот блок с перечисленных сторон. */
    public static int count(Level level, BlockPos pos, Iterable<Direction> sides) {
        int bellows = 0;
        for (Direction side : sides) {
            BlockPos near = pos.relative(side);
            BlockState state = level.getBlockState(near);
            if (state.getBlock() instanceof BellowsBlock && state.getValue(FACING) == side.getOpposite() && !level.hasNeighborSignal(near)) {
                bellows++;
            }
        }
        return bellows;
    }

    /** TileBellows оригинала: на клиенте мехи «дышат», на сервере подгоняют обычную печь. */
    public static class Bellows extends BlockEntity {
        private float inflation = 1f;
        private float inflationOld = 1f;
        private boolean inflating;
        private boolean firstRun = true;
        private int delay;

        public Bellows(BlockPos pos, BlockState state) {
            super(ModBlockEntities.BELLOWS.get(), pos, state);
        }

        /** Насколько раздуты мехи: от 0.35 до 1. */
        public float getInflation(float partialTick) {
            return inflationOld + (inflation - inflationOld) * partialTick;
        }

        public static void clientTick(Level level, BlockPos pos, BlockState state, Bellows bellows) {
            bellows.inflationOld = bellows.inflation;
            if (level.hasNeighborSignal(pos)) {
                return;
            }
            if (bellows.firstRun) {
                // чтобы мехи рядом не дышали в унисон
                bellows.inflation = bellows.inflationOld = 0.35f + level.random.nextFloat() * 0.55f;
                bellows.firstRun = false;
            }
            if (bellows.inflation > 0.35f && !bellows.inflating) {
                bellows.inflation -= 0.075f;
            }
            if (bellows.inflation <= 0.35f && !bellows.inflating) {
                bellows.inflating = true;
            }
            if (bellows.inflation < 1f && bellows.inflating) {
                bellows.inflation += 0.025f;
            }
            if (bellows.inflation >= 1f && bellows.inflating) {
                bellows.inflating = false;
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS,
                        0.01f, 0.5f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f, false);
            }
        }

        /** Раз в два тика добавляет готовке в обычной печи лишний тик: топливо при этом не тратится быстрее. */
        public static void serverTick(Level level, BlockPos pos, BlockState state, Bellows bellows) {
            Direction facing = state.getValue(FACING);
            if (facing.getAxis().isVertical() || level.hasNeighborSignal(pos) || ++bellows.delay < 2) {
                return;
            }
            bellows.delay = 0;
            if (level.getBlockEntity(pos.relative(facing)) instanceof AbstractFurnaceBlockEntity furnace) {
                // у печи нет открытого способа подвинуть готовку, поэтому через её сохранённые данные
                CompoundTag tag = furnace.saveWithoutMetadata();
                int cook = tag.getInt("CookTime");
                if (cook > 0 && cook < tag.getInt("CookTimeTotal") - 1) {
                    tag.putInt("CookTime", cook + 1);
                    furnace.load(tag);
                }
            }
        }
    }
}
