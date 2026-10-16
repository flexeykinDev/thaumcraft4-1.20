package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.menu.DeconstructionTableMenu;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.research.ResearchHelper;

import java.util.List;

/**
 * Стол разборки (BlockTable оригинала, мета 14): за две секунды уничтожает предмет и иногда извлекает из него
 * одну точку исследования первичного аспекта. Чем больше в предмете аспектов, тем выше шанс.
 */
public class DeconstructionTableBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 8, 0, 16, 16, 16), box(0, 0, 0, 16, 4, 16), box(1, 4, 1, 15, 8, 15));

    public DeconstructionTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Table(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DECONSTRUCTION_TABLE.get(), Table::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Table table && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, table, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Table table) {
            Containers.dropContents(level, pos, table);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Есть ли у предмета аспекты: только такие можно разбирать. */
    public static boolean hasAspects(ItemStack stack) {
        return !stack.isEmpty() && !ItemAspectManager.INSTANCE.getAspects(stack).isEmpty();
    }

    /** TileDeconstructionTable оригинала. */
    public static class Table extends BlockEntity implements WorldlyContainer, MenuProvider {
        public static final int BREAK_TIME = 40;
        private static final int[] SLOTS = {0};
        private static final int[] NO_SLOTS = {};

        /** Извлечённая точка исследования, которая ждёт, пока её заберут. Пока она лежит, стол не работает. */
        @Nullable
        public Aspect aspect;
        public int breaktime;
        private ItemStack item = ItemStack.EMPTY;
        private final ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return breaktime;
            }

            @Override
            public void set(int index, int value) {
                breaktime = value;
            }

            @Override
            public int getCount() {
                return 1;
            }
        };

        public Table(BlockPos pos, BlockState state) {
            super(ModBlockEntities.DECONSTRUCTION_TABLE.get(), pos, state);
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Table table) {
            boolean changed = false;
            if (table.breaktime == 0 && table.canBreak()) {
                table.breaktime = BREAK_TIME;
                changed = true;
            }
            if (table.breaktime > 0 && table.canBreak()) {
                if (--table.breaktime == 0) {
                    table.breakItem();
                    changed = true;
                }
            } else {
                table.breaktime = 0;
            }
            if (changed) {
                table.sync();
            }
        }

        private boolean canBreak() {
            return aspect == null && hasAspects(item);
        }

        public void breakItem() {
            if (!canBreak()) {
                return;
            }
            AspectList primals = ResearchHelper.reduceToPrimals(ItemAspectManager.INSTANCE.getAspects(item));
            List<Aspect> kinds = primals.getAspects();
            if (!kinds.isEmpty() && level.random.nextInt(80) < primals.visSize()) {
                aspect = kinds.get(level.random.nextInt(kinds.size()));
            }
            item.shrink(1);
        }

        /** Игрок забирает точку исследования. */
        public void claim(ServerPlayer player) {
            if (aspect == null) {
                return;
            }
            thaumcraft.common.research.PlayerKnowledge.get(player).addPool(aspect, 1);
            thaumcraft.common.network.SyncKnowledgePacket.send(player);
            aspect = null;
            sync();
        }

        // ---------- окно и ячейка ----------

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.thaumcraft.deconstruction_table");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new DeconstructionTableMenu(id, inventory, this, data);
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
            sync();
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
            sync();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return hasAspects(stack);
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && level.getBlockEntity(worldPosition) == this
                    && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
        }

        @Override
        public void clearContent() {
            item = ItemStack.EMPTY;
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
            return true;
        }

        // ---------- сохранение ----------

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (aspect != null) {
                tag.putString("Aspect", aspect.getTag());
            }
            if (!item.isEmpty()) {
                tag.put("Item", item.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspect = Aspect.byTag(tag.getString("Aspect"));
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
    }
}
