package thaumcraft.common.items.tools;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.api.WarpingGear;
import thaumcraft.common.registry.ModMaterials;

/** Пустотная мотыга (ItemVoidHoe). Сам чинится, ранит слабостью, даёт искажение. */
public class VoidHoeItem extends HoeItem implements WarpingGear {
    public VoidHoeItem() {
        super(ModMaterials.VOID_TOOL, -3, 0f, new Item.Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidGear.selfRepair(stack, level, entity);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return VoidGear.repairs(repair) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof LivingEntity living) {
            VoidGear.sap(living, player, 80);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }
}
