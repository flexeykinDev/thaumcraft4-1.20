package thaumcraft.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registry.ModRecipes;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.Map;

/**
 * Рецепт тигля — перенос CrucibleRecipe оригинала: предмет-катализатор, брошенный в тигель, где уже растворено
 * достаточно аспектов, превращается в результат.
 * <pre>{ "type": "thaumcraft:crucible", "catalyst": { "item": "minecraft:iron_ingot" },
 *   "aspects": { "praecantatio": 4 }, "result": { "item": "thaumcraft:thaumium_ingot" }, "research": "THAUMIUM" }</pre>
 */
public class CrucibleRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient catalyst;
    private final AspectList aspects;
    private final ItemStack result;
    private final String research;

    public CrucibleRecipe(ResourceLocation id, Ingredient catalyst, AspectList aspects, ItemStack result, String research) {
        this.id = id;
        this.catalyst = catalyst;
        this.aspects = aspects;
        this.result = result;
        this.research = research;
    }

    public Ingredient getCatalyst() {
        return catalyst;
    }

    public AspectList getAspects() {
        return aspects.copy();
    }

    public ItemStack getResult() {
        return result.copy();
    }

    /** Исследование, без которого рецепт не сработает; пустая строка — доступен всем. */
    public String getResearch() {
        return research;
    }

    /** Подходит ли катализатор и хватает ли в тигле каждого аспекта. */
    public boolean matches(AspectList dissolved, ItemStack stack) {
        if (!catalyst.test(stack)) {
            return false;
        }
        for (Aspect aspect : aspects.getAspects()) {
            if (dissolved.getAmount(aspect) < aspects.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Как в оригинале: из подходящих и изученных игроком рецептов берётся тот, где больше разных аспектов.
     *
     * @param knowledge знания бросившего предмет; null — никто не бросал (воронка, раздатчик), рецептов нет
     */
    @Nullable
    public static CrucibleRecipe find(Level level, @Nullable PlayerKnowledge knowledge, AspectList dissolved, ItemStack stack) {
        if (knowledge == null) {
            return null;
        }
        CrucibleRecipe best = null;
        int highest = 0;
        for (CrucibleRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get())) {
            if (recipe.aspects.size() > highest && ArcaneRecipe.isResearched(recipe.research, knowledge)
                    && recipe.matches(dissolved, stack)) {
                highest = recipe.aspects.size();
                best = recipe;
            }
        }
        return best;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, catalyst);
    }

    /** Чтобы книга рецептов ванили не пыталась показывать такие рецепты. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CRUCIBLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.CRUCIBLE.get();
    }

    public static class Serializer implements RecipeSerializer<CrucibleRecipe> {
        @Override
        public CrucibleRecipe fromJson(ResourceLocation id, JsonObject json) {
            AspectList aspects = new AspectList();
            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "aspects").entrySet()) {
                Aspect aspect = Aspect.byTag(e.getKey());
                if (aspect == null) {
                    throw new IllegalArgumentException("Неизвестный аспект: " + e.getKey());
                }
                aspects.add(aspect, e.getValue().getAsInt());
            }
            return new CrucibleRecipe(id, Ingredient.fromJson(json.get("catalyst")), aspects,
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")),
                    GsonHelper.getAsString(json, "research", ""));
        }

        @Override
        public CrucibleRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new CrucibleRecipe(id, Ingredient.fromNetwork(buf), AspectList.read(buf), buf.readItem(), buf.readUtf());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, CrucibleRecipe recipe) {
            recipe.catalyst.toNetwork(buf);
            recipe.aspects.write(buf);
            buf.writeItem(recipe.result);
            buf.writeUtf(recipe.research);
        }
    }
}
