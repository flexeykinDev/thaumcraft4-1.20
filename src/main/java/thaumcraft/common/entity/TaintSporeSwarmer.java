package thaumcraft.common.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Рой спор (EntityTaintSporeSwarmer оригинала): живучий нарост, в который изредка превращается корка порчи.
 * Сам не нападает, но раз в 25 секунд, если рядом игрок, выпускает рой порчи; ещё один — погибая.
 */
public class TaintSporeSwarmer extends TaintSpore {
    private int spawnCounter = 500;

    public TaintSporeSwarmer(EntityType<? extends TaintSporeSwarmer> type, Level level) {
        super(type, level);
        setSporeSize(10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 75.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(1f, 1f);
    }

    /** Нарост стоит на месте, но, в отличие от споры, держится на земле сам. */
    @Override
    public void move(MoverType type, Vec3 movement) {
        if (!onStalk()) {
            super.move(type, new Vec3(0, Math.min(0, movement.y), 0));
        }
    }

    /** Доля времени до следующего роя: по ней на клиенте копятся мошки. */
    public int getSpawnCounter() {
        return spawnCounter;
    }

    @Override
    protected void sporeTick() {
        if (spawnCounter > 0) {
            spawnCounter--;
        }
        if (level().isClientSide) {
            displaySize = getSporeSize();
            if (swarmCount < (500 - spawnCounter) / 25) {
                swarmCount++;
                thaumcraft.client.fx.Fx.swarm(getId(), 0.1f, 10f, 0f);
            }
            return;
        }
        Player near = level().getNearestPlayer(this, 16);
        if (spawnCounter <= 0 && near != null && !near.getAbilities().invulnerable) {
            spawnCounter = 500;
            swarmBurst();
        }
        if (deathTime == 1) {
            swarmBurst();
        }
    }

    @Override
    public void playerTouch(Player player) {
    }

    private void swarmBurst() {
        playSound(ModSounds.GORE.get(), 1f, 0.9f + random.nextFloat() * 0.1f);
        TaintSwarm swarm = ModEntities.TAINT_SWARM.get().create(level());
        if (swarm != null) {
            swarm.moveTo(getX(), getY() + 0.5, getZ(), random.nextFloat() * 360f, 0f);
            level().addFreshEntity(swarm);
        }
        level().broadcastEntityEvent(this, (byte) 6);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            sploosh(10);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 6) {
            spawnCounter = 500;
            sploosh(25);
            // мошки улетают с роем: счётчик сбрасывается, прежние частицы сами гаснут
            swarmCount = 0;
            thaumcraft.client.fx.Fx.releaseSwarm(getId());
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ROOTS.get();
    }
}
