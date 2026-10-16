package thaumcraft.common.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.registry.ModItems;

/**
 * Общее у вещей из пустотного металла (ItemVoidSword, ItemVoidPickaxe, ItemVoidArmor и др. оригинала): раз в секунду
 * сами чинятся на единицу, пока лежат в инвентаре или надеты; ранят «истощением» — слабостью; дают единицу искажения.
 */
public final class VoidGear {
    private VoidGear() {
    }

    public static void selfRepair(ItemStack stack, Level level, Entity entity) {
        if (!level.isClientSide && stack.isDamaged() && entity instanceof LivingEntity && entity.tickCount % 20 == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    /** Слабость на ticks тиков; игроку — только если на сервере разрешён PvP. */
    public static void sap(LivingEntity target, Entity attacker, int ticks) {
        if (target.level().isClientSide) {
            return;
        }
        if (target instanceof Player && attacker instanceof Player && target.getServer() != null && !target.getServer().isPvpAllowed()) {
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks));
    }

    /**
     * В оригинале инструменты чинятся первозданным оберегом (видимо, по ошибке — броня чинится слитком); принимаем и то, и другое.
     */
    public static boolean repairs(ItemStack repair) {
        return repair.is(ModItems.VOID_METAL_INGOT.get()) || repair.is(ModItems.PRIMAL_CHARM.get());
    }

    public static Component sapless() {
        return Component.translatable("enchantment.special.sapless").withStyle(ChatFormatting.GOLD);
    }
}
