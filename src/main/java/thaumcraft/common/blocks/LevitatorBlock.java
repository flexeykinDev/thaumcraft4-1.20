package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Чародейский левитатор — перенос BlockLifter и TileLifter оригинала. Поднимает всё, что над ним: предметы, существ,
 * игроков — на десять блоков, и ещё на десять за каждый левитатор, поставленный под ним столбиком. Присевший
 * не поднимается, а медленно опускается. Сигнал красного камня выключает левитатор.
 */
public class LevitatorBlock extends BaseEntityBlock {
    /** Работает ли левитатор: от этого зависит, светится ли он в полную силу. */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public LevitatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Lifter(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LEVITATOR.get(), Lifter::tick);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null && direction.getAxis().isHorizontal();
    }

    /** updateLifterStack: стоящим ниже левитаторам пора пересчитать, докуда они достают. */
    private static void updateStack(Level level, BlockPos pos) {
        for (BlockPos below = pos; level.getBlockEntity(below) instanceof Lifter lifter; below = below.below()) {
            lifter.requiresUpdate = true;
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!old.is(this)) {
            updateStack(level, pos.below());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this)) {
            updateStack(level, pos.below());
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.getBlockEntity(pos) instanceof Lifter lifter && Lifter.powered(level, pos) != lifter.lastPower) {
            updateStack(level, pos);
        }
        super.neighborChanged(state, level, pos, block, from, moving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            Fx.sparkle(pos.getX() + 0.2 + random.nextFloat() * 0.6, pos.getY() + 1, pos.getZ() + 0.2 + random.nextFloat() * 0.6, 1f, 3, -0.3f);
        }
    }

    public static class Lifter extends BlockEntity {
        public int rangeAbove;
        boolean requiresUpdate = true;
        boolean lastPower;
        private int counter;

        public Lifter(BlockPos pos, BlockState state) {
            super(ModBlockEntities.LEVITATOR.get(), pos, state);
        }

        static boolean powered(Level level, BlockPos pos) {
            return level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        }

        static void tick(Level level, BlockPos pos, BlockState state, Lifter lifter) {
            lifter.counter++;
            boolean powered = powered(level, pos);
            if (lifter.requiresUpdate || lifter.counter % 100 == 0) {
                lifter.lastPower = powered;
                lifter.requiresUpdate = false;
                int max = 10;
                for (BlockPos below = pos.below(); level.getBlockState(below).is(state.getBlock()) && !level.hasNeighborSignal(below);
                     below = below.below()) {
                    max += 10;
                }
                lifter.rangeAbove = 0;
                while (lifter.rangeAbove < max && !level.getBlockState(pos.above(1 + lifter.rangeAbove)).isSolidRender(level,
                        pos.above(1 + lifter.rangeAbove))) {
                    lifter.rangeAbove++;
                }
                boolean active = !powered && lifter.rangeAbove > 0;
                if (!level.isClientSide && state.getValue(ACTIVE) != active) {
                    level.setBlock(pos, state.setValue(ACTIVE, active), 2);
                }
            }
            if (lifter.rangeAbove <= 0 || powered) {
                return;
            }
            for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1,
                    pos.getY() + 1 + lifter.rangeAbove, pos.getZ() + 1))) {
                if (!(entity instanceof ItemEntity) && !entity.isPushable() && !(entity instanceof AbstractHorse)) {
                    continue;
                }
                Vec3 motion = entity.getDeltaMovement();
                // в оригинале смотрели на клавишу приседания у того, кто играет; теперь — на то, присел ли сам игрок
                if (entity.isShiftKeyDown()) {
                    if (motion.y < 0.0) {
                        entity.setDeltaMovement(motion.x, motion.y * 0.9, motion.z);
                    }
                } else if (motion.y < 0.35) {
                    entity.setDeltaMovement(motion.x, motion.y + 0.1, motion.z);
                }
                entity.fallDistance = 0f;
            }
        }
    }
}
