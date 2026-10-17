package thaumcraft.common.aspects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.registry.ModRecipes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Аспекты предметов, блоков и существ. Читаются из датапаков: data/&lt;namespace&gt;/thaumcraft_aspects/*.json.
 * <p>
 * Для предметов запись на конкретный id важнее записи на тег; среди тегов побеждает объявленный позже.
 * Предмет без записи получает аспекты от ингредиентов своего рецепта (75%, делённые на размер выхода).
 */
public class ItemAspectManager extends SimpleJsonResourceReloadListener {
    // GSON объявлен первым: он нужен конструктору INSTANCE
    private static final Gson GSON = new GsonBuilder().create();

    public static final ItemAspectManager INSTANCE = new ItemAspectManager();

    private static final int ASPECT_CAP = 64;
    private static final int MAX_DEPTH = 100;

    /**
     * @param complex аспекты добавляются поверх выведенных из рецепта, а не заменяют их
     * @param base    "как у этого предмета, но...": берутся его аспекты, из них вычитается remove, затем добавляется aspects
     * @param remove  сколько чего убрать из аспектов базы; 0 — убрать аспект целиком
     */
    private record Entry(AspectList aspects, boolean complex, @Nullable ResourceLocation base, Map<Aspect, Integer> remove) {
    }

    private record TagEntry(TagKey<Item> tag, Entry entry) {
    }

    private record EntityEntry(@Nullable CompoundTag nbt, AspectList aspects) {
    }

    /** Рецепты одного предмета: с верстака и запасные (печь, камнерез). */
    private record Recipes(List<Recipe<?>> crafting, List<Recipe<?>> other, List<CrucibleRecipe> crucible) {
    }

    private Map<ResourceLocation, Entry> byItem = Map.of();
    private List<TagEntry> byTag = List.of();
    private Map<ResourceLocation, AspectList> byBlock = Map.of();
    private Map<ResourceLocation, List<EntityEntry>> byEntity = Map.of();

    private final Map<Item, AspectList> cache = new IdentityHashMap<>();
    @Nullable
    private Map<Item, Recipes> recipesByOutput;
    @Nullable
    private RecipeManager recipeManager;
    @Nullable
    private RegistryAccess registryAccess;

    private ItemAspectManager() {
        super(GSON, "thaumcraft_aspects");
    }

    /** Вызывается при каждой перезагрузке датапаков: рецепты нужны для вывода аспектов. */
    public void setContext(RecipeManager recipes, RegistryAccess registries) {
        synchronized (cache) {
            this.recipeManager = recipes;
            this.registryAccess = registries;
            this.recipesByOutput = null;
            cache.clear();
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Entry> items = new HashMap<>();
        List<TagEntry> tags = new ArrayList<>();
        Map<ResourceLocation, AspectList> blocks = new HashMap<>();
        Map<ResourceLocation, List<EntityEntry>> entities = new HashMap<>();

        // по алфавиту, чтобы приоритет записей не зависел от порядка обхода файлов
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(fileEntry -> {
            ResourceLocation file = fileEntry.getKey();
            JsonElement json = fileEntry.getValue();
            try {
                for (JsonElement element : GsonHelper.getAsJsonArray(json.getAsJsonObject(), "entries")) {
                    JsonObject object = element.getAsJsonObject();
                    AspectList aspects = readAspects(GsonHelper.getAsJsonObject(object, "aspects"));
                    if (object.has("block")) {
                        blocks.put(thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "block")), aspects);
                    } else if (object.has("entity")) {
                        CompoundTag nbt = object.has("nbt") ? TagParser.parseTag(GsonHelper.getAsString(object, "nbt")) : null;
                        entities.computeIfAbsent(thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "entity")),
                                k -> new ArrayList<>()).add(new EntityEntry(nbt, aspects));
                    } else {
                        Map<Aspect, Integer> remove = new HashMap<>();
                        if (object.has("remove")) {
                            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(object, "remove").entrySet()) {
                                remove.put(Aspect.byTag(e.getKey()), e.getValue().getAsInt());
                            }
                        }
                        Entry entry = new Entry(aspects, GsonHelper.getAsBoolean(object, "complex", false),
                                object.has("base") ? thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "base")) : null,
                                remove);
                        if (object.has("item")) {
                            items.put(thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "item")), entry);
                        } else {
                            ResourceLocation tag = thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "tag"));
                            tags.add(new TagEntry(TagKey.create(Registries.ITEM, tag), entry));
                        }
                    }
                }
            } catch (RuntimeException | CommandSyntaxException e) {
                Thaumcraft.LOGGER.error("Не удалось прочитать аспекты из {}", file, e);
            }
        });

        synchronized (cache) {
            byItem = items;
            byTag = tags;
            byBlock = blocks;
            byEntity = entities;
            recipesByOutput = null;
            cache.clear();
        }
        Thaumcraft.LOGGER.info("Аспекты: {} предметов, {} тегов, {} блоков, {} существ",
                items.size(), tags.size(), blocks.size(), entities.size());
    }

    private static AspectList readAspects(JsonObject json) {
        AspectList list = new AspectList();
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            Aspect aspect = Aspect.byTag(e.getKey());
            if (aspect == null) {
                throw new IllegalArgumentException("Неизвестный аспект: " + e.getKey());
            }
            list.add(aspect, e.getValue().getAsInt());
        }
        return list;
    }

    /** Полные аспекты конкретного предмета, с добавками за чары, тип инструмента и т.п. */
    public AspectList getAspects(ItemStack stack) {
        if (stack.isEmpty()) {
            return new AspectList();
        }
        AspectList aspects = AspectBonus.apply(stack, getBaseAspects(stack.getItem()));
        // эссенция в колбе и в банке тоже считается: её можно вылить в тигель или изучить
        Aspect phial = thaumcraft.common.items.PhialItem.getAspect(stack);
        if (phial != null) {
            aspects.add(phial, thaumcraft.common.items.PhialItem.SIZE);
        }
        Aspect wisp = thaumcraft.common.items.WispEssenceItem.getAspect(stack);
        if (wisp != null) {
            aspects.add(wisp, thaumcraft.common.items.WispEssenceItem.SIZE);
        }
        Aspect bean = thaumcraft.common.items.ManaBeanItem.getAspect(stack);
        if (bean != null) {
            aspects.add(bean, 1);
        }
        Aspect crystal = thaumcraft.common.items.CrystalEssenceItem.getAspect(stack);
        if (crystal != null) {
            aspects.add(crystal, 1);
        }
        Aspect jar = thaumcraft.common.items.JarBlockItem.getAspect(stack);
        if (jar != null) {
            aspects.add(jar, thaumcraft.common.items.JarBlockItem.getAmount(stack));
        }
        return aspects;
    }

    /** Базовые аспекты предмета: из таблицы или выведенные из рецепта. */
    public AspectList getBaseAspects(Item item) {
        synchronized (cache) {
            return resolve(item, new HashSet<>()).copy();
        }
    }

    /** Аспекты блока, у которого нет предмета (вода, огонь, порталы). */
    public AspectList getBlockAspects(Block block) {
        AspectList aspects = byBlock.get(ForgeRegistries.BLOCKS.getKey(block));
        return aspects == null ? new AspectList() : aspects.copy();
    }

    public AspectList getEntityAspects(Entity entity) {
        // у каждого вида печей свой набор
        if (entity instanceof thaumcraft.common.entity.Pech pech) {
            AspectList own = new AspectList().add(Aspect.MAN, 2).add(Aspect.MAGIC, pech.getPechType() == thaumcraft.common.entity.Pech.STALKER ? 4 : 2)
                    .add(Aspect.EXCHANGE, 2);
            if (pech.getPechType() == thaumcraft.common.entity.Pech.FORAGER) {
                own.add(Aspect.GREED, 2);
            } else if (pech.getPechType() == thaumcraft.common.entity.Pech.MAGE) {
                own.add(Aspect.WEAPON, 2);
            }
            return own;
        }
        // висп состоит из своего аспекта, магии и воздуха
        if (entity instanceof thaumcraft.common.entity.Wisp wisp) {
            AspectList own = new AspectList();
            if (wisp.getAspect() != null) {
                own.add(wisp.getAspect(), 2);
            }
            return own.add(Aspect.MAGIC, 1).add(Aspect.AIR, 1);
        }
        List<EntityEntry> entries = byEntity.get(EntityType.getKey(entity.getType()));
        if (entries == null) {
            return new AspectList();
        }
        AspectList result = new AspectList();
        CompoundTag data = null;
        // более поздние и более конкретные (с условием по NBT) записи перекрывают общие
        for (EntityEntry entry : entries) {
            if (entry.nbt() == null) {
                result = entry.aspects();
            } else {
                if (data == null) {
                    data = entity.saveWithoutId(new CompoundTag());
                }
                if (NbtUtils.compareNbt(entry.nbt(), data, true)) {
                    result = entry.aspects();
                }
            }
        }
        return result.copy();
    }

    private AspectList resolve(Item item, Set<Item> inProgress) {
        AspectList cached = cache.get(item);
        if (cached != null) {
            return cached;
        }
        if (item == Items.AIR || inProgress.size() >= MAX_DEPTH || !inProgress.add(item)) {
            return new AspectList();
        }

        Entry entry = byItem.get(ForgeRegistries.ITEMS.getKey(item));
        if (entry == null) {
            for (int i = byTag.size() - 1; i >= 0; i--) {
                if (item.builtInRegistryHolder().is(byTag.get(i).tag())) {
                    entry = byTag.get(i).entry();
                    break;
                }
            }
        }

        AspectList result;
        if (entry != null && entry.base() != null) {
            result = resolve(ForgeRegistries.ITEMS.getValue(entry.base()), inProgress).copy();
            entry.remove().forEach((aspect, amount) -> {
                if (amount <= 0 || !result.reduce(aspect, amount)) {
                    result.remove(aspect);
                }
            });
            result.add(entry.aspects());
        } else if (entry != null && !entry.complex()) {
            result = entry.aspects().copy();
        } else {
            result = fromRecipes(item, inProgress);
            if (entry != null) {
                result.add(entry.aspects());
            }
        }
        AspectList capped = cap(result);

        inProgress.remove(item);
        cache.put(item, capped);
        return capped;
    }

    private AspectList fromRecipes(Item item, Set<Item> inProgress) {
        Recipes recipes = recipesFor(item);
        if (recipes == null) {
            return new AspectList();
        }
        // как в оригинале, рецепт тигля важнее всех прочих
        if (!recipes.crucible().isEmpty()) {
            return fromCrucible(recipes.crucible().get(0), inProgress);
        }
        // порядок оригинала: магический верстак, затем инфузия, затем обычный верстак
        List<Recipe<?>> arcane = new ArrayList<>();
        List<Recipe<?>> infusion = new ArrayList<>();
        List<Recipe<?>> plain = new ArrayList<>();
        for (Recipe<?> recipe : recipes.crafting()) {
            (recipe instanceof thaumcraft.common.crafting.ArcaneRecipe ? arcane
                    : recipe instanceof thaumcraft.common.crafting.InfusionRecipe ? infusion : plain).add(recipe);
        }
        AspectList result = cheapest(arcane, inProgress);
        if (result.isEmpty()) {
            result = cheapest(infusion, inProgress);
        }
        if (result.isEmpty()) {
            result = cheapest(plain, inProgress);
        }
        // В оригинале учитывался только верстак; печь и камнерез — запасной путь для блоков новых версий
        return result.isEmpty() ? cheapest(recipes.other(), inProgress) : result;
    }

    /** Аспекты катализатора плюс корень из каждого аспекта рецепта, делённый на число предметов на выходе. */
    private AspectList fromCrucible(CrucibleRecipe recipe, Set<Item> inProgress) {
        AspectList out = new AspectList();
        for (ItemStack option : recipe.getCatalyst().getItems()) {
            AspectList aspects = resolve(option.getItem(), inProgress);
            if (!aspects.isEmpty()) {
                out.add(aspects);
                break;
            }
        }
        AspectList cost = recipe.getAspects();
        int count = Math.max(1, recipe.getResult().getCount());
        for (Aspect aspect : cost.getAspects()) {
            out.add(aspect, (int) (Math.sqrt(cost.getAmount(aspect)) / count));
        }
        return out;
    }

    /** Из рецептов берётся тот, что даёт наименьшую ненулевую сумму аспектов, как в оригинале. */
    private AspectList cheapest(List<Recipe<?>> recipes, Set<Item> inProgress) {
        AspectList best = new AspectList();
        int bestSize = Integer.MAX_VALUE;
        for (Recipe<?> recipe : recipes) {
            AspectList total = new AspectList();
            for (Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack option : ingredient.getItems()) {
                    AspectList aspects = resolve(option.getItem(), inProgress);
                    if (!aspects.isEmpty()) {
                        total.add(aspects);
                        break;
                    }
                }
            }
            int count = Math.max(1, recipe.getResultItem(registryAccess).getCount());
            AspectList scaled = new AspectList();
            for (Aspect aspect : total.getAspects()) {
                scaled.add(aspect, (int) (total.getAmount(aspect) * 0.75f / count));
            }
            // цена самого рецепта — вис магического верстака или эссенция инфузии — тоже остаётся в предмете:
            // корень из количества, делённый на число предметов на выходе
            AspectList cost = recipe instanceof thaumcraft.common.crafting.ArcaneWrappedRecipe wrapped ? wrapped.getAspects(null)
                    : recipe instanceof thaumcraft.common.crafting.InfusionRecipe infused ? infused.getAspects() : null;
            if (cost != null) {
                for (Aspect aspect : cost.getAspects()) {
                    int extra = (int) (Math.sqrt(cost.getAmount(aspect)) / count);
                    if (extra > 0) {
                        scaled.add(aspect, extra);
                    }
                }
            }
            int size = scaled.visSize();
            if (size > 0 && size < bestSize) {
                best = scaled;
                bestSize = size;
            }
        }
        return best;
    }

    @Nullable
    private Recipes recipesFor(Item item) {
        if (recipeManager == null || registryAccess == null) {
            return null;
        }
        if (recipesByOutput == null) {
            List<Recipe<?>> sorted = new ArrayList<>(recipeManager.getRecipes());
            sorted.sort(Comparator.comparing(r -> r.getId().toString()));

            Map<Item, Recipes> index = new IdentityHashMap<>();
            for (Recipe<?> recipe : sorted) {
                RecipeType<?> type = recipe.getType();
                boolean crafting = type == RecipeType.CRAFTING || type == ModRecipes.ARCANE.get()
                        || recipe.getClass() == thaumcraft.common.crafting.InfusionRecipe.class;
                if (recipe instanceof CrucibleRecipe crucible) {
                    // удвоение (катализатор тот же, что результат) об аспектах предмета ничего не говорит
                    if (!crucible.getCatalyst().test(crucible.getResult())) {
                        index.computeIfAbsent(crucible.getResult().getItem(),
                                k -> new Recipes(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()))
                                .crucible().add(crucible);
                    }
                    continue;
                }
                boolean infusion = recipe.getClass() == thaumcraft.common.crafting.InfusionRecipe.class;
                // магические рецепты помечены «особыми» только ради книги рецептов — аспекты по ним считаются как прежде
                boolean special = recipe.isSpecial() && !(recipe instanceof thaumcraft.common.crafting.ArcaneWrappedRecipe);
                if (special && !infusion || !crafting && type != RecipeType.SMELTING && type != RecipeType.STONECUTTING) {
                    continue;
                }
                ItemStack result = recipe.getResultItem(registryAccess);
                if (result.isEmpty()) {
                    continue;
                }
                Recipes recipes = index.computeIfAbsent(result.getItem(),
                        k -> new Recipes(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
                (crafting ? recipes.crafting() : recipes.other()).add(recipe);
            }
            recipesByOutput = index;
        }
        return recipesByOutput.get(item);
    }

    private static AspectList cap(AspectList aspects) {
        AspectList out = new AspectList();
        for (Aspect aspect : aspects.getAspects()) {
            out.add(aspect, Math.min(ASPECT_CAP, aspects.getAmount(aspect)));
        }
        return out;
    }

    /** Базовые аспекты всех зарегистрированных предметов, у которых они есть. Используется для отправки клиенту. */
    public Map<Item, AspectList> resolveAll() {
        Map<Item, AspectList> out = new IdentityHashMap<>();
        for (Item item : ForgeRegistries.ITEMS) {
            AspectList aspects = getBaseAspects(item);
            if (!aspects.isEmpty()) {
                out.put(item, aspects);
            }
        }
        return out;
    }
}
