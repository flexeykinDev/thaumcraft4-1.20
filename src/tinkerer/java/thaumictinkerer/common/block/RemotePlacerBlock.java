package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTMenus;

/**
 * Удалённый установщик (BlockRPlacer оригинала): по сигналу красного камня ставит блок из своей ячейки в одну–четыре
 * клетки перед лицевой стороной. Жезлом лицевая сторона поворачивается к грани щелчка, Shift+жезл открывает окно.
 * Маскируется под другие блоки.
 */
public class RemotePlacerBlock extends TransvectorBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public RemotePlacerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CAMO, false).setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAMO, FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof WandItem) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof Tile tile) {
                if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openScreen(serverPlayer, tile, pos);
                } else {
                    level.setBlock(pos, state.setValue(FACING, hit.getDirection()), Block.UPDATE_ALL);
                    level.playSound(null, pos, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.6f, 1f);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        if (powered && !state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(pos) instanceof Tile tile) {
                tile.place();
            }
        } else if (!powered && state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Tile tile) {
            Containers.dropContents(level, pos, tile);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Tile(pos, state);
    }

    /** TileRPlacer оригинала: ячейка с блоками и дальность 1–4. */
    public static class Tile extends CamoBlockEntity implements Container, MenuProvider {
        private ItemStack stack = ItemStack.EMPTY;
        public int distance = 1;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.REMOTE_PLACER.get(), pos, state);
        }

        public void place() {
            if (!(stack.getItem() instanceof BlockItem item) || !(level instanceof ServerLevel server)) {
                return;
            }
            Direction facing = getBlockState().getValue(FACING);
            BlockPos target = worldPosition.relative(facing, distance);
            if (!level.isEmptyBlock(target)) {
                return;
            }
            var player = FakePlayerFactory.getMinecraft(server);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), facing.getOpposite(), target, false);
            item.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            sync();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!stack.isEmpty()) {
                tag.put("Item", stack.save(new CompoundTag()));
            }
            tag.putInt("Distance", distance);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            stack = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
            distance = Math.max(1, Math.min(4, tag.getInt("Distance")));
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return stack;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = stack.split(amount);
            sync();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = stack;
            stack = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.stack = stack;
            sync();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof BlockItem;
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            stack = ItemStack.EMPTY;
        }

        @Override
        public Component getDisplayName() {
            return getBlockState().getBlock().getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new Menu(id, inventory, this);
        }

        public void setDistance(int distance) {
            this.distance = Math.max(1, Math.min(4, distance));
            sync();
        }
    }

    /** ContainerRemotePlacer оригинала: ячейка (20, 30); кнопки окна 1–4 — дальность. */
    public static class Menu extends AbstractContainerMenu {
        private final Container container;
        @Nullable
        public final Tile tile;

        public Menu(int id, Inventory inventory, BlockPos pos) {
            this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof Tile tile ? tile : null);
        }

        public Menu(int id, Inventory inventory, @Nullable Tile tile) {
            super(TTMenus.REMOTE_PLACER.get(), id);
            this.tile = tile;
            this.container = tile != null ? tile : new SimpleContainer(1);
            addSlot(new Slot(container, 0, 20, 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof BlockItem;
                }
            });
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column, 8 + column * 18, 142));
            }
        }

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (tile != null && id >= 1 && id <= 4) {
                tile.setDistance(id);
            }
            return true;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return original;
        }

        @Override
        public boolean stillValid(Player player) {
            return container.stillValid(player);
        }
    }
}
