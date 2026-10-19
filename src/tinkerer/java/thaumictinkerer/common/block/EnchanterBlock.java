package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumictinkerer.common.enchantment.EnchanterData;
import thaumictinkerer.common.registry.TTBlockEntities;
import thaumictinkerer.common.registry.TTMenus;

import java.util.ArrayList;
import java.util.List;

/**
 * Осмотический зачарователь (BlockEnchanter оригинала). В окне выбирают чары и их уровни для незачарованной вещи;
 * зачарование идёт, только если вокруг (в квадрате 9×9 на том же уровне) стоят шесть столбов из обсидиановых тотемов
 * высотой от 2 до 12 блоков, увенчанных нитором. Каждый тик столбы вытягивают из жезла в соседней ячейке единицу
 * нужной стихии, а когда наберётся всё — чары ложатся на вещь.
 */
public class EnchanterBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    public static final int START = 0;
    public static final int ADD = 100;
    public static final int EDIT = 1000;

    public EnchanterBlock(Properties properties) {
        super(properties);
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TTBlockEntities.ENCHANTER.get(), (l, p, s, tile) -> tile.serverTick());
    }

    /** TileEnchanter оригинала: ячейки вещи и жезла, выбранные чары с уровнями и счёт собранной вис. */
    public static class Tile extends BlockEntity implements Container, MenuProvider {
        private final ItemStack[] slots = {ItemStack.EMPTY, ItemStack.EMPTY};
        public final List<Integer> enchantments = new ArrayList<>();
        public final List<Integer> levels = new ArrayList<>();
        public AspectList totalAspects = new AspectList();
        public AspectList currentAspects = new AspectList();
        public boolean working;
        private final List<BlockPos> pillars = new ArrayList<>();

        public Tile(BlockPos pos, BlockState state) {
            super(TTBlockEntities.ENCHANTER.get(), pos, state);
        }

        void serverTick() {
            if (slots[0].isEmpty()) {
                if (!enchantments.isEmpty() || working) {
                    enchantments.clear();
                    levels.clear();
                    working = false;
                    updateAspectList();
                    changed();
                }
                return;
            }
            if (!working || !checkPillars()) {
                return;
            }
            List<Aspect> needed = new ArrayList<>();
            ItemStack wand = slots[1];
            boolean usableWand = wand.getItem() instanceof WandItem && !WandItem.isStaff(wand);
            for (Aspect aspect : Aspect.primals()) {
                if (currentAspects.getAmount(aspect) < totalAspects.getAmount(aspect) && usableWand && WandItem.getVis(wand, aspect) >= 100) {
                    needed.add(aspect);
                }
            }
            boolean done = true;
            for (Aspect aspect : Aspect.primals()) {
                done &= currentAspects.getAmount(aspect) >= totalAspects.getAmount(aspect);
            }
            if (done) {
                for (int i = 0; i < enchantments.size(); i++) {
                    slots[0].enchant(EnchanterData.get(enchantments.get(i)).enchantment().get(), levels.get(i));
                }
                working = false;
                enchantments.clear();
                levels.clear();
                updateAspectList();
                level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
                changed();
                return;
            }
            if (needed.isEmpty()) {
                return;
            }
            int pick = level.random.nextInt(needed.size());
            Aspect aspect = needed.get(pick);
            if (WandItem.consumeAllCentiVis(wand, null, new AspectList().add(aspect, 100), true)) {
                currentAspects.add(aspect, 1);
                if (level.random.nextBoolean()) {
                    BlockPos pillar = pillars.get(level.random.nextInt(pillars.size()));
                    FxPacket.send((ServerLevel) level, FxPacket.RUNES, Vec3.atLowerCornerOf(pillar), Vec3.ZERO, 1, 0);
                    FxPacket.send((ServerLevel) level, FxPacket.RUNES, Vec3.atLowerCornerOf(worldPosition), Vec3.ZERO, 1, 0);
                    if (level.random.nextInt(5) == 0) {
                        level.playSound(null, pillar, ModSounds.BRAIN.get(), SoundSource.BLOCKS, 0.5f, 1f);
                    }
                }
                changed();
            }
        }

        /** Шесть столбов в квадрате 9×9; каждый — от 2 до 12 обсидиановых тотемов и нитор на вершине. */
        public boolean checkPillars() {
            for (BlockPos pillar : pillars) {
                if (pillarTop(pillar.getX(), pillar.getZ()) == NO_PILLAR) {
                    pillars.clear();
                    break;
                }
            }
            if (pillars.isEmpty()) {
                for (int x = -4; x <= 4 && pillars.size() < 6; x++) {
                    for (int z = -4; z <= 4 && pillars.size() < 6; z++) {
                        int top = pillarTop(worldPosition.getX() + x, worldPosition.getZ() + z);
                        if (top != NO_PILLAR) {
                            pillars.add(new BlockPos(worldPosition.getX() + x, top, worldPosition.getZ() + z));
                        }
                    }
                }
                if (pillars.size() < 6) {
                    pillars.clear();
                    return false;
                }
            }
            return true;
        }

        /** Высота бывает отрицательной (мир ниже нуля), поэтому «столба нет» — отдельное значение. */
        private static final int NO_PILLAR = Integer.MIN_VALUE;

        private int pillarTop(int x, int z) {
            int height = 0;
            for (int y = worldPosition.getY(); y < level.getMaxBuildHeight(); y++) {
                BlockState state = level.getBlockState(new BlockPos(x, y, z));
                if (state.is(ModBlocks.OBSIDIAN_TOTEM.get())) {
                    height++;
                } else {
                    return state.is(ModBlocks.NITOR.get()) && height >= 2 && height < 13 ? y : NO_PILLAR;
                }
            }
            return NO_PILLAR;
        }

        public void updateAspectList() {
            totalAspects = new AspectList();
            currentAspects = working ? currentAspects : new AspectList();
            for (int i = 0; i < enchantments.size(); i++) {
                totalAspects.add(EnchanterData.get(enchantments.get(i)).cost(levels.get(i)));
            }
        }

        /** Действия из окна (PacketEnchanterAddEnchant и PacketEnchanterStartWorking оригинала). */
        public void click(Player player, int id) {
            if (working || slots[0].isEmpty()) {
                return;
            }
            if (id == START) {
                if (!enchantments.isEmpty()) {
                    working = true;
                    currentAspects = new AspectList();
                }
            } else if (id >= EDIT) {
                int index = (id - EDIT) / 3;
                int action = (id - EDIT) % 3;
                if (index >= enchantments.size()) {
                    return;
                }
                int max = EnchanterData.get(enchantments.get(index)).enchantment().get().getMaxLevel();
                int levelNow = levels.get(index);
                if (action == 0 || action == 1 && levelNow == 1) {
                    enchantments.remove(index);
                    levels.remove(index);
                } else {
                    levels.set(index, Math.max(1, Math.min(max, levelNow + (action == 1 ? -1 : 1))));
                }
            } else if (id >= ADD && id - ADD < EnchanterData.all().size()) {
                int index = id - ADD;
                EnchanterData.Entry entry = EnchanterData.get(index);
                Enchantment enchantment = entry.enchantment().get();
                if (enchantments.size() < 8 && EnchanterData.isUnlocked(PlayerKnowledge.get(player), entry)
                        && EnchanterData.canApply(slots[0], enchantment, enchantments)) {
                    enchantments.add(index);
                    levels.add(1);
                }
            }
            updateAspectList();
            changed();
        }

        void changed() {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            for (int i = 0; i < 2; i++) {
                if (!slots[i].isEmpty()) {
                    tag.put("Slot" + i, slots[i].save(new CompoundTag()));
                }
            }
            tag.putIntArray("Enchants", enchantments);
            tag.putIntArray("Levels", levels);
            tag.put("Total", totalAspects.save(new CompoundTag()));
            tag.put("Current", currentAspects.save(new CompoundTag()));
            tag.putBoolean("Working", working);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            for (int i = 0; i < 2; i++) {
                slots[i] = tag.contains("Slot" + i) ? ItemStack.of(tag.getCompound("Slot" + i)) : ItemStack.EMPTY;
            }
            enchantments.clear();
            for (int value : tag.getIntArray("Enchants")) {
                enchantments.add(value);
            }
            levels.clear();
            for (int value : tag.getIntArray("Levels")) {
                levels.add(value);
            }
            totalAspects = AspectList.load(tag.getCompound("Total"));
            currentAspects = AspectList.load(tag.getCompound("Current"));
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

        public ItemStack tool() {
            return slots[0];
        }

        public ItemStack wand() {
            return slots[1];
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return slots[0].isEmpty() && slots[1].isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slots[slot];
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = slots[slot].split(amount);
            if (slot == 0) {
                working = false;
                enchantments.clear();
                levels.clear();
                updateAspectList();
            }
            changed();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = slots[slot];
            slots[slot] = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            slots[slot] = stack;
            if (slot == 0 && !working) {
                enchantments.clear();
                levels.clear();
                updateAspectList();
            }
            changed();
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
            slots[0] = ItemStack.EMPTY;
            slots[1] = ItemStack.EMPTY;
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

    /** ContainerEnchanter оригинала: ячейка вещи (6,6), ячейка жезла (6,31) и инвентарь. */
    public static class Menu extends AbstractContainerMenu {
        private final Container container;
        @Nullable
        public final Tile tile;

        public Menu(int id, Inventory inventory, BlockPos pos) {
            this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof Tile tile ? tile : null);
        }

        public Menu(int id, Inventory inventory, @Nullable Tile tile) {
            super(TTMenus.ENCHANTER.get(), id);
            this.tile = tile;
            this.container = tile != null ? tile : new SimpleContainer(2);
            addSlot(new Slot(container, 0, 6, 6) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.isEnchantable() || stack.getEnchantmentValue() > 0 && !stack.isEnchanted();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
            addSlot(new Slot(container, 1, 6, 31) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    // железные и медные набалдашники слишком грубы для зачарования (SlotWand оригинала)
                    return stack.getItem() instanceof WandItem && !WandItem.isStaff(stack)
                            && WandItem.getCap(stack).getBaseCostModifier() <= 1f;
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

        @Override
        public boolean clickMenuButton(Player player, int id) {
            if (tile != null) {
                tile.click(player, id);
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
            if (index < 2) {
                if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 2, false)) {
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
