package thaumcraft.common.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.registries.ForgeRegistries;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registry.ModRecipes;

import java.util.Map;

/**
 * Зачарование наполнением (InfusionEnchantmentRecipe оригинала): вещь на центральном пьедестале получает следующий
 * уровень зачарования. Каждый новый уровень дороже: эссенции нужно во столько раз больше, каков уже имеющийся
 * уровень (плюс десятая доля за каждый уровень прочих зачарований), нестабильность растёт с общим числом уровней,
 * а кроме эссенции алтарь забирает уровни опыта у стоящих рядом.
 */
public class InfusionEnchantRecipe extends InfusionRecipe {
    private final Enchantment enchantment;
    private final AspectList baseAspects;
    private final int baseInstability;
    private final NonNullList<Ingredient> ingredients;

    public InfusionEnchantRecipe(ResourceLocation id, Enchantment enchantment, int instability, AspectList aspects,
                                 NonNullList<Ingredient> components, String research) {
        super(id, Ingredient.EMPTY, components, aspects, instability,
                EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, 1)), research);
        this.enchantment = enchantment;
        this.baseAspects = aspects;
        this.baseInstability = instability;
        this.ingredients = components;
    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    /** Сколько уровней опыта стоит первый уровень зачарования: треть его «цены» на столе зачарований, но не меньше одного. */
    public int getBaseXp() {
        return Math.max(1, enchantment.getMinCost(1) / 3);
    }

    /** Годится инструмент, оружие или доспех (не в стопке и с прочностью), на который это зачарование вообще ложится. */
    @Override
    public boolean centreMatches(ItemStack centre) {
        if (centre.isEmpty() || centre.getMaxStackSize() != 1 || !centre.isDamageableItem() || !enchantment.canEnchant(centre)) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> present : EnchantmentHelper.getEnchantments(centre).entrySet()) {
            Enchantment other = present.getKey();
            if (other == enchantment) {
                if (present.getValue() >= enchantment.getMaxLevel()) {
                    return false;
                }
            } else if (!enchantment.isCompatibleWith(other) || !other.isCompatibleWith(enchantment)) {
                return false;
            }
        }
        return true;
    }

    private float essentiaMod(ItemStack centre) {
        float mod = 0f;
        for (Map.Entry<Enchantment, Integer> present : EnchantmentHelper.getEnchantments(centre).entrySet()) {
            mod += present.getKey() == enchantment ? present.getValue() : present.getValue() * 0.1f;
        }
        return mod;
    }

    @Override
    public AspectList getAspects(ItemStack centre) {
        float mod = essentiaMod(centre);
        AspectList out = new AspectList();
        for (Aspect aspect : baseAspects.getAspects()) {
            int amount = baseAspects.getAmount(aspect);
            out.add(aspect, amount + (int) (amount * mod));
        }
        return out;
    }

    @Override
    public int getInstability(ItemStack centre) {
        int levels = 0;
        for (int level : EnchantmentHelper.getEnchantments(centre).values()) {
            levels += level;
        }
        return levels / 2 + baseInstability;
    }

    @Override
    public int getXpCost(ItemStack centre) {
        return getBaseXp() * (1 + EnchantmentHelper.getTagEnchantmentLevel(enchantment, centre));
    }

    @Override
    public ItemStack getResult(ItemStack centre) {
        ItemStack out = centre.copy();
        Map<Enchantment, Integer> all = EnchantmentHelper.getEnchantments(out);
        all.put(enchantment, all.getOrDefault(enchantment, 0) + 1);
        EnchantmentHelper.setEnchantments(all, out);
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INFUSION_ENCHANT_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<InfusionEnchantRecipe> {
        @Override
        public InfusionEnchantRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation name = thaumcraft.common.Res.parse(GsonHelper.getAsString(json, "enchantment"));
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(name);
            if (enchantment == null) {
                throw new IllegalArgumentException("Неизвестное зачарование: " + name);
            }
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
            return new InfusionEnchantRecipe(id, enchantment, GsonHelper.getAsInt(json, "instability", 0), aspects, components,
                    GsonHelper.getAsString(json, "research", ""));
        }

        @Override
        public InfusionEnchantRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(buf.readResourceLocation());
            int instability = buf.readVarInt();
            AspectList aspects = AspectList.read(buf);
            NonNullList<Ingredient> components = NonNullList.withSize(buf.readVarInt(), Ingredient.EMPTY);
            components.replaceAll(ignored -> Ingredient.fromNetwork(buf));
            return new InfusionEnchantRecipe(id, enchantment, instability, aspects, components, buf.readUtf());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, InfusionEnchantRecipe recipe) {
            buf.writeResourceLocation(ForgeRegistries.ENCHANTMENTS.getKey(recipe.enchantment));
            buf.writeVarInt(recipe.baseInstability);
            recipe.baseAspects.write(buf);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient component : recipe.ingredients) {
                component.toNetwork(buf);
            }
            buf.writeUtf(recipe.getResearch());
        }
    }
}
