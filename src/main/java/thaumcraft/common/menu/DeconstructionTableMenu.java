package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.DeconstructionTableBlock;
import thaumcraft.common.registry.ModMenus;

/** Окно стола разборки (ContainerDeconstructionTable оригинала): ячейка для предмета и добытая точка исследования. */
public class DeconstructionTableMenu extends AbstractContainerMenu {
    public final DeconstructionTableBlock.Table table;
    private final ContainerData data;

    public DeconstructionTableMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (DeconstructionTableBlock.Table) inventory.player.level().getBlockEntity(pos), new SimpleContainerData(1));
    }

    public DeconstructionTableMenu(int id, Inventory inventory, DeconstructionTableBlock.Table table, ContainerData data) {
        super(ModMenus.DECONSTRUCTION_TABLE.get(), id);
        this.table = table;
        this.data = data;
        addSlot(new Slot(table, 0, 64, 16) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DeconstructionTableBlock.hasAspects(stack);
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

    /** Сколько пикселей полоски высотой size осталось до конца разборки. */
    public int breakScaled(int size) {
        return data.get(0) * size / DeconstructionTableBlock.Table.BREAK_TIME;
    }

    /** Кнопка 1 — забрать точку исследования. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 1 && table.aspect != null && player instanceof ServerPlayer serverPlayer) {
            table.claim(serverPlayer);
            return true;
        }
        return false;
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
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!DeconstructionTableBlock.hasAspects(stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
