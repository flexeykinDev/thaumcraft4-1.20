package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTMenus;

/**
 * Аспектализатор (BlockAspectAnalyzer оригинала): в окне одна ячейка, и для уже изученной вещи под ней показаны
 * её аспекты — удобно держать рядом с местом крафта. В оригинале он ещё служил периферией ComputerCraft.
 */
public class AspectAnalyzerBlock extends BaseEntityBlock {
    public AspectAnalyzerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Tile tile && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, tile, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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

    /** У вещи есть аспекты (SlotLimitedHasAspects оригинала). */
    public static boolean hasAspects(Level level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return level.isClientSide ? !thaumcraft.client.ClientItemAspects.get(stack.getItem()).isEmpty()
                : !thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getAspects(stack).isEmpty();
    }

    /** TileAspectAnalyzer оригинала. */
    public static class Tile extends BlockEntity implements Container, MenuProvider {
        private ItemStack item = ItemStack.EMPTY;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.ASPECT_ANALYZER.get(), pos, state);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!item.isEmpty()) {
                tag.put("Item", item.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            item = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
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
            setChanged();
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
            setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return level != null && hasAspects(level, stack);
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            item = ItemStack.EMPTY;
        }

        @Override
        public Component getDisplayName() {
            return getBlockState().getBlock().getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new Menu(id, inventory, this);
        }
    }

    /** ContainerAspectAnalyzer оригинала: ячейка вещи и инвентарь игрока. */
    public static class Menu extends AbstractContainerMenu {
        private final Container analyzer;

        public Menu(int id, Inventory inventory, BlockPos pos) {
            this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof Tile tile ? tile
                    : new net.minecraft.world.SimpleContainer(1));
        }

        public Menu(int id, Inventory inventory, Container analyzer) {
            super(TTMenus.ASPECT_ANALYZER.get(), id);
            this.analyzer = analyzer;
            Level level = inventory.player.level();
            addSlot(new Slot(analyzer, 0, 20, 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return hasAspects(level, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
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

        public ItemStack analyzed() {
            return analyzer.getItem(0);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!slots.get(0).mayPlace(stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
            return analyzer.stillValid(player);
        }
    }
}
