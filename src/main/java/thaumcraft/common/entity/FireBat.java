package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Огненная летучая мышь (EntityFireBat оригинала). Дикие живут в Нижнем мире: висят под потолком, кидаются
 * на игрока, поджигают и изредка взрываются. Призванные фокусом «Девять кругов ада» летят к указанной цели,
 * а оставшись без неё, быстро сгорают.
 */
public class FireBat extends Bat implements Enemy {
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(FireBat.class, EntityDataSerializers.BYTE);
    private static final int SUMMONED = 2;
    private static final int EXPLOSIVE = 4;
    private static final int DEVIL = 8;
    private static final int VAMPIRE = 16;

    @Nullable
    public Player owner;
    public int damBonus;
    @Nullable
    private BlockPos flightTarget;
    private int attackTime;

    public FireBat(EntityType<? extends FireBat> type, Level level) {
        super(type, level);
        setResting(true);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bat.createAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.FOLLOW_RANGE, 32);
    }

    /** Как в оригинале: появляется только в темноте (чем светлее, тем реже) и не на мирной сложности. */
    public static boolean canSpawn(EntityType<FireBat> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getMaxLocalRawBrightness(pos) <= random.nextInt(7);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FLAGS, (byte) 0);
    }

    private boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    private void flag(int bit, boolean on) {
        byte flags = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? flags | bit : flags & ~bit));
    }

    public boolean isSummoned() {
        return flag(SUMMONED);
    }

    public boolean isExplosive() {
        return flag(EXPLOSIVE);
    }

    public boolean isDevil() {
        return flag(DEVIL);
    }

    public boolean isVampire() {
        return flag(VAMPIRE);
    }

    private void refreshDamage() {
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(isSummoned() ? (isDevil() ? 3 : 2) + damBonus : 1);
    }

    public void setSummoned(boolean summoned) {
        flag(SUMMONED, summoned);
        refreshDamage();
    }

    public void setExplosive(boolean explosive) {
        flag(EXPLOSIVE, explosive);
    }

    /** Дьявольская мышь крупнее и втрое живучее, зато никогда не взрывается. */
    public void setDevil(boolean devil) {
        flag(DEVIL, devil);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(devil ? 15 : 5);
        if (devil) {
            setHealth(getMaxHealth());
        }
        refreshDamage();
    }

    public void setVampire(boolean vampire) {
        flag(VAMPIRE, vampire);
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    protected boolean shouldDropLoot() {
        return !isSummoned();
    }

    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide && isInWaterRainOrBubble()) {
            hurt(damageSources().drown(), 1f);
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !isVampire()) {
            double x = xo + (random.nextFloat() - random.nextFloat()) * 0.2f;
            double y = yo + getBbHeight() / 2f + (random.nextFloat() - random.nextFloat()) * 0.2f;
            double z = zo + (random.nextFloat() - random.nextFloat()) * 0.2f;
            level().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
            level().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0, 0);
        }
    }

    private void steer(double dx, double dy, double dz) {
        Vec3 motion = getDeltaMovement();
        Vec3 next = motion.add((Math.signum(dx) * 0.5 - motion.x) * 0.1, (Math.signum(dy) * 0.7 - motion.y) * 0.1,
                (Math.signum(dz) * 0.5 - motion.z) * 0.1);
        setDeltaMovement(next);
        float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90f;
        zza = 0.5f;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
    }

    /** updateEntityActionState оригинала: своя простая логика вместо обычных задач моба. */
    @Override
    protected void customServerAiStep() {
        if (attackTime > 0) {
            attackTime--;
        }
        BlockPos above = blockPosition().above();
        if (isResting()) {
            boolean ceiling = level().getBlockState(above).isRedstoneConductor(level(), above);
            if (!ceiling || level().getNearestPlayer(this, 4) != null) {
                setResting(false);
                if (!isSilent()) {
                    level().levelEvent(null, 1025, blockPosition(), 0);
                }
            } else if (random.nextInt(200) == 0) {
                yHeadRot = random.nextInt(360);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target instanceof Player player && player.getAbilities().invulnerable)) {
            setTarget(null);
            target = null;
        }
        if (target == null && !isSummoned()) {
            Player prey = level().getNearestPlayer(this, 12);
            if (prey != null && !prey.getAbilities().invulnerable && level().getDifficulty() != Difficulty.PEACEFUL) {
                setTarget(prey);
                target = prey;
            }
        }
        if (target == null) {
            // призванная мышь без цели сгорает за пару мгновений
            if (isSummoned()) {
                hurt(damageSources().generic(), 2f);
            }
            if (flightTarget != null && (!level().isEmptyBlock(flightTarget) || flightTarget.getY() <= level().getMinBuildHeight())) {
                flightTarget = null;
            }
            if (flightTarget == null || random.nextInt(30) == 0 || flightTarget.distSqr(blockPosition()) < 4) {
                flightTarget = blockPosition().offset(random.nextInt(7) - random.nextInt(7), random.nextInt(6) - 2,
                        random.nextInt(7) - random.nextInt(7));
            }
            steer(flightTarget.getX() + 0.5 - getX(), flightTarget.getY() + 0.1 - getY(), flightTarget.getZ() + 0.5 - getZ());
            if (random.nextInt(100) == 0 && level().getBlockState(above).isRedstoneConductor(level(), above)) {
                setResting(true);
            }
        } else {
            steer(target.getX() - getX(), target.getY() + target.getEyeHeight() * 0.66f - getY(), target.getZ() - getZ());
            attack(target);
        }
    }

    /** attackEntity оригинала: укус, поджог либо взрыв — раз в секунду и только вплотную. */
    private void attack(LivingEntity target) {
        if (attackTime > 0 || distanceTo(target) >= Math.max(2.5f, target.getBbWidth() * 1.1f)
                || target.getBoundingBox().maxY <= getBoundingBox().minY || target.getBoundingBox().minY >= getBoundingBox().maxY) {
            return;
        }
        if (isSummoned() && owner != null) {
            // добыча и опыт с убитого достаются призвавшему
            target.setLastHurtByPlayer(owner);
        }
        if (isVampire()) {
            if (owner != null && !owner.hasEffect(MobEffects.REGENERATION)) {
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 26, 1));
            }
            heal(1f);
        }
        attackTime = 20;
        if ((isExplosive() || random.nextInt(10) == 0) && !isDevil()) {
            target.invulnerableTime = 0;
            // взрыв ранит, но блоки не ломает
            level().explode(this, getX(), getY(), getZ(), 1.5f + (isExplosive() ? damBonus * 0.33f : 0f), false, Level.ExplosionInteraction.NONE);
            discard();
        } else if (!isVampire() && !random.nextBoolean()) {
            target.setSecondsOnFire(isSummoned() ? 4 : 2);
        } else {
            // укус не отбрасывает жертву
            Vec3 before = target.getDeltaMovement();
            doHurtTarget(target);
            target.setDeltaMovement(before);
        }
        playSound(SoundEvents.BAT_HURT, 0.5f, 0.9f + random.nextFloat() * 0.2f);
    }

    /** Огонь и взрывы ей нипочём. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("FireBatFlags", entityData.get(FLAGS));
        tag.putByte("damBonus", (byte) damBonus);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damBonus = tag.getByte("damBonus");
        entityData.set(FLAGS, tag.getByte("FireBatFlags"));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(isDevil() ? 15 : 5);
        refreshDamage();
    }
}
