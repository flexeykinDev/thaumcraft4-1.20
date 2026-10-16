package thaumcraft.common.items.tools;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.WarpingGear;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;

/** Пустотный меч (ItemVoidSword): ранит слабостью на 3 секунды. Сам чинится, ранит слабостью, даёт искажение. */
public class VoidSwordItem extends SwordItem implements WarpingGear {
    public VoidSwordItem() {
        super(ModMaterials.VOID_TOOL, 3, -2.4f, new Item.Properties().rarity(Rarity.UNCOMMON));
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
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        VoidGear.sap(target, attacker, 60);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(VoidGear.sapless());
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
