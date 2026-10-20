package thaumictinkerer.common.item.kami;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.common.items.wands.FocusPouchItem;
import thaumictinkerer.common.registry.TTMenus;

import java.util.ArrayList;
import java.util.List;

/**
 * Бездонный подсумок (ItemIchorPouch): сумка для фокусов на 117 ячеек, в которую можно класть что угодно, кроме других
 * сумок. Носится на поясе; жезл, как и из обычной сумки, достаёт из неё фокусы.
 */
public class IchorPouchItem extends FocusPouchItem {
    public static final int SLOTS = 117;

    public IchorPouchItem() {
        super(new Item.Properties().stacksTo(1).rarity(Kami.RARITY));
    }

    @Override
    public int slots() {
        return SLOTS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            int slot = player.getInventory().selected;
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, p) -> new Menu(id, inventory, slot),
                    stack.getHoverName()), buf -> buf.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** ContainerIchorPouch: 13×9 ячеек сумки над инвентарём игрока; сама сумка в хотбаре заперта. */
    public static class Menu extends AbstractContainerMenu {
        private final SimpleContainer input = new SimpleContainer(SLOTS);
        private final Player player;
        private final int pouchSlot;
        private final int lockedSlot;

        public Menu(int id, Inventory inventory, int pouchSlot) {
            super(TTMenus.ICHOR_POUCH.get(), id);
            this.player = inventory.player;
            this.pouchSlot = pouchSlot;
            this.lockedSlot = SLOTS + 27 + pouchSlot;
            for (int row = 0; row < 9; row++) {
                for (int column = 0; column < 13; column++) {
                    addSlot(new Slot(input, row * 13 + column, 12 + column * 18, 8 + row * 18) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return !(stack.getItem() instanceof FocusPouchItem);
                        }
                    });
                }
            }
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 9; column++) {
                    addSlot(new Slot(inventory, column + row * 9 + 9, 48 + column * 18, 177 + row * 18));
                }
            }
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column, 48 + column * 18, 235));
            }
            if (!player.level().isClientSide) {
                List<ItemStack> stored = FocusPouchItem.getInventory(pouch());
                for (int i = 0; i < stored.size(); i++) {
                    input.setItem(i, stored.get(i));
                }
            }
            input.addListener(container -> save());
        }

        public int getPouchSlot() {
            return pouchSlot;
        }

        private ItemStack pouch() {
            return player.getInventory().getItem(pouchSlot);
        }

        private void save() {
            if (!player.level().isClientSide && pouch().getItem() instanceof IchorPouchItem) {
                List<ItemStack> stacks = new ArrayList<>();
                for (int i = 0; i < input.getContainerSize(); i++) {
                    stacks.add(input.getItem(i));
                }
                FocusPouchItem.setInventory(pouch(), stacks);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return pouch().getItem() instanceof IchorPouchItem;
        }

        @Override
        public void clicked(int slot, int button, ClickType type, Player player) {
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
            if (!slot.hasItem() || slot.getItem().getItem() instanceof FocusPouchItem) {
                return ItemStack.EMPTY;
            }
            ItemStack inSlot = slot.getItem();
            ItemStack copy = inSlot.copy();
            if (index < SLOTS) {
                if (!moveItemStackTo(inSlot, SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(inSlot, 0, SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (inSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return copy;
        }
    }
}
