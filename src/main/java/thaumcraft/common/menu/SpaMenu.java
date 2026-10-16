package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.ArcaneSpaBlock;
import thaumcraft.common.registry.ModMenus;

/** Окно магической купальни (ContainerSpa оригинала): одна ячейка для солей и переключатель смешивания. */
public class SpaMenu extends AbstractContainerMenu {
    public final ArcaneSpaBlock.Spa spa;

    public SpaMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (ArcaneSpaBlock.Spa) inventory.player.level().getBlockEntity(pos));
    }

    public SpaMenu(int id, Inventory inventory, ArcaneSpaBlock.Spa spa) {
        super(ModMenus.SPA.get(), id);
        this.spa = spa;
        addSlot(new Slot(spa, 0, 65, 31) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return spa.canPlaceItem(0, stack);
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

    /** Кнопка 1 — переключатель «смешивать с солями / лить как есть». */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 1) {
            spa.toggleMix();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return spa.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!spa.canPlaceItem(0, stack) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
