package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.wands.WandItem;
import thaumictinkerer.common.item.SoulMouldItem;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTMenus;

import java.util.function.Predicate;

/**
 * Кинетический магнит и магнит для существ (BlockMagnet оригинала). Под сигналом красного камня тянет к себе или
 * отталкивает (переключается щелчком) выпавшие предметы — или существ — в радиусе половины силы сигнала. У магнита
 * для существ жезлом открывается окно: литейная форма души ограничивает вид, переключатель — взрослых или детёнышей.
 */
public class MagnetBlock extends BaseEntityBlock {
    public static final BooleanProperty PULL = BooleanProperty.create("pull");
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 2, 15);
    private final boolean mobs;

    public MagnetBlock(Properties properties, boolean mobs) {
        super(properties);
        this.mobs = mobs;
        registerDefaultState(stateDefinition.any().setValue(PULL, true));
    }

    public boolean isMobMagnet() {
        return mobs;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PULL);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof WandItem) {
            if (mobs && !level.isClientSide && level.getBlockEntity(pos) instanceof Tile tile && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, tile, pos);
                level.playSound(null, pos, thaumcraft.common.registry.ModSounds.KEY.get(), SoundSource.BLOCKS, 1f, 0.5f);
            }
            return mobs ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.cycle(PULL), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1f, 0.5f);
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TTBlockEntities.MAGNET.get(), (l, p, s, tile) -> tile.tick());
    }

    /** TileMagnet и TileMobMagnet оригинала. */
    public static class Tile extends BlockEntity implements Container, MenuProvider {
        private ItemStack mould = ItemStack.EMPTY;
        public boolean adult = true;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.MAGNET.get(), pos, state);
        }

        public int power() {
            return level == null ? 15 : level.getBestNeighborSignal(worldPosition);
        }

        private Predicate<Entity> selector() {
            if (!((MagnetBlock) getBlockState().getBlock()).mobs) {
                return entity -> entity instanceof ItemEntity;
            }
            EntityType<?> pattern = SoulMouldItem.pattern(mould);
            return entity -> {
                if (!(entity instanceof LivingEntity) || entity instanceof Player) {
                    return false;
                }
                if (pattern != null) {
                    return entity.getType() == pattern;
                }
                return !(entity instanceof AgeableMob ageable) || adult != ageable.isBaby();
            };
        }

        void tick() {
            int power = power();
            if (power <= 0) {
                return;
            }
            boolean pull = getBlockState().getValue(PULL);
            Vec3 centre = Vec3.atCenterOf(worldPosition);
            double range = power / 2;
            AABB box = new AABB(centre.x - range, worldPosition.getY(), centre.z - range, centre.x + range, centre.y + range, centre.z + range);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box, selector())) {
                Vec3 offset = centre.subtract(entity.position());
                if (!pull || offset.lengthSqr() > 1) {
                    entity.setDeltaMovement(offset.normalize().scale(pull ? 0.25 : -0.25));
                    if (level.isClientSide && level.random.nextInt(4) == 0) {
                        thaumcraft.client.fx.Fx.sparkle(entity.getX(), entity.getY(), entity.getZ(), 1.5f, pull ? 2 : 4, 0f);
                    }
                }
            }
        }

        public ItemStack mould() {
            return mould;
        }

        /** Кнопка окна: 0 — взрослые, 1 — детёныши. */
        public void setAdult(boolean adult) {
            this.adult = adult;
            sync();
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
            if (!mould.isEmpty()) {
                tag.put("Mould", mould.save(new CompoundTag()));
            }
            tag.putBoolean("Adult", adult);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            mould = tag.contains("Mould") ? ItemStack.of(tag.getCompound("Mould")) : ItemStack.EMPTY;
            adult = !tag.contains("Adult") || tag.getBoolean("Adult");
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
            return mould.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return mould;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = mould.split(amount);
            sync();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = mould;
            mould = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            mould = stack;
            sync();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            mould = ItemStack.EMPTY;
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

    /** ContainerMobMagnet оригинала: ячейка формы души (53, 37). */
    public static class Menu extends AbstractContainerMenu {
        private final Container container;
        @Nullable
        public final Tile tile;

        public Menu(int id, Inventory inventory, BlockPos pos) {
            this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof Tile tile ? tile : null);
        }

        public Menu(int id, Inventory inventory, @Nullable Tile tile) {
            super(TTMenus.MOB_MAGNET.get(), id);
            this.tile = tile;
            this.container = tile != null ? tile : new SimpleContainer(1);
            addSlot(new Slot(container, 0, 53, 37) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof SoulMouldItem;
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
            if (tile != null && (id == 0 || id == 1)) {
                tile.setAdult(id == 0);
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
