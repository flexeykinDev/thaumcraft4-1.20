package thaumcraft.common.entity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.registry.ModEntities;

/**
 * Сфера Элдрича — перенос EntityEldritchOrb оригинала: заряд стража и хранителя. Летит без силы тяжести,
 * разрывается и ранит магией всех живых (кроме нежити) в двух блоках на две трети урона бросившего, ослабляя их
 * на восемь секунд; лопаясь, разбрасывает тёмные огоньки.
 */
public class EldritchOrb extends ThrowableProjectile {
    public EldritchOrb(EntityType<? extends EldritchOrb> type, Level level) {
        super(type, level);
    }

    public EldritchOrb(Level level, LivingEntity thrower) {
        super(ModEntities.ELDRITCH_ORB.get(), thrower, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return 0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100) {
            discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (level().isClientSide || !(getOwner() instanceof LivingEntity owner)) {
            return;
        }
        var damage = owner.getAttribute(Attributes.ATTACK_DAMAGE);
        float amount = (float) (damage == null ? 4.0 : damage.getValue()) * 0.666f;
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.0), e -> e != owner)) {
            if (!living.isInvertedHealAndHarm()) {
                living.hurt(damageSources().indirectMagic(this, owner), amount);
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            }
        }
        playSound(SoundEvents.FIRE_EXTINGUISH, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f);
        level().broadcastEntityEvent(this, (byte) 16);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            for (int a = 0; a < 30; a++) {
                float fx = (random.nextFloat() - random.nextFloat()) * 0.3f;
                float fy = (random.nextFloat() - random.nextFloat()) * 0.3f;
                float fz = (random.nextFloat() - random.nextFloat()) * 0.3f;
                thaumcraft.client.fx.Fx.wispTo(getX() + fx, getY() + fy, getZ() + fz, getX() + fx * 8, getY() + fy * 8, getZ() + fz * 8, 0.3f, 5, true,
                        0.02f);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
