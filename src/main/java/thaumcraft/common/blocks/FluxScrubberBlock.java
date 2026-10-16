package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

import java.util.Random;

/**
 * Очиститель скверны — перенос blockStoneDevice:14 и TileFluxScrubber оригинала. Тянет Aer из сети вис и, накопив
 * силы, выискивает в шестнадцати блоках вокруг слизь и газ скверны и выпаривает их по капле. Из каждых четырёх
 * капель с шансом в четверть выходит единица Praecantatio (до четырёх в запасе), которую можно вывести трубой
 * с той стороны, где очиститель крепится.
 */
public class FluxScrubberBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FluxScrubberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Труба подсоединяется к той стороне, которой очиститель приставлен к блоку. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scrubber(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FLUX_SCRUBBER.get(), Scrubber::tick);
    }

    public static class Scrubber extends BlockEntity implements EssentiaTransport {
        private static final int DISTANCE = 16;
        private static final int SIDE = DISTANCE * 2 + 1;
        public int essentia;
        public int charges;
        public int power;
        /** Случайный сдвиг для покачивания наконечника (count оригинала). */
        public final int phase = new Random().nextInt(1000);
        private int[] order;
        private int cursor;

        public Scrubber(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FLUX_SCRUBBER.get(), pos, state);
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        static void tick(Level level, BlockPos pos, BlockState state, Scrubber scrubber) {
            if (scrubber.charges >= 4) {
                scrubber.charges -= 4;
                if (level.random.nextInt(4) == 0) {
                    scrubber.essentia = Math.min(4, scrubber.essentia + 1);
                    scrubber.setChanged();
                }
            }
            if (scrubber.power < 5) {
                scrubber.power += VisNetHandler.drainVis(level, pos, Aspect.AIR, 10);
            }
            if (scrubber.power >= 5) {
                scrubber.checkFlux((ServerLevel) level);
            }
        }

        private static boolean isFlux(BlockState state) {
            return state.is(ModBlocks.FLUX_GOO.get()) || state.is(ModBlocks.FLUX_GAS.get());
        }

        /** Шестнадцать случайных клеток за такт, пока не обойдён весь куб (перемешанный список оригинала). */
        private void checkFlux(ServerLevel level) {
            if (order == null || cursor >= order.length) {
                order = new int[SIDE * SIDE * SIDE];
                for (int i = 0; i < order.length; i++) {
                    order[i] = i;
                }
                for (int i = order.length - 1; i > 0; i--) {
                    int j = level.random.nextInt(i + 1);
                    int t = order[i];
                    order[i] = order[j];
                    order[j] = t;
                }
                cursor = 0;
            }
            for (int checked = 0; checked < 16 && cursor < order.length; checked++) {
                int index = order[cursor++];
                BlockPos at = worldPosition.offset(index % SIDE - DISTANCE, index / SIDE % SIDE - DISTANCE, index / (SIDE * SIDE) - DISTANCE);
                BlockState state = level.getBlockState(at);
                if (state.isAir() || !isFlux(state) || at.getCenter().distanceToSqr(worldPosition.getCenter()) >= DISTANCE * DISTANCE) {
                    continue;
                }
                power -= 5;
                int amount = state.getValue(FluxBlock.LEVEL);
                if (amount > 0) {
                    level.setBlock(at, state.setValue(FluxBlock.LEVEL, amount - 1), 3);
                } else {
                    level.removeBlock(at, false);
                }
                FxPacket.blockSparkle(level, at, 14483711, 1);
                charges++;
                setChanged();
                return;
            }
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side == facing();
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return false;
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return side == facing();
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
        }

        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            int taken = Math.min(essentia, amount);
            essentia -= taken;
            setChanged();
            return taken;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            return Aspect.MAGIC;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return essentia;
        }

        @Override
        public int getMinimumSuction() {
            return 0;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putInt("charges", charges);
            tag.putInt("power", power);
            tag.putInt("essentia", essentia);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            charges = tag.getInt("charges");
            power = tag.getInt("power");
            essentia = tag.getInt("essentia");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
