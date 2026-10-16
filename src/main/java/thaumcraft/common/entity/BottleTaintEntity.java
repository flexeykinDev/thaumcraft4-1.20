package thaumcraft.common.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.taint.Taint;

/**
 * Брошенная порча в бутылке (EntityBottleTaint оригинала): разбившись, отравляет порченым ядом всё живое в пяти
 * блоках и заражает землю вокруг, как взрыв порченого крипера.
 */
public class BottleTaintEntity extends ThrowableItemProjectile {
    public BottleTaintEntity(EntityType<? extends BottleTaintEntity> type, Level level) {
        super(type, level);
    }

    public BottleTaintEntity(Level level, LivingEntity thrower) {
        super(ModEntities.BOTTLE_TAINT.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BOTTLE_TAINT.get();
    }

    @Override
    protected float getGravity() {
        return 0.05f;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5.0))) {
            if (!(living instanceof TaintedMob) && living.getMobType() != MobType.UNDEAD) {
                living.addEffect(new MobEffectInstance(ModEffects.TAINT_POISON.get(), 100, 0, false, true));
            }
        }
        Taint.splash(level, blockPosition(), 5f, false);
        level.broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    /** Брызги слизи во все стороны, осколки бутылки и звон разбитого стекла. */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            thaumcraft.client.fx.Fx.taintsplosion(this);
            for (int i = 0; i < 8; i++) {
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.BOTTLE_TAINT.get())), getX(), getY(), getZ(),
                        random.nextGaussian() * 0.15, random.nextDouble() * 0.2, random.nextGaussian() * 0.15);
            }
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1f,
                    random.nextFloat() * 0.1f + 0.9f, false);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
