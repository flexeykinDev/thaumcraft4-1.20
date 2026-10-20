package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModSounds;
import thaumictinkerer.common.item.kami.SkyPearlItem;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTMenus;

/**
 * Врата искривления (BlockWarpGate + TileWarpGate): в них кладут до десяти настроенных небесных жемчужин. Встав на врата
 * и присев, игрок видит карту и Shift+щелчком по жемчужине переносится к другим вратам в этом измерении. Запертые врата
 * не принимают гостей.
 */
public class WarpGateBlock extends BaseEntityBlock {
    public WarpGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof SkyPearlItem && !SkyPearlItem.isAttuned(player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof Tile tile) {
            NetworkHooks.openScreen(server, new SimpleMenuProvider((id, inventory, p) -> new Menu(id, inventory, pos, tile.items),
                    Component.translatable(getDescriptionId())), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Tile tile) {
            Containers.dropContents(level, pos, tile.items);
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
        return level.isClientSide ? createTickerHelper(type, TTBlockEntities.WARP_GATE.get(), (l, p, s, tile) -> tile.clientTick()) : null;
    }

    /**
     * Переносит игрока на врата в pos (TileWarpGate.teleportPlayer). Возвращает true, если перенос удался.
     */
    public static boolean teleport(Player player, BlockPos pos) {
        Level level = player.level();
        if (!(level.getBlockEntity(pos) instanceof Tile gate)) {
            player.displayClientMessage(Component.translatable("ttmisc.noDest"), true);
            return false;
        }
        if (gate.locked) {
            player.displayClientMessage(Component.translatable("ttmisc.noTeleport"), true);
            return false;
        }
        if (level instanceof ServerLevel server) {
            FxPacket.send(server, FxPacket.SPARKLES, player.position().add(0, 1, 0), Vec3.ZERO, 2, 60);
            level.playSound(null, player.blockPosition(), ModSounds.WAND.get(), SoundSource.PLAYERS, 1f, 1f);
            player.stopRiding();
            player.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            FxPacket.send(server, FxPacket.SPARKLES, player.position().add(0, 1, 0), Vec3.ZERO, 2, 60);
            level.playSound(null, player.blockPosition(), ModSounds.WAND.get(), SoundSource.PLAYERS, 1f, 0.1f);
        }
        return true;
    }

    public static class Tile extends BlockEntity {
        public static final int SLOTS = 10;
        public final SimpleContainer items = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                Tile.this.sync();
            }
        };
        public boolean locked;
        private long lastTeleport;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.WARP_GATE.get(), pos, state);
        }

        /** На клиенте: присевший на вратах игрок открывает карту направлений. */
        void clientTick() {
            thaumictinkerer.client.TTClientHooks.warpGateTick(this);
        }

        /** Перенос по жемчужине из ячейки index (не чаще раза в тик). */
        public void teleport(Player player, int index) {
            long now = level.getGameTime();
            if (now == lastTeleport || index < 0 || index >= SLOTS
                    || !new AABB(worldPosition.above()).inflate(0.5, 1, 0.5).intersects(player.getBoundingBox())) {
                return;
            }
            ItemStack pearl = items.getItem(index);
            BlockPos target = SkyPearlItem.pos(pearl);
            if (target != null && level.dimension().equals(SkyPearlItem.dimension(pearl)) && WarpGateBlock.teleport(player, target)) {
                lastTeleport = now;
            }
        }

        void sync() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("locked", locked);
            NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            for (int i = 0; i < SLOTS; i++) {
                list.set(i, items.getItem(i));
            }
            net.minecraft.world.ContainerHelper.saveAllItems(tag, list);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            locked = tag.getBoolean("locked");
            NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            net.minecraft.world.ContainerHelper.loadAllItems(tag, list);
            for (int i = 0; i < SLOTS; i++) {
                items.setItem(i, list.get(i));
            }
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

    /** ContainerWarpGate: десять ячеек под настроенные жемчужины и кнопка «заперто». */
    public static class Menu extends AbstractContainerMenu {
        public static final int LOCK = 0;
        private final BlockPos pos;

        public Menu(int id, Inventory inventory, BlockPos pos) {
            this(id, inventory, pos, inventory.player.level().getBlockEntity(pos) instanceof Tile tile ? tile.items : new SimpleContainer(Tile.SLOTS));
        }

        public Menu(int id, Inventory inventory, BlockPos pos, SimpleContainer gate) {
            super(TTMenus.WARP_GATE.get(), id);
            this.pos = pos;
            for (int row = 0; row < 2; row++) {
                for (int column = 0; column < 5; column++) {
                    addSlot(new Slot(gate, row * 5 + column, 30 + column * 25, 27 + row * 25) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return SkyPearlItem.isAttuned(stack);
                        }

                        @Override
                        public int getMaxStackSize() {
                            return 1;
                        }
                    });
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column, 8 + column * 18, 142));
            }
        }

        public BlockPos pos() {
            return pos;
        }

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (id == LOCK && player.level().getBlockEntity(pos) instanceof Tile tile) {
                tile.locked = !tile.locked;
                tile.sync();
                return true;
            }
            return false;
        }

        @Override
        public boolean stillValid(Player player) {
            return player.distanceToSqr(Vec3.atCenterOf(pos)) < 64 && player.level().getBlockEntity(pos) instanceof Tile;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack copy = stack.copy();
            if (index < Tile.SLOTS) {
                if (!moveItemStackTo(stack, Tile.SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!SkyPearlItem.isAttuned(stack) || !moveItemStackTo(stack, 0, Tile.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return copy;
        }
    }
}
