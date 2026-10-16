package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.registry.ModEntities;

/**
 * Поплавок голема-рыбака (EntityGolemBobber оригинала): летит дугой к выбранной воде и качается на ней,
 * пока голем не выудит добычу. Упав на сушу или оставшись без хозяина, исчезает.
 */
public class GolemBobber extends Entity {
    private static final EntityDataAccessor<Integer> FISHER = SynchedEntityData.defineId(GolemBobber.class, EntityDataSerializers.INT);

    public GolemBobber(EntityType<? extends GolemBobber> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    public GolemBobber(Level level, Golem fisher, BlockPos water) {
        this(ModEntities.GOLEM_BOBBER.get(), level);
        entityData.set(FISHER, fisher.getId());
        double dx = water.getX() + 0.5 - fisher.getX();
        double dy = water.getY() + 1 - fisher.getY();
        double dz = water.getZ() + 0.5 - fisher.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(distance) * 0.08, dz * 0.1);
        setPos(fisher.getX(), fisher.getY(), fisher.getZ());
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FISHER, -1);
    }

    @Nullable
    public Golem getFisher() {
        return level().getEntity(entityData.get(FISHER)) instanceof Golem golem ? golem : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            Golem fisher = getFisher();
            if (fisher == null || !fisher.isAlive() || tickCount > 4000) {
                discard();
                return;
            }
            if (random.nextFloat() < 0.02f) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.SPLASH, getX() + random.nextFloat() - random.nextFloat(),
                        getY() + random.nextFloat(), getZ() + random.nextFloat() - random.nextFloat(), 2 + random.nextInt(2), 0.1, 0.0, 0.1, 0.0);
            }
        }
        Vec3 motion = getDeltaMovement();
        BlockHitResult hit = level().clip(new ClipContext(position(), position().add(motion), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            // упёрся в твёрдое: рыбачить тут нечего
            if (!level().isClientSide && !level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
                discard();
            }
            setDeltaMovement(motion.scale(random.nextFloat() * 0.2f));
            return;
        }
        move(MoverType.SELF, motion);
        motion = getDeltaMovement();
        float friction = onGround() || horizontalCollision ? 0.5f : 0.92f;
        // какая доля поплавка под водой
        double submerged = 0.0;
        for (int slice = 0; slice < 5; slice++) {
            double y = getBoundingBox().minY + getBbHeight() * (slice + 0.5) / 5.0;
            if (level().getFluidState(BlockPos.containing(getX(), y, getZ())).is(FluidTags.WATER)) {
                submerged += 0.2;
            }
        }
        double buoyancy = submerged * 2.0 - 1.0;
        motion = motion.add(0, 0.04 * buoyancy, 0);
        if (submerged > 0.0) {
            friction *= 0.9f;
            motion = new Vec3(motion.x, motion.y * 0.8, motion.z);
        }
        setDeltaMovement(motion.scale(friction));
        setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        setPos(getX(), getY(), getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** Поплавок не сохраняется вместе с миром: голем забросит новый. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
