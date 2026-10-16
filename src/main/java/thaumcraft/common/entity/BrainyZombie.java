package thaumcraft.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * Злой зомби (EntityBrainyZombie оригинала, «Angry Zombie»): зомби покрепче обычного, обитатель жутких земель.
 * Не зовёт подкрепление, зато из него выпадает мозг.
 */
public class BrainyZombie extends Zombie {
    public BrainyZombie(EntityType<? extends BrainyZombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 5.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    /** У обычного зомби шанс подкрепления назначается случайно при появлении; у злого его нет вовсе. */
    @Override
    protected void randomizeReinforcementsChance() {
        getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
    }

    /**
     * Яростный зомби (EntityGiantBrainyZombie оригинала, «Furious Zombie»): тот же злой зомби, только здоровее, и от
     * каждого удара он злится — растёт и бьёт больнее, а потом понемногу остывает. Его призывают тёмные узлы.
     */
    public static class Giant extends BrainyZombie {
        private static final EntityDataAccessor<Float> ANGER = SynchedEntityData.defineId(Giant.class, EntityDataSerializers.FLOAT);

        public Giant(EntityType<? extends Giant> type, Level level) {
            super(type, level);
            this.xpReward = 15;
            // размеры типа — как у обычного зомби; свои, увеличенные, надо пересчитать сразу
            refreshDimensions();
        }

        public static AttributeSupplier.Builder createAttributes() {
            return BrainyZombie.createAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 7.0);
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            entityData.define(ANGER, 1f);
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        }

        /** Злость: от 1 (спокоен) до 2. */
        public float getAnger() {
            return entityData.get(ANGER);
        }

        public void setAnger(float anger) {
            entityData.set(ANGER, anger);
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
            if (ANGER.equals(key)) {
                refreshDimensions();
            }
            super.onSyncedDataUpdated(key);
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return super.getDimensions(pose).scale(1.2f + getAnger());
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide) {
                return;
            }
            if (getAnger() > 1f) {
                setAnger(getAnger() - 0.002f);
            }
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7f + (getAnger() - 1f) * 5f);
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (!level().isClientSide) {
                setAnger(Math.min(2f, getAnger() + 0.1f));
            }
            return super.hurt(source, amount);
        }
    }
}
