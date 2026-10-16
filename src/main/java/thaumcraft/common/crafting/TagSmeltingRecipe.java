package thaumcraft.common.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import thaumcraft.common.registry.ModRecipes;

/**
 * Переплавка, результат которой задан тегом (FurnaceRecipes оригинала брал первый слиток из словаря руд): кластер
 * олова, серебра или свинца плавится в два слитка того мода, что дал металл. Предпочтение — предметам Minecraft,
 * потом по алфавиту, чтобы результат не зависел от порядка загрузки модов.
 */
public class TagSmeltingRecipe extends SmeltingRecipe {
    private final ResourceLocation resultTag;
    private final int count;

    public TagSmeltingRecipe(ResourceLocation id, Ingredient ingredient, ResourceLocation resultTag, int count, float experience, int time) {
        super(id, "", CookingBookCategory.MISC, ingredient, ItemStack.EMPTY, experience, time);
        this.resultTag = resultTag;
        this.count = count;
    }

    private ItemStack result() {
        Item best = null;
        String bestKey = null;
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, resultTag))) {
            String key = BuiltInRegistries.ITEM.getKey(holder.value()).toString();
            String sort = (key.startsWith("minecraft:") ? "0" : "1") + key;
            if (bestKey == null || sort.compareTo(bestKey) < 0) {
                bestKey = sort;
                best = holder.value();
            }
        }
        return best == null ? ItemStack.EMPTY : new ItemStack(best, count);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result();
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TAG_SMELTING.get();
    }

    public static class Serializer implements RecipeSerializer<TagSmeltingRecipe> {
        @Override
        public TagSmeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new TagSmeltingRecipe(id, Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient")),
                    thaumcraft.common.Res.parse(GsonHelper.getAsString(json, "result_tag")), GsonHelper.getAsInt(json, "count", 1),
                    GsonHelper.getAsFloat(json, "experience", 0f), GsonHelper.getAsInt(json, "cookingtime", 200));
        }

        @Override
        public TagSmeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new TagSmeltingRecipe(id, Ingredient.fromNetwork(buf), buf.readResourceLocation(), buf.readVarInt(), buf.readFloat(), buf.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TagSmeltingRecipe recipe) {
            recipe.getIngredients().get(0).toNetwork(buf);
            buf.writeResourceLocation(recipe.resultTag);
            buf.writeVarInt(recipe.count);
            buf.writeFloat(recipe.getExperience());
            buf.writeVarInt(recipe.getCookingTime());
        }
    }
}
