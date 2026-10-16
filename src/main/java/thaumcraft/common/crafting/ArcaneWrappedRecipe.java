package thaumcraft.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Форменный или бесформенный рецепт магического верстака. Раскладку и результат разбирает ванильный
 * рецепт того же вида; здесь добавляются только цена в вис и исследование:
 * <pre>{ "type": "thaumcraft:arcane_shaped", "pattern": [...], "key": {...}, "result": {...},
 *   "aspects": { "ordo": 3 }, "research": "CAP_gold" }</pre>
 */
public class ArcaneWrappedRecipe implements ArcaneRecipe {
    private final CraftingRecipe inner;
    private final AspectList aspects;
    private final String research;
    private final Supplier<RecipeSerializer<?>> serializer;

    public ArcaneWrappedRecipe(CraftingRecipe inner, AspectList aspects, String research,
                               Supplier<RecipeSerializer<?>> serializer) {
        this.inner = inner;
        this.aspects = aspects;
        this.research = research;
        this.serializer = serializer;
    }

    /** Ванильный рецепт с раскладкой: по нему книга рисует сетку. */
    public CraftingRecipe getInner() {
        return inner;
    }

    @Override
    public AspectList getAspects(CraftingContainer container) {
        return aspects.copy();
    }

    @Override
    public String getResearch() {
        return research;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return inner.matches(container, level);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        return inner.assemble(container, registries);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return inner.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return inner.getResultItem(registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public ResourceLocation getId() {
        return inner.getId();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer.get();
    }

    /** Один и тот же код для обоих видов: отличается только ванильный разборщик раскладки. */
    public static class Serializer<T extends CraftingRecipe> implements RecipeSerializer<ArcaneWrappedRecipe> {
        private final RecipeSerializer<T> vanilla;
        private final Supplier<RecipeSerializer<?>> self;

        public Serializer(RecipeSerializer<T> vanilla, Supplier<RecipeSerializer<?>> self) {
            this.vanilla = vanilla;
            this.self = self;
        }

        @Override
        public ArcaneWrappedRecipe fromJson(ResourceLocation id, JsonObject json) {
            AspectList aspects = new AspectList();
            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "aspects").entrySet()) {
                Aspect aspect = Aspect.byTag(e.getKey());
                if (aspect == null) {
                    throw new IllegalArgumentException("Неизвестный аспект: " + e.getKey());
                }
                aspects.add(aspect, e.getValue().getAsInt());
            }
            return new ArcaneWrappedRecipe(vanilla.fromJson(id, json), aspects,
                    GsonHelper.getAsString(json, "research", ""), self);
        }

        @Override
        public ArcaneWrappedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            T inner = vanilla.fromNetwork(id, buf);
            return new ArcaneWrappedRecipe(inner, AspectList.read(buf), buf.readUtf(), self);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void toNetwork(FriendlyByteBuf buf, ArcaneWrappedRecipe recipe) {
            vanilla.toNetwork(buf, (T) recipe.inner);
            recipe.aspects.write(buf);
            buf.writeUtf(recipe.research);
        }
    }
}
