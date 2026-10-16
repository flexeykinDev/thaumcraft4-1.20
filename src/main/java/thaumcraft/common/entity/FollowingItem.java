package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.client.fx.Fx;
import thaumcraft.common.registry.ModEntities;

/**
 * Предмет, который сам летит к цели — перенос EntityFollowingItem оригинала. Так добыча стихийных топора и лопаты
 * собирается к игроку: предмет летит сквозь листву и блоки, всё ускоряясь, а долетев, становится обычным.
 */
public class FollowingItem extends ItemEntity {
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(FollowingItem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAIL = SynchedEntityData.defineId(FollowingItem.class, EntityDataSerializers.INT);

    private int approach = 20;

    public FollowingItem(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
    }

    public FollowingItem(Level level, double x, double y, double z, ItemStack stack, Entity target, int trail) {
        this(ModEntities.FOLLOWING_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setYRot(random.nextFloat() * 360f);
        entityData.set(TARGET, target.getId());
        entityData.set(TRAIL, trail);
        setPickUpDelay(10);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TARGET, -1);
        entityData.define(TRAIL, 3);
    }

    /** Летит ли предмет ещё к цели. */
    public boolean isFollowing() {
        return entityData.get(TARGET) >= 0;
    }

    @Override
    public void tick() {
        int id = entityData.get(TARGET);
        Entity target = id < 0 ? null : level().getEntity(id);
        if (target == null || !target.isAlive()) {
            if (id >= 0 && !level().isClientSide) {
                entityData.set(TARGET, -1);
            }
            super.tick();
            return;
        }
        if (getItem().isEmpty()) {
            discard();
            return;
        }
        baseTick();
        Vec3 to = new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2f, target.getZ()).subtract(position());
        if (approach > 1) {
            approach--;
        }
        double distance = to.length();
        if (distance > 0.5) {
            setDeltaMovement(to.scale(1.0 / (distance * approach)));
            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.1));
            if (!level().isClientSide) {
                entityData.set(TARGET, -1);
                setPickUpDelay(0);
            }
        }
        if (level().isClientSide) {
            double px = xo + (random.nextFloat() - random.nextFloat()) * 0.125f;
            double py = yo + getBbHeight() / 2f + (random.nextFloat() - random.nextFloat()) * 0.125f;
            double pz = zo + (random.nextFloat() - random.nextFloat()) * 0.125f;
            int trail = entityData.get(TRAIL);
            if (trail == 10) {
                Fx.crucibleBubble(px, py, pz, 0.33f, 0.33f, 1f);
            } else {
                Fx.sparkle(px, py, pz, 1.5f, trail, 0f);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putShort("type", entityData.get(TRAIL).shortValue());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(TRAIL, (int) tag.getShort("type"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
