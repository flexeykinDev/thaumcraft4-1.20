package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

/**
 * Чародейская лампа — перенос блока blockMetalDevice:7 и TileArcaneLamp оригинала. Светит сама и понемногу
 * расставляет невидимые огоньки во всех тёмных местах в шестнадцати блоках вокруг; сломанная лампа гасит их.
 */
public class ArcaneLampBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

    public ArcaneLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Лампа крепится к блоку, по которому щёлкнули. */
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
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Lamp(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ARCANE_LAMP.get(), Lamp::tick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            // removeLights оригинала
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-15, -15, -15), pos.offset(15, 15, 15))) {
                if (level.getBlockState(near).is(ModBlocks.LAMP_LIGHT.get())) {
                    level.removeBlock(near, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    public static class Lamp extends BlockEntity {
        public Lamp(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ARCANE_LAMP.get(), pos, state);
        }

        static void tick(Level level, BlockPos pos, BlockState state, Lamp lamp) {
            int x = pos.getX() + level.random.nextInt(16) - level.random.nextInt(16);
            int y = pos.getY() + level.random.nextInt(16) - level.random.nextInt(16);
            int z = pos.getZ() + level.random.nextInt(16) - level.random.nextInt(16);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 4;
            y = Math.max(Math.min(y, surface), level.getMinBuildHeight() + 5);
            BlockPos target = new BlockPos(x, y, z);
            if (level.isLoaded(target) && level.getBlockState(target).isAir() && !level.getBlockState(target).is(ModBlocks.LAMP_LIGHT.get())
                    && level.getMaxLocalRawBrightness(target) < 9) {
                level.setBlock(target, ModBlocks.LAMP_LIGHT.get().defaultBlockState(), 3);
                if (level.getBlockEntity(target) instanceof Light light) {
                    light.source = pos;
                    light.setChanged();
                }
            }
        }
    }

    /** Невидимый огонёк лампы (blockAiry:3 оригинала): воздух, который светится. */
    public static class LightBlock extends AirBlock implements EntityBlock {
        public LightBlock(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Light(pos, state);
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide || type != ModBlockEntities.LAMP_LIGHT.get() ? null : (BlockEntityTicker<T>) (BlockEntityTicker<Light>) Light::tick;
        }
    }

    /** TileArcaneLampLight: раз в пять секунд огонёк проверяет, цела ли его лампа, и без неё гаснет. */
    public static class Light extends BlockEntity {
        BlockPos source = BlockPos.ZERO;
        private int count;

        public Light(BlockPos pos, BlockState state) {
            super(ModBlockEntities.LAMP_LIGHT.get(), pos, state);
        }

        static void tick(Level level, BlockPos pos, BlockState state, Light light) {
            if (light.count == 0) {
                light.count = level.random.nextInt(100);
            }
            if (++light.count % 100 == 0 && !(level.getBlockEntity(light.source) instanceof Lamp)) {
                level.removeBlock(pos, false);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("sourceX", source.getX());
            tag.putInt("sourceY", source.getY());
            tag.putInt("sourceZ", source.getZ());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            source = new BlockPos(tag.getInt("sourceX"), tag.getInt("sourceY"), tag.getInt("sourceZ"));
        }
    }
}
