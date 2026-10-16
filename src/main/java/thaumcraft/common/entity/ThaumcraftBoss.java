package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModItems;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранитель Запредельных земель — общее у боссов (EntityThaumcraftBoss оригинала). Полоса здоровья на экране;
 * не исчезает; медленно лечится; бьёт того, кто нанёс ему больше всего урона; за каждого лишнего игрока в бою
 * крепнет (+50 здоровья и +0,5 урона, до пяти раз). Удар сильнее 35 приводит его в ярость: он получает лишь 35,
 * а сам на 10 секунд ускоряется, крепнет и лечится. Пока не прошло время появления, неуязвим. Из него выпадают
 * первозданная жемчужина и редкий мешок с добычей.
 */
public abstract class ThaumcraftBoss extends Monster {
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(ThaumcraftBoss.class, EntityDataSerializers.INT);
    private static final UUID[] HP_BUFF = {UUID.fromString("54d621c1-dd4d-4b43-8bd2-5531c8875797"), UUID.fromString("f51257dc-b7fa-4f7a-92d7-75d68e8592c4"),
            UUID.fromString("3d6b2e42-4141-4364-b76d-0e8664bbd0bb"), UUID.fromString("02c97a08-801c-4131-afa2-1427a6151934"),
            UUID.fromString("0f354f6a-33c5-40be-93be-81b1338567f1")};
    private static final UUID[] DMG_BUFF = {UUID.fromString("534f8c57-929a-48cf-bbd6-0fd851030748"), UUID.fromString("d317a76e-0e7c-4c61-acfd-9fa286053b32"),
            UUID.fromString("ff462d63-26a2-4363-830e-143ed97e2a4f"), UUID.fromString("cf1eb39e-0c67-495f-887c-0d3080828d2f"),
            UUID.fromString("3cfab9da-2701-43d8-ac07-885f16fa4117")};
    protected final ServerBossEvent bossEvent = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Map<Integer, Integer> aggro = new HashMap<>();
    protected int spawnTimer;

    protected ThaumcraftBoss(EntityType<? extends ThaumcraftBoss> type, Level level) {
        super(type, level);
        xpReward = 50;
        setPersistenceRequired();
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANGER, 0);
    }

    public int getAnger() {
        return entityData.get(ANGER);
    }

    public void setAnger(int anger) {
        entityData.set(ANGER, anger);
    }

    public int getSpawnTimer() {
        return spawnTimer;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        restrictTo(blockPosition(), 24);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    /** Имя с титулом и свойством чемпиона (generateName оригинала); вызывается, когда босс уже стал чемпионом. */
    public void generateName() {
    }

    @Override
    protected void customServerAiStep() {
        if (spawnTimer == 0) {
            super.customServerAiStep();
        }
        if (getTarget() != null && !getTarget().isAlive()) {
            setTarget(null);
        }
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || spawnTimer > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (spawnTimer > 0) {
            spawnTimer--;
        }
        if (getAnger() > 0) {
            setAnger(getAnger() - 1);
        }
        if (level().isClientSide && random.nextInt(15) == 0 && getAnger() > 0) {
            level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2, getY() + getBbHeight()
                    + random.nextFloat() * 0.5, getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2, 0, 0, 0);
        }
        if (level().isClientSide) {
            return;
        }
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 30 == 0) {
            heal(1f);
        }
        if (getTarget() != null && tickCount % 20 == 0) {
            retarget();
        }
    }

    /** Переключение на самого опасного обидчика и усиление за каждого игрока, бьющего босса. */
    private void retarget() {
        int current = getTarget().getId();
        int currentAggro = aggro.getOrDefault(current, 0);
        int best = currentAggro;
        Entity newTarget = null;
        int players = 0;
        for (var it = aggro.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            int amount = entry.getValue();
            if (amount > currentAggro + 25 && amount > currentAggro * 1.1 && amount > best) {
                Entity candidate = level().getEntity(entry.getKey());
                if (candidate == null || !candidate.isAlive() || distanceToSqr(candidate) > 16384.0) {
                    it.remove();
                    continue;
                }
                newTarget = candidate;
                best = amount;
            }
        }
        for (Integer id : aggro.keySet()) {
            if (level().getEntity(id) instanceof Player player && player.isAlive()) {
                players++;
            }
        }
        if (newTarget instanceof LivingEntity living && newTarget.getId() != current) {
            setTarget(living);
        }
        float oldMax = getMaxHealth();
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damage = getAttribute(Attributes.ATTACK_DAMAGE);
        for (int a = 0; a < 5; a++) {
            health.removeModifier(HP_BUFF[a]);
            if (damage != null) {
                damage.removeModifier(DMG_BUFF[a]);
            }
        }
        for (int a = 0; a < Math.min(5, players - 1); a++) {
            health.addTransientModifier(new AttributeModifier(HP_BUFF[a], "Boss health buff", 50.0, AttributeModifier.Operation.ADDITION));
            if (damage != null) {
                damage.addTransientModifier(new AttributeModifier(DMG_BUFF[a], "Boss damage buff", 0.5, AttributeModifier.Operation.ADDITION));
            }
        }
        setHealth(getHealth() * (getMaxHealth() / oldMax));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (spawnTimer > 0 || isInvulnerableTo(source)) {
            return false;
        }
        if (!level().isClientSide) {
            if (source.getEntity() instanceof LivingEntity attacker) {
                aggro.merge(attacker.getId(), (int) amount, Integer::sum);
            }
            if (amount > 35f) {
                if (getAnger() == 0) {
                    addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (amount / 15f)));
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, (int) (amount / 40f)));
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, (int) (amount / 40f)));
                    setAnger(200);
                    if (source.getEntity() instanceof Player player) {
                        player.sendSystemMessage(getDisplayName().copy().append(" ").append(Component.translatable("tc.boss.enrage")));
                    }
                }
                amount = 35f;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && spawnTimer == 0;
    }

    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 speed) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof EldritchMob || super.isAlliedTo(other);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        ItemEntity pearl = spawnAtLocation(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), getBbHeight() / 2f);
        if (pearl != null) {
            pearl.setUnlimitedLifetime();
            pearl.setNoGravity(true);
        }
        spawnAtLocation(new ItemStack(ModItems.LOOT_BAG_RARE.get()), 1.5f);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) {
            BlockPos home = getRestrictCenter();
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
            tag.putInt("HomeD", (int) getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) {
            restrictTo(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), tag.getInt("HomeD"));
        }
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }
}
