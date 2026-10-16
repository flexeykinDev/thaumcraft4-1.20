package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModSounds;

/**
 * Краб Элдрича — перенос EntityEldritchCrab оригинала. Мелкий, прыгучий; часть крабов в шлеме из лат культа
 * (броня 5, медленнее; шлем слетает, когда у краба остаётся половина здоровья). Без шлема краб, допрыгнув,
 * цепляется за жертву и грызёт её сверху. Враждует с игроками и культистами; из него иногда выпадает жемчуг Края.
 */
public class EldritchCrab extends Monster {
    private static final EntityDataAccessor<Boolean> HELM = SynchedEntityData.defineId(EldritchCrab.class, EntityDataSerializers.BOOLEAN);
    private int biteDelay;

    public EldritchCrab(EntityType<? extends EldritchCrab> type, Level level) {
        super(type, level);
        xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HELM, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.63f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    public boolean hasHelm() {
        return entityData.get(HELM);
    }

    public void setHelm(boolean helm) {
        entityData.set(HELM, helm);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(helm ? 0.275 : 0.3);
        }
    }

    @Override
    public int getArmorValue() {
        return hasHelm() ? 5 : 0;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        setHelm(level.getDifficulty() == Difficulty.HARD || random.nextFloat() < 0.33f);
        // как у пауков: на высокой сложности краб иногда рождается с полезным эффектом
        if (level.getDifficulty() == Difficulty.HARD && random.nextFloat() < 0.1f * difficulty.getSpecialMultiplier()) {
            var effect = switch (random.nextInt(5)) {
                case 0 -> MobEffects.MOVEMENT_SPEED;
                case 1 -> MobEffects.DAMAGE_BOOST;
                case 2 -> MobEffects.REGENERATION;
                case 3 -> MobEffects.INVISIBILITY;
                default -> null;
            };
            if (effect != null) {
                addEffect(new MobEffectInstance(effect, -1));
            }
        }
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public double getMyRidingOffset() {
        return isPassenger() ? 0.5 : 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount < 20) {
            fallDistance = 0f;
        }
        LivingEntity attacker = getLastHurtByMob();
        if (!level().isClientSide && getVehicle() == null && attacker != null && attacker.getPassengers().isEmpty() && !onGround() && !hasHelm()
                && attacker.isAlive() && getY() - attacker.getY() >= attacker.getBbHeight() / 2f && distanceToSqr(attacker) < 4.0) {
            startRiding(attacker, true);
        }
        if (!level().isClientSide && getVehicle() != null && --biteDelay <= 0) {
            biteDelay = 10 + random.nextInt(10);
            doHurtTarget(getVehicle());
            if (getVehicle() != null && random.nextFloat() < 0.2f) {
                stopRiding();
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            playSound(ModSounds.CRAB_CLAW.get(), 1f, 0.9f + random.nextFloat() * 0.2f);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hasHelm() && getHealth() / getMaxHealth() <= 0.5f) {
            setHelm(false);
            playSound(SoundEvents.ITEM_BREAK, 0.8f, 0.8f + random.nextFloat() * 0.4f);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit && (random.nextInt(3) == 0 || random.nextInt(1 + looting) > 0)) {
            spawnAtLocation(Items.ENDER_PEARL);
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof EldritchCrab || super.isAlliedTo(other);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRAB_TALK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOSTILE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRAB_DEATH.get();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState state) {
        playSound(SoundEvents.SPIDER_STEP, 0.15f, 1f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Helm", hasHelm());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHelm(tag.getBoolean("Helm"));
    }
}
