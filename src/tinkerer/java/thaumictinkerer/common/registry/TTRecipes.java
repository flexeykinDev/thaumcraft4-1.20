package thaumictinkerer.common.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumictinkerer.ThaumicTinkerer;
import thaumictinkerer.common.item.SpellClothItem;

/** Особые рецепты Thaumic Tinkerer. */
public final class TTRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ThaumicTinkerer.MODID);

    public static final RegistryObject<RecipeSerializer<SpellClothItem.Disenchant>> SPELL_CLOTH = SERIALIZERS.register("spell_cloth",
            () -> new SimpleCraftingRecipeSerializer<>(SpellClothItem.Disenchant::new));

    private TTRecipes() {
    }
}
