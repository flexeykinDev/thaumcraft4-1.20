package thaumictinkerer.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.WispEssenceItem;
import thaumcraft.common.registry.ModItems;
import thaumictinkerer.common.registry.TTItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Добыча спелого наполненного зерна по его аспекту (AspectCropLootManager): у каждого аспекта свой взвешенный список.
 * Предметы, которых в сборке нет, пропускаются.
 */
public final class AspectCropLoot {
    private record Loot(ItemStack stack, int weight) {
    }

    private static Map<Aspect, List<Loot>> table;

    private AspectCropLoot() {
    }

    public static ItemStack get(Aspect aspect, RandomSource random) {
        List<Loot> list = table().getOrDefault(aspect, List.of());
        int total = list.stream().mapToInt(Loot::weight).sum();
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        int roll = random.nextInt(total);
        for (Loot loot : list) {
            roll -= loot.weight();
            if (roll < 0) {
                return loot.stack().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static void add(Map<Aspect, List<Loot>> map, Aspect aspect, ItemStack stack, int weight) {
        if (!stack.isEmpty()) {
            map.computeIfAbsent(aspect, k -> new ArrayList<>()).add(new Loot(stack, weight));
        }
    }

    private static void add(Map<Aspect, List<Loot>> map, Aspect aspect, Item item, int count, int weight) {
        add(map, aspect, new ItemStack(item, count), weight);
    }

    /** Предмет по названию: его может не быть, если кусок Thaumcraft не перенесён. */
    private static void add(Map<Aspect, List<Loot>> map, Aspect aspect, String id, int count, int weight) {
        Item item = ForgeRegistries.ITEMS.getValue(thaumcraft.common.Res.parse(id));
        if (item != null && item != Items.AIR) {
            add(map, aspect, item, count, weight);
        }
    }

    private static void addTag(Map<Aspect, List<Loot>> map, Aspect aspect, TagKey<Item> tag) {
        BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> add(map, aspect, holder.value(), 1, 1));
    }

    private static Map<Aspect, List<Loot>> table() {
        if (table != null) {
            return table;
        }
        Map<Aspect, List<Loot>> map = new HashMap<>();
        add(map, Aspect.AIR, TTItems.INFUSED_GRAIN_AIR.get(), 1, 1);
        add(map, Aspect.FIRE, TTItems.INFUSED_GRAIN_FIRE.get(), 1, 1);
        add(map, Aspect.EARTH, TTItems.INFUSED_GRAIN_EARTH.get(), 1, 1);
        add(map, Aspect.WATER, TTItems.INFUSED_GRAIN_WATER.get(), 1, 1);
        add(map, Aspect.ORDER, Items.GLASS, 64, 1);
        add(map, Aspect.ENTROPY, Items.SAND, 64, 1);
        add(map, Aspect.ELDRITCH, Items.ENDER_PEARL, 4, 10);
        add(map, Aspect.ELDRITCH, Items.ENDER_EYE, 4, 5);
        addTag(map, Aspect.TREE, ItemTags.LOGS);
        add(map, Aspect.TREE, Items.OAK_LOG, 1, 1);
        for (Aspect aspect : Aspect.all()) {
            add(map, Aspect.AURA, WispEssenceItem.of(ModItems.WISP_ESSENCE.get(), aspect), 1);
        }
        // в оригинале — яйца с номерами 0–23, которыми никого нельзя призвать; здесь — яйца мирных животных
        for (Item egg : List.of(Items.PIG_SPAWN_EGG, Items.COW_SPAWN_EGG, Items.SHEEP_SPAWN_EGG, Items.CHICKEN_SPAWN_EGG,
                Items.RABBIT_SPAWN_EGG, Items.HORSE_SPAWN_EGG)) {
            add(map, Aspect.BEAST, egg, 1, 1);
        }
        add(map, Aspect.MIND, Items.PAPER, 64, 15);
        add(map, Aspect.MIND, Items.BOOK, 32, 10);
        add(map, Aspect.MIND, Items.BOOKSHELF, 16, 5);
        ForgeRegistries.ENCHANTMENTS.getValues().stream().filter(e -> e.isAllowedOnBooks() && e.isDiscoverable())
                .forEach(e -> add(map, Aspect.MIND, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e, 1)), 1));
        add(map, Aspect.FLESH, "thaumcraft:zombie_brain", 16, 5);
        add(map, Aspect.UNDEAD, Items.ROTTEN_FLESH, 32, 1);
        add(map, Aspect.CRAFT, TTItems.DARK_QUARTZ_BLOCK.get(), 32, 1);
        add(map, Aspect.CRAFT, "thaumcraft:arcane_stone", 16, 1);
        add(map, Aspect.HUNGER, Items.NETHER_WART, 16, 1);
        add(map, Aspect.COLD, Items.SNOWBALL, 16, 1);
        addTag(map, Aspect.PLANT, ItemTags.SAPLINGS);
        ForgeRegistries.ITEMS.getKeys().stream().filter(id -> id.getNamespace().equals("thaumcraft")
                && id.getPath().startsWith("golem_core_") && !id.getPath().equals("golem_core_blank"))
                .forEach(id -> add(map, Aspect.MAN, id.toString(), 1, 1));
        for (Item armor : List.of(Items.DIAMOND_BOOTS, Items.DIAMOND_LEGGINGS, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_HELMET)) {
            add(map, Aspect.ARMOR, armor, 1, 1);
        }
        add(map, Aspect.MINE, "thaumcraft:thaumium_pickaxe", 1, 1);
        add(map, Aspect.HARVEST, "thaumcraft:thaumium_hoe", 1, 1);
        add(map, Aspect.WEAPON, "thaumcraft:thaumium_sword", 1, 1);
        add(map, Aspect.TOOL, "thaumcraft:thaumium_shovel", 1, 1);
        add(map, Aspect.TOOL, "thaumcraft:thaumium_axe", 1, 1);
        add(map, Aspect.TRAVEL, "thaumcraft:travel_paving", 8, 1);
        add(map, Aspect.SLIME, Items.SLIME_BALL, 16, 1);
        add(map, Aspect.SLIME, Items.SLIME_BLOCK, 1, 1);
        add(map, Aspect.GREED, Items.GOLD_INGOT, 4, 1);
        add(map, Aspect.LIGHT, Items.GLOWSTONE_DUST, 16, 5);
        add(map, Aspect.LIGHT, "thaumcraft:nitor", 4, 1);
        add(map, Aspect.LIGHT, TTItems.BRIGHT_NITOR.get(), 1, 1);
        add(map, Aspect.MECHANISM, Items.PISTON, 8, 1);
        add(map, Aspect.CROP, Items.WHEAT, 32, 1);
        add(map, Aspect.METAL, Items.IRON_INGOT, 4, 100);
        for (Item iron : List.of(Items.IRON_NUGGET, Items.IRON_BLOCK, Items.IRON_ORE, Items.RAW_IRON)) {
            add(map, Aspect.METAL, iron, 1, 1);
        }
        add(map, Aspect.DEATH, Items.BONE, 32, 1);
        add(map, Aspect.MOTION, Items.RAIL, 1, 10);
        add(map, Aspect.MOTION, Items.ACTIVATOR_RAIL, 1, 1);
        add(map, Aspect.CLOTH, Items.WHITE_WOOL, 16, 30);
        add(map, Aspect.CLOTH, Items.STRING, 15, 10);
        BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.WOOL).forEach(holder -> add(map, Aspect.CLOTH, holder.value(), 4, 1));
        add(map, Aspect.EXCHANGE, Items.COPPER_INGOT, 4, 1);
        add(map, Aspect.EXCHANGE, "thaumcraft:cinnabar_ore", 4, 1);
        add(map, Aspect.ENERGY, "thaumcraft:alumentum", 12, 1);
        for (String shard : List.of("air", "fire", "water", "earth", "order", "entropy", "balanced")) {
            add(map, Aspect.MAGIC, "thaumcraft:shard_" + shard, 1, 1);
        }
        add(map, Aspect.HEAL, Items.GOLDEN_APPLE, 1, 1);
        add(map, Aspect.HEAL, Items.CAKE, 1, 1);
        add(map, Aspect.SENSES, Items.LAPIS_LAZULI, 20, 1);
        add(map, Aspect.SOUL, Items.SOUL_SAND, 64, 2);
        add(map, Aspect.SOUL, Items.NETHERRACK, 64, 2);
        add(map, Aspect.SOUL, Items.NETHER_BRICKS, 1, 1);
        add(map, Aspect.DARKNESS, Items.OBSIDIAN, 10, 1);
        add(map, Aspect.VOID, Items.BUCKET, 1, 1);
        add(map, Aspect.POISON, "thaumcraft:quicksilver", 16, 1);
        add(map, Aspect.LIFE, Items.EGG, 8, 1);
        add(map, Aspect.TRAP, Items.COBWEB, 4, 1);
        add(map, Aspect.TAINT, "thaumcraft:taint_goo", 4, 1);
        add(map, Aspect.CRYSTAL, Items.DIAMOND, 1, 1);
        table = map;
        return map;
    }
}
