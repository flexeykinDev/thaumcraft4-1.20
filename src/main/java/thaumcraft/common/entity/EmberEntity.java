package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.registry.ModEntities;

/**
 * Уголёк струи пламени (EntityEmber оригинала): летит прямо, без тяжести, быстро теряет скорость и гаснет
 * через секунду. Обжигает и поджигает первое встреченное существо.
 */
public class EmberEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(EmberEntity.class, EntityDataSerializers.INT);

    public int firey;
    public float damage = 1f;

    public EmberEntity(EntityType<? extends EmberEntity> type, Level level) {
        super(type, level);
    }

    public EmberEntity(Level level, LivingEntity thrower, float scatter) {
        super(ModEntities.EMBER.get(), thrower, level);
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0f, 1f, scatter);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DURATION, 20);
    }

    public int getDuration() {
        return entityData.get(DURATION);
    }

    public void setDuration(int duration) {
        entityData.set(DURATION, duration);
    }

    @Override
    protected float getGravity() {
        return 0f;
    }

    @Override
    public void tick() {
        if (tickCount > getDuration()) {
            discard();
            return;
        }
        double drag = getDuration() <= 20 ? 0.95 : 0.975;
        setDeltaMovement(getDeltaMovement().scale(onGround() ? drag * 0.66 : drag));
        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (level().isClientSide || target.fireImmune()) {
            return;
        }
        DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FIREBALL), this, getOwner());
        if (target.hurt(source, damage)) {
            target.setSecondsOnFire(3 + firey);
        }
    }

    /** С «Алхимическим огнём» уголёк изредка поджигает и то место, куда попал. */
    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide && random.nextFloat() < 0.025f * firey) {
            BlockPos pos = result.getBlockPos().relative(result.getDirection());
            if (level().isEmptyBlock(pos)) {
                level().setBlockAndUpdate(pos, BaseFireBlock.getState(level(), pos));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("firey", firey);
        tag.putInt("duration", getDuration());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        firey = tag.getInt("firey");
        setDuration(tag.getInt("duration"));
    }
}
