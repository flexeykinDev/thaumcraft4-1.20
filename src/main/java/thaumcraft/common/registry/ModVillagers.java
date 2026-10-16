package thaumcraft.common.registry;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Деревенские тауматург и меняла (VillageWizardManager и VillageBankerManager оригинала). В оригинале жители этих
 * профессий жили в своих постройках — башне мага и доме менялы; в 1.20 профессию держит рабочее место, поэтому
 * тауматург работает за магическим верстаком, меняла — за столом. Их постройки добавляются в деревни всех видов.
 */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Thaumcraft.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, Thaumcraft.MODID);

    public static final RegistryObject<PoiType> THAUMATURGE_POI = POI_TYPES.register("thaumaturge",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.ARCANE_WORKBENCH.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final RegistryObject<PoiType> MONEYCHANGER_POI = POI_TYPES.register("moneychanger",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.TABLE.get().getStateDefinition().getPossibleStates()), 1, 1));

    public static final RegistryObject<VillagerProfession> THAUMATURGE = PROFESSIONS.register("thaumaturge",
            () -> profession("thaumaturge", THAUMATURGE_POI.getKey()));
    public static final RegistryObject<VillagerProfession> MONEYCHANGER = PROFESSIONS.register("moneychanger",
            () -> profession("moneychanger", MONEYCHANGER_POI.getKey()));

    private static final String[] VILLAGES = {"plains", "desert", "savanna", "taiga", "snowy"};

    private ModVillagers() {
    }

    private static VillagerProfession profession(String name, ResourceKey<PoiType> poi) {
        return new VillagerProfession(name, holder -> holder.is(poi), holder -> holder.is(poi), ImmutableSet.of(), ImmutableSet.of(),
                SoundEvents.VILLAGER_WORK_LIBRARIAN);
    }

    // ---------- торговля ----------

    private static VillagerTrades.ItemListing sell(Supplier<? extends net.minecraft.world.level.ItemLike> item, int min, int spread, int level) {
        return (trader, random) -> new MerchantOffer(new ItemStack(item.get(), min + random.nextInt(spread + 1)),
                new ItemStack(Items.EMERALD), 12, xp(level), 0.05f);
    }

    private static VillagerTrades.ItemListing buy(Function<RandomSource, ItemStack> item, int emeralds, int spread, int level) {
        return (trader, random) -> new MerchantOffer(new ItemStack(Items.EMERALD, emeralds + random.nextInt(spread + 1)),
                item.apply(random), 12, xp(level), 0.05f);
    }

    private static VillagerTrades.ItemListing coins(int min, int spread, Item result, int level) {
        return (trader, random) -> new MerchantOffer(new ItemStack(ModItems.COIN.get(), min + random.nextInt(spread + 1)),
                new ItemStack(result), 12, xp(level), 0.05f);
    }

    private static int xp(int level) {
        return new int[]{2, 5, 10, 15, 30}[level - 1];
    }

    /**
     * Сделки оригинала (в 1.7 все сразу) разложены по пяти уровням мастерства: сначала простые обмены, потом
     * редкие вещи вроде рунных колец и амулетов.
     */
    public static void trades(VillagerTradesEvent event) {
        if (event.getType() == THAUMATURGE.get()) {
            var t = event.getTrades();
            t.get(1).add(sell(ModItems.COIN, 20, 2, 1));
            t.get(1).add(buy(r -> new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), 1, 0, 1));
            t.get(1).add(sell(ModItems.QUICKSILVER, 4, 2, 1));
            t.get(1).add(buy(r -> new ItemStack(ModItems.ALUMENTUM.get()), 1, 0, 1));
            t.get(2).add(sell(ModItems.AMBER, 4, 2, 2));
            t.get(2).add(buy(r -> new ItemStack(ModItems.NITOR.get()), 1, 0, 2));
            t.get(2).add(sell(ModItems.NUGGET_CHICKEN, 24, 7, 2));
            t.get(2).add((trader, random) -> new MerchantOffer(new ItemStack(Items.BOOK, 4 + random.nextInt(3)),
                    new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), 12, xp(2), 0.05f));
            t.get(3).add(sell(ModItems.NUGGET_BEEF, 24, 7, 3));
            t.get(3).add(buy(r -> new ItemStack(shard(r), 2 + r.nextInt(2)), 1, 0, 3));
            t.get(3).add(buy(r -> new ItemStack(ModItems.MANA_BEAN.get(), 1 + r.nextInt(2)), 1, 0, 3));
            t.get(4).add(buy(r -> new ItemStack(ModItems.BATH_SALTS.get()), 5, 2, 4));
            t.get(4).add(buy(r -> new ItemStack(ModItems.RUNIC_RING_LESSER.get()), 5, 2, 4));
            t.get(5).add(buy(r -> new ItemStack(ModItems.VIS_STONE.get()), 5, 2, 5));
            t.get(5).add(buy(r -> new ItemStack(apprenticeRing(r)), 5, 2, 5));
        } else if (event.getType() == MONEYCHANGER.get()) {
            var t = event.getTrades();
            t.get(1).add(sell(ModItems.COIN, 20, 2, 1));
            t.get(1).add(coins(2, 1, Items.ARROW, 1));
            t.get(1).add(coins(6, 2, Items.WHITE_WOOL, 1));
            t.get(2).add(coins(3, 1, Items.PAPER, 2));
            t.get(2).add(coins(7, 2, Items.BOOK, 2));
            t.get(2).add(coins(2, 1, Items.COAL, 2));
            t.get(3).add(coins(16, 4, Items.EXPERIENCE_BOTTLE, 3));
            t.get(3).add(coins(9, 3, Items.GLOWSTONE, 3));
            t.get(4).add(coins(6, 2, Items.IRON_INGOT, 4));
            t.get(4).add(coins(10, 2, ModItems.THAUMIUM_INGOT.get(), 4));
            t.get(5).add(coins(22, 2, Items.DIAMOND, 5));
            t.get(5).add(coins(25, 7, Items.SADDLE, 5));
        }
    }

    private static Item shard(RandomSource random) {
        String[] shards = {"air", "fire", "water", "earth", "order", "entropy"};
        return ForgeRegistries.ITEMS.getValue(Thaumcraft.id("shard_" + shards[random.nextInt(shards.length)]));
    }

    private static Item apprenticeRing(RandomSource random) {
        String[] rings = {"air", "fire", "water", "earth", "order", "entropy"};
        return ForgeRegistries.ITEMS.getValue(Thaumcraft.id("apprentice_ring_" + rings[random.nextInt(rings.length)]));
    }

    // ---------- постройки в деревнях ----------

    /** Добавляет башню мага и дом менялы в наборы домов всех деревень (getVillagePieceWeight оригинала). */
    @SuppressWarnings("unchecked")
    public static void addVillageHouses(MinecraftServer server) {
        Registry<StructureTemplatePool> pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors = server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        Holder<StructureProcessorList> empty = processors.getHolderOrThrow(
                ResourceKey.create(Registries.PROCESSOR_LIST, new ResourceLocation("minecraft", "empty")));
        for (String village : VILLAGES) {
            StructureTemplatePool pool = pools.get(new ResourceLocation("minecraft", "village/" + village + "/houses"));
            if (pool == null) {
                continue;
            }
            try {
                List<StructurePoolElement> templates = null;
                Field rawField = null;
                for (Field field : StructureTemplatePool.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getType() == it.unimi.dsi.fastutil.objects.ObjectArrayList.class) {
                        templates = (List<StructurePoolElement>) field.get(pool);
                    } else if (field.getType() == List.class) {
                        rawField = field;
                    }
                }
                if (templates == null) {
                    continue;
                }
                List<Pair<StructurePoolElement, Integer>> raw = rawField == null ? null
                        : new ArrayList<>((List<Pair<StructurePoolElement, Integer>>) rawField.get(pool));
                add(templates, raw, "village/" + village + "/wizard_tower", 2, empty);
                add(templates, raw, "village/" + village + "/banker_home_1", 1, empty);
                add(templates, raw, "village/" + village + "/banker_home_2", 1, empty);
                if (rawField != null) {
                    rawField.set(pool, raw);
                }
            } catch (ReflectiveOperationException e) {
                Thaumcraft.LOGGER.warn("Не удалось добавить постройки Thaumcraft в деревни {}", village, e);
            }
        }
    }

    private static void add(List<StructurePoolElement> templates, List<Pair<StructurePoolElement, Integer>> raw, String path, int weight,
                            Holder<StructureProcessorList> processors) {
        SinglePoolElement element = StructurePoolElement.legacy(Thaumcraft.MODID + ":" + path, processors)
                .apply(StructureTemplatePool.Projection.RIGID);
        for (int i = 0; i < weight; i++) {
            templates.add(element);
        }
        if (raw != null) {
            raw.add(Pair.of(element, weight));
        }
    }
}
