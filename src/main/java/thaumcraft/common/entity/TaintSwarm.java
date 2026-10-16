package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.taint.Taint;

/**
 * Рой порчи (EntityTaintSwarm оригинала): облако мошек без тела. Летает над порченой землёй, гонится за игроком
 * в двенадцати блоках и жалит вплотную, ослабляя. Самого роя не видно — видны только мошки.
 */
public class TaintSwarm extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(TaintSwarm.class, EntityDataSerializers.BYTE);
    private static final int SUMMONED = 2;

    public int damBonus;
    @Nullable
    private BlockPos flightTarget;
    private int attackTime;
    /** Только на клиенте: сколько мошек уже кружит. */
    private int swarmCount;

    public TaintSwarm(EntityType<? extends TaintSwarm> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    /** Рой появляется сам только в темноте, как летучая мышь. */
    public static boolean canSpawn(EntityType<TaintSwarm> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getMaxLocalRawBrightness(pos) <= random.nextInt(7);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FLAGS, (byte) 0);
    }

    /** Призванный рой (жезлом или мобом) без цели быстро гибнет и на игроков сам не охотится. */
    public boolean isSummoned() {
        return (entityData.get(FLAGS) & SUMMONED) != 0;
    }

    public void setSummoned(boolean summoned) {
        byte flags = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (summoned ? flags | SUMMONED : flags & ~SUMMONED));
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
        if (level().isClientSide && swarmCount < 25) {
            swarmCount++;
            thaumcraft.client.fx.Fx.swarm(getId(), 0.22f, 15f, 0.08f);
        }
    }

    private void steer(double dx, double dy, double dz, double turn) {
        Vec3 motion = getDeltaMovement();
        Vec3 next = motion.add((Math.signum(dx) * 0.5 - motion.x) * turn, (Math.signum(dy) * 0.7 - motion.y) * 0.1,
                (Math.signum(dz) * 0.5 - motion.z) * turn);
        setDeltaMovement(next);
        float yaw = (float) (Mth.atan2(next.z, next.x) * Mth.RAD_TO_DEG) - 90f;
        zza = 0.1f;
        setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
    }

    @Override
    protected void customServerAiStep() {
        if (attackTime > 0) {
            attackTime--;
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
            if (isSummoned()) {
                hurt(damageSources().generic(), 5f);
            }
            // свободный рой держится над порченой землёй и невысоко над поверхностью
            if (flightTarget != null && (!level().isEmptyBlock(flightTarget) || flightTarget.getY() <= level().getMinBuildHeight()
                    || flightTarget.getY() > level().getHeight(Heightmap.Types.MOTION_BLOCKING, flightTarget.getX(), flightTarget.getZ()) + 8
                    || !Taint.isTainted(level(), flightTarget))) {
                flightTarget = null;
            }
            if (flightTarget == null || random.nextInt(30) == 0 || flightTarget.distSqr(blockPosition()) < 4) {
                flightTarget = blockPosition().offset(random.nextInt(7) - random.nextInt(7), random.nextInt(6) - 2,
                        random.nextInt(7) - random.nextInt(7));
            }
            steer(flightTarget.getX() + 0.5 - getX(), flightTarget.getY() + 0.1 - getY(), flightTarget.getZ() + 0.5 - getZ(), 0.015);
        } else {
            steer(target.getX() - getX(), target.getY() + target.getEyeHeight() - getY(), target.getZ() - getZ(), 0.025);
            attack(target);
        }
    }

    /** Укус вплотную раз в полсекунды: урон и слабость на пять секунд, жертву не отбрасывает. */
    private void attack(LivingEntity target) {
        if (attackTime > 0 || distanceTo(target) >= 3f || target.getBoundingBox().maxY <= getBoundingBox().minY
                || target.getBoundingBox().minY >= getBoundingBox().maxY) {
            return;
        }
        attackTime = 10 + random.nextInt(5);
        Vec3 before = target.getDeltaMovement();
        if (doHurtTarget(target)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }
        target.setDeltaMovement(before);
        playSound(ModSounds.SWARM_ATTACK.get(), 0.3f, 0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("Flags", entityData.get(FLAGS));
        tag.putByte("damBonus", (byte) damBonus);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FLAGS, tag.getByte("Flags"));
        damBonus = tag.getByte("damBonus");
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2 + damBonus);
    }

    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SWARM_ATTACK.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SWARM_ATTACK.get();
    }
}
