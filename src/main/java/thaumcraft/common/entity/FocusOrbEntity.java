package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Шары улучшенных фокусов — EntityExplosiveOrb и EntityShockOrb оригинала в одной сущности.
 * Огненный шар («Огненный шар» фокуса огня) взрывается при ударе; шар разряда («Земной разряд») бьёт всех
 * вокруг и оставляет на земле искрящие участки. Оба можно отбить ударом — шар улетит туда, куда смотрит бьющий.
 */
public class FocusOrbEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Boolean> SHOCK = SynchedEntityData.defineId(FocusOrbEntity.class, EntityDataSerializers.BOOLEAN);

    /** Огненный шар: сила взрыва и поджигает ли он всё вокруг. */
    public float strength = 1f;
    public boolean onFire;
    /** Шар разряда: радиус и урон. */
    public int area = 4;
    public int damage = 5;

    public FocusOrbEntity(EntityType<? extends FocusOrbEntity> type, Level level) {
        super(type, level);
    }

    public FocusOrbEntity(Level level, LivingEntity thrower, boolean shock) {
        super(ModEntities.FOCUS_ORB.get(), thrower, level);
        entityData.set(SHOCK, shock);
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0f, 1.5f, 1f);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SHOCK, false);
    }

    public boolean isShock() {
        return entityData.get(SHOCK);
    }

    @Override
    protected float getGravity() {
        return isShock() ? 0.05f : 0.01f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !isShock()) {
            // дымный огненный след
            thaumcraft.client.fx.Fx.wisp(xo + (random.nextFloat() - random.nextFloat()) * 0.3f, yo + (random.nextFloat() - random.nextFloat()) * 0.3f,
                    zo + (random.nextFloat() - random.nextFloat()) * 0.3f, 0.5f, 1, true, true, 0f);
        }
        if (tickCount > 500) {
            discard();
        }
    }

    private boolean canSee(Vec3 point) {
        return level().clip(new ClipContext(position(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (!isShock()) {
            if (result instanceof EntityHitResult hit) {
                DamageSource source = new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(getOwner() == null ? DamageTypes.ON_FIRE : DamageTypes.FIREBALL), this, getOwner());
                hit.getEntity().hurt(source, strength * 1.5f);
            }
            // взрыв ранит и поджигает, но блоки не ломает
            server.explode(null, getX(), getY(), getZ(), strength, onFire, Level.ExplosionInteraction.NONE);
        } else {
            for (Entity entity : server.getEntities(this, getBoundingBox().inflate(area), e -> e.distanceToSqr(this) <= area * area)) {
                if (canSee(entity.getBoundingBox().getCenter())) {
                    entity.hurt(damageSources().indirectMagic(this, getOwner()), damage);
                }
            }
            // двадцать попыток наэлектризовать землю вокруг: искры ложатся на первую твёрдую поверхность под точкой
            for (int i = 0; i < 20; i++) {
                int x = blockPosition().getX() + random.nextInt(area) - random.nextInt(area);
                int z = blockPosition().getZ() + random.nextInt(area) - random.nextInt(area);
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, blockPosition().getY() + area, z);
                while (server.isEmptyBlock(cursor) && cursor.getY() > blockPosition().getY() - area) {
                    cursor.move(0, -1, 0);
                }
                BlockPos above = cursor.above();
                if (server.isEmptyBlock(above) && !server.isEmptyBlock(cursor) && !server.getBlockState(above).is(ModBlocks.ELECTRIFIED_GROUND.get())
                        && canSee(Vec3.atCenterOf(above).add(0, 0.5, 0))) {
                    server.setBlock(above, ModBlocks.ELECTRIFIED_GROUND.get().defaultBlockState(), 3);
                } else if (thaumcraft.Thaumcraft.LOGGER.isDebugEnabled()) {
                    thaumcraft.Thaumcraft.LOGGER.debug("разряд: мимо {} (опора {}, над ней {}, видно {})", above.immutable(), server.getBlockState(cursor),
                            server.getBlockState(above), canSee(Vec3.atCenterOf(above).add(0, 0.5, 0)));
                }
            }
            thaumcraft.Thaumcraft.LOGGER.debug("разряд: удар {} в {}, область {}", result.getType(), position(), area);
            for (int i = 0; i < 4; i++) {
                FxPacket.send(server, FxPacket.SPARKLES, position(), Vec3.ZERO, 2, 150);
            }
            server.playSound(null, getX(), getY(), getZ(), ModSounds.SHOCK.get(), SoundSource.PLAYERS, 1f,
                    1f + (random.nextFloat() - random.nextFloat()) * 0.2f);
        }
        discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    /** Удар отбивает шар в сторону взгляда бьющего. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || source.getEntity() == null) {
            return false;
        }
        markHurt();
        setDeltaMovement(source.getEntity().getLookAngle().scale(0.9));
        if (isShock()) {
            playSound(ModSounds.ZAP.get(), 1f, 1f + (random.nextFloat() - random.nextFloat()) * 0.2f);
        }
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("shock", isShock());
        tag.putFloat("strength", strength);
        tag.putBoolean("onFire", onFire);
        tag.putInt("area", area);
        tag.putInt("damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SHOCK, tag.getBoolean("shock"));
        strength = tag.getFloat("strength");
        onFire = tag.getBoolean("onFire");
        area = Math.max(1, tag.getInt("area"));
        damage = tag.getInt("damage");
    }
}
