package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.BathSaltsItem;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

/**
 * Магическая купальня (BlockStoneDevice оригинала, мета 12): раз в две секунды выливает над собой блок жидкости из
 * своего бака, а затем понемногу заполняет ею площадку 5×5. В режиме смешивания берёт воду и соли для ванн и
 * выливает очищающую жидкость; иначе — просто то, что в баке. Сигнал красного камня её останавливает.
 */
public class ArcaneSpaBlock extends BaseEntityBlock {
    public ArcaneSpaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Spa(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ARCANE_SPA.get(), Spa::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof Spa spa)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // ведром с жидкостью купальню заправляют, всё остальное открывает окно
            if (FluidUtil.getFluidContained(player.getItemInHand(hand)).isEmpty() || !FluidUtil.interactWithFluidHandler(player, hand, spa.tank)) {
                NetworkHooks.openScreen(serverPlayer, spa, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Spa spa) {
            Containers.dropContents(level, pos, spa);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** TileSpa оригинала. */
    public static class Spa extends BlockEntity implements WorldlyContainer, MenuProvider {
        public static final int CAPACITY = 5000;
        private static final int[] SLOTS = {0};
        private static final int[] NO_SLOTS = {};

        private ItemStack salts = ItemStack.EMPTY;
        private boolean mix = true;
        private int counter;
        public final FluidTank tank = new FluidTank(CAPACITY) {
            @Override
            protected void onContentsChanged() {
                sync();
            }
        };
        private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

        public Spa(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ARCANE_SPA.get(), pos, state);
        }

        public boolean getMix() {
            return mix;
        }

        public void toggleMix() {
            mix = !mix;
            sync();
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        // ---------- работа ----------

        public static void serverTick(Level level, BlockPos pos, BlockState state, Spa spa) {
            if (spa.counter++ % 40 == 0 && !level.hasNeighborSignal(pos) && spa.hasIngredients()) {
                spa.pour();
            }
        }

        /** Что купальня собирается выливать; воздух, если жидкость из бака нельзя поставить в мир. */
        private BlockState product() {
            return mix ? ModBlocks.PURIFYING_FLUID.get().defaultBlockState()
                    : tank.getFluid().getFluid().defaultFluidState().createLegacyBlock();
        }

        private boolean hasIngredients() {
            if (mix) {
                return tank.getFluid().getFluid() == Fluids.WATER && tank.getFluidAmount() >= 1000 && salts.getItem() instanceof BathSaltsItem;
            }
            return tank.getFluidAmount() >= 1000 && !product().isAir();
        }

        private void consumeIngredients() {
            if (mix) {
                salts.shrink(1);
            }
            tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        }

        private void pour() {
            BlockState product = product();
            BlockPos above = worldPosition.above();
            if (isSource(above, product)) {
                // над купальней уже налито: разливаем дальше по площадке, от уже налитого
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos target = above.offset(dx, 0, dz);
                        if (isValidLocation(target, true, product)) {
                            consumeIngredients();
                            level.setBlockAndUpdate(target, product);
                            return;
                        }
                    }
                }
            } else if (isValidLocation(above, false, product)) {
                consumeIngredients();
                level.setBlockAndUpdate(above, product);
            }
        }

        private boolean isSource(BlockPos pos, BlockState product) {
            BlockState state = level.getBlockState(pos);
            return state.is(product.getBlock()) && state.getFluidState().isSource();
        }

        private boolean isValidLocation(BlockPos pos, boolean mustBeAdjacent, BlockState product) {
            // вода в Нижнем мире испаряется
            if (product.getFluidState().is(FluidTags.WATER) && level.dimensionType().ultraWarm()) {
                return false;
            }
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) || !level.getBlockState(pos).canBeReplaced()
                    || isSource(pos, product)) {
                return false;
            }
            if (!mustBeAdjacent) {
                return true;
            }
            for (Direction side : Direction.values()) {
                if (isSource(pos.relative(side), product)) {
                    return true;
                }
            }
            return false;
        }

        // ---------- бак ----------

        /** Трубы подключаются откуда угодно, кроме верха: сверху купальня выливает. */
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeCapabilities.FLUID_HANDLER && side != Direction.UP) {
                return fluidHandler.cast();
            }
            return super.getCapability(capability, side);
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            fluidHandler.invalidate();
        }

        // ---------- окно и ячейка ----------

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.thaumcraft.arcane_spa");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new SpaMenu(id, inventory, this);
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return salts.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return salts;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = salts.split(amount);
            setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = salts;
            salts = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            salts = stack;
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof BathSaltsItem;
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && level.getBlockEntity(worldPosition) == this
                    && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void clearContent() {
            salts = ItemStack.EMPTY;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side != Direction.UP ? SLOTS : NO_SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return side != Direction.UP && canPlaceItem(slot, stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return side != Direction.UP;
        }

        // ---------- сохранение ----------

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putBoolean("Mix", mix);
            tag.put("Tank", tank.writeToNBT(new CompoundTag()));
            if (!salts.isEmpty()) {
                tag.put("Salts", salts.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            mix = !tag.contains("Mix") || tag.getBoolean("Mix");
            tank.readFromNBT(tag.getCompound("Tank"));
            salts = tag.contains("Salts") ? ItemStack.of(tag.getCompound("Salts")) : ItemStack.EMPTY;
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
