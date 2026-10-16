package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModItems;

import java.util.EnumSet;

/**
 * Культист Багрового культа — общее у рыцаря и клирика (EntityCultist оригинала): ломают двери, держатся своего
 * дома (алтаря), все культисты заодно. Из них выпадают фрагменты знаний, семена пустоты и монеты, а редко — книга
 * «Багровые обряды».
 */
public abstract class Cultist extends Monster {
    protected Cultist(EntityType<? extends Cultist> type, Level level) {
        super(type, level);
        xpReward = 10;
        setCanPickUpLoot(false);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createCultistAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    /** addRandomArmor оригинала. */
    protected abstract void equip(RandomSource random, DifficultyInstance difficulty);

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        equip(level.getRandom(), difficulty);
        return result;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        int roll = random.nextInt(10);
        if (roll == 0) {
            spawnAtLocation(new ItemStack(ModItems.KNOWLEDGE_FRAGMENT.get()), 1.5f);
        } else if (roll <= 1) {
            spawnAtLocation(new ItemStack(ModItems.VOID_SEED.get()), 1.5f);
        } else if (roll <= 3 + looting) {
            spawnAtLocation(new ItemStack(ModItems.COIN.get()), 1.5f);
        }
        // dropRareDrop оригинала: при убийстве игроком (2,5% + 0,5% за уровень добычи) — книга обрядов
        if (recentlyHit && random.nextInt(200) - looting < 5) {
            spawnAtLocation(new ItemStack(ModItems.CRIMSON_RITES.get()), 1f);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof Cultist || other instanceof CultistLeader || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Cultist) && !(target instanceof CultistLeader) && super.canAttack(target);
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
    }

    /**
     * AICultistHurtByTarget оригинала: раненый культист зовёт на помощь всех культистов вокруг; клирики, занятые
     * обрядом, отвлекаются от него лишь иногда.
     */
    public static class HurtByTargetGoal extends TargetGoal {
        private int timestamp;

        public HurtByTargetGoal(Mob mob) {
            super(mob, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity attacker = mob.getLastHurtByMob();
            return mob.getLastHurtByMobTimestamp() != timestamp && attacker != null && canAttack(attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            LivingEntity attacker = mob.getLastHurtByMob();
            mob.setTarget(attacker);
            timestamp = mob.getLastHurtByMobTimestamp();
            double range = getFollowDistance();
            for (Cultist other : mob.level().getEntitiesOfClass(Cultist.class, new AABB(mob.blockPosition()).inflate(range, 10, range))) {
                if (other == mob || other.getTarget() != null || other.isAlliedTo(attacker)) {
                    continue;
                }
                if (other instanceof CultistCleric cleric && cleric.isRitualist()) {
                    if (mob.getRandom().nextInt(3) == 0) {
                        cleric.setRitualist(false);
                        cleric.setTarget(attacker);
                    }
                } else {
                    other.setTarget(attacker);
                }
            }
            super.start();
        }
    }
}
