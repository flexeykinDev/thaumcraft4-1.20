package thaumcraft.common.items.wands;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.common.compat.Baubles;
import thaumcraft.common.menu.FocusPouchMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Сумка для фокусов (ItemFocusPouch оригинала): 18 ячеек только под фокусы. Жезл достаёт фокусы прямо из сумки,
 * лежит ли она в инвентаре или надета на пояс, и туда же убирает снятый.
 */
public class FocusPouchItem extends Item {
    public static final int SIZE = 18;
    private static final String TAG_INVENTORY = "Inventory";

    public FocusPouchItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            int slot = player.getInventory().selected;
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, p) -> new FocusPouchMenu(id, inventory, slot), stack.getHoverName()), buf -> buf.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static NonNullList<ItemStack> getInventory(ItemStack pouch) {
        NonNullList<ItemStack> out = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (pouch.hasTag() && pouch.getTag().contains(TAG_INVENTORY, Tag.TAG_LIST)) {
            ListTag list = pouch.getTag().getList(TAG_INVENTORY, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                int slot = entry.getByte("Slot") & 255;
                if (slot < SIZE) {
                    out.set(slot, ItemStack.of(entry));
                }
            }
        }
        return out;
    }

    public static void setInventory(ItemStack pouch, List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.size() && i < SIZE; i++) {
            if (!stacks.get(i).isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                stacks.get(i).save(entry);
                list.add(entry);
            }
        }
        pouch.getOrCreateTag().put(TAG_INVENTORY, list);
    }

    /** Все сумки игрока в том порядке, в каком их перебирал оригинал: сначала надетые, потом из инвентаря. */
    public static List<ItemStack> findPouches(Player player) {
        List<ItemStack> out = new ArrayList<>(Baubles.find(player, stack -> stack.getItem() instanceof FocusPouchItem));
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof FocusPouchItem) {
                out.add(stack);
            }
        }
        return out;
    }

    /** Кладёт фокус в первую свободную ячейку любой из сумок. */
    public static boolean addFocus(Player player, ItemStack focus) {
        for (ItemStack pouch : findPouches(player)) {
            NonNullList<ItemStack> inventory = getInventory(pouch);
            for (int i = 0; i < inventory.size(); i++) {
                if (inventory.get(i).isEmpty()) {
                    inventory.set(i, focus.copy());
                    setInventory(pouch, inventory);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable Level level, List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        for (ItemStack focus : getInventory(stack)) {
            if (!focus.isEmpty()) {
                tooltip.add(focus.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }
}
