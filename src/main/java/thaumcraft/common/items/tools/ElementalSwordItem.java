package thaumcraft.common.items.tools;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.entity.golem.Golem;
import thaumcraft.common.registry.ModMaterials;
import thaumcraft.common.registry.ModSounds;

/**
 * Меч зефира — перенос ItemElementalSword оригинала. Удар задевает всех, кто стоит вплотную к жертве, в полную
 * силу; а если меч зажать, вокруг игрока поднимается вихрь: он несёт хозяина вверх и расталкивает всех вокруг.
 */
public class ElementalSwordItem extends SwordItem {
    public ElementalSwordItem() {
        super(ModMaterials.ELEMENTAL_TOOL, 3, -2.4f, new Item.Properties().rarity(Rarity.RARE));
    }

    /** Свой круговой удар заменяет обычный «размашистый» удар мечей 1.9+: иначе соседи получали бы урон дважды. */
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int ticks = getUseDuration(stack) - remaining;
        Vec3 motion = user.getDeltaMovement();
        double my = motion.y;
        if (my < 0.0) {
            my /= 1.2;
        }
        // сервер не знает скорости игрока и гасит накопленную высоту падения всё время, пока тот не на земле
        if (my < 0.0 || !level.isClientSide && !user.onGround()) {
            user.fallDistance /= 1.2f;
        }
        my += 0.08;
        if (my > 0.5) {
            my = 0.2;
        }
        user.setDeltaMovement(motion.x, my, motion.z);
        if (user instanceof ServerPlayer player) {
            ToolHelper.resetFloatCounter(player);
        }
        for (Entity entity : level.getEntities(user, user.getBoundingBox().inflate(2.5))) {
            if (entity instanceof Player || !entity.isAlive() || user.getVehicle() == entity) {
                continue;
            }
            Vec3 away = entity.position().subtract(user.position());
            double distance = away.length() + 0.1;
            entity.setDeltaMovement(entity.getDeltaMovement().add(away.scale(1.0 / 2.5 / distance)));
        }
        if (level.isClientSide) {
            int floor = (int) (user.getBoundingBox().minY - 2.0);
            if (user.onGround()) {
                floor = Mth.floor(user.getBoundingBox().minY);
            }
            for (int i = 0; i < 5; i++) {
                Fx.smokeSpiral(user.getX(), user.getBoundingBox().minY + user.getBbHeight() / 2f, user.getZ(), 1.5f,
                        level.random.nextInt(360), floor, 0xDDDDDD);
            }
            if (user.onGround()) {
                float angle = level.random.nextFloat() * 360f;
                level.addParticle(ParticleTypes.SMOKE, user.getX(), user.getBoundingBox().minY + 0.1, user.getZ(),
                        -Mth.sin(angle * Mth.DEG_TO_RAD) / 5f, 0.0, Mth.cos(angle * Mth.DEG_TO_RAD) / 5f);
            }
        } else {
            if (ticks % 20 == 0) {
                level.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.WIND.get(), SoundSource.PLAYERS, 0.5f,
                        0.9f + level.random.nextFloat() * 0.2f);
                stack.hurtAndBreak(1, user, broken -> broken.broadcastBreakEvent(user.getUsedItemHand()));
            }
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity.isAlive() && !player.level().isClientSide) {
            // сила замаха берётся до удара: к самой жертве обычный удар применит её сам
            float strength = player.getAttackStrengthScale(0.5f);
            int count = 0;
            for (Entity near : player.level().getEntities(player, entity.getBoundingBox().inflate(1.2, 1.1, 1.2))) {
                if (near == entity || !near.isAlive() || !(near instanceof Mob mob)
                        || near instanceof Golem golem && player.getUUID().equals(golem.getOwnerUUID())
                        || near instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) {
                    continue;
                }
                strike(stack, player, mob, strength);
                count++;
            }
            if (count > 0) {
                player.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.SWING.get(), SoundSource.PLAYERS, 1f,
                        0.9f + player.level().random.nextFloat() * 0.2f);
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    /** attackTargetEntityWithCurrentItem оригинала: обычный удар игрока по ещё одной цели, без повторного кругового. */
    private static void strike(ItemStack stack, Player player, LivingEntity target, float strength) {
        if (!target.isAttackable() || target.skipAttackInteraction(player)) {
            return;
        }
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2f + strength * strength * 0.8f);
        float magic = EnchantmentHelper.getDamageBonus(stack, target.getMobType()) * strength;
        int knockback = EnchantmentHelper.getKnockbackBonus(player) + (player.isSprinting() && strength > 0.9f ? 1 : 0);
        if (damage <= 0f && magic <= 0f) {
            return;
        }
        boolean critical = strength > 0.9f && player.fallDistance > 0f && !player.onGround() && !player.onClimbable() && !player.isInWater()
                && !player.isPassenger();
        if (critical) {
            damage *= 1.5f;
        }
        damage += magic;
        int fire = EnchantmentHelper.getFireAspect(player);
        boolean lit = false;
        if (fire > 0 && !target.isOnFire()) {
            lit = true;
            target.setSecondsOnFire(1);
        }
        if (target.hurt(player.damageSources().playerAttack(player), damage)) {
            if (knockback > 0) {
                target.knockback(knockback * 0.5f, Mth.sin(player.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(player.getYRot() * Mth.DEG_TO_RAD));
            }
            if (critical) {
                player.crit(target);
            }
            if (magic > 0f) {
                player.magicCrit(target);
            }
            player.setLastHurtMob(target);
            EnchantmentHelper.doPostHurtEffects(target, player);
            EnchantmentHelper.doPostDamageEffects(player, target);
            stack.hurtEnemy(target, player);
            if (fire > 0) {
                target.setSecondsOnFire(fire * 4);
            }
            player.causeFoodExhaustion(0.1f);
        } else if (lit) {
            target.clearFire();
        }
    }
}
