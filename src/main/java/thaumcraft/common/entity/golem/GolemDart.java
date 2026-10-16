package thaumcraft.common.entity.golem;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.common.registry.ModEntities;

/** Дротик голема с дротикомётом (EntityDart оригинала): стрела, которую нельзя подобрать, с дымком при выстреле. */
public class GolemDart extends AbstractArrow {
    private boolean first = true;

    public GolemDart(EntityType<? extends GolemDart> type, Level level) {
        super(type, level);
        pickup = Pickup.DISALLOWED;
    }

    public GolemDart(Level level, LivingEntity shooter, LivingEntity target, float velocity, float inaccuracy) {
        super(ModEntities.GOLEM_DART.get(), level);
        pickup = Pickup.DISALLOWED;
        setOwner(shooter);
        double y = shooter.getY() + shooter.getEyeHeight() - 0.1;
        double dx = target.getX() - shooter.getX();
        double dy = target.getY() + target.getEyeHeight() - 0.7 - y;
        double dz = target.getZ() - shooter.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat >= 1.0E-7) {
            setPos(shooter.getX() + dx / flat / 5.0, y, shooter.getZ() + dz / flat / 5.0);
            shoot(dx, dy + flat * 0.2, dz, velocity, inaccuracy);
        }
    }

    @Override
    public void tick() {
        if (first && level().isClientSide) {
            first = false;
            Vec3 motion = getDeltaMovement();
            for (int i = 0; i < 5; i++) {
                level().addParticle(ParticleTypes.SMOKE, getX() - motion.x / 1.5, getY() - motion.y / 1.5, getZ() - motion.z / 1.5,
                        motion.x / 9.0 + random.nextGaussian() * 0.01, motion.y / 9.0 + random.nextGaussian() * 0.01,
                        motion.z / 9.0 + random.nextGaussian() * 0.01);
            }
        }
        super.tick();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
