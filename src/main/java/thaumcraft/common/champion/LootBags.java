package thaumcraft.common.champion;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisAmuletItem;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Содержимое мешков с добычей (Config.initLoot и Utils.generateLoot оригинала): три взвешенных списка — для обычного,
 * необычного и редкого мешка. В основном это золотые монеты; чем мешок реже, тем больше в нём самоцветов,
 * зачарованных книг, зелий и случайного снаряжения.
 */
public final class LootBags {
    private record Entry(int weight, Function<RandomSource, ItemStack> item) {
    }

    @SuppressWarnings("unchecked")
    private static final List<Entry>[] LISTS = new List[]{new ArrayList<Entry>(), new ArrayList<Entry>(), new ArrayList<Entry>()};
    private static boolean built;

    private LootBags() {
    }

    private static void add(Function<RandomSource, ItemStack> item, int weight, int... tiers) {
        for (int tier : tiers) {
            LISTS[tier].add(new Entry(weight, item));
        }
    }

    private static void add(Item item, int count, int weight, int... tiers) {
        add(random -> new ItemStack(item, count), weight, tiers);
    }

    private static void build() {
        built = true;
        Item coin = ModItems.COIN.get();
        add(coin, 1, 2500, 0);
        add(coin, 2, 2250, 1);
        add(coin, 3, 2000, 2);
        add(Items.NETHER_STAR, 1, 1, 2);
        add(Items.DIAMOND, 1, 10, 0);
        add(Items.DIAMOND, 1, 50, 1, 2);
        add(Items.EMERALD, 1, 15, 0);
        add(Items.EMERALD, 1, 75, 1, 2);
        add(Items.GOLD_INGOT, 1, 100, 0, 1, 2);
        add(Items.ENDER_PEARL, 1, 100, 0, 1, 2);
        add(ModItems.KNOWLEDGE_FRAGMENT.get(), 1, 25, 0, 1, 2);
        add(ModItems.BAUBLE_AMULET.get(), 1, 10, 0);
        add(ModItems.BAUBLE_RING.get(), 1, 10, 0);
        add(ModItems.BAUBLE_BELT.get(), 1, 10, 0);
        for (var ring : java.util.List.of(ModItems.APPRENTICE_RING_AIR, ModItems.APPRENTICE_RING_FIRE, ModItems.APPRENTICE_RING_WATER,
                ModItems.APPRENTICE_RING_EARTH, ModItems.APPRENTICE_RING_ORDER, ModItems.APPRENTICE_RING_ENTROPY)) {
            add(ring.get(), 1, 5, 1);
            add(ring.get(), 1, 7, 2);
        }
        add(ModItems.RUNIC_RING_LESSER.get(), 1, 5, 1, 2);
        // камень вис с небольшим случайным запасом каждой стихии
        add(random -> {
            ItemStack stone = new ItemStack(ModItems.VIS_STONE.get());
            for (Aspect aspect : Aspect.primals()) {
                ((VisAmuletItem) stone.getItem()).addVis(stone, aspect, random.nextInt(5), true);
            }
            return stone;
        }, 6, 1, 2);
        add(Items.EXPERIENCE_BOTTLE, 1, 5, 0);
        add(Items.EXPERIENCE_BOTTLE, 1, 10, 1);
        add(Items.EXPERIENCE_BOTTLE, 1, 20, 2);
        add(Items.ENCHANTED_GOLDEN_APPLE, 1, 1, 0);
        add(Items.ENCHANTED_GOLDEN_APPLE, 1, 2, 1);
        add(Items.ENCHANTED_GOLDEN_APPLE, 1, 3, 2);
        add(Items.GOLDEN_APPLE, 1, 3, 0);
        add(Items.GOLDEN_APPLE, 1, 6, 1);
        add(Items.GOLDEN_APPLE, 1, 9, 2);
        add(Items.BOOK, 1, 10, 0, 1, 2);
        // все зелья игры, обычные и взрывные: простое реже усиленного, усиленное реже продлённого
        for (Potion potion : ForgeRegistries.POTIONS) {
            ResourceLocation id = ForgeRegistries.POTIONS.getKey(potion);
            if (id == null || potion.getEffects().isEmpty()) {
                continue;
            }
            int weight = id.getPath().startsWith("long_") ? 3 : id.getPath().startsWith("strong_") ? 2 : 1;
            add(random -> PotionUtils.setPotion(new ItemStack(Items.POTION), potion), weight, 0, 1, 2);
            add(random -> PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion), weight, 0, 1, 2);
        }
    }

    private static ItemStack pick(List<Entry> list, RandomSource random) {
        int total = 0;
        for (Entry entry : list) {
            total += entry.weight();
        }
        int roll = random.nextInt(total);
        for (Entry entry : list) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.item().apply(random);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Один предмет из мешка редкости tier (0 — обычный, 1 — необычный, 2 — редкий). */
    public static ItemStack generate(int tier, RandomSource random) {
        if (!built) {
            build();
        }
        ItemStack stack = null;
        if (tier > 0 && random.nextFloat() < 0.025f * tier) {
            stack = gear(tier, random);
        }
        if (stack == null) {
            stack = pick(LISTS[tier], random);
        }
        if (stack.is(Items.BOOK)) {
            stack = EnchantmentHelper.enchantItem(random, stack, (int) (5f + tier * 0.75f * random.nextInt(18)), false);
        }
        return stack;
    }

    /** Случайное оружие или часть брони: чаще простое и потрёпанное, в редких мешках нередко зачарованное. */
    @Nullable
    private static ItemStack gear(int tier, RandomSource random) {
        int quality = random.nextInt(2);
        for (float chance : new float[]{0.2f, 0.15f, 0.1f, 0.095f, 0.095f}) {
            if (random.nextFloat() < chance) {
                quality++;
            }
        }
        Item item = gearFor(random.nextInt(5), quality);
        if (item == null) {
            return null;
        }
        ItemStack stack = new ItemStack(item);
        stack.setDamageValue(random.nextInt(1 + stack.getMaxDamage() / 6));
        if (random.nextInt(4) < tier) {
            stack = EnchantmentHelper.enchantItem(random, stack, (int) (5f + tier * 0.75f * random.nextInt(18)), false);
        }
        return stack;
    }

    /** Седьмая ступень качества в оригинале — снаряжение из пустотного металла; пока его нет, выпадает обычный предмет. */
    @Nullable
    private static Item gearFor(int slot, int quality) {
        Item[][] table = {
                {Items.IRON_AXE, Items.IRON_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_SWORD, ModItems.THAUMIUM_SWORD.get(), Items.DIAMOND_SWORD},
                {Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, ModItems.THAUMIUM_BOOTS.get(),
                        Items.DIAMOND_BOOTS},
                {Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS,
                        ModItems.THAUMIUM_LEGGINGS.get(), Items.DIAMOND_LEGGINGS},
                {Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE,
                        ModItems.THAUMIUM_CHESTPLATE.get(), Items.DIAMOND_CHESTPLATE},
                {Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, ModItems.THAUMIUM_HELMET.get(),
                        Items.DIAMOND_HELMET}};
        return quality < table[slot].length ? table[slot][quality] : null;
    }
}
