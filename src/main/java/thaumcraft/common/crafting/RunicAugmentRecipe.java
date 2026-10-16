package thaumcraft.common.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModRecipes;
import thaumcraft.common.runic.RunicShield;

import java.util.ArrayList;
import java.util.List;

/**
 * «Рунное усиление» (InfusionRunicAugmentRecipe оригинала): наполнение, которое прибавляет вещи единицу рунного щита.
 * Годится любая вещь с рунной защитой, в том числе с нулевой — простая броня мага и заготовки украшений. Цена растёт
 * вместе с уже имеющимся щитом вещи: алмаз и по щепотке соли мира на каждую единицу плюс одна, а эссенции — вдвое
 * больше с каждой единицей.
 * <pre>{ "type": "thaumcraft:runic_augment", "research": "RUNICAUGMENTATION" }</pre>
 */
public class RunicAugmentRecipe extends InfusionRecipe {
    public RunicAugmentRecipe(ResourceLocation id, String research) {
        // значения для страницы книги: пример для вещи без собственного щита
        super(id, Ingredient.of(RunicShield.RUNIC_ARMOR), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DIAMOND),
                        Ingredient.of(ModItems.SALIS_MUNDUS.get())), aspectsFor(0), 5, example(), research);
    }

    private static ItemStack example() {
        ItemStack stack = new ItemStack(ModItems.GOGGLES.get());
        stack.getOrCreateTag().putByte("RS.HARDEN", (byte) 1);
        return stack;
    }

    private static AspectList aspectsFor(int charge) {
        AspectList out = new AspectList();
        int vis = (int) (32.0 * Math.pow(2.0, charge));
        if (vis > 0) {
            out.add(Aspect.byTag("tutamen"), vis / 2);
            out.add(Aspect.byTag("praecantatio"), vis / 2);
            out.add(Aspect.byTag("potentia"), vis);
        }
        return out;
    }

    @Override
    public boolean centreMatches(ItemStack centre) {
        return RunicShield.isRunicArmor(centre);
    }

    @Override
    public List<Ingredient> getComponents(ItemStack centre) {
        List<Ingredient> out = new ArrayList<>();
        out.add(Ingredient.of(Items.DIAMOND));
        out.add(Ingredient.of(ModItems.SALIS_MUNDUS.get()));
        for (int i = 0; i < RunicShield.getFinalCharge(centre); i++) {
            out.add(Ingredient.of(ModItems.SALIS_MUNDUS.get()));
        }
        return out;
    }

    @Override
    public AspectList getAspects(ItemStack centre) {
        return aspectsFor(RunicShield.getFinalCharge(centre));
    }

    @Override
    public int getInstability(ItemStack centre) {
        return 5 + RunicShield.getFinalCharge(centre) / 2;
    }

    @Override
    public ItemStack getResult(ItemStack centre) {
        ItemStack out = centre.copy();
        out.getOrCreateTag().putByte("RS.HARDEN", (byte) (RunicShield.getHardening(centre) + 1));
        return out;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RUNIC_AUGMENT_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<RunicAugmentRecipe> {
        @Override
        public RunicAugmentRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new RunicAugmentRecipe(id, GsonHelper.getAsString(json, "research", ""));
        }

        @Override
        public RunicAugmentRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new RunicAugmentRecipe(id, buf.readUtf());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RunicAugmentRecipe recipe) {
            buf.writeUtf(recipe.getResearch());
        }
    }
}
