package thaumcraft.common.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.wands.WandCap;
import thaumcraft.api.wands.WandRod;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModRecipes;
import thaumcraft.common.research.PlayerKnowledge;

/**
 * Сборка жезла из любого стержня и пары одинаковых набалдашников — перенос ArcaneWandRecipe оригинала.
 * Набалдашники в правом верхнем и левом нижнем углах, стержень в центре, остальное пусто.
 * Цена: (стоимость набалдашника x стоимость стержня) каждого из шести первичных аспектов.
 * Простейший жезл (дерево и железо) сюда не входит: он собирается в обычном верстаке.
 */
public class ArcaneWandRecipe implements ArcaneRecipe {
    private final ResourceLocation id;

    public ArcaneWandRecipe(ResourceLocation id) {
        this.id = id;
    }

    private record Parts(WandRod rod, WandCap cap, boolean sceptre) {
    }

    @Nullable
    private static Parts findParts(CraftingContainer container) {
        if (container.getWidth() != 3 || container.getHeight() != 3) {
            return null;
        }
        // Жезл и посох: набалдашники по диагонали, стержень в центре. Скипетр (ArcaneSceptreRecipe оригинала):
        // три набалдашника вокруг стержня и первозданный амулет в правом верхнем углу.
        ItemStack rodStack = container.getItem(4);
        boolean sceptre = container.getItem(2).is(ModItems.PRIMAL_CHARM.get());
        ItemStack cap1;
        if (sceptre) {
            cap1 = container.getItem(1);
            if (!ItemStack.isSameItem(cap1, container.getItem(5)) || !ItemStack.isSameItem(cap1, container.getItem(6))) {
                return null;
            }
            for (int slot : new int[]{0, 3, 7, 8}) {
                if (!container.getItem(slot).isEmpty()) {
                    return null;
                }
            }
        } else {
            cap1 = container.getItem(2);
            if (!ItemStack.isSameItem(cap1, container.getItem(6))) {
                return null;
            }
            for (int slot : new int[]{0, 1, 3, 5, 7, 8}) {
                if (!container.getItem(slot).isEmpty()) {
                    return null;
                }
            }
        }
        if (cap1.isEmpty() || rodStack.isEmpty()) {
            return null;
        }
        WandCap cap = null;
        for (WandCap candidate : WandCap.all()) {
            if (candidate.getItemId().equals(ForgeRegistries.ITEMS.getKey(cap1.getItem()))) {
                cap = candidate;
                break;
            }
        }
        WandRod rod = null;
        for (WandRod candidate : WandRod.all()) {
            if (candidate.getItemId().equals(ForgeRegistries.ITEMS.getKey(rodStack.getItem()))) {
                rod = candidate;
                break;
            }
        }
        // скипетр из посоха не собрать; простейший жезл делается в обычном верстаке
        if (cap == null || rod == null || sceptre && rod.isStaff() || !sceptre && rod == WandRod.WOOD && cap == WandCap.IRON) {
            return null;
        }
        return new Parts(rod, cap, sceptre);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findParts(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        Parts parts = findParts(container);
        if (parts == null) {
            return ItemStack.EMPTY;
        }
        ItemStack wand = WandItem.create(ModItems.WAND.get(), parts.rod(), parts.cap(), false);
        if (parts.sceptre()) {
            WandItem.setSceptre(wand);
        }
        return wand;
    }

    @Override
    public AspectList getAspects(CraftingContainer container) {
        AspectList aspects = new AspectList();
        Parts parts = findParts(container);
        if (parts != null) {
            int cost = parts.cap().getCraftCost() * parts.rod().getCraftCost();
            if (parts.sceptre()) {
                cost = (int) (cost * 1.5f);
            }
            for (Aspect aspect : Aspect.primals()) {
                aspects.add(aspect, cost);
            }
        }
        return aspects;
    }

    @Override
    public String getResearch() {
        return "";
    }

    /** Как в оригинале: нужны исследования и набалдашника, и стержня (CAP_gold, ROD_greatwood и т.п.). */
    @Override
    public boolean isUnlocked(CraftingContainer container, PlayerKnowledge knowledge) {
        Parts parts = findParts(container);
        return parts != null && (!parts.sceptre() || ArcaneRecipe.isResearched("SCEPTRE", knowledge))
                && ArcaneRecipe.isResearched("CAP_" + parts.cap().getTag(), knowledge)
                && ArcaneRecipe.isResearched("ROD_" + parts.rod().getTag(), knowledge);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    /** Результат зависит от частей, поэтому книга рецептов и вывод аспектов этот рецепт пропускают. */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ARCANE_WAND.get();
    }

    public static class Serializer implements RecipeSerializer<ArcaneWandRecipe> {
        @Override
        public ArcaneWandRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ArcaneWandRecipe(id);
        }

        @Override
        public ArcaneWandRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            return new ArcaneWandRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ArcaneWandRecipe recipe) {
        }
    }
}
