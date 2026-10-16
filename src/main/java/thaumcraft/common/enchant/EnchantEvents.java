package thaumcraft.common.enchant;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.registry.ModEnchantments;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.runic.RunicShield;

/** Действие зачарований мода: «Починка» и «Спешка» (EventHandlerEntity.doRepair и updateSpeed оригинала). */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class EnchantEvents {
    private EnchantEvents() {
    }

    /**
     * Починка: вещь возвращает себе единицу прочности за уровень зачарования, если владелец оплатит это вис —
     * по каждому первичному аспекту вещи корень из его удвоенного количества, умноженный на уровень (в сотых долях вис).
     */
    public static boolean doRepair(ItemStack stack, Player player) {
        int level = Math.min(2, EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.REPAIR.get(), stack));
        if (level <= 0) {
            return false;
        }
        AspectList primals = ResearchHelper.reduceToPrimals(ItemAspectManager.INSTANCE.getAspects(stack));
        if (primals.isEmpty()) {
            return false;
        }
        AspectList cost = new AspectList();
        for (Aspect aspect : primals.getAspects()) {
            cost.merge(aspect, (int) Math.sqrt(primals.getAmount(aspect) * 2) * level);
        }
        if (!RunicShield.consumeVisFromInventory(player, cost)) {
            return false;
        }
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - level));
        return true;
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // раз в две секунды чинится то, что лежит на панели быстрого доступа и надето
        if (!player.level().isClientSide && player.tickCount % 40 == 0 && !player.isCreative()) {
            for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
                repairIfNeeded(player.getInventory().getItem(slot), player);
            }
            for (ItemStack armor : player.getArmorSlots()) {
                repairIfNeeded(armor, player);
            }
        }
        // спешка: толчок вперёд при каждом шаге, вдвое слабее в воздухе и в воде
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!player.getAbilities().flying && !boots.isEmpty() && player.zza > 0f) {
            int haste = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.HASTE.get(), boots);
            if (haste > 0) {
                float bonus = haste * 0.015f;
                if (!player.onGround()) {
                    bonus /= 2f;
                }
                if (player.isInWater()) {
                    bonus /= 2f;
                }
                player.moveRelative(bonus, new Vec3(0, 0, 1));
            }
        }
    }

    private static void repairIfNeeded(ItemStack stack, Player player) {
        if (!stack.isEmpty() && stack.isDamaged() && stack.is(ModEnchantments.REPAIRABLE)) {
            doRepair(stack, player);
        }
    }
}
