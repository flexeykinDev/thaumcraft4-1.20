package thaumcraft.common.menu;

import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import thaumcraft.common.entity.Pech;
import thaumcraft.common.registry.ModEnchantments;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModMenus;
import thaumcraft.common.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Торг с печом (ContainerPech оригинала): в левую ячейку кладётся ценная вещь, нажатие на кости забирает одну штуку
 * и выдаёт взамен товары на сумму, равную цене вещи (плюс-минус немного удачи). Чем дороже подарок, тем дороже
 * и товары; иногда печ отдаёт что-нибудь из собственного тюка, иногда редкость из сундука подземелья.
 */
public class PechMenu extends AbstractContainerMenu {
    /** Пометка на предметах, выброшенных при закрытии окна: печ их не подбирает. */
    public static final String DROP_TAG = "PechDrop";

    /** Товар: цена от 1 до 5 и сам предмет. */
    private record Ware(int price, Supplier<ItemStack> stack) {
    }

    private static Ware ware(int price, ItemLike item) {
        return new Ware(price, () -> new ItemStack(item));
    }

    private static Ware potion(int price, Potion potion) {
        return new Ware(price, () -> PotionUtils.setPotion(new ItemStack(Items.POTION), potion));
    }

    private static Ware book(int price, Supplier<Enchantment> enchantment) {
        return new Ware(price, () -> EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment.get(), 1)));
    }

    /** tradeInventory оригинала для каждого из трёх видов печей. */
    private static List<Ware> wares(int type) {
        List<Ware> list = new ArrayList<>();
        list.add(ware(1, ModItems.MANA_BEAN.get()));
        if (type == Pech.FORAGER) {
            list.add(ware(1, ModItems.CLUSTER_IRON.get()));
            list.add(ware(1, ModItems.CLUSTER_GOLD.get()));
            list.add(ware(1, ModItems.CLUSTER_CINNABAR.get()));
            list.add(ware(1, ModItems.CLUSTER_COPPER.get()));
            list.add(ware(2, Items.BLAZE_ROD));
            list.add(ware(2, ModItems.GREATWOOD_SAPLING.get()));
            list.add(potion(2, Potions.STRENGTH));
            list.add(potion(2, Potions.SWIFTNESS));
            list.add(ware(3, Items.EXPERIENCE_BOTTLE));
            list.add(ware(3, ModItems.KNOWLEDGE_FRAGMENT.get()));
            list.add(ware(3, Items.GOLDEN_APPLE));
            list.add(potion(3, Potions.LONG_STRENGTH));
            list.add(potion(3, Potions.LONG_NIGHT_VISION));
            list.add(ware(5, Items.ENCHANTED_GOLDEN_APPLE));
            list.add(ware(4, ModItems.THAUMIUM_PICKAXE.get()));
            list.add(ware(5, ModItems.SILVERWOOD_SAPLING.get()));
            list.add(ware(5, ModItems.SILVERWOOD_SAPLING.get()));
        } else if (type == Pech.MAGE) {
            for (var shard : List.of(ModItems.SHARD_AIR, ModItems.SHARD_FIRE, ModItems.SHARD_WATER, ModItems.SHARD_EARTH,
                    ModItems.SHARD_ORDER, ModItems.SHARD_ENTROPY)) {
                list.add(ware(1, shard.get()));
            }
            list.add(ware(1, ModItems.KNOWLEDGE_FRAGMENT.get()));
            list.add(ware(2, ModItems.KNOWLEDGE_FRAGMENT.get()));
            list.add(potion(2, Potions.REGENERATION));
            list.add(potion(2, Potions.HEALING));
            list.add(book(3, ModEnchantments.HASTE));
            list.add(ware(3, Items.GOLDEN_APPLE));
            list.add(potion(3, Potions.STRONG_REGENERATION));
            list.add(potion(3, Potions.STRONG_HEALING));
            for (var crystal : List.of(ModItems.CRYSTAL_AIR, ModItems.CRYSTAL_FIRE, ModItems.CRYSTAL_WATER, ModItems.CRYSTAL_EARTH,
                    ModItems.CRYSTAL_ORDER, ModItems.CRYSTAL_ENTROPY, ModItems.CRYSTAL_MIXED)) {
                list.add(ware(4, crystal.get()));
            }
            list.add(ware(5, Items.ENCHANTED_GOLDEN_APPLE));
            list.add(book(5, ModEnchantments.REPAIR));
            list.add(ware(5, ModItems.FOCUS_POUCH.get()));
            list.add(ware(5, ModItems.FOCUS_PECH.get()));
            list.add(ware(5, ModItems.VIS_AMULET.get()));
        } else {
            // пятнадцать цветных сальных свечей (все, кроме чёрной)
            for (int color = 0; color < 15; color++) {
                list.add(ware(1, ModItems.TALLOW_CANDLES.get(color).get()));
            }
            list.add(ware(2, Items.GHAST_TEAR));
            list.add(potion(2, Potions.SWIFTNESS));
            list.add(potion(2, Potions.STRENGTH));
            list.add(book(2, () -> Enchantments.POWER_ARROWS));
            list.add(ware(3, Items.EXPERIENCE_BOTTLE));
            list.add(ware(3, ModItems.KNOWLEDGE_FRAGMENT.get()));
            list.add(potion(3, Potions.LONG_INVISIBILITY));
            list.add(potion(3, Potions.STRONG_REGENERATION));
            list.add(ware(3, Items.GOLDEN_APPLE));
            list.add(ware(5, Items.ENCHANTED_GOLDEN_APPLE));
            list.add(ware(4, ModItems.THAUMIUM_BOOTS.get()));
            list.add(ware(5, ModItems.RUNIC_RING.get()));
            list.add(book(5, () -> Enchantments.FLAMING_ARROWS));
            list.add(book(5, () -> Enchantments.INFINITY_ARROWS));
        }
        return list;
    }

    private final Pech pech;
    private final Player player;
    private final SimpleContainer slots = new SimpleContainer(5);

    public PechMenu(int id, Inventory inventory, Pech pech) {
        super(ModMenus.PECH.get(), id);
        this.pech = pech;
        this.player = inventory.player;
        if (pech != null) {
            pech.trading = true;
        }
        addSlot(new Slot(slots, 0, 36, 29));
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                addSlot(new Slot(slots, 1 + j + i * 2, 106 + 18 * j, 20 + 18 * i) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
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

    /** Можно ли бросить кости: слева ценная вещь, справа пусто. */
    public boolean canRoll() {
        if (!Pech.isValued(slots.getItem(0))) {
            return false;
        }
        for (int i = 1; i < 5; i++) {
            if (!slots.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean clickMenuButton(Player clicker, int button) {
        if (button == 0) {
            generateContents();
            return true;
        }
        return super.clickMenuButton(clicker, button);
    }

    private void give(ItemStack stack) {
        stack.onCraftedBy(player.level(), player, stack.getCount());
        deliver(stack);
    }

    /** Кладёт товар в ячейки выдачи: сперва к такому же, потом в пустую. Игроку класть туда нельзя, поэтому обычный перенос не годится. */
    private void deliver(ItemStack stack) {
        for (int i = 1; i < 5 && !stack.isEmpty(); i++) {
            ItemStack present = slots.getItem(i);
            if (!present.isEmpty() && ItemStack.isSameItemSameTags(present, stack) && present.getCount() < present.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), present.getMaxStackSize() - present.getCount());
                present.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int i = 1; i < 5 && !stack.isEmpty(); i++) {
            if (slots.getItem(i).isEmpty()) {
                slots.setItem(i, stack.copy());
                stack.setCount(0);
            }
        }
    }

    /** Редкость из сундука подземелья: в оригинале бралась из тех записей сундука, что выпадают реже всего. */
    private ItemStack dungeonRarity(RandomSource random) {
        return switch (random.nextInt(5)) {
            case 0 -> new ItemStack(Items.GOLDEN_APPLE);
            case 1 -> new ItemStack(Items.GOLDEN_HORSE_ARMOR);
            case 2 -> new ItemStack(Items.IRON_HORSE_ARMOR);
            case 3 -> new ItemStack(Items.DIAMOND_HORSE_ARMOR);
            default -> EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK), 30, true);
        };
    }

    private void generateContents() {
        if (pech == null || player.level().isClientSide || !canRoll()) {
            return;
        }
        RandomSource random = player.level().random;
        int value = Pech.getValue(slots.getItem(0));
        // чем дороже подарок, тем вероятнее, что печ после сделки снова насторожится
        if (random.nextInt(100) <= value / 2) {
            pech.setTamed(false);
            pech.playSound(ModSounds.PECH_TRADE.get(), 0.4f, 1f);
        }
        if (random.nextInt(5) == 0) {
            value += random.nextInt(3);
        } else if (random.nextBoolean()) {
            value -= random.nextInt(3);
        }
        List<Ware> wares = wares(pech.getPechType());
        while (value > 0) {
            int price = Math.min(5, Math.max((value + 1) / 2, random.nextInt(value) + 1));
            value -= price;
            List<Integer> packed = new ArrayList<>();
            for (int i = 0; i < pech.loot.getContainerSize(); i++) {
                if (!pech.loot.getItem(i).isEmpty()) {
                    packed.add(i);
                }
            }
            if (price == 1 && random.nextBoolean() && !packed.isEmpty()) {
                // мелочь печ достаёт из собственного тюка
                int slot = packed.get(random.nextInt(packed.size()));
                deliver(pech.loot.removeItem(slot, 1));
            } else if (price >= 4 && random.nextBoolean()) {
                give(dungeonRarity(random));
            } else {
                List<Ware> fitting = new ArrayList<>();
                for (Ware ware : wares) {
                    if (ware.price() == price) {
                        fitting.add(ware);
                    }
                }
                if (!fitting.isEmpty()) {
                    give(fitting.get(random.nextInt(fitting.size())).stack().get());
                }
            }
        }
        slots.removeItem(0, 1);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player user) {
        return pech != null && pech.isAlive() && pech.isTamed() && pech.distanceToSqr(user) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player user, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 5) {
            if (!moveItemStackTo(stack, 5, 41, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, true)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(user, stack);
        return copy;
    }

    /** Всё, что осталось в окне, падает к ногам игрока, и печ этого не подбирает. */
    @Override
    public void removed(Player user) {
        super.removed(user);
        if (pech != null) {
            pech.trading = false;
        }
        if (!user.level().isClientSide) {
            for (int i = 0; i < 5; i++) {
                ItemStack stack = slots.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    ItemEntity dropped = user.drop(stack, false);
                    if (dropped != null) {
                        dropped.getPersistentData().putBoolean(DROP_TAG, true);
                    }
                }
            }
        }
    }
}
