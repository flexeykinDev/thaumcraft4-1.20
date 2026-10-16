package thaumcraft.common.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.FocusPouchItem;
import thaumcraft.common.registry.ModMenus;

/**
 * Окно сумки для фокусов (ContainerFocusPouch оригинала): 18 ячеек только под фокусы.
 * Сама сумка лежит в выбранной ячейке панели, и пока окно открыто, её нельзя сдвинуть.
 */
public class FocusPouchMenu extends AbstractContainerMenu {
    private final SimpleContainer input = new SimpleContainer(FocusPouchItem.SIZE);
    private final Player player;
    private final int pouchSlot;
    /** Номер ячейки меню, в которой лежит сама сумка. */
    private final int lockedSlot;

    public FocusPouchMenu(int id, Inventory inventory, int pouchSlot) {
        super(ModMenus.FOCUS_POUCH.get(), id);
        this.player = inventory.player;
        this.pouchSlot = pouchSlot;
        this.lockedSlot = FocusPouchItem.SIZE + 27 + pouchSlot;
        for (int i = 0; i < FocusPouchItem.SIZE; i++) {
            addSlot(new Slot(input, i, 37 + i % 6 * 18, 51 + i / 6 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof FocusItem;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 209));
        }
        if (!player.level().isClientSide) {
            var stored = FocusPouchItem.getInventory(pouch());
            for (int i = 0; i < stored.size(); i++) {
                input.setItem(i, stored.get(i));
            }
        }
        // содержимое пишется в сумку сразу при каждом изменении, чтобы ничего не потерялось
        input.addListener(container -> save());
    }

    public int getPouchSlot() {
        return pouchSlot;
    }

    private ItemStack pouch() {
        return player.getInventory().getItem(pouchSlot);
    }

    private void save() {
        if (!player.level().isClientSide && pouch().getItem() instanceof FocusPouchItem) {
            java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
            for (int i = 0; i < input.getContainerSize(); i++) {
                stacks.add(input.getItem(i));
            }
            FocusPouchItem.setInventory(pouch(), stacks);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return pouch().getItem() instanceof FocusPouchItem;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        // сумку нельзя ни взять, ни подменить цифровой клавишей
        if (slot == lockedSlot || type == ClickType.SWAP && button == pouchSlot) {
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == lockedSlot) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack copy = inSlot.copy();
        if (!(inSlot.getItem() instanceof FocusItem)) {
            return ItemStack.EMPTY;
        }
        if (index < FocusPouchItem.SIZE) {
            if (!moveItemStackTo(inSlot, FocusPouchItem.SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 0, FocusPouchItem.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (inSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        save();
    }
}
