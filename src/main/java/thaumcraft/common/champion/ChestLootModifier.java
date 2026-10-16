package thaumcraft.common.champion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisAmuletItem;
import thaumcraft.common.registry.ModItems;

import java.util.List;
import java.util.function.Supplier;

/**
 * Добыча мода в сундуках мира (ChestGenHooks в Config.initLoot оригинала). В 1.7.10 предметы мода просто дописывались
 * во взвешенный список сундука; здесь то же воспроизведено поверх таблицы добычи: сундук делает столько же бросков,
 * сколько делал в оригинале, и в каждом с вероятностью «вес предметов мода / общий вес списка» кладёт один из них.
 * Три набора — обычный, необычный, редкий — входят в список с весами tiers; extra — отдельные добавки
 * (фрагменты знаний в библиотеке, слитки тауминия в кузнице).
 */
public class ChestLootModifier extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Thaumcraft.MODID);

    public record Extra(ResourceLocation item, int weight, int min, int max) {
        static final Codec<Extra> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Extra::item),
                Codec.INT.fieldOf("weight").forGetter(Extra::weight),
                Codec.INT.fieldOf("min").forGetter(Extra::min),
                Codec.INT.fieldOf("max").forGetter(Extra::max)).apply(inst, Extra::new));
    }

    public static final Supplier<Codec<ChestLootModifier>> CODEC_SUPPLIER = () -> RecordCodecBuilder.create(inst -> codecStart(inst).and(inst.group(
            Codec.INT.fieldOf("rolls_min").forGetter(m -> m.rollsMin),
            Codec.INT.fieldOf("rolls_max").forGetter(m -> m.rollsMax),
            Codec.INT.fieldOf("vanilla_weight").forGetter(m -> m.vanillaWeight),
            Codec.INT.listOf().fieldOf("tiers").forGetter(m -> m.tiers),
            Extra.CODEC.listOf().optionalFieldOf("extra", List.of()).forGetter(m -> m.extra))).apply(inst, ChestLootModifier::new));

    public static final RegistryObject<Codec<ChestLootModifier>> CODEC = SERIALIZERS.register("chest_loot", CODEC_SUPPLIER);
    public static final RegistryObject<Codec<thaumcraft.common.loot.ClusterLootModifier>> CLUSTERS =
            SERIALIZERS.register("native_clusters", thaumcraft.common.loot.ClusterLootModifier.CODEC);

    /** Сколько предметов в каждом наборе оригинала. */
    private static final int[] SET_SIZE = {3, 5, 14};
    /** Сколько штук кладётся за раз: обычного 1–3, необычного 1–2, редкого 1. */
    private static final int[] MAX_COUNT = {3, 2, 1};

    private final int rollsMin;
    private final int rollsMax;
    private final int vanillaWeight;
    private final List<Integer> tiers;
    private final List<Extra> extra;

    public ChestLootModifier(LootItemCondition[] conditions, int rollsMin, int rollsMax, int vanillaWeight, List<Integer> tiers,
                             List<Extra> extra) {
        super(conditions);
        this.rollsMin = rollsMin;
        this.rollsMax = rollsMax;
        this.vanillaWeight = vanillaWeight;
        this.tiers = tiers;
        this.extra = extra;
    }

    private static List<Supplier<? extends Item>> set(int tier) {
        return switch (tier) {
            case 0 -> List.of(ModItems.LOOT_BAG_COMMON, ModItems.THAUMIUM_INGOT, ModItems.AMBER);
            case 1 -> List.of(ModItems.LOOT_BAG_UNCOMMON, ModItems.BAUBLE_AMULET, ModItems.BAUBLE_RING, ModItems.BAUBLE_BELT,
                    ModItems.KNOWLEDGE_FRAGMENT);
            default -> List.of(ModItems.LOOT_BAG_RARE, ModItems.THAUMONOMICON, ModItems.THAUMIUM_SWORD, ModItems.THAUMIUM_PICKAXE,
                    ModItems.THAUMIUM_AXE, ModItems.THAUMIUM_HOE, ModItems.RUNIC_RING_LESSER, ModItems.APPRENTICE_RING_AIR,
                    ModItems.APPRENTICE_RING_FIRE, ModItems.APPRENTICE_RING_WATER, ModItems.APPRENTICE_RING_EARTH,
                    ModItems.APPRENTICE_RING_ORDER, ModItems.APPRENTICE_RING_ENTROPY, ModItems.VIS_STONE);
        };
    }

    private int tierWeight(int tier) {
        return tier < tiers.size() ? tiers.get(tier) : 0;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource random = context.getRandom();
        int modWeight = 0;
        for (int tier = 0; tier < 3; tier++) {
            modWeight += SET_SIZE[tier] * tierWeight(tier);
        }
        for (Extra entry : extra) {
            modWeight += entry.weight();
        }
        if (modWeight <= 0) {
            return loot;
        }
        int rolls = rollsMin + random.nextInt(Math.max(1, rollsMax - rollsMin + 1));
        for (int i = 0; i < rolls; i++) {
            int roll = random.nextInt(modWeight + vanillaWeight);
            if (roll >= modWeight) {
                continue;
            }
            ItemStack stack = pick(roll, random);
            if (!stack.isEmpty()) {
                loot.add(stack);
            }
        }
        return loot;
    }

    private ItemStack pick(int roll, RandomSource random) {
        for (int tier = 0; tier < 3; tier++) {
            int weight = tierWeight(tier);
            int span = SET_SIZE[tier] * weight;
            if (roll < span) {
                List<Supplier<? extends Item>> items = set(tier);
                int index = roll / weight;
                if (index >= items.size()) {
                    return ItemStack.EMPTY;
                }
                ItemStack stack = new ItemStack(items.get(index).get(), 1 + random.nextInt(MAX_COUNT[tier]));
                if (stack.getItem() instanceof VisAmuletItem stone) {
                    // камень вис с небольшим случайным запасом каждой стихии
                    for (Aspect aspect : Aspect.primals()) {
                        stone.addVis(stack, aspect, random.nextInt(5), true);
                    }
                }
                return stack;
            }
            roll -= span;
        }
        for (Extra entry : extra) {
            if (roll < entry.weight()) {
                Item item = ForgeRegistries.ITEMS.getValue(entry.item());
                return item == null ? ItemStack.EMPTY : new ItemStack(item, entry.min() + random.nextInt(Math.max(1, entry.max() - entry.min() + 1)));
            }
            roll -= entry.weight();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
