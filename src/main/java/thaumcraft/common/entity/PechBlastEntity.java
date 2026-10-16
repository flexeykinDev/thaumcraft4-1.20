package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModEntities;

/**
 * Сгусток проклятия печа (EntityPechBlast оригинала): при ударе ранит всех вокруг и накладывает яд,
 * замедление или слабость — что-то одно наугад, а с улучшением «Паслён» все три сразу.
 */
public class PechBlastEntity extends ThrowableProjectile {
    private int strength;
    private int duration;
    private boolean nightshade;

    public PechBlastEntity(EntityType<? extends PechBlastEntity> type, Level level) {
        super(type, level);
    }

    public PechBlastEntity(Level level, LivingEntity thrower, int strength, int duration, boolean nightshade) {
        super(ModEntities.PECH_BLAST.get(), thrower, level);
        this.strength = strength;
        this.duration = duration;
        this.nightshade = nightshade;
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0f, 1.5f, 1f);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return 0.025f;
    }

    private float spread(float amount) {
        return (random.nextFloat() - random.nextFloat()) * amount;
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                Fx.wisp(getX() + spread(0.2f), getY() + spread(0.2f), getZ() + spread(0.2f), 0.3f, 3, true, true, 0.02f);
                Fx.wisp((getX() + xo) / 2 + spread(0.2f), (getY() + yo) / 2 + spread(0.2f), (getZ() + zo) / 2 + spread(0.2f),
                        0.3f, 2, true, true, 0.02f);
                Fx.sparkle(getX() + spread(0.1f), getY() + spread(0.1f), getZ() + spread(0.1f), 1f, 5, 0f);
            }
        }
        super.tick();
        if (tickCount > 500) {
            discard();
        }
    }

    /** Вспышка из трёх цветов огоньков, разлетающихся от места удара. */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 9; i++) {
                for (int type : new int[]{3, 2, 0}) {
                    float fx = spread(0.3f);
                    float fy = spread(0.3f);
                    float fz = spread(0.3f);
                    Fx.wispTo(getX() + fx, getY() + fy, getZ() + fz, getX() + fx * 8f, getY() + fy * 8f, getZ() + fz * 8f,
                            0.3f, type, true, 0.02f);
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
        int time = 100 + duration * 40;
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2), e -> e != getOwner())) {
            // печи к собственному проклятию невосприимчивы
            if (target instanceof Pech) {
                continue;
            }
            target.hurt(damageSources().thrown(this, getOwner()), strength + 2);
            if (nightshade) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, time, strength));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, time, strength + 1));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, time, strength));
            } else {
                switch (random.nextInt(3)) {
                    case 0 -> target.addEffect(new MobEffectInstance(MobEffects.POISON, time, strength));
                    case 1 -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, time, strength + 1));
                    default -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, time, strength));
                }
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("strength", strength);
        tag.putInt("duration", duration);
        tag.putBoolean("nightshade", nightshade);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        strength = tag.getInt("strength");
        duration = tag.getInt("duration");
        nightshade = tag.getBoolean("nightshade");
    }
}
