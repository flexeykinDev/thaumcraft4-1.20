package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blockentity.ResearchTableBlockEntity;
import thaumcraft.common.items.ResearchNotesItem;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.registry.ModMenus;

/** Окно стола исследований (ContainerResearchTable оригинала): ячейки письменных принадлежностей и заметки. */
public class ResearchTableMenu extends AbstractContainerMenu {
    private final ResearchTableBlockEntity table;

    public ResearchTableMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (ResearchTableBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public ResearchTableMenu(int id, Inventory inventory, ResearchTableBlockEntity table) {
        super(ModMenus.RESEARCH_TABLE.get(), id);
        this.table = table;
        Container container = new TableContainer();
        addSlot(new Slot(container, ResearchTableBlockEntity.TOOLS_SLOT, 14, 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ScribingToolsItem;
            }
        });
        addSlot(new Slot(container, ResearchTableBlockEntity.NOTES_SLOT, 70, 10) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ResearchNotesItem;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 48 + column * 18, 175 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 48 + column * 18, 233));
        }
    }

    public ResearchTableBlockEntity getTable() {
        return table;
    }

    @Override
    public boolean stillValid(Player player) {
        return table.stillValid(player);
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

    /** Ячейки самого стола: вещи остаются в нём, когда окно закрыто. */
    private class TableContainer implements Container {
        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return table.getItems().stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return table.getItems().get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(table.getItems(), slot, amount);
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(table.getItems(), slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            table.getItems().set(slot, stack);
            setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            table.contentsChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return table.stillValid(player);
        }

        @Override
        public void clearContent() {
            table.getItems().clear();
        }
    }
}
