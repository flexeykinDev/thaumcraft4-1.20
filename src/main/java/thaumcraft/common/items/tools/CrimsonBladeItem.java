package thaumcraft.common.items.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeTier;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.WarpingGear;
import thaumcraft.common.registry.ModItems;

import java.util.List;

/**
 * Багровый клинок — перенос ItemCrimsonSword оригинала, меч предводителя культа: сам чинится, ранит сильным
 * истощением (слабость на 3 секунды и голод на 6) и даёт 2 единицы искажения.
 */
public class CrimsonBladeItem extends SwordItem implements WarpingGear {
    /** Материал CVOID: уровень 4, прочность 200, скорость 8, урон +3.5, зачаровываемость 20. */
    public static final Tier TIER = new ForgeTier(4, 200, 8f, 3.5f, 20, net.minecraftforge.common.Tags.Blocks.NEEDS_NETHERITE_TOOL,
            () -> net.minecraft.world.item.crafting.Ingredient.of(ModItems.VOID_METAL_INGOT.get()));

    public CrimsonBladeItem() {
        super(TIER, 3, -2.4f, new Item.Properties().rarity(Rarity.RARE));
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
        boolean pvpBlocked = target instanceof Player && attacker instanceof Player && target.getServer() != null
                && !target.getServer().isPvpAllowed();
        if (!target.level().isClientSide && !pvpBlocked) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
