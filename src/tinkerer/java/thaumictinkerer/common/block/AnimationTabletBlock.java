package thaumictinkerer.common.block;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModSounds;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTMenus;

import java.util.List;
import java.util.UUID;

/**
 * Анимационная табличка (BlockAnimationTablet оригинала): машет положенной в неё вещью, как игрок, по блоку или
 * существу перед собой — левой кнопкой (бьёт, копает) или правой (использует). Работает сама или по сигналу
 * красного камня. Жезлом табличка поворачивается, щелчком открывается окно.
 */
public class AnimationTabletBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("5fbc8a62-2f1e-4b0e-9a58-6a2d8b1c4e11"), "[ThaumicTinkerer]");

    public AnimationTabletBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.getItemInHand(hand).getItem() instanceof WandItem) {
            if (!state.getValue(POWERED)) {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.6f, 1f);
            } else {
                player.sendSystemMessage(Component.translatable("ttmisc.animationTablet.notRotatable"));
            }
        } else if (level.getBlockEntity(pos) instanceof Tile tile && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, tile, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
        boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
            if (powered && level.getBlockEntity(pos) instanceof Tile tile && tile.redstone && tile.swing == 0) {
                tile.swingStep = 3;
                tile.swing = 1;
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Tile tile) {
            tile.stopBreaking();
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
        return createTickerHelper(type, TTBlockEntities.ANIMATION_TABLET.get(), (l, p, s, tile) -> tile.tick());
    }

    /** TileAnimationTablet оригинала: замах до 45°, удар на пике. */
    public static class Tile extends BlockEntity implements Container, MenuProvider {
        private static final int MAX_SWING = 45;
        private ItemStack stack = ItemStack.EMPTY;
        public boolean leftClick = true;
        public boolean redstone;
        public int swing;
        int swingStep;
        private boolean breaking;
        private float progress;
        private int shownProgress = -1;

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.ANIMATION_TABLET.get(), pos, state);
        }

        public BlockPos target() {
            return worldPosition.relative(getBlockState().getValue(FACING));
        }

        private List<Entity> entities() {
            return level.getEntitiesOfClass(Entity.class, new AABB(target()), Entity::isAlive);
        }

        void tick() {
            if (!stack.isEmpty()) {
                if (swing >= MAX_SWING && !level.isClientSide) {
                    hit();
                }
                swingStep = swing <= 0 ? 0 : swing >= MAX_SWING ? -3 : swingStep;
                swing = Math.max(0, swing + swingStep);
            } else {
                swing = 0;
                swingStep = 0;
                if (breaking) {
                    stopBreaking();
                }
            }
            if (level.isClientSide) {
                return;
            }
            boolean found = !level.isEmptyBlock(target()) || !entities().isEmpty();
            if (!found) {
                stopBreaking();
            }
            if (found && breaking) {
                continueBreaking();
            }
            if ((!redstone || breaking) && found && swing == 0 && !stack.isEmpty()) {
                swingStep = 3;
                swing = 1;
                sync();
            }
        }

        private FakePlayer player() {
            FakePlayer player = FakePlayerFactory.get((ServerLevel) level, PROFILE);
            Direction facing = getBlockState().getValue(FACING);
            player.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
            player.setYRot(facing.toYRot());
            player.setXRot(0);
            // «рука» стоит на табличке: иначе копание в воздухе впятеро медленнее
            player.setOnGround(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return player;
        }

        /** swingHit оригинала. */
        private void hit() {
            FakePlayer player = player();
            List<Entity> entities = entities();
            Entity entity = entities.isEmpty() ? null : entities.get(level.random.nextInt(entities.size()));
            BlockPos pos = target();
            if (leftClick) {
                if (entity != null) {
                    player.attack(entity);
                } else if (!breaking && !level.isEmptyBlock(pos) && level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                    breaking = true;
                    progress = 0;
                    // копание само возвращает вещь в ячейку — второй раз забирать её нельзя
                    takeBack(player);
                    continueBreaking();
                    return;
                }
            } else {
                InteractionResult result = InteractionResult.PASS;
                if (entity != null) {
                    result = player.interactOn(entity, InteractionHand.MAIN_HAND);
                }
                Direction face = getBlockState().getValue(FACING).getOpposite();
                if (!result.consumesAction() && level.isEmptyBlock(pos)) {
                    pos = pos.below();
                    face = Direction.UP;
                }
                if (!result.consumesAction()) {
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
                    result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
                }
                if (!result.consumesAction()) {
                    player.gameMode.useItem(player, level, stack, InteractionHand.MAIN_HAND);
                }
            }
            takeBack(player);
        }

        /** Что стало с вещью у «руки» — возвращается в ячейку; лишнее из её карманов выпадает рядом. */
        private void takeBack(FakePlayer player) {
            stack = player.getMainHandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack extra = player.getInventory().removeItemNoUpdate(i);
                if (!extra.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, extra);
                }
            }
            sync();
        }

        private void continueBreaking() {
            BlockPos pos = target();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                stopBreaking();
                return;
            }
            FakePlayer player = player();
            progress += state.getDestroyProgress(player, level, pos);
            int shown = (int) (progress * 10);
            if (shown != shownProgress) {
                level.destroyBlockProgress(player.getId(), pos, Math.min(9, shown));
                shownProgress = shown;
            }
            if (progress >= 1) {
                player.gameMode.destroyBlock(pos);
                stopBreaking();
            }
            takeBack(player);
        }

        void stopBreaking() {
            if (breaking && level instanceof ServerLevel server) {
                server.destroyBlockProgress(FakePlayerFactory.get(server, PROFILE).getId(), target(), -1);
            }
            breaking = false;
            progress = 0;
            shownProgress = -1;
        }

        void sync() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        public void click(int id) {
            if (id == 0) {
                redstone = !redstone;
            } else if (id == 1 || id == 2) {
                leftClick = id == 1;
            }
            sync();
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!stack.isEmpty()) {
                tag.put("Item", stack.save(new CompoundTag()));
            }
            tag.putBoolean("LeftClick", leftClick);
            tag.putBoolean("Redstone", redstone);
            tag.putInt("Swing", swing);
            tag.putInt("SwingStep", swingStep);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            stack = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
            leftClick = !tag.contains("LeftClick") || tag.getBoolean("LeftClick");
            redstone = tag.getBoolean("Redstone");
            swing = tag.getInt("Swing");
            swingStep = tag.getInt("SwingStep");
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
        public int getMaxStackSize() {
            return 1;
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
    }

    /** ContainerAnimationTablet оригинала: ячейка (80, 15); кнопки 0 — красный камень, 1 — левая, 2 — правая. */
    public static class Menu extends AbstractContainerMenu {
        private final Container container;
        @Nullable
        public final Tile tile;

        public Menu(int id, Inventory inventory, BlockPos pos) {
            this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof Tile tile ? tile : null);
        }

        public Menu(int id, Inventory inventory, @Nullable Tile tile) {
            super(TTMenus.ANIMATION_TABLET.get(), id);
            this.tile = tile;
            this.container = tile != null ? tile : new SimpleContainer(1);
            addSlot(new Slot(container, 0, 80, 15) {
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

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (tile != null) {
                tile.click(id);
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
