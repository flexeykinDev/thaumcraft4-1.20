package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumictinkerer.common.registry.TTItems;
import thaumictinkerer.common.registry.TTRecipes;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Аспект души (ItemMobAspect оригинала) — обычный, сгущённый (девять обычных в сетке крафта) и наполненный (наполнение
 * из сгущённых). Три одинаковых набора аспектов на пьедесталах вокруг пентакля призыва дают существо; наполненные
 * не тратятся, но работают реже. Падает с существ, убитых кровавым мечом в режиме сбора.
 */
public class SoulAspectItem extends Item {
    /** NumericAspectHelper оригинала: аспекты, из которых состоят души существ. */
    public static final List<Aspect> ASPECTS = List.of(Aspect.WATER, Aspect.MAN, Aspect.AIR, Aspect.FLIGHT, Aspect.FIRE, Aspect.MAGIC,
            Aspect.UNDEAD, Aspect.FLESH, Aspect.BEAST, Aspect.POISON, Aspect.EARTH, Aspect.ELDRITCH, Aspect.TRAVEL, Aspect.METAL,
            Aspect.SLIME, Aspect.CLOTH, Aspect.CROP);

    public enum Kind { PLAIN, CONDENSED, INFUSED }

    private final Kind kind;

    public SoulAspectItem(Kind kind) {
        super(new Item.Properties());
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    public static Aspect aspect(ItemStack stack) {
        return stack.hasTag() ? Aspect.byTag(stack.getTag().getString("aspect")) : null;
    }

    public static ItemStack of(Kind kind, Aspect aspect) {
        Item item = switch (kind) {
            case PLAIN -> TTItems.SOUL_ASPECT.get();
            case CONDENSED -> TTItems.CONDENSED_SOUL_ASPECT.get();
            case INFUSED -> TTItems.INFUSED_SOUL_ASPECT.get();
        };
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("aspect", aspect.getTag());
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind == Kind.INFUSED;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Aspect aspect = aspect(stack);
        if (aspect != null) {
            tooltip.add(aspect.getName().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    /** EnumMobAspect оригинала: из каких трёх аспектов состоит душа существа. */
    public static final Map<Supplier<? extends EntityType<?>>, Aspect[]> MOBS = Map.ofEntries(
            mob(() -> EntityType.SNOW_GOLEM, Aspect.WATER, Aspect.WATER, Aspect.MAN),
            mob(() -> EntityType.BAT, Aspect.AIR, Aspect.AIR, Aspect.FLIGHT),
            mob(() -> EntityType.BLAZE, Aspect.FIRE, Aspect.FIRE, Aspect.FIRE),
            mob(thaumcraft.common.registry.ModEntities.BRAINY_ZOMBIE, Aspect.MAGIC, Aspect.UNDEAD, Aspect.FLESH),
            mob(thaumcraft.common.registry.ModEntities.FIRE_BAT, Aspect.FLIGHT, Aspect.FIRE, Aspect.MAGIC),
            mob(() -> EntityType.CAVE_SPIDER, Aspect.BEAST, Aspect.POISON, Aspect.POISON),
            mob(() -> EntityType.CHICKEN, Aspect.CROP, Aspect.FLIGHT, Aspect.BEAST),
            mob(() -> EntityType.COW, Aspect.BEAST, Aspect.EARTH, Aspect.BEAST),
            mob(() -> EntityType.CREEPER, Aspect.MAGIC, Aspect.BEAST, Aspect.ELDRITCH),
            mob(() -> EntityType.ENDERMAN, Aspect.ELDRITCH, Aspect.ELDRITCH, Aspect.MAN),
            mob(() -> EntityType.GHAST, Aspect.FIRE, Aspect.FLIGHT, Aspect.FLIGHT),
            mob(() -> EntityType.HORSE, Aspect.BEAST, Aspect.BEAST, Aspect.TRAVEL),
            mob(() -> EntityType.IRON_GOLEM, Aspect.METAL, Aspect.METAL, Aspect.MAN),
            mob(() -> EntityType.MAGMA_CUBE, Aspect.FIRE, Aspect.SLIME, Aspect.SLIME),
            mob(() -> EntityType.MOOSHROOM, Aspect.BEAST, Aspect.EARTH, Aspect.CROP),
            mob(() -> EntityType.OCELOT, Aspect.BEAST, Aspect.EARTH, Aspect.ELDRITCH),
            mob(() -> EntityType.PIG, Aspect.BEAST, Aspect.EARTH, Aspect.TRAVEL),
            mob(() -> EntityType.ZOMBIFIED_PIGLIN, Aspect.UNDEAD, Aspect.FLESH, Aspect.FIRE),
            mob(() -> EntityType.SHEEP, Aspect.EARTH, Aspect.CLOTH, Aspect.BEAST),
            mob(() -> EntityType.SILVERFISH, Aspect.METAL, Aspect.METAL, Aspect.EARTH),
            mob(() -> EntityType.SKELETON, Aspect.UNDEAD, Aspect.MAN, Aspect.UNDEAD),
            mob(() -> EntityType.SLIME, Aspect.SLIME, Aspect.SLIME, Aspect.BEAST),
            mob(() -> EntityType.SPIDER, Aspect.BEAST, Aspect.UNDEAD, Aspect.UNDEAD),
            mob(() -> EntityType.SQUID, Aspect.WATER, Aspect.WATER, Aspect.WATER),
            mob(() -> EntityType.VILLAGER, Aspect.MAN, Aspect.MAN, Aspect.MAN),
            mob(thaumcraft.common.registry.ModEntities.WISP, Aspect.AIR, Aspect.MAGIC, Aspect.MAGIC),
            mob(() -> EntityType.WITCH, Aspect.MAGIC, Aspect.UNDEAD, Aspect.ELDRITCH),
            mob(() -> EntityType.WOLF, Aspect.BEAST, Aspect.BEAST, Aspect.BEAST),
            mob(() -> EntityType.ZOMBIE, Aspect.FLESH, Aspect.FLESH, Aspect.UNDEAD));

    private static Map.Entry<Supplier<? extends EntityType<?>>, Aspect[]> mob(Supplier<? extends EntityType<?>> type, Aspect... aspects) {
        return Map.entry(type, aspects);
    }

    @Nullable
    public static Aspect[] aspectsOf(EntityType<?> type) {
        for (var entry : MOBS.entrySet()) {
            if (entry.getKey().get() == type) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** Рецепт SUMMON1 оригинала: девять одинаковых аспектов души сгущаются в один. */
    public static class Condense extends CustomRecipe {
        public Condense(ResourceLocation id, CraftingBookCategory category) {
            super(id, category);
        }

        @Nullable
        private static Aspect common(CraftingContainer grid) {
            Aspect found = null;
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack stack = grid.getItem(i);
                if (!(stack.getItem() instanceof SoulAspectItem soul) || soul.kind != Kind.PLAIN) {
                    return null;
                }
                Aspect aspect = aspect(stack);
                if (aspect == null || found != null && found != aspect) {
                    return null;
                }
                found = aspect;
            }
            return found;
        }

        @Override
        public boolean matches(CraftingContainer grid, Level level) {
            return grid.getContainerSize() == 9 && common(grid) != null;
        }

        @Override
        public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
            return of(Kind.CONDENSED, common(grid));
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer grid) {
            return NonNullList.withSize(grid.getContainerSize(), ItemStack.EMPTY);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return width * height >= 9;
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return TTRecipes.CONDENSE.get();
        }
    }
}
