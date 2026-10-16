package thaumcraft.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registry.ModRecipes;

import java.util.Map;

/**
 * Наполнение, которое не превращает предмет в другой, а добавляет ему метку (рецепты оригинала с результатом вида
 * {"advanced", 1}): продвинутый голем, шлем с очками, маски. Предмет в центре остаётся собой со всеми своими данными.
 */
public class InfusionNbtRecipe extends InfusionRecipe {
    private final CompoundTag nbt;
    private final Ingredient centre;
    private final NonNullList<Ingredient> ingredients;

    public InfusionNbtRecipe(ResourceLocation id, Ingredient central, NonNullList<Ingredient> components, AspectList aspects, int instability,
                             CompoundTag nbt, String research) {
        // образец результата здесь не вычисляется: рецепты читаются раньше тегов, и ингредиент-тег запомнил бы себя пустым
        super(id, central, components, aspects, instability, ItemStack.EMPTY, research);
        this.nbt = nbt;
        this.centre = central;
        this.ingredients = components;
    }

    /** Образец результата для книги: первый подходящий предмет с добавленной меткой. */
    @Override
    public ItemStack getResult() {
        ItemStack[] items = centre.getItems();
        if (items.length == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = items[0].copy();
        out.getOrCreateTag().merge(nbt);
        return out;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registries) {
        return getResult();
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registries) {
        return getResult();
    }

    /** Предмет подходит, только если метки на нём ещё нет. */
    @Override
    public boolean centreMatches(ItemStack stack) {
        return centre.test(stack) && !(stack.hasTag() && NbtUtils.compareNbt(nbt, stack.getTag(), true));
    }

    @Override
    public ItemStack getResult(ItemStack stack) {
        ItemStack out = stack.copy();
        out.getOrCreateTag().merge(nbt);
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSION_NBT_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<InfusionNbtRecipe> {
        @Override
        public InfusionNbtRecipe fromJson(ResourceLocation id, JsonObject json) {
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
            CompoundTag nbt;
            try {
                nbt = TagParser.parseTag(GsonHelper.getAsString(json, "nbt"));
            } catch (CommandSyntaxException bad) {
                throw new IllegalArgumentException("Не разобрать метку рецепта " + id + ": " + bad.getMessage());
            }
            return new InfusionNbtRecipe(id, Ingredient.fromJson(json.get("central")), components, aspects,
                    GsonHelper.getAsInt(json, "instability", 0), nbt, GsonHelper.getAsString(json, "research", ""));
        }

        @Override
        public InfusionNbtRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient central = Ingredient.fromNetwork(buf);
            NonNullList<Ingredient> components = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
            components.replaceAll(ignored -> Ingredient.fromNetwork(buf));
            AspectList aspects = AspectList.read(buf);
            int instability = buf.readVarInt();
            CompoundTag nbt = buf.readNbt();
            return new InfusionNbtRecipe(id, central, components, aspects, instability, nbt == null ? new CompoundTag() : nbt, buf.readUtf());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusionNbtRecipe recipe) {
            recipe.centre.toNetwork(buf);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient component : recipe.ingredients) {
                component.toNetwork(buf);
            }
            recipe.getAspects().write(buf);
            buf.writeVarInt(recipe.getInstability());
            buf.writeNbt(recipe.nbt);
            buf.writeUtf(recipe.getResearch());
        }
    }
}
