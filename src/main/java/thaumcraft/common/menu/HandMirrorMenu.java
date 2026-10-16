package thaumcraft.common.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.HandMirrorItem;
import thaumcraft.common.registry.ModMenus;

/** Окно ручного зеркала (ContainerHandMirror): вещь, положенная в ячейку, уходит в привязанное зеркало. */
public class HandMirrorMenu extends AbstractContainerMenu {
    private final Player player;
    private final SimpleContainer input = new SimpleContainer(1);

    public HandMirrorMenu(int id, Inventory inventory) {
        super(ModMenus.HAND_MIRROR.get(), id);
        this.player = inventory.player;
        input.addListener(container -> send());
        addSlot(new Slot(input, 0, 80, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(stack.getItem() instanceof HandMirrorItem);
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

    private ItemStack mirror() {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof HandMirrorItem ? held : player.getOffhandItem();
    }

    private void send() {
        ItemStack stack = input.getItem(0);
        if (!stack.isEmpty() && player instanceof ServerPlayer server && HandMirrorItem.transport(mirror(), stack, server)) {
            input.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return mirror().getItem() instanceof HandMirrorItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || slot.getItem().getItem() instanceof HandMirrorItem) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, input);
        }
    }
}
