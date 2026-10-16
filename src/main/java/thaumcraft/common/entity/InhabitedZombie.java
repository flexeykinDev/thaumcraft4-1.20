package thaumcraft.common.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

/**
 * Одержимый зомби — перенос EntityInhabitedZombie оригинала: зомби в латах культа, внутри которого сидит краб
 * Элдрича. Не зовёт подмогу, не превращает жертв; убитый, лопается, и из него выбирается краб в шлеме. Встречается
 * в Запредельных землях, не ближе 32 блоков к другому такому же.
 */
public class InhabitedZombie extends Zombie {
    public InhabitedZombie(EntityType<? extends InhabitedZombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean killedEntity(ServerLevel level, net.minecraft.world.entity.LivingEntity victim) {
        return true;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        float chance = level.getDifficulty() == Difficulty.HARD ? 0.9f : 0.6f;
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CULTIST_PLATE_HELMET.get()));
        if (random.nextFloat() <= chance) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CULTIST_PLATE_CHESTPLATE.get()));
        }
        if (random.nextFloat() <= chance) {
            setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CULTIST_PLATE_LEGGINGS.get()));
        }
        return data;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
    }

    @Override
    protected void dropAllDeathLoot(DamageSource source) {
    }

    /** onDeathUpdate оригинала: сразу после смерти тело лопается, и наружу выбирается краб в шлеме. */
    @Override
    protected void tickDeath() {
        if (!level().isClientSide) {
            EldritchCrab crab = ModEntities.ELDRITCH_CRAB.get().create(level());
            if (crab != null) {
                crab.moveTo(getX(), getY() + getEyeHeight(), getZ(), getYRot(), getXRot());
                crab.setHelm(true);
                level().addFreshEntity(crab);
            }
            if (lastHurtByPlayerTime > 0 && shouldDropExperience() && level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                ExperienceOrb.award((ServerLevel) level(), position(), getExperienceReward());
            }
        }
        for (int i = 0; i < 20; i++) {
            level().addParticle(ParticleTypes.POOF, getRandomX(1.0), getRandomY(), getRandomZ(1.0), random.nextGaussian() * 0.02,
                    random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
        }
        remove(RemovalReason.KILLED);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CRAB_TALK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HOSTILE_HURT;
    }

    /** getCanSpawnHere оригинала: не больше одного на 32 блока вокруг. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        return level.getEntitiesOfClass(InhabitedZombie.class, new AABB(blockPosition()).inflate(32, 16, 32)).isEmpty()
                && super.checkSpawnRules(level, reason);
    }
}
