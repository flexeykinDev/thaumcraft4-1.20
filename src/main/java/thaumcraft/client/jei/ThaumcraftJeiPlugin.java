package thaumcraft.client.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.Thaumcraft;
import thaumcraft.api.wands.WandCap;
import thaumcraft.client.ClientItemAspects;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.crafting.ArcaneWrappedRecipe;
import thaumcraft.common.crafting.InfusionEnchantRecipe;
import thaumcraft.common.crafting.InfusionRecipe;
import thaumcraft.common.crafting.RunicAugmentRecipe;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModCreativeTabs;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI: рецепты магического верстака, тигля и наполнения, а также подсказки к вещам, которые делаются
 * не крафтом, а щелчком жезла (Таумономикон, магический верстак, тигель, стол исследований).
 * Класс грузится только при установленном JEI.
 */
@JeiPlugin
public class ThaumcraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = Thaumcraft.id("jei");
    /** Столько примеров центрального предмета показывается у зачарования наполнением. */
    private static final int ENCHANT_SAMPLES = 24;

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    /** Добавлены ли уже аспекты предметов: сервер присылает их отдельным пакетом, иногда уже после запуска JEI. */
    private static boolean itemAspectsAdded;

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        registration.register(AspectIngredient.TYPE, AspectIngredient.all(), new AspectIngredient.Helper(), new AspectIngredient.Renderer());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new ArcaneCategory(gui), new CrucibleCategory(gui), new InfusionCategory(gui),
                new AspectCategories.ItemAspectsCategory(gui), new AspectCategories.CombinationCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipes = Minecraft.getInstance().level.getRecipeManager();
        List<ArcaneWrappedRecipe> arcane = new ArrayList<>();
        for (ArcaneRecipe recipe : recipes.getAllRecipesFor(ModRecipes.ARCANE.get())) {
            // сборка жезла из частей — особый рецепт, его показывают страницы книги
            if (recipe instanceof ArcaneWrappedRecipe wrapped) {
                arcane.add(wrapped);
            }
        }
        registration.addRecipes(ArcaneCategory.TYPE, arcane);
        registration.addRecipes(CrucibleCategory.TYPE, recipes.getAllRecipesFor(ModRecipes.CRUCIBLE.get()));

        List<InfusionCategory.Shown> infusion = new ArrayList<>();
        for (InfusionRecipe recipe : recipes.getAllRecipesFor(ModRecipes.INFUSION.get())) {
            if (recipe instanceof RunicAugmentRecipe) {
                // усиление рунного щита подходит к любой броне и описано в книге
                continue;
            }
            if (recipe instanceof InfusionEnchantRecipe) {
                List<ItemStack> centres = new ArrayList<>();
                for (Item item : ForgeRegistries.ITEMS) {
                    ItemStack stack = new ItemStack(item);
                    if (recipe.centreMatches(stack)) {
                        centres.add(stack);
                        if (centres.size() >= ENCHANT_SAMPLES) {
                            break;
                        }
                    }
                }
                if (!centres.isEmpty()) {
                    infusion.add(new InfusionCategory.Shown(recipe, centres));
                }
            } else {
                infusion.add(new InfusionCategory.Shown(recipe, List.of()));
            }
        }
        registration.addRecipes(InfusionCategory.TYPE, infusion);
        registration.addRecipes(AspectCategories.CombinationCategory.TYPE, AspectCategories.combinations());
        List<AspectCategories.ItemAspects> itemAspects = AspectCategories.itemAspects();
        registration.addRecipes(AspectCategories.ItemAspectsCategory.TYPE, itemAspects);
        itemAspectsAdded = !itemAspects.isEmpty();

        info(registration, ModItems.THAUMONOMICON.get(), "thaumonomicon");
        info(registration, ModItems.TABLE.get(), "arcane_workbench");
        info(registration, ModItems.CRUCIBLE.get(), "crucible");
        info(registration, ModItems.SCRIBING_TOOLS.get(), "research_table");
        info(registration, ModItems.RESEARCH_NOTES.get(), "research_notes");
        info(registration, ModItems.INFUSION_MATRIX.get(), "infusion_altar");
        info(registration, ModItems.WAND.get(), "wand");
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable("jei.thaumcraft.info." + key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack wand = WandItem.create(ModItems.WAND.get(), WandRod.WOOD, WandCap.IRON, false);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.TABLE.get()), ArcaneCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, wand, ArcaneCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.CRUCIBLE.get()), CrucibleCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.ALCHEMICAL_CONSTRUCT.get()), CrucibleCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.INFUSION_MATRIX.get()), InfusionCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.PEDESTAL.get()), InfusionCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.THAUMOMETER.get()), AspectCategories.ItemAspectsCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.CRUCIBLE.get()), AspectCategories.ItemAspectsCategory.TYPE);
        registration.addRecipeCatalyst(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.SCRIBING_TOOLS.get()), AspectCategories.CombinationCategory.TYPE);
    }

    @Override
    public void onRuntimeUnavailable() {
        ClientItemAspects.listener = null;
        JeiDebug.runtime = null;
        itemAspectsAdded = false;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiDebug.runtime = runtime;
        ClientItemAspects.listener = () -> {
            if (!itemAspectsAdded) {
                List<AspectCategories.ItemAspects> itemAspects = AspectCategories.itemAspects();
                if (!itemAspects.isEmpty()) {
                    runtime.getRecipeManager().addRecipes(AspectCategories.ItemAspectsCategory.TYPE, itemAspects);
                    itemAspectsAdded = true;
                }
            }
        };
        if (!itemAspectsAdded) {
            ClientItemAspects.listener.run();
        }
        // олова, серебра и свинца в этой игре может не быть: тогда их вещей нет и в списке JEI, как во вкладке
        List<ItemStack> hidden = new ArrayList<>();
        for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
            if (!ModCreativeTabs.metalPresent(item.getId().getPath())) {
                hidden.add(new ItemStack(item.get()));
            }
        }
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
