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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Рецепт инфузии — перенос InfusionRecipe оригинала: предмет на центральном пьедестале, набор предметов
 * на пьедесталах вокруг (порядок не важен) и эссенция из банок поблизости.
 * <pre>{ "type": "thaumcraft:infusion", "central": {...}, "components": [{...}, ...],
 *   "aspects": { "potentia": 12 }, "instability": 5, "result": {...}, "research": "CAP_thaumium" }</pre>
 */
public class InfusionRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient central;
    private final NonNullList<Ingredient> components;
    private final AspectList aspects;
    private final int instability;
    private final ItemStack result;
    private final String research;

    public InfusionRecipe(ResourceLocation id, Ingredient central, NonNullList<Ingredient> components, AspectList aspects,
                          int instability, ItemStack result, String research) {
        this.id = id;
        this.central = central;
        this.components = components;
        this.aspects = aspects;
        this.instability = instability;
        this.result = result;
        this.research = research;
    }

    public Ingredient getCentral() {
        return central;
    }

    public List<Ingredient> getComponents() {
        return components;
    }

    public AspectList getAspects() {
        return aspects.copy();
    }

    public int getInstability() {
        return instability;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public String getResearch() {
        return research;
    }

    // ---------- то же с оглядкой на предмет в центре: у обычного рецепта ответ от него не зависит ----------

    public boolean centreMatches(ItemStack centre) {
        return central.test(centre);
    }

    public List<Ingredient> getComponents(ItemStack centre) {
        return components;
    }

    public AspectList getAspects(ItemStack centre) {
        return getAspects();
    }

    public int getInstability(ItemStack centre) {
        return instability;
    }

    public ItemStack getResult(ItemStack centre) {
        return getResult();
    }

    /** Сколько уровней опыта алтарь заберёт у стоящих рядом; у обычных рецептов — нисколько. */
    public int getXpCost(ItemStack centre) {
        return 0;
    }

    /** Центральный предмет подходит, а предметы вокруг — ровно те, что нужны, без лишних. */
    public boolean matches(ItemStack centre, List<ItemStack> around) {
        List<Ingredient> components = getComponents(centre);
        if (!centreMatches(centre) || around.size() != components.size()) {
            return false;
        }
        List<ItemStack> left = new ArrayList<>(around);
        for (Ingredient component : components) {
            boolean found = false;
            for (int i = 0; i < left.size(); i++) {
                if (component.test(left.get(i))) {
                    left.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static InfusionRecipe find(Level level, PlayerKnowledge knowledge, ItemStack centre, List<ItemStack> around) {
        for (InfusionRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.INFUSION.get())) {
            if (ArcaneRecipe.isResearched(recipe.research, knowledge) && recipe.matches(centre, around)) {
                return recipe;
            }
        }
        return null;
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
        NonNullList<Ingredient> all = NonNullList.create();
        all.add(central);
        all.addAll(components);
        return all;
    }

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
        return ModRecipes.INFUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.INFUSION.get();
    }

    public static class Serializer implements RecipeSerializer<InfusionRecipe> {
        @Override
        public InfusionRecipe fromJson(ResourceLocation id, JsonObject json) {
            AspectList aspects = new AspectList();
            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "aspects").entrySet()) {
                Aspect aspect = Aspect.byTag(e.getKey());
                if (aspect == null) {
                    throw new IllegalArgumentException("Неизвестный аспект: " + e.getKey());
                }
                aspects.add(aspect, e.getValue().getAsInt());
            }
            NonNullList<Ingredient> components = NonNullList.create();
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "components")) {
                components.add(Ingredient.fromJson(element));
            }
            return new InfusionRecipe(id, Ingredient.fromJson(json.get("central")), components, aspects,
                    GsonHelper.getAsInt(json, "instability", 0),
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")),
                    GsonHelper.getAsString(json, "research", ""));
        }

        @Override
        public InfusionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient central = Ingredient.fromNetwork(buf);
            NonNullList<Ingredient> components = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
            components.replaceAll(ignored -> Ingredient.fromNetwork(buf));
            return new InfusionRecipe(id, central, components, AspectList.read(buf), buf.readVarInt(), buf.readItem(), buf.readUtf());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusionRecipe recipe) {
            recipe.central.toNetwork(buf);
            buf.writeVarInt(recipe.components.size());
            for (Ingredient component : recipe.components) {
                component.toNetwork(buf);
            }
            recipe.aspects.write(buf);
            buf.writeVarInt(recipe.instability);
            buf.writeItem(recipe.result);
            buf.writeUtf(recipe.research);
        }
    }
}
