package thaumcraft.common.crafting;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registry.ModRecipes;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchRegistry;

/**
 * Рецепт магического верстака: обычная сетка 3x3 плюс цена в вис, которую платит жезл.
 * Все такие рецепты одного типа, чтобы обычный верстак их не видел.
 */
public interface ArcaneRecipe extends Recipe<CraftingContainer> {

    /** Цена в целых единицах вис для данной раскладки (у сборки жезла она зависит от частей). */
    AspectList getAspects(CraftingContainer container);

    /** Исследование, без которого рецепт недоступен; пустая строка — доступен всем. */
    String getResearch();

    /** Изучил ли игрок всё, что нужно для этого рецепта при данной раскладке. */
    default boolean isUnlocked(CraftingContainer container, PlayerKnowledge knowledge) {
        return isResearched(getResearch(), knowledge);
    }

    /** Исследование, которого нет в книге (например, убрано датапаком), ничего не запирает. */
    static boolean isResearched(String research, PlayerKnowledge knowledge) {
        return research.isEmpty() || ResearchRegistry.get(research) == null || knowledge.isResearchComplete(research);
    }

    @Override
    default RecipeType<?> getType() {
        return ModRecipes.ARCANE.get();
    }
}
