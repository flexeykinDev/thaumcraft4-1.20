package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTBlocks;

/**
 * Левитационный локомотив (BlockMobilizer оригинала) и его ретранслятор. Два ретранслятора на одной высоте и одной
 * линии (до 32 блоков) находят друг друга; локомотив между ними раз в пять секунд, если на него не подан сигнал
 * красного камня, сдвигается на блок вдоль линии вместе с блоком, стоящим на нём, а упёршись — едет обратно.
 */
public class MobilizerBlock extends BaseEntityBlock {
    private final boolean relay;

    public MobilizerBlock(Properties properties, boolean relay) {
        super(properties);
        this.relay = relay;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return relay ? new Relay(pos, state) : new Mobilizer(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (relay) {
            return createTickerHelper(type, TTBlockEntities.RELAY.get(), (l, p, s, t) -> t.tick());
        }
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.MOBILIZER.get(), (l, p, s, t) -> t.tick());
    }

    /** TileEntityRelay оригинала. */
    public static class Relay extends BlockEntity {
        @Nullable
        private BlockPos partner;

        public Relay(BlockPos pos, BlockState state) {
            super(TTBlockEntities.RELAY.get(), pos, state);
        }

        @Nullable
        public BlockPos partner() {
            if (partner != null && !(level.getBlockEntity(partner) instanceof Relay other && worldPosition.equals(other.partner))) {
                partner = null;
            }
            return partner;
        }

        void tick() {
            long time = level.getGameTime();
            if (level.isClientSide) {
                if (partner != null && time % 10 == 0) {
                    // искры бегут по линии между ретрансляторами
                    Direction step = Direction.getNearest(partner.getX() - worldPosition.getX(), 0, partner.getZ() - worldPosition.getZ());
                    for (BlockPos pos = worldPosition.relative(step); !pos.equals(partner); pos = pos.relative(step)) {
                        thaumcraft.client.fx.Fx.sparkle(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1f, 2, 0f);
                    }
                }
                return;
            }
            if (time % 40 != 0) {
                return;
            }
            if (partner() == null) {
                for (int i = -32; i < 32 && partner == null; i++) {
                    if (i != 0) {
                        link(worldPosition.offset(0, 0, i));
                        link(worldPosition.offset(i, 0, 0));
                    }
                }
            }
            if (partner != null) {
                Direction step = Direction.getNearest(partner.getX() - worldPosition.getX(), 0, partner.getZ() - worldPosition.getZ());
                for (BlockPos pos = worldPosition.relative(step); !pos.equals(partner); pos = pos.relative(step)) {
                    if (level.getBlockEntity(pos) instanceof Mobilizer mobilizer && !mobilizer.isLinked()) {
                        mobilizer.link(worldPosition, partner, partner.getX() != worldPosition.getX() ? Direction.EAST : Direction.NORTH);
                    }
                }
            }
        }

        private void link(BlockPos pos) {
            if (partner == null && level.getBlockEntity(pos) instanceof Relay other && other.partner() == null) {
                partner = pos;
                other.partner = worldPosition;
                setChanged();
                other.setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                level.sendBlockUpdated(pos, other.getBlockState(), other.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (partner != null) {
                tag.putLong("Partner", partner.asLong());
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            partner = tag.contains("Partner") ? BlockPos.of(tag.getLong("Partner")) : null;
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
            return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }
    }

    /** TileEntityMobilizer оригинала. */
    public static class Mobilizer extends BlockEntity {
        @Nullable
        private BlockPos first;
        @Nullable
        private BlockPos second;
        private Direction direction = Direction.EAST;

        public Mobilizer(BlockPos pos, BlockState state) {
            super(TTBlockEntities.MOBILIZER.get(), pos, state);
        }

        public boolean isLinked() {
            return first != null && second != null && level.getBlockEntity(first) instanceof Relay relay && second.equals(relay.partner());
        }

        void link(BlockPos first, BlockPos second, Direction direction) {
            this.first = first;
            this.second = second;
            this.direction = direction;
            setChanged();
        }

        void tick() {
            long time = level.getGameTime();
            if (!isLinked() || level.hasNeighborSignal(worldPosition)) {
                return;
            }
            BlockPos next = worldPosition.relative(direction);
            if (time % 100 == 0 && !(level.isEmptyBlock(next) && level.isEmptyBlock(next.above()))) {
                direction = direction.getOpposite();
                setChanged();
            } else if (time % 100 == 1 && level.isEmptyBlock(next) && level.isEmptyBlock(next.above())) {
                move(next);
            }
        }

        /** Переезд на соседнюю клетку вместе с блоком сверху и его содержимым. */
        private void move(BlockPos next) {
            Level world = level;
            BlockPos above = worldPosition.above();
            BlockState cargo = world.getBlockState(above);
            boolean carry = !cargo.isAir() && cargo.getDestroySpeed(world, above) >= 0 && cargo.canSurvive(world, next.above());
            CompoundTag cargoData = null;
            if (carry) {
                BlockEntity tile = world.getBlockEntity(above);
                cargoData = tile == null ? null : tile.saveWithoutMetadata();
                world.removeBlockEntity(above);
                world.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                world.setBlock(next.above(), cargo, Block.UPDATE_ALL);
                if (cargoData != null && world.getBlockEntity(next.above()) != null) {
                    world.getBlockEntity(next.above()).load(cargoData);
                }
            }
            CompoundTag self = saveWithoutMetadata();
            BlockPos old = worldPosition;
            world.removeBlockEntity(old);
            world.setBlock(old, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            world.setBlock(next, TTBlocks.MOBILIZER.get().defaultBlockState(), Block.UPDATE_ALL);
            if (world.getBlockEntity(next) instanceof Mobilizer moved) {
                moved.load(self);
                moved.setChanged();
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (first != null && second != null) {
                tag.putLong("First", first.asLong());
                tag.putLong("Second", second.asLong());
            }
            tag.putInt("Direction", direction.get3DDataValue());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            first = tag.contains("First") ? BlockPos.of(tag.getLong("First")) : null;
            second = tag.contains("Second") ? BlockPos.of(tag.getLong("Second")) : null;
            direction = Direction.from3DDataValue(tag.getInt("Direction"));
            if (direction.getAxis() == Direction.Axis.Y) {
                direction = Direction.EAST;
            }
        }
    }
}
