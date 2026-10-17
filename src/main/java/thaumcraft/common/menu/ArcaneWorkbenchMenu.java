package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.crafting.ArcaneRecipe;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModMenus;
import thaumcraft.common.registry.ModRecipes;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.List;
import java.util.Optional;

/**
 * Окно магического верстака (ContainerArcaneWorkbench оригинала). Работает и как обычный верстак;
 * если обычного рецепта нет, ищется магический, и за него платит вис жезл из отдельной ячейки.
 */
public class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    public static final int RESULT_SLOT = 0;
    public static final int WAND_SLOT = 1;
    private static final int GRID_START = 2;
    private static final int INVENTORY_START = 11;
    private static final int HOTBAR_START = 38;
    private static final int END = 47;

    private final ArcaneWorkbenchBlockEntity workbench;
    private final Player player;
    private final Grid grid = new Grid();
    private final ResultContainer result = new ResultContainer();

    /** Клиентская сторона: стол находится по координатам из пакета открытия окна. */
    public ArcaneWorkbenchMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, (ArcaneWorkbenchBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public ArcaneWorkbenchMenu(int id, Inventory inventory, ArcaneWorkbenchBlockEntity workbench) {
        super(ModMenus.ARCANE_WORKBENCH.get(), id);
        this.workbench = workbench;
        this.player = inventory.player;

        addSlot(new ArcaneResultSlot(160, 64));
        addSlot(new Slot(new WandContainer(), 0, 160, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // как в оригинале, посох в ячейку жезла не кладётся
                return stack.getItem() instanceof WandItem && !WandItem.isStaff(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(grid, column + row * 3, 40 + column * 24, 40 + row * 24));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 16 + column * 18, 151 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 16 + column * 18, 209));
        }
        updateResult();
    }

    public ItemStack getWand() {
        return workbench.getWand();
    }

    public CraftingContainer getGrid() {
        return grid;
    }

    /** Магический рецепт для текущей раскладки; обычные рецепты имеют приоритет и сюда не попадают. */
    @Nullable
    public ArcaneRecipe findArcaneRecipe() {
        Level level = player.level();
        if (level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isPresent()) {
            return null;
        }
        ArcaneRecipe recipe = level.getRecipeManager().getRecipeFor(ModRecipes.ARCANE.get(), grid, level).orElse(null);
        // рецепт, исследование которого игрок не завершил, для него не существует
        return recipe != null && recipe.isUnlocked(grid, PlayerKnowledge.forSide(player)) ? recipe : null;
    }

    /** Хватает ли в жезле вис на рецепт; при pay = true вис ещё и списывается. */
    public boolean payVis(ArcaneRecipe recipe, boolean pay) {
        ItemStack wand = getWand();
        if (!(wand.getItem() instanceof WandItem)) {
            return false;
        }
        AspectList cost = recipe.getAspects(grid);
        List<Aspect> aspects = cost.getAspects();
        for (Aspect aspect : aspects) {
            int price = (int) (cost.getAmount(aspect) * 100 * WandItem.getConsumptionModifier(wand, player, aspect, true));
            if (WandItem.getVis(wand, aspect) < price) {
                return false;
            }
        }
        if (pay) {
            for (Aspect aspect : aspects) {
                WandItem.consumeVis(wand, player, aspect, cost.getAmount(aspect) * 100, true);
            }
            workbench.contentsChanged();
        }
        return true;
    }

    private void updateResult() {
        Level level = player.level();
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = ItemStack.EMPTY;
        Optional<CraftingRecipe> vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        if (vanilla.isPresent()) {
            CraftingRecipe recipe = vanilla.get();
            if (result.setRecipeUsed(level, serverPlayer, recipe)) {
                ItemStack assembled = recipe.assemble(grid, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    stack = assembled;
                }
            }
        } else {
            ArcaneRecipe arcane = findArcaneRecipe();
            if (arcane != null && payVis(arcane, false)) {
                stack = arcane.assemble(grid, level.registryAccess());
            }
        }
        result.setItem(0, stack);
        setRemoteSlot(RESULT_SLOT, stack);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), RESULT_SLOT, stack));
    }

    @Override
    public void slotsChanged(Container container) {
        updateResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return workbench.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index >= INVENTORY_START) {
            boolean fromHotbar = index >= HOTBAR_START;
            if (stack.getItem() instanceof WandItem) {
                if (!moveItemStackTo(stack, WAND_SLOT, WAND_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (fromHotbar ? !moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)
                    : !moveItemStackTo(stack, HOTBAR_START, END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, INVENTORY_START, END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        if (index == RESULT_SLOT) {
            player.drop(stack, false);
        }
        return original;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    /** Ячейка результата: перед тем как забрать предмет магического рецепта, жезл платит за него вис. */
    private class ArcaneResultSlot extends ResultSlot {
        ArcaneResultSlot(int x, int y) {
            super(player, grid, result, 0, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (!player.level().isClientSide) {
                ArcaneRecipe arcane = findArcaneRecipe();
                if (arcane != null) {
                    payVis(arcane, true);
                    thaumcraft.common.ModAdvancements.grant(player, thaumcraft.common.ModAdvancements.ARCANE_CRAFT);
                }
            }
            super.onTake(player, stack);
        }
    }

    /** Сетка 3x3 поверх ячеек самого стола: вещи не выпадают при закрытии окна. */
    private class Grid implements CraftingContainer {
        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return List.copyOf(workbench.getItems().subList(0, ArcaneWorkbenchBlockEntity.GRID_SIZE));
        }

        @Override
        public int getContainerSize() {
            return ArcaneWorkbenchBlockEntity.GRID_SIZE;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < getContainerSize() ? workbench.getItems().get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack removed = ContainerHelper.removeItem(gridItems(), slot, amount);
            if (!removed.isEmpty()) {
                setChanged();
            }
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(gridItems(), slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            workbench.getItems().set(slot, stack);
            setChanged();
        }

        private NonNullList<ItemStack> gridItems() {
            return workbench.getItems();
        }

        @Override
        public void setChanged() {
            workbench.setChanged();
            slotsChanged(this);
        }

        @Override
        public boolean stillValid(Player player) {
            return workbench.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) {
                workbench.getItems().set(i, ItemStack.EMPTY);
            }
            setChanged();
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (int i = 0; i < getContainerSize(); i++) {
                contents.accountSimpleStack(getItem(i));
            }
        }
    }

    /** Ячейка жезла: от неё зависит, доступен ли магический рецепт, поэтому её изменения пересчитывают результат. */
    private class WandContainer implements Container {
        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return workbench.getWand().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return workbench.getWand();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack wand = workbench.getWand();
            if (wand.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            workbench.setWand(ItemStack.EMPTY);
            slotsChanged(this);
            return wand;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack wand = workbench.getWand();
            workbench.getItems().set(ArcaneWorkbenchBlockEntity.WAND_SLOT, ItemStack.EMPTY);
            return wand;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            workbench.setWand(stack);
            slotsChanged(this);
        }

        @Override
        public void setChanged() {
            workbench.contentsChanged();
            slotsChanged(this);
        }

        @Override
        public boolean stillValid(Player player) {
            return workbench.stillValid(player);
        }

        @Override
        public void clearContent() {
            workbench.setWand(ItemStack.EMPTY);
        }
    }
}
