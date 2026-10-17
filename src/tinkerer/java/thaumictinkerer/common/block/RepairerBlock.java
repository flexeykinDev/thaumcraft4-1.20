package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.network.FxPacket;
import thaumictinkerer.common.registry.TTBlockEntities;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Таумический восстановитель (BlockRepairer оригинала): чинит положенную в него вещь эссенцией, которую тянет по трубе
 * с лицевой стороны. Раз в полсекунды берёт единицу: инструмент чинит 8 прочности, ремесло — 5, порядок — 3.
 */
public class RepairerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RepairerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Пока вещь в нём не починена до конца, восстановитель подаёт сигнал красного камня (isProvidingStrongPower оригинала). */
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof Tile repairer && repairer.item.getDamageValue() > 0 ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    public static boolean canRepair(ItemStack stack) {
        return stack.isDamageableItem() && stack.getItem().isRepairable(stack);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Tile repairer)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (repairer.item.isEmpty()) {
            if (!canRepair(held)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                repairer.item = held.split(1);
                repairer.changed();
            }
        } else if (!level.isClientSide) {
            if (!player.getInventory().add(repairer.item)) {
                player.drop(repairer.item, false);
            }
            repairer.item = ItemStack.EMPTY;
            repairer.changed();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Tile repairer) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), repairer.item);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TTBlockEntities.REPAIRER.get(), (l, p, s, tile) -> tile.tick());
    }

    /** TileRepairer оригинала: одна ячейка, тянет эссенцию по трубе и с силой 128 всасывает инструмент. */
    public static class Tile extends BlockEntity implements WorldlyContainer, EssentiaTransport, thaumcraft.api.aspects.AspectContainer {
        private static final Map<Aspect, Integer> REPAIR = new LinkedHashMap<>();

        static {
            REPAIR.put(Aspect.TOOL, 8);
            REPAIR.put(Aspect.CRAFT, 5);
            REPAIR.put(Aspect.ORDER, 3);
        }

        private ItemStack item = ItemStack.EMPTY;
        /** Чинил ли в прошлый раз — по этому на верхушке горит или гаснет значок. */
        private boolean working;
        private int lastDamage;
        public int ticks;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.REPAIRER.get(), pos, state);
        }

        public ItemStack getItem() {
            return item;
        }

        public boolean isWorking() {
            return working;
        }

        private Direction facing() {
            return getBlockState().getValue(FACING);
        }

        void tick() {
            ticks++;
            if (level.isClientSide || ticks % 10 != 0) {
                return;
            }
            boolean wasWorking = working;
            if (!item.isEmpty() && item.getDamageValue() > 0) {
                int damage = item.getDamageValue();
                int fixed = drawEssentia();
                item.setDamageValue(Math.max(0, damage - fixed));
                working = lastDamage != 0 && lastDamage != damage;
                if (working) {
                    FxPacket.send((ServerLevel) level, FxPacket.SPARKLES, Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), Vec3.ZERO, 0, 25);
                }
                if (fixed > 0) {
                    changed();
                }
            } else {
                working = false;
            }
            lastDamage = item.isEmpty() ? 0 : item.getDamageValue();
            if (wasWorking != working) {
                changed();
            }
        }

        private int drawEssentia() {
            Direction side = facing();
            EssentiaTransport tube = EssentiaTransport.neighbour(level, worldPosition, side);
            if (tube == null || !tube.canOutputTo(side.getOpposite())) {
                return 0;
            }
            for (Map.Entry<Aspect, Integer> repair : REPAIR.entrySet()) {
                if (tube.getSuctionType(side.getOpposite()) == repair.getKey()
                        && tube.getSuctionAmount(side.getOpposite()) < getSuctionAmount(side)
                        && tube.takeEssentia(repair.getKey(), 1, side.getOpposite()) == 1) {
                    return repair.getValue();
                }
            }
            return 0;
        }

        void changed() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }

        /** Сквозь очки откровения износ вещи виден как энтропия (getAspects оригинала). */
        @Nullable
        @Override
        public thaumcraft.api.aspects.AspectList getContainedAspects() {
            return item.isEmpty() || item.getDamageValue() == 0 ? null
                    : new thaumcraft.api.aspects.AspectList().add(Aspect.ENTROPY, item.getDamageValue());
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!item.isEmpty()) {
                tag.put("Item", item.save(new CompoundTag()));
            }
            tag.putBoolean("Working", working);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            item = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
            working = tag.getBoolean("Working");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        // ---------- трубы: подключаются только с лицевой стороны
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

        @Nullable
        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return side == facing() ? Aspect.TOOL : null;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return side == facing() ? 128 : 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return 0;
        }

        @Nullable
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

        // ---------- хоппер может подать вещь на починку и забрать её
        @Override
        public int[] getSlotsForFace(Direction side) {
            return new int[]{0};
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return canRepair(stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return true;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return item.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return item;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = item.split(amount);
            changed();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = item;
            item = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            item = stack;
            changed();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
            item = ItemStack.EMPTY;
        }
    }
}
