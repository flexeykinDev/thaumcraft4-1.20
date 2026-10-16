package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blockentity.AlchemicalFurnaceBlockEntity;
import thaumcraft.common.registry.ModMenus;

/** Окно алхимической печи (ContainerAlchemyFurnace оригинала): сырьё сверху, топливо снизу. */
public class AlchemicalFurnaceMenu extends AbstractContainerMenu {
    private final Container furnace;
    private final ContainerData data;

    public AlchemicalFurnaceMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (AlchemicalFurnaceBlockEntity) inventory.player.level().getBlockEntity(pos), new SimpleContainerData(5));
    }

    public AlchemicalFurnaceMenu(int id, Inventory inventory, AlchemicalFurnaceBlockEntity furnace, ContainerData data) {
        super(ModMenus.ALCHEMICAL_FURNACE.get(), id);
        this.furnace = furnace;
        this.data = data;
        addSlot(new Slot(furnace, AlchemicalFurnaceBlockEntity.INPUT_SLOT, 80, 8) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return furnace.canPlaceItem(AlchemicalFurnaceBlockEntity.INPUT_SLOT, stack);
            }
        });
        addSlot(new Slot(furnace, AlchemicalFurnaceBlockEntity.FUEL_SLOT, 80, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return furnace.canPlaceItem(AlchemicalFurnaceBlockEntity.FUEL_SLOT, stack);
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
        addDataSlots(data);
    }

    public boolean isBurning() {
        return data.get(0) > 0;
    }

    /** Остаток топлива в пикселях полоски высотой size. */
    public int burnScaled(int size) {
        int total = data.get(1) == 0 ? 200 : data.get(1);
        return data.get(0) * size / total;
    }

    public int cookScaled(int size) {
        return data.get(2) * size / Math.max(1, data.get(3));
    }

    public int contentsScaled(int size) {
        return data.get(4) * size / AlchemicalFurnaceBlockEntity.MAX_VIS;
    }

    @Override
    public boolean stillValid(Player player) {
        return furnace.stillValid(player);
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
}
