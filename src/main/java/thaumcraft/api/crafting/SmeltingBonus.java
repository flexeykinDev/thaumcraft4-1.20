package thaumcraft.api.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Бонусы плавки (ThaumcraftApi.addSmeltingBonus оригинала): что изредка выпадает сверх обычного результата,
 * когда предмет плавится в адской печи. Бонус задаётся для предмета или для тега; у тега-бонуса берётся первый
 * предмет тега — так олово, серебро и свинец из чужих модов дают свои самородки, если такие есть.
 */
public final class SmeltingBonus {
    private static final Map<Item, Supplier<ItemStack>> BY_ITEM = new LinkedHashMap<>();
    private static final List<Map.Entry<TagKey<Item>, Supplier<ItemStack>>> BY_TAG = new ArrayList<>();

    private SmeltingBonus() {
    }

    public static void add(Item input, Supplier<ItemStack> bonus) {
        BY_ITEM.put(input, bonus);
    }

    public static void add(TagKey<Item> input, Supplier<ItemStack> bonus) {
        BY_TAG.add(Map.entry(input, bonus));
    }

    /** Первый предмет тега, например forge:nuggets/tin; пусто, если тег пуст. */
    public static Supplier<ItemStack> firstOf(String tag) {
        TagKey<Item> key = ItemTags.create(new ResourceLocation(tag));
        return () -> BuiltInRegistries.ITEM.getTag(key).flatMap(set -> set.stream().findFirst())
                .map(holder -> new ItemStack(holder.value())).orElse(ItemStack.EMPTY);
    }

    /** Бонус для предмета (количество 1) или пустой стек. */
    public static ItemStack get(ItemStack input) {
        Supplier<ItemStack> bonus = BY_ITEM.get(input.getItem());
        if (bonus == null) {
            for (Map.Entry<TagKey<Item>, Supplier<ItemStack>> entry : BY_TAG) {
                if (input.is(entry.getKey())) {
                    bonus = entry.getValue();
                    break;
                }
            }
        }
        ItemStack stack = bonus == null ? ItemStack.EMPTY : bonus.get();
        return stack.isEmpty() || stack.is(Items.AIR) ? ItemStack.EMPTY : stack.copyWithCount(1);
    }
}
