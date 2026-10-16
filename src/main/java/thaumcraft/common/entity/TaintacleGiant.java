package thaumcraft.common.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.champion.Champions;
import thaumcraft.common.registry.ModItems;

/**
 * Гигантское щупальце порчи — перенос EntityTaintacleGiant оригинала, хранитель «порченого» зала лабиринта:
 * шестиметровое щупальце-чемпион (125 здоровья, 9 урона) с полосой здоровья на экране. Как прочие хранители,
 * от удара сильнее 35 приходит в ярость; не исчезает, не тонет, медленно лечится. Первозданная жемчужина
 * выпадает из последнего гигантского щупальца в округе.
 */
public class TaintacleGiant extends Taintacle {
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(TaintacleGiant.class, EntityDataSerializers.INT);
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public TaintacleGiant(EntityType<? extends TaintacleGiant> type, Level level) {
        super(type, level);
        xpReward = 20;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Taintacle.createAttributes().add(Attributes.MAX_HEALTH, 125.0).add(Attributes.ATTACK_DAMAGE, 9.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANGER, 0);
    }

    public int getAnger() {
        return entityData.get(ANGER);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        Champions.makeChampion(this, true);
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (getAnger() > 0) {
            entityData.set(ANGER, getAnger() - 1);
        }
        if (level().isClientSide) {
            if (random.nextInt(15) == 0 && getAnger() > 0) {
                level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2, getY() + getBbHeight()
                        + random.nextFloat() * 0.5, getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2, random.nextGaussian() * 0.02,
                        random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
            }
            return;
        }
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 30 == 0) {
            heal(1f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 35f) {
            if (getAnger() == 0) {
                addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (amount / 15f)));
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, (int) (amount / 40f)));
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, (int) (amount / 40f)));
                entityData.set(ANGER, 200);
                if (source.getEntity() instanceof Player player) {
                    player.sendSystemMessage(getDisplayName().copy().append(" ").append(Component.translatable("tc.boss.enrage")));
                }
            }
            amount = 35f;
        }
        return super.hurt(source, amount);
    }

    /** Жемчужина — только из последнего гиганта в 48 блоках. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        if (level().getEntitiesOfClass(TaintacleGiant.class, new AABB(blockPosition()).inflate(48.0), e -> e != this && e.isAlive()).isEmpty()) {
            ItemEntity pearl = spawnAtLocation(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), getBbHeight() / 2f);
            if (pearl != null) {
                pearl.setUnlimitedLifetime();
                pearl.setNoGravity(true);
                pearl.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
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
}
