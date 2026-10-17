package thaumcraft.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import thaumcraft.common.crafting.ArcaneWrappedRecipe;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Магический верстак: сетка 3x3 как у обычного верстака, ниже — цена в вис и нужное исследование. */
class ArcaneCategory implements IRecipeCategory<ArcaneWrappedRecipe> {
    static final RecipeType<ArcaneWrappedRecipe> TYPE = RecipeType.create("thaumcraft", "arcane", ArcaneWrappedRecipe.class);
    private static final int ASPECTS_Y = 60;

    private final IDrawable background;
    private final IDrawable grid;
    private final IDrawable icon;
    private final ICraftingGridHelper gridHelper;

    ArcaneCategory(IGuiHelper gui) {
        background = gui.createBlankDrawable(116, 98);
        // сетка, стрелка и ячейка результата из окна обычного верстака
        grid = gui.createDrawable(JeiDraw.CRAFTING, 29, 16, 116, 54);
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.TABLE.get()));
        gridHelper = gui.createCraftingGridHelper();
    }

    @Override
    public RecipeType<ArcaneWrappedRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("recipe.type.arcane");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ArcaneWrappedRecipe recipe, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            inputs.add(Arrays.asList(ingredient.getItems()));
        }
        int width = 0;
        int height = 0;
        if (recipe.getInner() instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        } else {
            builder.setShapeless();
        }
        gridHelper.createAndSetInputs(builder, VanillaTypes.ITEM_STACK, inputs, width, height);
        gridHelper.createAndSetOutputs(builder, VanillaTypes.ITEM_STACK,
                List.of(recipe.getResultItem(Minecraft.getInstance().level.registryAccess())));
        JeiDraw.aspectSlots(builder, recipe.getAspects(null), 1, ASPECTS_Y);
    }

    @Override
    public void draw(ArcaneWrappedRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        grid.draw(graphics, 0, 0);
        JeiDraw.drawResearch(graphics, recipe.getResearch(), 1, 60 + (recipe.getAspects(null).size() + 5) / 6 * 18 + 2, 116);
    }
}
