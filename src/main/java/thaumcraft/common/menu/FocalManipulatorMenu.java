package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.FocalManipulatorBlock;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.registry.ModMenus;
import thaumcraft.common.registry.ModSounds;

/** Окно фокального манипулятора (ContainerFocalManipulator оригинала): одна ячейка под фокус и инвентарь игрока. */
public class FocalManipulatorMenu extends AbstractContainerMenu {
    private final FocalManipulatorBlock.Manipulator table;

    public FocalManipulatorMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (FocalManipulatorBlock.Manipulator) inventory.player.level().getBlockEntity(pos));
    }

    public FocalManipulatorMenu(int id, Inventory inventory, FocalManipulatorBlock.Manipulator table) {
        super(ModMenus.FOCAL_MANIPULATOR.get(), id);
        this.table = table;
        addSlot(new Slot(table, 0, 88, 60) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof FocusItem;
            }

            /** Пока улучшение не закончено, фокус из манипулятора не вынуть. */
            @Override
            public boolean mayPickup(Player player) {
                return table.getSize() <= 0;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        }
    }

    public FocalManipulatorBlock.Manipulator getTable() {
        return table;
    }

    /** Кнопка «начать улучшение»: id — номер выбранного улучшения. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && !table.startCraft(id, player)) {
            player.level().playSound(null, table.getBlockPos(), ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 0.33f, 1f);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return table.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack inSlot = slot.getItem();
        ItemStack copy = inSlot.copy();
        if (index == 0) {
            if (!moveItemStackTo(inSlot, 1, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else if (inSlot.getItem() instanceof FocusItem) {
            if (!moveItemStackTo(inSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 28) {
            if (!moveItemStackTo(inSlot, 28, 37, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(inSlot, 1, 28, false)) {
            return ItemStack.EMPTY;
        }
        if (inSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return inSlot.getCount() == copy.getCount() ? ItemStack.EMPTY : copy;
    }
}
