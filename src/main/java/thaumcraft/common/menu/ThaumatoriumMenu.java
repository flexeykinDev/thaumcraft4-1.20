package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blocks.ThaumatoriumBlock;
import thaumcraft.common.crafting.CrucibleRecipe;
import thaumcraft.common.registry.ModMenus;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно тауматория (ContainerThaumatorium оригинала): ячейка катализатора и список рецептов тигля, которые к нему
 * подходят и изучены игроком, плюс уже заданные. Номер кнопки — номер рецепта в общем списке {@link ThaumatoriumBlock#allRecipes}.
 */
public class ThaumatoriumMenu extends AbstractContainerMenu {
    public final ThaumatoriumBlock.Core thaumatorium;
    private final Player player;
    /** Рецепты, которые сейчас можно листать в окне. */
    public final List<CrucibleRecipe> recipes = new ArrayList<>();

    public ThaumatoriumMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (ThaumatoriumBlock.Core) inventory.player.level().getBlockEntity(pos));
    }

    public ThaumatoriumMenu(int id, Inventory inventory, ThaumatoriumBlock.Core thaumatorium) {
        super(ModMenus.THAUMATORIUM.get(), id);
        this.thaumatorium = thaumatorium;
        this.player = inventory.player;
        thaumatorium.listener = this::updateRecipes;
        addSlot(new Slot(thaumatorium, 0, 48, 16));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
        updateRecipes();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        thaumatorium.listener = null;
    }

    public void updateRecipes() {
        recipes.clear();
        PlayerKnowledge knowledge = PlayerKnowledge.forSide(player);
        ItemStack input = thaumatorium.getItem(0);
        for (CrucibleRecipe recipe : ThaumatoriumBlock.allRecipes(player.level())) {
            if (thaumatorium.recipes.contains(recipe.getId())
                    || !input.isEmpty() && knowledge.isResearchComplete(recipe.getResearch()) && recipe.getCatalyst().test(input)) {
                recipes.add(recipe);
            }
        }
    }

    /** Кнопка — номер рецепта в общем списке: задать его или снять. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        List<CrucibleRecipe> all = ThaumatoriumBlock.allRecipes(player.level());
        if (button < 0 || button >= all.size()) {
            return false;
        }
        CrucibleRecipe recipe = all.get(button);
        updateRecipes();
        if (!recipes.contains(recipe)) {
            return false;
        }
        thaumatorium.toggleRecipe(recipe, player);
        updateRecipes();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return thaumatorium.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
