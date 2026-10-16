package thaumcraft.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.JarBlockItem;
import thaumcraft.common.items.armor.HoverHarnessItem;
import thaumcraft.common.registry.ModMenus;

/** Окно летательной упряжи (ContainerHoverHarness): одна ячейка для банки с эссенцией. */
public class HoverHarnessMenu extends AbstractContainerMenu {
    private final Player player;
    /** Ячейка игрока, в которой лежит упряжь (её нельзя трогать, пока окно открыто). */
    public final int harnessSlot;
    private final SimpleContainer jar = new SimpleContainer(1);

    public HoverHarnessMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readInt());
    }

    public HoverHarnessMenu(int id, Inventory inventory, int harnessSlot) {
        super(ModMenus.HOVER_HARNESS.get(), id);
        this.player = inventory.player;
        this.harnessSlot = harnessSlot;
        jar.setItem(0, HoverHarnessItem.getJar(harness()));
        addSlot(new Slot(jar, 0, 80, 32) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof JarBlockItem;
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
            final int index = column;
            addSlot(new Slot(inventory, column, 8 + column * 18, 142) {
                @Override
                public boolean mayPickup(Player player) {
                    return index != harnessSlot;
                }
            });
        }
    }

    private ItemStack harness() {
        return harnessSlot == 40 ? player.getOffhandItem() : player.getInventory().getItem(harnessSlot);
    }

    @Override
    public boolean stillValid(Player player) {
        return harness().getItem() instanceof HoverHarnessItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index == harnessSlot + 28) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!(stack.getItem() instanceof JarBlockItem) || !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /** Банка остаётся в упряжи. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && harness().getItem() instanceof HoverHarnessItem) {
            HoverHarnessItem.setJar(harness(), jar.getItem(0));
        }
    }
}
