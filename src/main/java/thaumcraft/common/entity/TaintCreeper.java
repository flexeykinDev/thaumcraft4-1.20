package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.taint.Taint;

import java.util.EnumSet;

/**
 * Порченый крипер (EntityTaintCreeper оригинала): крипер, умерший от порченого яда. Взрыв у него слабый и блоков
 * не ломает, зато отравляет порченым ядом всё живое в шести блоках и заражает землю вокруг.
 */
public class TaintCreeper extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Integer> SWELL_DIR = SynchedEntityData.defineId(TaintCreeper.class, EntityDataSerializers.INT);

    private int oldSwell;
    private int swell;
    private int fuseTime = 30;

    public TaintCreeper(EntityType<? extends TaintCreeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SWELL_DIR, -1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SwellGoal(this));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Ocelot.class, 6f, 1.0, 1.2));
        goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Cat.class, 6f, 1.0, 1.2));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return getTarget() == null ? 3 : 3 + (int) (getHealth() - 1f);
    }

    /** Упав с высоты, крипер почти готов взорваться. */
    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        boolean hurt = super.causeFallDamage(distance, multiplier, source);
        swell = Math.min((int) (swell + distance * 1.5f), fuseTime - 5);
        return hurt;
    }

    public int getSwellDir() {
        return entityData.get(SWELL_DIR);
    }

    public void setSwellDir(int dir) {
        entityData.set(SWELL_DIR, dir);
    }

    @Override
    public void tick() {
        if (isAlive()) {
            oldSwell = swell;
            int dir = getSwellDir();
            if (dir > 0 && swell == 0) {
                playSound(SoundEvents.CREEPER_PRIMED, 1f, 0.5f);
            }
            swell = Math.max(0, swell + dir);
            if (swell >= 30) {
                swell = 30;
                if (level() instanceof ServerLevel level) {
                    explode(level);
                } else {
                    thaumcraft.client.fx.Fx.taintsplosion(this);
                }
            }
        }
        super.tick();
    }

    private void explode(ServerLevel level) {
        level.explode(this, getX(), getY() + getBbHeight() / 2f, getZ(), 1.5f, Level.ExplosionInteraction.NONE);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6.0))) {
            if (!(living instanceof TaintedMob) && living.getMobType() != MobType.UNDEAD) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 100, 0, false, true));
            }
        }
        Taint.splash(level, blockPosition(), 5f, false);
        discard();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && tickCount < 5) {
            thaumcraft.client.fx.Fx.sploosh(this);
        }
    }

    /** Насколько крипер раздулся перед взрывом: от 0 до 1 с небольшим. */
    public float getSwelling(float partialTick) {
        return Mth.lerp(partialTick, (float) oldSwell, (float) swell) / 28f;
    }

    /** Сам он не бьёт: подошёл вплотную — взрывается. */
    @Override
    public boolean doHurtTarget(Entity target) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putShort("Fuse", (short) fuseTime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Fuse")) {
            fuseTime = tag.getShort("Fuse");
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CREEPER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CREEPER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    /** AICreeperSwell оригинала: раздувается рядом с целью и сдувается, потеряв её из виду или отстав на семь блоков. */
    private static class SwellGoal extends Goal {
        private final TaintCreeper creeper;
        private LivingEntity target;

        SwellGoal(TaintCreeper creeper) {
            this.creeper = creeper;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity living = creeper.getTarget();
            return creeper.getSwellDir() > 0 || living != null && creeper.distanceToSqr(living) < 9.0;
        }

        @Override
        public void start() {
            creeper.getNavigation().stop();
            target = creeper.getTarget();
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null || creeper.distanceToSqr(target) > 49.0 || !creeper.getSensing().hasLineOfSight(target)) {
                creeper.setSwellDir(-1);
            } else {
                creeper.setSwellDir(1);
            }
        }
    }
}
