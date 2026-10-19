package thaumictinkerer.common.enchantment;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumictinkerer.ThaumicTinkerer;

/** Действие чар Thaumic Tinkerer (ModEnchantmentHandler оригинала). */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public final class TTEnchantmentEvents {
    private TTEnchantmentEvents() {
    }

    private static int level(ItemStack stack, Enchantment enchantment) {
        return stack.isEmpty() ? 0 : stack.getEnchantmentLevel(enchantment);
    }

    private static int armorLevel(LivingEntity entity, Enchantment enchantment) {
        return EnchantmentHelper.getEnchantmentLevel(enchantment, entity);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        float amount = event.getAmount();
        // «Наскок»: удар в прыжке сильнее на четверть за уровень
        int pounce = level(attacker.getItemBySlot(EquipmentSlot.LEGS), TTEnchantments.POUNCE.get());
        if (pounce > 0 && attacker instanceof Player && attacker.level().isEmptyBlock(attacker.blockPosition().below())) {
            amount *= 1 + 0.25f * pounce;
        }
        if (!weapon.isEmpty()) {
            int finalStrike = level(weapon, TTEnchantments.FINAL_STRIKE.get());
            if (finalStrike > 0 && attacker.getRandom().nextInt(20 - finalStrike) == 0) {
                amount *= 3;
            }
            int valiance = level(weapon, TTEnchantments.VALIANCE.get());
            if (valiance > 0 && attacker.getHealth() / attacker.getMaxHealth() < 0.5f) {
                amount *= 1 + 0.1f * valiance;
            }
            int focused = level(weapon, TTEnchantments.FOCUSED_STRIKES.get());
            int dispersed = level(weapon, TTEnchantments.DISPERSED_STRIKES.get());
            if (focused > 0 || dispersed > 0) {
                CompoundTag tag = weapon.getOrCreateTag();
                int same = tag.getInt("TTEnchantLastTarget") == event.getEntity().getId() ? 1 : 0;
                // «Сосредоточенные удары» вдвое слабее по новой цели и сильнее по той же; «рассеянные» — наоборот
                if (focused > 0) {
                    amount = amount / 2 + 0.5f * same * (amount / 2) * focused;
                }
                if (dispersed > 0) {
                    amount /= 1 + same * 2;
                }
                tag.putInt("TTEnchantLastTarget", event.getEntity().getId());
            }
            int vampirism = level(weapon, TTEnchantments.VAMPIRISM.get());
            if (vampirism > 0) {
                attacker.heal(vampirism);
                attacker.level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                        thaumcraft.common.registry.ModSounds.ZAP.get(), SoundSource.PLAYERS, 0.6f, 1f);
            }
        }
        event.setAmount(amount);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // «Плавное падение»: после трёх блоков полёта падение замедляется
        int slowFall = armorLevel(player, TTEnchantments.SLOW_FALL.get());
        Vec3 motion = player.getDeltaMovement();
        if (slowFall > 0 && !player.isShiftKeyDown() && motion.y < -0.0784 && player.fallDistance >= 2.9f) {
            player.setDeltaMovement(motion.x, motion.y / (1 + slowFall * 0.33f), motion.z);
            player.fallDistance = Math.max(2.9f, player.fallDistance - slowFall / 3f);
            player.level().addParticle(ParticleTypes.CLOUD, player.getX() + 0.25, player.getY(), player.getZ() + 0.25,
                    -motion.x, motion.y, -motion.z);
        }
    }

    /** «Быстрая стрельба»: лук натягивается быстрее. */
    @SubscribeEvent
    public static void useTick(LivingEntityUseItemEvent.Tick event) {
        int quickDraw = level(event.getItem(), TTEnchantments.QUICK_DRAW.get());
        if (quickDraw > 0 && event.getItem().getItem() instanceof BowItem
                && (event.getItem().getUseDuration() - event.getDuration()) % (6 - quickDraw) == 0) {
            event.setDuration(event.getDuration() - 1);
        }
    }

    /** «Подъём»: прыжок выше. */
    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player && !player.isShiftKeyDown()) {
            int boost = armorLevel(player, TTEnchantments.ASCENT_BOOST.get());
            if (boost > 0) {
                Vec3 motion = player.getDeltaMovement();
                player.setDeltaMovement(motion.x, motion.y * (boost + 2) / 2.0, motion.z);
            }
        }
    }

    /** «Ударная волна»: приземление с высоты ранит всех вокруг. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getDistance() <= 3 || player.level().isClientSide) {
            return;
        }
        int shockwave = level(player.getItemBySlot(EquipmentSlot.FEET), TTEnchantments.SHOCKWAVE.get());
        if (shockwave > 0) {
            for (LivingEntity near : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(10))) {
                if (near != player) {
                    near.hurt(player.damageSources().fall(), 0.1f * shockwave * event.getDistance());
                }
            }
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent event) {
        ItemStack tool = event.getPlayer().getMainHandItem();
        if (level(tool, TTEnchantments.TUNNEL.get()) > 0) {
            tool.getOrCreateTag().putFloat("TTEnchantTunnelDir", event.getPlayer().getYRot());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()) {
            return;
        }
        float speed = event.getNewSpeed();
        float hardness = event.getPosition().map(pos -> event.getState().getDestroySpeed(player.level(), pos)).orElse(1f);
        // «Сокрушение»: очень твёрдые блоки ломаются в разы быстрее, остальные — медленнее
        int shatter = level(tool, TTEnchantments.SHATTER.get());
        if (shatter > 0) {
            speed = hardness > 20 ? speed * 3 * shatter : speed * 0.8f;
        }
        // «Туннель»: копать в ту же сторону быстрее, вбок — медленнее
        int tunnel = level(tool, TTEnchantments.TUNNEL.get());
        if (tunnel > 0 && tool.hasTag() && tool.getTag().contains("TTEnchantTunnelDir")) {
            float turn = Math.abs(tool.getTag().getFloat("TTEnchantTunnelDir") - player.getYRot());
            speed = turn < 50 ? speed * (1 + 0.2f * tunnel) : speed * 0.3f;
        }
        // «Распад» — мгновенно мягкие блоки, «Плавка» — мгновенно дерево; прочее такой инструмент не берёт
        boolean desintegrate = level(tool, TTEnchantments.DESINTEGRATE.get()) > 0;
        boolean autoSmelt = level(tool, TTEnchantments.AUTO_SMELT.get()) > 0;
        if (desintegrate || autoSmelt) {
            if (desintegrate && hardness <= 1.5f || autoSmelt && event.getState().is(BlockTags.MINEABLE_WITH_AXE)) {
                speed = Float.MAX_VALUE;
                if (!player.level().isClientSide) {
                    tool.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                }
            } else {
                event.setCanceled(true);
                return;
            }
        }
        event.setNewSpeed(speed);
    }
}
