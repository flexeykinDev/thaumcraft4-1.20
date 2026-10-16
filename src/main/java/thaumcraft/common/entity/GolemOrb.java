package thaumcraft.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Сгусток — перенос EntityGolemOrb оригинала: самонаводящийся шар энергии клириков культа, их предводителя и голема
 * Элдрича. Без силы тяжести тянется к цели, ранит магией на урон бросившего (красный — полностью, синий — на 60%),
 * лопается с треском; удар отбивает его туда, куда смотрит ударивший. Живёт 8 (красный — 12) секунд.
 */
public class GolemOrb extends ThrowableProjectile {
    private static final EntityDataAccessor<Boolean> RED = SynchedEntityData.defineId(GolemOrb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(GolemOrb.class, EntityDataSerializers.INT);

    public GolemOrb(EntityType<? extends GolemOrb> type, Level level) {
        super(type, level);
    }

    public GolemOrb(Level level, LivingEntity thrower, @Nullable LivingEntity target, boolean red) {
        super(ModEntities.GOLEM_ORB.get(), thrower, level);
        entityData.set(RED, red);
        entityData.set(TARGET, target == null ? -1 : target.getId());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RED, false);
        entityData.define(TARGET, -1);
    }

    public boolean isRed() {
        return entityData.get(RED);
    }

    @Override
    protected float getGravity() {
        return 0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > (isRed() ? 240 : 160)) {
            discard();
            return;
        }
        Entity target = level().getEntity(entityData.get(TARGET));
        if (target instanceof LivingEntity) {
            double d = distanceToSqr(target);
            double dx = (target.getX() - getX()) / d;
            double dy = (target.getY() + target.getBbHeight() * 0.6 - getY()) / d;
            double dz = (target.getZ() - getZ()) / d;
            Vec3 motion = getDeltaMovement().add(dx * 0.2, dy * 0.2, dz * 0.2);
            setDeltaMovement(Mth.clamp(motion.x, -0.25, 0.25), Mth.clamp(motion.y, -0.25, 0.25), Mth.clamp(motion.z, -0.25, 0.25));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (!level().isClientSide && getOwner() instanceof LivingEntity owner && result instanceof EntityHitResult hit) {
            var damage = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            float amount = (float) (damage == null ? 4.0 : damage.getValue()) * (isRed() ? 1f : 0.6f);
            hit.getEntity().hurt(damageSources().indirectMagic(this, owner), amount);
        }
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.SHOCK.get(), SoundSource.HOSTILE, 1f,
                    1f + (random.nextFloat() - random.nextFloat()) * 0.2f);
            FxPacket.send((net.minecraft.server.level.ServerLevel) level(), FxPacket.BURST, position(), Vec3.ZERO, 0, 0);
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof GolemOrb);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    /** Удар отбивает сгусток туда, куда смотрит ударивший. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        markHurt();
        if (source.getEntity() != null) {
            Vec3 look = source.getEntity().getLookAngle();
            setDeltaMovement(look.scale(0.9));
            playSound(ModSounds.ZAP.get(), 1f, 1f + (random.nextFloat() - random.nextFloat()) * 0.2f);
            return true;
        }
        return false;
    }
}
