package thaumcraft.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;

import java.util.List;

/** Для самосъёмки: открыть окно рецептов JEI и сосчитать рецепты категорий. Вызывается только при установленном JEI. */
public final class JeiDebug {
    static IJeiRuntime runtime;

    public static boolean ready() {
        return runtime != null;
    }

    public static long count(String category) {
        return runtime.getRecipeManager().createRecipeLookup(type(category)).get().count();
    }

    public static void show(String category) {
        runtime.getRecipesGui().showTypes(List.of(type(category)));
    }

    /** Что делается из предмета и как его получить, включая страницу-подсказку (клавиша R над предметом). */
    public static void lookup(ItemStack stack) {
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory()
                .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
    }

    public static long countItemAspects() {
        return runtime.getRecipeManager().createRecipeLookup(AspectCategories.ItemAspectsCategory.TYPE).get().count();
    }

    /** Где взять аспект (клавиша R над ним). */
    public static void aspectSources(String tag) {
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,
                AspectIngredient.TYPE, new AspectIngredient.AspectStack(thaumcraft.api.aspects.Aspect.byTag(tag), 0)));
    }

    /** На что аспект тратится (клавиша U над ним). */
    public static void aspectUses(String tag) {
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT,
                AspectIngredient.TYPE, new AspectIngredient.AspectStack(thaumcraft.api.aspects.Aspect.byTag(tag), 0)));
    }

    public static boolean hidden(ItemStack stack) {
        return runtime.getIngredientManager().getAllItemStacks().stream().noneMatch(other -> ItemStack.isSameItem(other, stack));
    }

    private static RecipeType<?> type(String category) {
        return switch (category) {
            case "arcane" -> ArcaneCategory.TYPE;
            case "crucible" -> CrucibleCategory.TYPE;
            default -> InfusionCategory.TYPE;
        };
    }

    private JeiDebug() {
    }

    static void log(String text) {
        Thaumcraft.LOGGER.info("JEI {}", text);
    }
}
