package thaumcraft.client.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientItemAspects;
import thaumcraft.client.jei.AspectIngredient.AspectStack;
import thaumcraft.common.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Две категории про сами аспекты: из каких предметов их добыть и из каких двух аспектов они состоят. */
final class AspectCategories {

    /** Предмет и аспекты, которые он даёт при изучении таумометром и при растворении в тигле. */
    record ItemAspects(ItemStack stack, AspectList aspects) {
    }

    /** Составной аспект и две его части. */
    record Combination(Aspect result) {
    }

    static List<ItemAspects> itemAspects() {
        List<ItemAspects> list = new ArrayList<>();
        for (Map.Entry<Item, AspectList> entry : ClientItemAspects.all().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                list.add(new ItemAspects(new ItemStack(entry.getKey()), entry.getValue()));
            }
        }
        list.sort((a, b) -> a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString()));
        return list;
    }

    static List<Combination> combinations() {
        List<Combination> list = new ArrayList<>();
        for (Aspect aspect : Aspect.compounds()) {
            list.add(new Combination(aspect));
        }
        return list;
    }

    static final class ItemAspectsCategory implements IRecipeCategory<ItemAspects> {
        static final RecipeType<ItemAspects> TYPE = RecipeType.create("thaumcraft", "item_aspects", ItemAspects.class);
        private final IDrawable background;
        private final IDrawable icon;
        private final JeiDraw parts;

        ItemAspectsCategory(IGuiHelper gui) {
            background = gui.createBlankDrawable(150, 56);
            icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.THAUMOMETER.get()));
            parts = new JeiDraw(gui);
        }

        @Override
        public RecipeType<ItemAspects> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.thaumcraft.item_aspects");
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
        public void setRecipe(IRecipeLayoutBuilder builder, ItemAspects recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 20).addItemStack(recipe.stack());
            int index = 0;
            for (AspectStack stack : AspectIngredient.of(recipe.aspects())) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 48 + index % 6 * 17, 2 + index / 6 * 18)
                        .addIngredient(AspectIngredient.TYPE, stack);
                index++;
            }
        }

        @Override
        public void draw(ItemAspects recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            parts.slot.draw(graphics, 0, 19);
            parts.arrow.draw(graphics, 21, 19);
        }
    }

    /** Плюс между частями рисуется текстом: значка «+» в старых версиях JEI нет. */
    static final class CombinationCategory implements IRecipeCategory<Combination> {
        static final RecipeType<Combination> TYPE = RecipeType.create("thaumcraft", "aspect_combination", Combination.class);
        private final IDrawable background;
        private final IDrawable icon;
        private final JeiDraw parts;

        CombinationCategory(IGuiHelper gui) {
            background = gui.createBlankDrawable(100, 20);
            icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.RESEARCH_NOTES.get()));
            parts = new JeiDraw(gui);
        }

        @Override
        public RecipeType<Combination> getRecipeType() {
            return TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.thaumcraft.aspect_combination");
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
        public void setRecipe(IRecipeLayoutBuilder builder, Combination recipe, IFocusGroup focuses) {
            Aspect aspect = recipe.result();
            builder.addSlot(RecipeIngredientRole.INPUT, 2, 2).addIngredient(AspectIngredient.TYPE, new AspectStack(aspect.getFirstComponent(), 0));
            builder.addSlot(RecipeIngredientRole.INPUT, 30, 2).addIngredient(AspectIngredient.TYPE, new AspectStack(aspect.getSecondComponent(), 0));
            builder.addSlot(RecipeIngredientRole.OUTPUT, 82, 2).addIngredient(AspectIngredient.TYPE, new AspectStack(aspect, 0));
        }

        @Override
        public void draw(Combination recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "+", 21, 6, 0x404040, false);
            parts.arrow.draw(graphics, 50, 1);
        }
    }

    private AspectCategories() {
    }
}
