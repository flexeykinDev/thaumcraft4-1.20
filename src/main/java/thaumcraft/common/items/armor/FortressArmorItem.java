package thaumcraft.common.items.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.api.Goggles;
import thaumcraft.api.nodes.Revealer;
import thaumcraft.common.registry.ModMaterials;

import java.util.List;
import java.util.function.Consumer;

/**
 * Тауминиевые крепостные доспехи — перенос ItemFortressArmor оригинала. Защищают как алмазные, но дольше служат и
 * вдобавок прикрывают от магии и огня; чем больше частей надето, тем крепче каждая. Шлем можно улучшить очками
 * откровения и одной из трёх масок: «Ухмыляющийся бес» смягчает приступы искажения, «Злобный призрак» иссушает тех,
 * кто ранит хозяина, «Пьющий изверг» понемногу лечит хозяина, когда тот наносит удары.
 */
public class FortressArmorItem extends ArmorItem implements Revealer, Goggles {
    public FortressArmorItem(Type type) {
        super(ModMaterials.FORTRESS_ARMOR, type, new Item.Properties().rarity(Rarity.RARE));
    }

    public static int getMask(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("mask") ? stack.getTag().getInt("mask") : -1;
    }

    public static boolean hasGoggles(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("goggles");
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity wearer) {
        return hasGoggles(stack);
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
        return hasGoggles(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasGoggles(stack)) {
            tooltip.add(Component.translatable("item.thaumcraft.goggles").withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (getMask(stack) >= 0) {
            tooltip.add(Component.translatable("item.HelmetFortress.mask." + getMask(stack)).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "thaumcraft:textures/models/fortress_armor.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                                                                    HumanoidModel<?> original) {
                thaumcraft.client.render.FortressArmorModel model = thaumcraft.client.render.FortressArmorModel.get(slot == EquipmentSlot.LEGS);
                model.prepare(entity);
                return model;
            }
        });
    }

    /** Сколько частей комплекта надето (шлем, кираса, поножи) и надета ли маска. */
    private static double setFactor(LivingEntity wearer) {
        double factor = 0.875;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            ItemStack piece = wearer.getItemBySlot(slot);
            if (piece.getItem() instanceof FortressArmorItem) {
                factor += 0.125;
                if (getMask(piece) >= 0) {
                    factor += 0.05;
                }
            }
        }
        return factor;
    }

    /**
     * Имя класса не должно совпадать с другими обработчиками этого пакета: Forge называет созданный для метода
     * класс по короткому имени (__Events_hurt_LivingHurtEvent), и одноимённые обработчики подменяли друг друга.
     */
    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class FortressEvents {
        private FortressEvents() {
        }

        @SubscribeEvent
        public static void hurt(LivingHurtEvent event) {
            DamageSource source = event.getSource();
            LivingEntity victim = event.getEntity();
            // «Пьющий изверг»: тот, кто бьёт, иногда залечивает себе полсердца
            if (source.getDirectEntity() instanceof Player leecher && getMask(leecher.getItemBySlot(EquipmentSlot.HEAD)) == 2
                    && leecher.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof FortressArmorItem
                    && leecher.getRandom().nextFloat() < event.getAmount() / 12f) {
                leecher.heal(1f);
            }
            if (!(victim instanceof Player player)) {
                return;
            }
            ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
            // «Злобный призрак»: обидчик иногда иссыхает
            if (helm.getItem() instanceof FortressArmorItem && getMask(helm) == 1 && source.getDirectEntity() instanceof LivingEntity attacker
                    && player.getRandom().nextFloat() < event.getAmount() / 10f) {
                attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
            }
            // защита от магии и огня, которой у обычной брони нет (ISpecialArmor оригинала); от прочего — как у алмазной
            boolean magic = source.is(DamageTypeTags.WITCH_RESISTANT_TO);
            boolean fire = source.is(DamageTypeTags.IS_FIRE);
            if (!magic && !fire || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return;
            }
            double ratio = 0.0;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
                if (player.getItemBySlot(slot).getItem() instanceof FortressArmorItem armor) {
                    ratio += armor.getDefense() / (magic ? 35.0 : 20.0);
                }
            }
            if (ratio > 0.0) {
                ratio = Math.min(1.0, ratio * setFactor(player));
                event.setAmount((float) (event.getAmount() * (1.0 - ratio)));
            }
        }
    }
}
