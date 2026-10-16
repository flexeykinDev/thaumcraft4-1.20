package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.blockentity.TubeBlockEntity;
import thaumcraft.common.items.LabelItem;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

import java.util.EnumMap;
import java.util.Map;

/**
 * Труба для эссенции (BlockTube оригинала) и её разновидности. Отводы к соседям — свойства блока, по ним
 * строится и модель, и форма; пересчитываются, когда рядом что-то меняется или жезл закрывает отвод.
 */
public class TubeBlock extends BaseEntityBlock {
    /** Виды трубы: мета 0, 1, 3, 5 и 6 оригинала. */
    public enum Kind { NORMAL, VALVE, FILTER, RESTRICT, ONEWAY, BUFFER }

    /** То, у чего есть открываемые жезлом отводы: сама труба и буфер эссенции. */
    public interface Sided {
        boolean isOpen(Direction side);

        void setOpen(Direction side, boolean open);

        Direction getFacing();

        boolean isFlowAllowed();
    }

    public static final Map<Direction, BooleanProperty> ARMS = new EnumMap<>(Direction.class);
    /** Нужен ли узел посередине: у прямого отрезка его нет. */
    public static final BooleanProperty JOINT = BooleanProperty.create("joint");
    /** Направление обратного клапана и ручки вентиля. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** Вентиль открыт. */
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private static final VoxelShape CENTER = box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) {
            ARMS.put(side, BooleanProperty.create(side.getName()));
        }
        ARM_SHAPES.put(Direction.DOWN, box(6, 0, 6, 10, 5, 10));
        ARM_SHAPES.put(Direction.UP, box(6, 11, 6, 10, 16, 10));
        ARM_SHAPES.put(Direction.NORTH, box(6, 6, 0, 10, 10, 5));
        ARM_SHAPES.put(Direction.SOUTH, box(6, 6, 11, 10, 10, 16));
        ARM_SHAPES.put(Direction.WEST, box(0, 6, 6, 5, 10, 10));
        ARM_SHAPES.put(Direction.EAST, box(11, 6, 6, 16, 10, 10));
    }

    public final Kind kind;

    public TubeBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState state = stateDefinition.any().setValue(JOINT, true).setValue(FACING, Direction.NORTH).setValue(OPEN, true);
        for (BooleanProperty arm : ARMS.values()) {
            state = state.setValue(arm, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JOINT, FACING, OPEN);
        // ARMS ещё не заполнена, когда конструктор родителя строит состояния, поэтому имена заданы здесь же
        for (Direction side : Direction.values()) {
            builder.add(arm(side));
        }
    }

    private static BooleanProperty arm(Direction side) {
        return ARMS.computeIfAbsent(side, s -> BooleanProperty.create(s.getName()));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        for (Direction side : Direction.values()) {
            if (state.getValue(arm(side))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(side));
            }
        }
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return kind == Kind.BUFFER ? new thaumcraft.common.blockentity.EssentiaBufferBlockEntity(pos, state)
                : new TubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (kind == Kind.BUFFER) {
            return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ESSENTIA_BUFFER.get(),
                    thaumcraft.common.blockentity.EssentiaBufferBlockEntity::serverTick);
        }
        if (level.isClientSide) {
            return kind == Kind.VALVE ? createTickerHelper(type, ModBlockEntities.TUBE.get(), TubeBlockEntity::clientTick) : null;
        }
        return createTickerHelper(type, ModBlockEntities.TUBE.get(), TubeBlockEntity::serverTick);
    }

    /** Пересчитывает отводы, узел и поворот по данным трубы и соседям. */
    public static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TubeBlock) || !(level.getBlockEntity(pos) instanceof Sided tube)) {
            return;
        }
        BlockState updated = state.setValue(FACING, tube.getFacing()).setValue(OPEN, tube.isFlowAllowed());
        int arms = 0;
        Direction.Axis axis = null;
        boolean straight = true;
        for (Direction side : Direction.values()) {
            boolean connected = tube.isOpen(side) && level.getBlockEntity(pos.relative(side)) instanceof EssentiaTransport other
                    && (other instanceof Sided otherTube ? otherTube.isOpen(side.getOpposite()) : other.isConnectable(side.getOpposite()));
            updated = updated.setValue(arm(side), connected);
            if (connected) {
                arms++;
                if (axis == null) {
                    axis = side.getAxis();
                } else if (axis != side.getAxis()) {
                    straight = false;
                }
            }
        }
        // узел не рисуется только у сквозного прямого отрезка обычной трубы
        updated = updated.setValue(JOINT, !(straight && arms == 2));
        if (updated != state) {
            level.setBlock(pos, updated, 2);
        }
    }

    /** Жезлом по отводу — закрыть или открыть его, вместе с ответным отводом соседа. */
    public static void toggleSide(Level level, BlockPos pos, Direction side) {
        if (!(level.getBlockEntity(pos) instanceof Sided sided)) {
            return;
        }
        boolean open = !sided.isOpen(side);
        sided.setOpen(side, open);
        if (level.getBlockEntity(pos.relative(side)) instanceof Sided other) {
            other.setOpen(side.getOpposite(), open);
            refresh(level, pos.relative(side));
        }
        refresh(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!level.isClientSide) {
            refresh(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        if (!level.isClientSide) {
            refresh(level, pos);
        }
    }

    /**
     * На какую часть трубы пришёлся клик: отвод (сторона) или середина (null). В оригинале для этого трассировались
     * отдельные коробки; здесь сторона определяется по тому, куда от середины блока ушла точка попадания.
     */
    @Nullable
    public static Direction hitPart(BlockPos pos, Vec3 hit) {
        Vec3 local = hit.subtract(Vec3.atCenterOf(pos));
        Direction side = Direction.getNearest(local.x, local.y, local.z);
        double distance = Math.max(Math.abs(local.x), Math.max(Math.abs(local.y), Math.abs(local.z)));
        return distance > 0.2 ? side : null;
    }

    /** Ярлык с аспектом настраивает фильтр; Shift без предмета снимает ярлык. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (kind != Kind.FILTER || !(level.getBlockEntity(pos) instanceof TubeBlockEntity tube)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && tube.getAspectFilter() != null) {
            if (!level.isClientSide) {
                tube.setAspectFilter(null);
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(ModItems.LABEL.get())));
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1f, 1.1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        Aspect aspect = held.getItem() instanceof LabelItem ? LabelItem.getAspect(held) : null;
        if (aspect != null && tube.getAspectFilter() == null) {
            if (!level.isClientSide) {
                tube.setAspectFilter(aspect);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1f, 0.9f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TubeBlockEntity tube && tube.getAspectFilter() != null) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new ItemStack(ModItems.LABEL.get())));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
