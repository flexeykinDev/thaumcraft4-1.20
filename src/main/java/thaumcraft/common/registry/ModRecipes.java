package thaumcraft.common.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.crafting.ArcaneWandRecipe;
import thaumcraft.common.crafting.ArcaneWrappedRecipe;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.crafting.InfusionRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, Thaumcraft.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Thaumcraft.MODID);

    /** Все рецепты магического верстака. */
    public static final RegistryObject<RecipeType<ArcaneRecipe>> ARCANE =
            TYPES.register("arcane", () -> RecipeType.simple(Thaumcraft.id("arcane")));

    public static final RegistryObject<RecipeSerializer<?>> ARCANE_SHAPED = SERIALIZERS.register("arcane_shaped",
            () -> new ArcaneWrappedRecipe.Serializer<>(RecipeSerializer.SHAPED_RECIPE, () -> ModRecipes.ARCANE_SHAPED.get()));
    public static final RegistryObject<RecipeSerializer<?>> ARCANE_SHAPELESS = SERIALIZERS.register("arcane_shapeless",
            () -> new ArcaneWrappedRecipe.Serializer<>(RecipeSerializer.SHAPELESS_RECIPE, () -> ModRecipes.ARCANE_SHAPELESS.get()));
    public static final RegistryObject<RecipeSerializer<?>> ARCANE_WAND =
            SERIALIZERS.register("arcane_wand", ArcaneWandRecipe.Serializer::new);

    /** Рецепты тигля. */
    public static final RegistryObject<RecipeType<CrucibleRecipe>> CRUCIBLE =
            TYPES.register("crucible", () -> RecipeType.simple(Thaumcraft.id("crucible")));
    public static final RegistryObject<RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER =
            SERIALIZERS.register("crucible", CrucibleRecipe.Serializer::new);

    /** Рецепты инфузии. */
    public static final RegistryObject<RecipeType<InfusionRecipe>> INFUSION =
            TYPES.register("infusion", () -> RecipeType.simple(Thaumcraft.id("infusion")));
    public static final RegistryObject<RecipeSerializer<InfusionRecipe>> INFUSION_SERIALIZER =
            SERIALIZERS.register("infusion", InfusionRecipe.Serializer::new);

    /** «Рунное усиление»: наполнение, цена которого зависит от вещи в центре. */
    public static final RegistryObject<RecipeSerializer<thaumcraft.common.crafting.RunicAugmentRecipe>> RUNIC_AUGMENT_SERIALIZER =
            SERIALIZERS.register("runic_augment", thaumcraft.common.crafting.RunicAugmentRecipe.Serializer::new);

    /** Наполнение, добавляющее предмету в центре метку: продвинутый голем и подобное. */
    public static final RegistryObject<RecipeSerializer<thaumcraft.common.crafting.InfusionNbtRecipe>> INFUSION_NBT_SERIALIZER =
            SERIALIZERS.register("infusion_nbt", thaumcraft.common.crafting.InfusionNbtRecipe.Serializer::new);
    /** Переплавка с результатом по тегу: кластеры олова, серебра и свинца — в слитки того мода, что дал металл. */
    public static final RegistryObject<RecipeSerializer<?>> TAG_SMELTING =
            SERIALIZERS.register("tag_smelting", thaumcraft.common.crafting.TagSmeltingRecipe.Serializer::new);

    /** Зачарование наполнением: следующий уровень зачарования на вещи в центре алтаря. */
    public static final RegistryObject<RecipeSerializer<thaumcraft.common.crafting.InfusionEnchantRecipe>> INFUSION_ENCHANT_SERIALIZER =
            SERIALIZERS.register("infusion_enchantment", thaumcraft.common.crafting.InfusionEnchantRecipe.Serializer::new);

    /** Ярлык с аспектом из чистого ярлыка и колбы эссенции. */
    public static final RegistryObject<RecipeSerializer<thaumcraft.common.crafting.LabelRecipe>> LABEL = SERIALIZERS.register("label",
            () -> new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(thaumcraft.common.crafting.LabelRecipe::new));

    private ModRecipes() {
    }
}
