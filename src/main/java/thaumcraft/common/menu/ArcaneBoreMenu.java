package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import thaumcraft.common.blocks.ArcaneBoreBlock;
import thaumcraft.common.items.wands.foci.ExcavationFocus;
import thaumcraft.common.registry.ModMenus;

/** Окно чародейского бура (ContainerArcaneBore): ячейка фокуса раскопок и ячейка кирки. */
public class ArcaneBoreMenu extends AbstractContainerMenu {
    public final ArcaneBoreBlock.Bore bore;

    public ArcaneBoreMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, (ArcaneBoreBlock.Bore) inventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public ArcaneBoreMenu(int id, Inventory inventory, ArcaneBoreBlock.Bore bore) {
        super(ModMenus.ARCANE_BORE.get(), id);
        this.bore = bore;
        addSlot(new Slot(bore, 0, 26, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ExcavationFocus;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(bore, 1, 74, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof PickaxeItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 59 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 117));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return bore.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
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
        return copy;
    }
}
