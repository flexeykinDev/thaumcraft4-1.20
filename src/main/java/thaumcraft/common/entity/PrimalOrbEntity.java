package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.world.NodeGenerator;

import java.util.Random;

/**
 * Первозданная сфера (EntityPrimalOrb оригинала): медленно плывёт, слегка рыская, и взрывается при ударе.
 * Изредка взрыв оставляет после себя узел ауры; влетев в портал, сфера взрывается вдвое сильнее.
 * С улучшением «Искатель» сама наводится на ближайшее существо.
 */
public class PrimalOrbEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Boolean> SEEKER = SynchedEntityData.defineId(PrimalOrbEntity.class, EntityDataSerializers.BOOLEAN);

    private int count;

    public PrimalOrbEntity(EntityType<? extends PrimalOrbEntity> type, Level level) {
        super(type, level);
    }

    public PrimalOrbEntity(Level level, LivingEntity thrower, boolean seeker) {
        super(ModEntities.PRIMAL_ORB.get(), thrower, level);
        entityData.set(SEEKER, seeker);
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0f, 0.5f, 1f);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SEEKER, false);
    }

    private boolean isSeeker() {
        return entityData.get(SEEKER);
    }

    @Override
    protected float getGravity() {
        return 0.001f;
    }

    private float spread(float amount) {
        return (random.nextFloat() - random.nextFloat()) * amount;
    }

    private boolean inPortal() {
        return level().getBlockState(blockPosition()).is(Blocks.NETHER_PORTAL);
    }

    @Override
    public void tick() {
        count++;
        if (!level().isClientSide && inPortal()) {
            onHit(BlockHitResult.miss(position(), net.minecraft.core.Direction.UP, blockPosition()));
            return;
        }
        if (level().isClientSide) {
            // шесть огоньков шести первичных цветов кружат вокруг сферы
            for (int type = 0; type < 6; type++) {
                Fx.wispTo(getX() + spread(0.2f), getY() + spread(0.2f), getZ() + spread(0.2f),
                        getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z, 0.2f, type, true, 0f);
            }
            Fx.wisp(getX() + spread(0.2f), getY() + spread(0.2f), getZ() + spread(0.2f), 0.1f, random.nextInt(6), true, true, 0f);
        }
        // рысканье одинаково на клиенте и сервере: зерно берётся из номера сущности и её возраста
        Random rr = new Random(getId() + count);
        if (tickCount > 20) {
            Vec3 motion = getDeltaMovement();
            if (!isSeeker()) {
                setDeltaMovement(motion.add((rr.nextFloat() - rr.nextFloat()) * 0.01f, (rr.nextFloat() - rr.nextFloat()) * 0.01f,
                        (rr.nextFloat() - rr.nextFloat()) * 0.01f));
            } else {
                LivingEntity target = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity near : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
                        e -> e != getOwner() && e.isAlive())) {
                    double distance = distanceToSqr(near);
                    if (distance < best) {
                        best = distance;
                        target = near;
                    }
                }
                if (target != null) {
                    // как в оригинале, тяга делится на квадрат расстояния: вдали сфера едва поворачивает
                    double dx = (target.getX() - getX()) / best;
                    double dy = (target.getY() + target.getBbHeight() * 0.9 - getY()) / best;
                    double dz = (target.getZ() - getZ()) / best;
                    setDeltaMovement(Mth.clamp(motion.x + dx * 0.2, -0.2, 0.2), Mth.clamp(motion.y + dy * 0.2, -0.2, 0.2),
                            Mth.clamp(motion.z + dz * 0.2, -0.2, 0.2));
                }
            }
        }
        super.tick();
        if (tickCount > 5000) {
            discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int a = 0; a < 6; a++) {
                for (int type = 0; type < 6; type++) {
                    float fx = spread(0.5f);
                    float fy = spread(0.5f);
                    float fz = spread(0.5f);
                    Fx.wispTo(getX() + fx, getY() + fy, getZ() + fz, getX() + fx * 10f, getY() + fy * 10f, getZ() + fz * 10f,
                            0.4f, type, true, 0.05f);
                }
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        level().broadcastEntityEvent(this, (byte) 3);
        float chance = 1f;
        float power = 2f;
        if (inPortal()) {
            power = 4f;
            chance = 10f;
        }
        // блоки ломаются, только если это разрешено правилом mobGriefing — как у алюментума
        level().explode(null, getX(), getY(), getZ(), power, Level.ExplosionInteraction.MOB);
        if (!isSeeker() && random.nextInt(100) <= chance) {
            if (random.nextBoolean()) {
                if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                    thaumcraft.common.taint.Taint.splash(server, blockPosition(), 6f, true);
                }
            } else {
                BlockPos pos = result instanceof BlockHitResult block ? block.getBlockPos() : blockPosition();
                NodeGenerator.createRandomNode(level(), pos, random, true);
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("seeker", isSeeker());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SEEKER, tag.getBoolean("seeker"));
    }
}
