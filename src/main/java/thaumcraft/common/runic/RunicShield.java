package thaumcraft.common.runic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.RunicArmor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.compat.Baubles;
import thaumcraft.common.items.RunicBaubleItem;
import thaumcraft.common.items.VisAmuletItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Рунный щит (EventHandlerRunic оригинала). Надетые вещи с рунной защитой складываются в общий запас единиц щита;
 * каждая единица поглощает единицу урона. Потраченный щит восстанавливается по единице раз в две секунды, и каждая
 * единица стоит по 0.5 вис воздуха и земли из амулета вис или жезла в инвентаре. Пробитый до нуля щит
 * начинает восстанавливаться только через четыре секунды.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class RunicShield {
    /** Сколько сотых долей вис воздуха и земли стоит единица щита (Config.shieldCost оригинала). */
    public static final int COST = 50;
    /** Миллисекунд между единицами восстановления (Config.shieldRecharge). */
    public static final int RECHARGE = 2000;
    /** Тактов до начала восстановления после того, как щит пробит (Config.shieldWait). */
    public static final int WAIT = 80;

    private static final class State {
        int max;
        int charged;
        int kinetic;
        int healing;
        int emergency;
        int charge;
        int lastSent = -1;
        int lastSentMax = -1;
        long nextCycle;
        int rechargeDelay;
        boolean dirty = true;
        final Map<Integer, Long> cooldowns = new HashMap<>();
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private RunicShield() {
    }

    private static State state(Entity entity) {
        return STATES.computeIfAbsent(entity.getUUID(), id -> new State());
    }

    /** Надели или сняли рунную вещь: запас пересчитывается сразу, а не раз в две секунды. */
    public static void markDirty(LivingEntity entity) {
        if (!entity.level().isClientSide) {
            state(entity).dirty = true;
        }
    }

    /**
     * Вещи с рунной защитой, у которых своего щита нет (в оригинале они тоже были IRunicArmor с нулём): броня мага
     * и простые украшения. Щит им даёт только «Рунное усиление».
     */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> RUNIC_ARMOR = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, Thaumcraft.id("runic_armor"));

    public static boolean isRunicArmor(ItemStack stack) {
        return stack.getItem() instanceof RunicArmor || stack.is(RUNIC_ARMOR);
    }

    /** Сколько единиц щита вещи добавлено «Рунным усилением». */
    public static int getHardening(ItemStack stack) {
        return isRunicArmor(stack) && stack.hasTag() ? stack.getTag().getByte("RS.HARDEN") : 0;
    }

    /** Единицы щита, которые даёт вещь: её собственные и добавленные наполнением «Рунное усиление». */
    public static int getFinalCharge(ItemStack stack) {
        if (!isRunicArmor(stack)) {
            return 0;
        }
        int base = stack.getItem() instanceof RunicArmor armor ? armor.getRunicCharge(stack) : 0;
        return base + getHardening(stack);
    }

    public static int getCharge(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.charge;
    }

    public static int getMax(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? 0 : state.max;
    }

    /** Для проверок: выставить текущий заряд. */
    public static void setCharge(Player player, int charge) {
        State state = state(player);
        state.charge = Math.min(charge, state.max);
    }

    /**
     * WandManager.consumeVisFromInventory оригинала: вис берётся сперва из надетых амулетов вис, затем из жезлов
     * в инвентаре, начиная с последних ячеек. Набор списывается из одного предмета целиком.
     */
    public static boolean consumeVisFromInventory(Player player, AspectList cost) {
        for (ItemStack amulet : Baubles.find(player, s -> s.getItem() instanceof VisAmuletItem)) {
            if (((VisAmuletItem) amulet.getItem()).consumeAllVis(amulet, cost, true)) {
                return true;
            }
        }
        for (int slot = player.getInventory().items.size() - 1; slot >= 0; slot--) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.getItem() instanceof WandItem && WandItem.consumeAllCentiVis(stack, player, cost, false)
                    && WandItem.consumeAllCentiVis(stack, player, cost, true)) {
                return true;
            }
        }
        return false;
    }

    private static void recount(ServerPlayer player, State state) {
        state.dirty = false;
        int max = 0;
        state.charged = 0;
        state.kinetic = 0;
        state.healing = 0;
        state.emergency = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            max += getFinalCharge(armor);
        }
        for (ItemStack bauble : Baubles.find(player, s -> getFinalCharge(s) > 0)) {
            max += getFinalCharge(bauble);
            if (bauble.getItem() instanceof RunicBaubleItem runic) {
                switch (runic.getKind()) {
                    case CHARGED -> state.charged++;
                    case HEALING -> state.healing++;
                    case EMERGENCY -> state.emergency++;
                    case KINETIC -> state.kinetic++;
                    default -> {
                    }
                }
            }
        }
        state.max = max;
        if (state.charge > max) {
            state.charge = max;
        }
    }

    private static void send(ServerPlayer player, State state) {
        if (state.lastSent != state.charge || state.lastSentMax != state.max) {
            state.lastSent = state.charge;
            state.lastSentMax = state.max;
            FxPacket.sendTo(player, FxPacket.RUNIC_CHARGE, Vec3.ZERO, Vec3.ZERO, state.charge, state.max);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        State state = state(player);
        if (state.dirty || player.tickCount % 40 == 0) {
            recount(player, state);
        }
        if (state.rechargeDelay > 0) {
            state.rechargeDelay--;
        } else if (state.max > 0 && state.charge < state.max) {
            long time = System.currentTimeMillis();
            if (state.nextCycle < time && consumeVisFromInventory(player,
                    new AspectList().add(Aspect.AIR, COST).add(Aspect.EARTH, COST))) {
                // каждое заряженное кольцо ускоряет восстановление на полсекунды
                state.nextCycle = time + Math.max(250, RECHARGE - state.charged * 500);
                state.charge++;
            }
        }
        send(player, state);
    }

    private static boolean ready(State state, int upgrade, long time, long cooldown) {
        Long until = state.cooldowns.get(upgrade);
        if (until != null && until >= time) {
            return false;
        }
        state.cooldowns.put(upgrade, time + cooldown);
        return true;
    }

    /** Куда повёрнута вспышка щита: -1 — со всех сторон, -2 — снизу (падение), -3 — сверху, иначе номер ударившего. */
    private static int fxTarget(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            return -2;
        }
        if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_STALACTITE)) {
            return -3;
        }
        return source.getEntity() != null ? source.getEntity().getId() : -1;
    }

    /** Вспышка щита вокруг существа видна всем поблизости. */
    public static void showShield(LivingEntity entity, DamageSource source) {
        if (entity.level() instanceof ServerLevel level) {
            FxPacket.send(level, FxPacket.SHIELD, entity.position(), Vec3.ZERO, entity.getId(), fxTarget(source));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void hurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        // от утопления, иссушения, пустоты и голода щит не спасает
        if (source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.STARVE)) {
            return;
        }
        State state = STATES.get(player.getUUID());
        if (state == null || state.max <= 0 || state.charge <= 0) {
            return;
        }
        long time = System.currentTimeMillis();
        showShield(player, source);
        int charge = state.charge;
        float amount = event.getAmount();
        if (charge > amount) {
            charge = (int) (charge - amount);
            amount = 0f;
        } else {
            amount -= charge;
            charge = 0;
        }
        event.setAmount(amount);
        Level level = player.level();
        if (charge <= 0 && state.kinetic > 0 && ready(state, 2, time, 20000L)) {
            // кинетический пояс: взрыв отбрасывает врагов, блоки не ломает
            level.explode(player, player.getX(), player.getY() + player.getBbHeight() / 2f, player.getZ(),
                    1.5f + state.kinetic * 0.5f, false, Level.ExplosionInteraction.NONE);
        }
        if (charge <= 0 && state.healing > 0 && ready(state, 3, time, 20000L)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, state.healing));
            level.playSound(null, player, ModSounds.RUNIC_SHIELD_EFFECT.get(), SoundSource.PLAYERS, 1f, 1f);
        }
        if (charge <= 0 && state.emergency > 0 && ready(state, 4, time, 60000L)) {
            charge = Math.min(state.max, 8 * state.emergency);
            state.dirty = true;
            level.playSound(null, player, ModSounds.RUNIC_SHIELD_CHARGE.get(), SoundSource.PLAYERS, 1f, 1f);
        }
        if (charge <= 0) {
            state.rechargeDelay = WAIT;
        }
        state.charge = charge;
        send(player, state);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
