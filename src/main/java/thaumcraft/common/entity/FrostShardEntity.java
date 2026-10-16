package thaumcraft.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.registry.ModEntities;

/**
 * Ледяной осколок (EntityFrostShard оригинала): отскакивает от стен и существ, теряя половину скорости,
 * и рассыпается после трёх ударов. Хрупкий осколок картечи бьётся о первое же существо.
 */
public class FrostShardEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(FrostShardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> FROSTY = SynchedEntityData.defineId(FrostShardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> FRAGILE = SynchedEntityData.defineId(FrostShardEntity.class, EntityDataSerializers.BOOLEAN);

    public double bounce = 0.5;
    public int bounceLimit = 3;
    public boolean fragile;

    public FrostShardEntity(EntityType<? extends FrostShardEntity> type, Level level) {
        super(type, level);
    }

    public FrostShardEntity(Level level, LivingEntity thrower, float scatter) {
        super(ModEntities.FROST_SHARD.get(), thrower, level);
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0f, 1f, scatter);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DAMAGE, 0f);
        entityData.define(FROSTY, (byte) 0);
        entityData.define(FRAGILE, false);
    }

    public void setDamage(float damage) {
        entityData.set(DAMAGE, damage);
        refreshDimensions();
    }

    public float getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setFrosty(int frosty) {
        entityData.set(FROSTY, (byte) frosty);
    }

    public int getFrosty() {
        return entityData.get(FROSTY);
    }

    /** Чем сильнее осколок, тем он крупнее. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float size = 0.15f + getDamage() * 0.15f;
        return EntityDimensions.scalable(size, size);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DAMAGE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected float getGravity() {
        return fragile || entityData.get(FRAGILE) ? 0.015f : 0.05f;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && entityData.get(FRAGILE) != fragile) {
            entityData.set(FRAGILE, fragile);
        }
        super.tick();
        if (level().isClientSide && getFrosty() > 0) {
            float s = getDamage() / 10f;
            for (int i = 0; i < getFrosty(); i++) {
                thaumcraft.client.fx.Fx.sparkle(getX() - s + random.nextFloat() * s * 2f, getY() - s + random.nextFloat() * s * 2f,
                        getZ() - s + random.nextFloat() * s * 2f, 0.4f, 6, 0.005f);
            }
        }
        if (tickCount > 600) {
            discard();
        }
    }

    private void crack(BlockState state, int count) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY(), getZ(), count, 0.1, 0.1, 0.1, 0.15);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        Vec3 motion = getDeltaMovement();
        double mx = motion.x;
        double my = motion.y;
        double mz = motion.z;
        Entity victim = null;
        if (result instanceof EntityHitResult entityHit) {
            victim = entityHit.getEntity();
            if (blockPosition().getZ() != victim.blockPosition().getZ()) {
                mz = -mz;
            }
            if (blockPosition().getX() != victim.blockPosition().getX()) {
                mx = -mx;
            }
            if (blockPosition().getY() != victim.blockPosition().getY()) {
                my *= -0.9;
            }
            mx *= 0.66;
            my *= 0.66;
            mz *= 0.66;
            crack(Blocks.ICE.defaultBlockState(), (int) Math.ceil(getDamage()));
        } else if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
            Direction side = blockHit.getDirection();
            if (side.getStepZ() != 0) {
                mz = -mz;
            }
            if (side.getStepX() != 0) {
                mx = -mx;
            }
            if (side.getStepY() != 0) {
                my *= -0.9;
            }
            BlockState hit = level().getBlockState(blockHit.getBlockPos());
            playSound(hit.getSoundType(level(), blockHit.getBlockPos(), this).getBreakSound(), 0.3f, 1.2f / (random.nextFloat() * 0.2f + 0.9f));
            crack(hit, (int) Math.ceil(getDamage()));
        }
        mx *= bounce;
        my *= bounce;
        mz *= bounce;
        setDeltaMovement(mx, my, mz);
        double speed = Math.sqrt(mx * mx + my * my + mz * mz);
        if (speed > 1.0e-6) {
            setPos(getX() - mx / speed * 0.05, getY() - my / speed * 0.05, getZ() - mz / speed * 0.05);
        }
        hasImpulse = true;

        if (!level().isClientSide && victim != null) {
            Vec3 before = victim.getDeltaMovement();
            victim.hurt(damageSources().thrown(this, getOwner()), getDamage());
            if (victim instanceof LivingEntity living && getFrosty() > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, getFrosty() - 1));
            }
            if (fragile) {
                // картечь: следующий осколок бьёт сразу, не дожидаясь неуязвимости, и почти не отбрасывает
                victim.invulnerableTime = 0;
                playSound(SoundEvents.GLASS_BREAK, 0.3f, 1.2f / (random.nextFloat() * 0.2f + 0.9f));
                victim.setDeltaMovement(before.add(victim.getDeltaMovement().subtract(before).scale(0.1)));
                discard();
                return;
            }
        }
        if (bounceLimit-- <= 0 && !level().isClientSide) {
            playSound(SoundEvents.GLASS_BREAK, 0.3f, 1.2f / (random.nextFloat() * 0.2f + 0.9f));
            crack(Blocks.ICE.defaultBlockState(), (int) (8 * getDamage()));
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", getDamage());
        tag.putBoolean("fragile", fragile);
        tag.putInt("frost", getFrosty());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDamage(tag.getFloat("damage"));
        fragile = tag.getBoolean("fragile");
        setFrosty(tag.getInt("frost"));
    }
}
