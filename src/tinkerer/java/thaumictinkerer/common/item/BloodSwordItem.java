package thaumictinkerer.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.api.aspects.Aspect;
import thaumictinkerer.ThaumicTinkerer;

import java.util.UUID;

/**
 * Кровавый меч (ItemBloodSword оригинала): бьёт на 10, хозяин бежит на четверть быстрее, но каждый удар ранит и его
 * самого магией. Shift+щелчок включает сбор эссенции: убитые им существа роняют вместо добычи аспекты своей души.
 */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public class BloodSwordItem extends SwordItem {
    private static final UUID SPEED = UUID.fromString("0f7dfb7d-1f2a-4d6c-9a3b-55a1c2e4b1f3");
    private static final Tier BLOOD = new Tier() {
        @Override
        public int getUses() {
            return 950;
        }

        @Override
        public float getSpeed() {
            return 1f;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0f;
        }

        @Override
        public int getLevel() {
            return 0;
        }

        @Override
        public int getEnchantmentValue() {
            return 25;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };
    private static boolean selfHurt;

    public BloodSwordItem() {
        super(BLOOD, 10, -2.4f, new Properties().rarity(Rarity.RARE));
    }

    public static boolean isHarvesting(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("Activated");
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(super.getAttributeModifiers(slot, stack))
                .put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED, "Blood sword", 0.25, AttributeModifier.Operation.MULTIPLY_BASE))
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            boolean on = !isHarvesting(stack);
            stack.getOrCreateTag().putBoolean("Activated", on);
            player.displayClientMessage(Component.translatable(on ? "ttmisc.bloodSword.activateEssentiaHarvest"
                    : "ttmisc.bloodSword.deactivateEssentiaHarvest"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Меч ранит и того, кто им бьёт. */
    @SubscribeEvent
    public static void attack(LivingAttackEvent event) {
        if (selfHurt || event.getEntity().level().isClientSide || !(event.getSource().getDirectEntity() instanceof LivingEntity attacker)
                || !(attacker.getMainHandItem().getItem() instanceof BloodSwordItem)) {
            return;
        }
        selfHurt = true;
        attacker.hurt(attacker.damageSources().magic(), 2f);
        selfHurt = false;
    }

    /** В режиме сбора добыча заменяется аспектами души убитого. */
    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !isHarvesting(player.getMainHandItem())
                || !(player.getMainHandItem().getItem() instanceof BloodSwordItem)) {
            return;
        }
        Aspect[] aspects = SoulAspectItem.aspectsOf(event.getEntity().getType());
        if (aspects == null) {
            return;
        }
        event.getDrops().clear();
        LivingEntity dead = event.getEntity();
        for (Aspect aspect : aspects) {
            ItemEntity drop = new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(),
                    SoulAspectItem.of(SoulAspectItem.Kind.PLAIN, aspect));
            drop.setPickUpDelay(10);
            event.getDrops().add(drop);
        }
    }
}
