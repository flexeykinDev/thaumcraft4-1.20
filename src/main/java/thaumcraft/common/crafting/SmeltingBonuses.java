package thaumcraft.common.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.crafting.SmeltingBonus;
import thaumcraft.common.registry.ModItems;

/**
 * Бонусы адской печи из ConfigRecipes оригинала. Руда даёт самородок своего металла, кластер — тоже, мясо —
 * мясной кусочек. В 1.20 руда добывается сырым металлом, поэтому сырой металл (forge:raw_materials) получает
 * тот же бонус, что и руда.
 */
public final class SmeltingBonuses {
    private SmeltingBonuses() {
    }

    private static void metal(String metal, java.util.function.Supplier<ItemStack> nugget) {
        SmeltingBonus.add(ItemTags.create(new ResourceLocation("forge", "ores/" + metal)), nugget);
        SmeltingBonus.add(ItemTags.create(new ResourceLocation("forge", "raw_materials/" + metal)), nugget);
    }

    public static void register() {
        metal("gold", () -> new ItemStack(Items.GOLD_NUGGET));
        metal("iron", () -> new ItemStack(Items.IRON_NUGGET));
        metal("cinnabar", () -> new ItemStack(ModItems.QUICKSILVER_NUGGET.get()));
        metal("copper", () -> new ItemStack(ModItems.COPPER_NUGGET.get()));
        // олово, серебро и свинец дают свои самородки Thaumcraft, как в оригинале (руды бывают только из других модов)
        metal("tin", () -> new ItemStack(ModItems.TIN_NUGGET.get()));
        metal("silver", () -> new ItemStack(ModItems.SILVER_NUGGET.get()));
        metal("lead", () -> new ItemStack(ModItems.LEAD_NUGGET.get()));
        SmeltingBonus.add(ModItems.CLUSTER_TIN.get(), () -> new ItemStack(ModItems.TIN_NUGGET.get()));
        SmeltingBonus.add(ModItems.CLUSTER_SILVER.get(), () -> new ItemStack(ModItems.SILVER_NUGGET.get()));
        SmeltingBonus.add(ModItems.CLUSTER_LEAD.get(), () -> new ItemStack(ModItems.LEAD_NUGGET.get()));
        SmeltingBonus.add(ModItems.CLUSTER_GOLD.get(), () -> new ItemStack(Items.GOLD_NUGGET));
        SmeltingBonus.add(ModItems.CLUSTER_IRON.get(), () -> new ItemStack(Items.IRON_NUGGET));
        SmeltingBonus.add(ModItems.CLUSTER_CINNABAR.get(), () -> new ItemStack(ModItems.QUICKSILVER_NUGGET.get()));
        SmeltingBonus.add(ModItems.CLUSTER_COPPER.get(), () -> new ItemStack(ModItems.COPPER_NUGGET.get()));
        SmeltingBonus.add(Items.CHICKEN, () -> new ItemStack(ModItems.NUGGET_CHICKEN.get()));
        SmeltingBonus.add(Items.BEEF, () -> new ItemStack(ModItems.NUGGET_BEEF.get()));
        SmeltingBonus.add(Items.PORKCHOP, () -> new ItemStack(ModItems.NUGGET_PORK.get()));
        SmeltingBonus.add(ItemTags.FISHES, () -> new ItemStack(ModItems.NUGGET_FISH.get()));
    }
}
