package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.entity.golem.GolemWorkGoals;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Лампы роста и плодородия — перенос blockMetalDevice:8 и 13 с TileArcaneLampGrowth и TileArcaneLampFertility оригинала.
 * Лампа тянет по трубе со стороны, которой крепится, эссенцию: лампа роста — Herba (каждая единица — сто толчков
 * росту растений в шести блоках вокруг), лампа плодородия — Victus (до четырёх зарядов; два заряда сводят пару
 * одинаковых животных, если их рядом не больше семи). Без эссенции лампа светит вполсилы.
 */
public class EssentiaLampBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

    private final boolean growth;

    public EssentiaLampBlock(boolean growth, Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 15 : 8));
        this.growth = growth;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

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
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ESSENTIA_LAMP.get(), Lamp::tick);
    }

    public static class Lamp extends BlockEntity implements EssentiaTransport {
        /** Лампа роста: заряды -1 — пусто; reserve — ещё одна единица эссенции про запас. */
        public int charges = -1;
        private boolean reserve;
        private int drawDelay;
        private int count;
        private final List<BlockPos> checklist = new ArrayList<>();
        private BlockPos last = BlockPos.ZERO;
        private BlockState lastState = Blocks.AIR.defaultBlockState();

        public Lamp(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ESSENTIA_LAMP.get(), pos, state);
            if (!growth(state)) {
                charges = 0;
            }
        }

        private static boolean growth(BlockState state) {
            return state.getBlock() instanceof EssentiaLampBlock lamp && lamp.growth;
        }

        private Direction facing() {
            return getBlockState().getValue(FACING);
        }

        static void tick(Level level, BlockPos pos, BlockState state, Lamp lamp) {
            if (growth(state)) {
                lamp.tickGrowth((ServerLevel) level);
            } else {
                lamp.tickFertility((ServerLevel) level);
            }
            boolean lit = lamp.charges > 0;
            if (state.getValue(LIT) != lit) {
                level.setBlock(pos, state.setValue(LIT, lit), 3);
            }
        }

        private void tickGrowth(ServerLevel level) {
            if (charges <= 0) {
                if (reserve) {
                    charges = 100;
                    reserve = false;
                    setChanged();
                } else if (drawEssentia(Aspect.PLANT)) {
                    charges = 100;
                    setChanged();
                }
            }
            if (!reserve && drawEssentia(Aspect.PLANT)) {
                reserve = true;
                setChanged();
            }
            if (charges == 0) {
                charges = -1;
                setChanged();
            }
            if (charges > 0) {
                updatePlant(level);
            }
        }

        /** Растение ли это (isPlant оригинала): то, что растёт само, кроме дёрна, мха и прочей «живой земли». */
        private static boolean isPlant(BlockState state) {
            Block block = state.getBlock();
            return (block instanceof BonemealableBlock || block instanceof BushBlock || state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE)
                    || state.is(Blocks.BAMBOO)) && state.isRandomlyTicking() && !state.is(BlockTags.DIRT) && !state.is(BlockTags.NYLIUM);
        }

        private void updatePlant(ServerLevel level) {
            BlockPos pos = getBlockPos();
            // предыдущее растение подросло — над ним вспыхивают зелёные искры
            if (level.getBlockState(last) != lastState) {
                if (level.getNearestPlayer(last.getX(), last.getY(), last.getZ(), 32.0, false) != null) {
                    FxPacket.blockSparkle(level, last, 4259648, 1);
                }
                lastState = level.getBlockState(last);
            }
            int distance = 6;
            if (checklist.isEmpty()) {
                for (int a = -distance; a <= distance; a++) {
                    for (int b = -distance; b <= distance; b++) {
                        checklist.add(pos.offset(a, distance, b));
                    }
                }
                Collections.shuffle(checklist, new java.util.Random(level.random.nextLong()));
            }
            BlockPos column = checklist.remove(0);
            for (int y = column.getY(); y >= pos.getY() - distance; y--) {
                BlockPos at = new BlockPos(column.getX(), y, column.getZ());
                BlockState state = level.getBlockState(at);
                if (!state.isAir() && isPlant(state) && at.getCenter().distanceToSqr(pos.getCenter()) < distance * distance
                        && !GolemWorkGoals.isGrownCrop(level, at)) {
                    charges--;
                    last = at;
                    lastState = state;
                    // в 1.7.10 рост шёл через внеочередное обновление блока; теперь растения растут от случайного такта
                    state.randomTick(level, at, level.random);
                    setChanged();
                    return;
                }
            }
        }

        private void tickFertility(ServerLevel level) {
            if (charges < 4 && drawEssentia(Aspect.LIFE)) {
                charges++;
                setChanged();
            }
            if (charges > 1 && count++ % 300 == 0) {
                updateAnimals(level);
            }
        }

        private void updateAnimals(ServerLevel level) {
            List<Animal> animals = level.getEntitiesOfClass(Animal.class, new AABB(getBlockPos()).inflate(7));
            for (Animal animal : animals) {
                if (animal.getAge() != 0 || animal.isInLove()) {
                    continue;
                }
                List<Animal> same = animals.stream().filter(other -> other.getClass().equals(animal.getClass())).toList();
                if (same.size() > 7) {
                    continue;
                }
                Animal partner = null;
                for (Animal other : same) {
                    if (other.getAge() == 0 && !other.isInLove()) {
                        if (partner != null) {
                            charges -= 2;
                            other.setInLove(null);
                            partner.setInLove(null);
                            setChanged();
                            return;
                        }
                        partner = other;
                    }
                }
            }
        }

        private boolean drawEssentia(Aspect aspect) {
            if (++drawDelay % 5 != 0) {
                return false;
            }
            Direction facing = facing();
            EssentiaTransport source = EssentiaTransport.neighbour(level, getBlockPos(), facing);
            if (source == null || !source.canOutputTo(facing.getOpposite())) {
                return false;
            }
            return source.getSuctionAmount(facing.getOpposite()) < getSuctionAmount(facing)
                    && source.takeEssentia(aspect, 1, facing.getOpposite()) == 1;
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return false;
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
        }

        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return growth(getBlockState()) ? Aspect.PLANT : Aspect.LIFE;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            if (side != facing()) {
                return 0;
            }
            if (growth(getBlockState())) {
                return reserve && charges > 0 ? 0 : 128;
            }
            return 128 - charges * 10;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return 0;
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("reserve", reserve);
            tag.putInt("charges", charges);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            reserve = tag.getBoolean("reserve");
            charges = tag.getInt("charges");
        }
    }
}
