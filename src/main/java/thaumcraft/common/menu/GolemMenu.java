package thaumcraft.common.menu;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.items.golem.GolemCoreItem;
import thaumcraft.common.registry.ModMenus;

/**
 * Окно голема (ContainerGolem оригинала): ячейки-образцы, в которые предметы не кладут, а «показывают» голему,
 * цвета ячеек и переключатели ядра. Пока окно открыто, голем стоит.
 */
public class GolemMenu extends AbstractContainerMenu {
    public static final int SCROLL_UP = 66;
    public static final int SCROLL_DOWN = 67;
    public static final int TOGGLE = 50;

    public final Golem golem;
    /** Сколько ячеек-образцов в окне (все страницы). */
    public final int ghostSlots;
    public int currentScroll;
    public final int maxScroll;

    public GolemMenu(int id, Inventory inventory, Golem golem) {
        super(ModMenus.GOLEM.get(), id);
        this.golem = golem;
        golem.paused = true;
        int core = golem.getCore();
        int slots = GolemCoreItem.hasInventory(core) ? golem.inventory.getContainerSize() : 0;
        ghostSlots = slots;
        maxScroll = Math.max(0, slots / 6 - 1);
        // у ядра наполнения образец помнит и количество, у остальных — только сам предмет
        int limit = core == GolemCoreItem.FILL ? 256 : 1;
        for (int index = 0; index < slots; index++) {
            int cell = index % 6;
            int page = index / 6;
            addSlot(new Ghost(golem, index, 100 + cell / 2 * 28, 16 + cell % 2 * 31, limit, page));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** Ячейка-образец: видна только на своей странице. */
    public class Ghost extends Slot {
        public final int limit;
        private final int page;

        Ghost(Golem golem, int index, int x, int y, int limit, int page) {
            super(golem.inventory, index, x, y);
            this.limit = limit;
            this.page = page;
        }

        @Override
        public boolean isActive() {
            return page == currentScroll;
        }

        @Override
        public int getMaxStackSize() {
            return limit;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return limit;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        golem.paused = false;
    }

    /**
     * Образцы не забирают предмет у игрока. Левый щелчок с предметом — запомнить его (у ядра наполнения — вместе
     * с количеством), правый — прибавить единицу; пустой рукой левый щелчок стирает образец, правый — убавляет на единицу.
     */
    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < ghostSlots && slots.get(slotId) instanceof Ghost ghost) {
            if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE || !ghost.isActive()) {
                return;
            }
            ItemStack held = getCarried();
            ItemStack sample = ghost.getItem();
            if (held.isEmpty()) {
                if (button == 1 && sample.getCount() > 1) {
                    sample.shrink(1);
                    ghost.setChanged();
                } else {
                    ghost.set(ItemStack.EMPTY);
                }
            } else if (button == 1 && ItemStack.isSameItemSameTags(sample, held)) {
                sample.setCount(Math.min(ghost.limit, sample.getCount() + 1));
                ghost.setChanged();
            } else {
                ghost.set(held.copyWithCount(Math.min(ghost.limit, button == 1 ? 1 : held.getCount())));
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button == SCROLL_UP && currentScroll > 0) {
            currentScroll--;
        }
        if (button == SCROLL_DOWN && currentScroll < maxScroll) {
            currentScroll++;
        }
        if (button >= TOGGLE && button <= TOGGLE + 7) {
            golem.setToggle(button - TOGGLE, !golem.getToggles()[button - TOGGLE]);
        }
        int slots = golem.inventory.getContainerSize();
        if (button >= 0 && button < slots) {
            int color = golem.getColors(button) - 1;
            golem.setColors(button, color < -1 ? 15 : color);
        }
        if (button >= slots && button < slots * 2 && button < TOGGLE) {
            int color = golem.getColors(button - slots) + 1;
            golem.setColors(button - slots, color > 15 ? -1 : color);
        }
        golem.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2f, 0.8f);
        return true;
    }

    /** Shift-щелчок по предмету в инвентаре показывает его голему в первой свободной ячейке текущей страницы. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < ghostSlots) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slots.get(index).getItem();
        for (int i = 0; i < ghostSlots; i++) {
            if (slots.get(i) instanceof Ghost ghost && ghost.isActive() && !ghost.hasItem() && !stack.isEmpty()) {
                ghost.set(stack.copyWithCount(Math.min(ghost.limit, stack.getCount())));
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return golem.isAlive() && player.distanceToSqr(golem) <= 64.0;
    }
}
