package thaumcraft.common.menu;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entity.golem.TravelingTrunk;
import thaumcraft.common.registry.ModMenus;

/** Окно дорожного сундука (ContainerTravelingTrunk оригинала): три ряда ячеек (четыре с улучшением земли) и кнопка «стоять». */
public class TrunkMenu extends AbstractContainerMenu {
    public final TravelingTrunk trunk;
    private final int rows;

    public TrunkMenu(int id, Inventory inventory, TravelingTrunk trunk) {
        this(id, inventory, trunk, trunk.getRows());
    }

    /**
     * rows приходит с сервера вместе с открытием окна: если улучшение земли вставили только что, данные сундука на
     * клиенте ещё не обновились, и окно с тремя рядами не совпало бы с серверным на четыре.
     */
    public TrunkMenu(int id, Inventory inventory, TravelingTrunk trunk, int rows) {
        super(ModMenus.TRUNK.get(), id);
        this.trunk = trunk;
        this.rows = rows;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(trunk.inventory, column + row * 9, 8 + column * 18, 15 + row * 23));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 118 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 176));
        }
        if (!trunk.level().isClientSide) {
            trunk.setOpen(true);
            trunk.playSound(SoundEvents.CHEST_OPEN, 0.5f, trunk.getRandom().nextFloat() * 0.1f + 0.9f);
        }
    }

    /** Кнопка 1 — велеть сундуку стоять на месте или снова идти следом. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == 1) {
            trunk.setStay(!trunk.getStay());
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < rows * 9) {
            if (!moveItemStackTo(stack, rows * 9, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, rows * 9, false)) {
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
        return trunk.isAlive() && player.distanceToSqr(trunk) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!trunk.level().isClientSide) {
            trunk.setOpen(false);
        }
    }
}
