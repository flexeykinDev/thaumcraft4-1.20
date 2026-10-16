package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.IForgeShearable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.taint.Taint;
import thaumcraft.common.taint.TaintFibresBlock;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Порченые животные и житель (EntityTaintCow, EntityTaintPig, EntityTaintSheep, EntityTaintChicken и
 * EntityTaintVillager оригинала): то, чем становится скотина или житель, умершие от порченого яда. Все они враждебны
 * и нападают на игроков и жителей, а корова, свинья и курица — ещё и на обычных животных.
 */
public final class TaintedBeasts {
    private TaintedBeasts() {
    }

    /** Общее для всех порченых существ: брызги слизи при появлении и низкий, «больной» голос. */
    public abstract static class Beast extends Monster implements TaintedMob {
        protected Beast(EntityType<? extends Beast> type, Level level) {
            super(type, level);
        }

        /** Обычный набор: плавает, бьёт в ближнем бою, бродит, глазеет на игроков. */
        protected void commonGoals(int wanderPriority) {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
            goalSelector.addGoal(wanderPriority, new WaterAvoidingRandomStrollGoal(this, 1.0));
            goalSelector.addGoal(wanderPriority + 1, new LookAtPlayerGoal(this, Player.class, 6f));
            goalSelector.addGoal(wanderPriority + 2, new RandomLookAroundGoal(this));
            targetSelector.addGoal(0, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide && tickCount < 5) {
                thaumcraft.client.fx.Fx.sploosh(this);
            }
        }

        @Override
        public float getVoicePitch() {
            return 0.7f;
        }
    }

    // ---------- корова ----------

    public static class Cow extends Beast {
        public Cow(EntityType<? extends Cow> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 6.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.27);
        }

        @Override
        protected void registerGoals() {
            commonGoals(5);
            targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Animal.class, false));
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.COW_AMBIENT;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.COW_HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.COW_HURT;
        }

        @Override
        protected void playStepSound(BlockPos pos, BlockState state) {
            playSound(SoundEvents.COW_STEP, 0.15f, 1f);
        }

        @Override
        protected float getSoundVolume() {
            return 0.4f;
        }
    }

    // ---------- свинья ----------

    public static class Pig extends Beast {
        public Pig(EntityType<? extends Pig> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.275).add(Attributes.ARMOR, 2.0);
        }

        @Override
        protected void registerGoals() {
            commonGoals(5);
            targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Animal.class, false));
        }

        @Override
        public boolean removeWhenFarAway(double distance) {
            return false;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.PIG_AMBIENT;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.PIG_AMBIENT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.PIG_DEATH;
        }

        @Override
        protected void playStepSound(BlockPos pos, BlockState state) {
            playSound(SoundEvents.PIG_STEP, 0.15f, 1f);
        }
    }

    // ---------- курица ----------

    public static class Chicken extends Beast {
        public float flap;
        public float flapSpeed;
        public float oFlapSpeed;
        public float oFlap;
        private float flapping = 1f;

        public Chicken(EntityType<? extends Chicken> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.4).add(Attributes.ARMOR, 2.0);
        }

        @Override
        protected void registerGoals() {
            commonGoals(3);
            goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.3f));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, false));
        }

        @Override
        public boolean removeWhenFarAway(double distance) {
            return false;
        }

        /** Машет крыльями в воздухе и планирует вниз, как обычная курица. */
        @Override
        public void aiStep() {
            super.aiStep();
            oFlap = flap;
            oFlapSpeed = flapSpeed;
            flapSpeed = Mth.clamp(flapSpeed + (onGround() ? -1f : 4f) * 0.3f, 0f, 1f);
            if (!onGround() && flapping < 1f) {
                flapping = 1f;
            }
            flapping *= 0.9f;
            Vec3 motion = getDeltaMovement();
            if (!onGround() && motion.y < 0) {
                setDeltaMovement(motion.multiply(1.0, 0.9, 1.0));
            }
            flap += flapping * 2f;
        }

        @Override
        public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
            return false;
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.CHICKEN_AMBIENT;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.CHICKEN_HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.CHICKEN_HURT;
        }
    }

    // ---------- овца ----------

    /**
     * Порченая овца на животных не охотится, зато «щиплет траву»: там, где она пасётся, вырастают волокна порчи,
     * а земля становится порченой. Её можно остричь — шерсть лиловая.
     */
    public static class Sheep extends Beast implements IForgeShearable {
        private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(Sheep.class, EntityDataSerializers.BOOLEAN);
        private int eatAnimationTick;
        private ConvertGrassGoal convertGoal;

        public Sheep(EntityType<? extends Sheep> type, Level level) {
            super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ARMOR, 1.0);
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            entityData.define(SHEARED, false);
        }

        @Override
        protected void registerGoals() {
            convertGoal = new ConvertGrassGoal(this);
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, convertGoal);
            goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6f));
            goalSelector.addGoal(8, new RandomLookAroundGoal(this));
            targetSelector.addGoal(0, new HurtByTargetGoal(this));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        }

        @Override
        public boolean removeWhenFarAway(double distance) {
            return false;
        }

        @Override
        protected void customServerAiStep() {
            eatAnimationTick = convertGoal.getEatAnimationTick();
            super.customServerAiStep();
        }

        @Override
        public void aiStep() {
            if (level().isClientSide) {
                eatAnimationTick = Math.max(0, eatAnimationTick - 1);
            }
            super.aiStep();
        }

        @Override
        public void handleEntityEvent(byte id) {
            if (id == 10) {
                eatAnimationTick = 40;
            } else {
                super.handleEntityEvent(id);
            }
        }

        /** Насколько опущена голова, пока овца пасётся. */
        public float getHeadEatPositionScale(float partialTick) {
            if (eatAnimationTick <= 0) {
                return 0f;
            } else if (eatAnimationTick >= 4 && eatAnimationTick <= 36) {
                return 1f;
            }
            return eatAnimationTick < 4 ? (eatAnimationTick - partialTick) / 4f : -(eatAnimationTick - 40 - partialTick) / 4f;
        }

        /** Наклон головы, пока овца пасётся: голова ходит вверх-вниз. */
        public float getHeadEatAngleScale(float partialTick) {
            if (eatAnimationTick > 4 && eatAnimationTick <= 36) {
                float progress = (eatAnimationTick - 4 - partialTick) / 32f;
                return (float) Math.PI / 5f + 0.21991149f * Mth.sin(progress * 28.7f);
            }
            return eatAnimationTick > 0 ? (float) Math.PI / 5f : getXRot() * Mth.DEG_TO_RAD;
        }

        public boolean isSheared() {
            return entityData.get(SHEARED);
        }

        public void setSheared(boolean sheared) {
            entityData.set(SHEARED, sheared);
        }

        @Override
        public boolean isShearable(@NotNull ItemStack item, Level level, BlockPos pos) {
            return isAlive() && !isSheared();
        }

        @NotNull
        @Override
        public List<ItemStack> onSheared(@Nullable Player player, @NotNull ItemStack item, Level level, BlockPos pos, int fortune) {
            level.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.HOSTILE, 1f, 1f);
            List<ItemStack> wool = new ArrayList<>();
            if (!level.isClientSide) {
                setSheared(true);
                int count = 1 + random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    wool.add(new ItemStack(Items.PURPLE_WOOL));
                }
            }
            return wool;
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            tag.putBoolean("Sheared", isSheared());
        }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            setSheared(tag.getBoolean("Sheared"));
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.SHEEP_AMBIENT;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.SHEEP_AMBIENT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.SHEEP_AMBIENT;
        }

        @Override
        protected void playStepSound(BlockPos pos, BlockState state) {
            playSound(SoundEvents.SHEEP_STEP, 0.15f, 1f);
        }
    }

    /**
     * AIConvertGrass оригинала: овца на траве изредка останавливается «попастись» — и на этом месте вырастают
     * волокна порчи, а земля заражается. Как и поедание травы обычной овцой, зависит от правила mobGriefing.
     */
    private static class ConvertGrassGoal extends Goal {
        private final Sheep sheep;
        private int eatAnimationTick;

        ConvertGrassGoal(Sheep sheep) {
            this.sheep = sheep;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (sheep.getRandom().nextInt(250) != 0) {
                return false;
            }
            BlockPos pos = sheep.blockPosition();
            Level level = sheep.level();
            return level.getBlockState(pos).is(Blocks.GRASS) || level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK);
        }

        @Override
        public void start() {
            eatAnimationTick = adjustedTickDelay(40);
            sheep.level().broadcastEntityEvent(sheep, (byte) 10);
            sheep.getNavigation().stop();
        }

        @Override
        public void stop() {
            eatAnimationTick = 0;
        }

        @Override
        public boolean canContinueToUse() {
            return eatAnimationTick > 0;
        }

        int getEatAnimationTick() {
            return eatAnimationTick;
        }

        @Override
        public void tick() {
            eatAnimationTick = Math.max(0, eatAnimationTick - 1);
            if (eatAnimationTick != adjustedTickDelay(4) || !(sheep.level() instanceof ServerLevel level)
                    || !net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, sheep)) {
                return;
            }
            BlockPos pos = sheep.blockPosition();
            if (level.getBlockState(pos).is(Blocks.GRASS)) {
                level.destroyBlock(pos, false);
            } else if (level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) && level.getBlockState(pos).canBeReplaced()) {
                level.levelEvent(2001, pos.below(), net.minecraft.world.level.block.Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
            } else {
                return;
            }
            Taint.setTainted(level, pos);
            level.setBlock(pos, TaintFibresBlock.stateFor(level, pos), 3);
            sheep.ate();
        }
    }

    // ---------- житель ----------

    /** Порченый житель: бродит по своей деревне, открывает двери и нападает на игроков. */
    public static class Villager extends Beast {
        public Villager(EntityType<? extends Villager> type, Level level) {
            super(type, level);
            ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
        }

        public static AttributeSupplier.Builder createAttributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                    .add(Attributes.MOVEMENT_SPEED, 0.3);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
            goalSelector.addGoal(4, new OpenDoorGoal(this, true));
            goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
            goalSelector.addGoal(6, new MoveThroughVillageGoal(this, 1.0, false, 4, () -> true));
            goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3f, 1f));
            goalSelector.addGoal(9, new LookAtPlayerGoal(this, AbstractVillager.class, 5f, 0.02f));
            goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, 1.0));
            goalSelector.addGoal(10, new LookAtPlayerGoal(this, net.minecraft.world.entity.LivingEntity.class, 8f));
            targetSelector.addGoal(0, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }

        @Override
        public boolean removeWhenFarAway(double distance) {
            return false;
        }

        /** Деревня считает порченого жителя своим: ударивший его портит себе репутацию у всех жителей вокруг. */
        @Override
        public void setLastHurtByMob(@Nullable net.minecraft.world.entity.LivingEntity attacker) {
            super.setLastHurtByMob(attacker);
            if (attacker instanceof Player && level() instanceof ServerLevel server) {
                for (net.minecraft.world.entity.npc.Villager villager : server.getEntitiesOfClass(
                        net.minecraft.world.entity.npc.Villager.class, getBoundingBox().inflate(32.0))) {
                    server.onReputationEvent(net.minecraft.world.entity.ai.village.ReputationEventType.VILLAGER_HURT, attacker, villager);
                }
            }
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return SoundEvents.VILLAGER_AMBIENT;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return SoundEvents.VILLAGER_HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return SoundEvents.VILLAGER_DEATH;
        }
    }
}
