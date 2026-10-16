package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Голодный сундук (BlockChestHungry оригинала): обычный сундук на 27 ячеек, который сам глотает предметы,
 * упавшие на него или коснувшиеся его, — на миг щёлкая крышкой.
 */
public class HungryChestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 14, 15);

    public HungryChestBlock(Properties properties) {
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        return new Chest(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.HUNGRY_CHEST.get(), Chest::lidTick) : null;
    }

    /** Сундук съедает предмет, который его коснулся, — сколько поместится. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ItemEntity item) || !item.isAlive() || !(level.getBlockEntity(pos) instanceof Chest chest)) {
            return;
        }
        ItemStack stack = item.getItem();
        int before = stack.getCount();
        ItemStack left = HopperBlockEntity.addItem(null, chest, stack.copy(), Direction.UP);
        if (left.getCount() != before) {
            level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.25f,
                    (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1f);
            level.blockEvent(pos, this, 2, 2);
            if (left.isEmpty()) {
                item.discard();
            } else {
                item.setItem(left);
            }
            chest.setChanged();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Chest chest && player instanceof ServerPlayer) {
            player.openMenu(chest);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Chest chest) {
            Containers.dropContents(level, pos, chest);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    /** TileChestHungry оригинала. */
    public static class Chest extends BaseContainerBlockEntity {
        private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
        private int numUsing;
        public float lidAngle;
        public float prevLidAngle;

        public Chest(BlockPos pos, BlockState state) {
            super(ModBlockEntities.HUNGRY_CHEST.get(), pos, state);
        }

        /** Крышка: плавно открывается, пока сундук кем-то открыт, и захлопывается, когда нет. */
        public static void lidTick(Level level, BlockPos pos, BlockState state, Chest chest) {
            chest.prevLidAngle = chest.lidAngle;
            if (chest.numUsing > 0 && chest.lidAngle == 0f) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f,
                        level.random.nextFloat() * 0.1f + 0.9f, false);
            }
            if (chest.numUsing == 0 && chest.lidAngle > 0f || chest.numUsing > 0 && chest.lidAngle < 1f) {
                float before = chest.lidAngle;
                chest.lidAngle = Math.min(1f, chest.lidAngle + (chest.numUsing > 0 ? 0.1f : -0.1f));
                if (chest.lidAngle < 0.5f && before >= 0.5f) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f,
                            level.random.nextFloat() * 0.1f + 0.9f, false);
                }
                if (chest.lidAngle < 0f) {
                    chest.lidAngle = 0f;
                }
            }
        }

        /** 1 — сколько игроков держат сундук открытым; 2 — крышка подскакивает (сундук что-то съел). */
        @Override
        public boolean triggerEvent(int id, int value) {
            if (id == 1) {
                numUsing = value;
                return true;
            }
            if (id == 2) {
                lidAngle = Math.max(lidAngle, value / 10f);
                return true;
            }
            return super.triggerEvent(id, value);
        }

        @Override
        public void startOpen(Player player) {
            if (!player.isSpectator() && level != null) {
                numUsing++;
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, numUsing);
            }
        }

        @Override
        public void stopOpen(Player player) {
            if (!player.isSpectator() && level != null) {
                numUsing = Math.max(0, numUsing - 1);
                level.blockEvent(worldPosition, getBlockState().getBlock(), 1, numUsing);
            }
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.thaumcraft.hungry_chest");
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return ChestMenu.threeRows(id, inventory, this);
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = ContainerHelper.removeItem(items, slot, amount);
            if (!taken.isEmpty()) {
                setChanged();
            }
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && level.getBlockEntity(worldPosition) == this
                    && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void clearContent() {
            items.clear();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            ContainerHelper.saveAllItems(tag, items);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            items = NonNullList.withSize(27, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);
        }
    }
}
