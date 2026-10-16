package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModEntities;

/**
 * Сфера аспекта (EntityAspectOrb оригинала): огонёк первичного аспекта, который выпадает из убитых игроком существ
 * и из нестабильных узлов. Ведёт себя как сфера опыта, но летит только к тому, у кого на панели есть жезл с местом
 * под этот аспект, и заряжает именно жезл. Живёт семь с половиной секунд.
 */
public class AspectOrbEntity extends Entity implements IEntityAdditionalSpawnData {
    public int orbAge;
    public int orbMaxAge = 150;
    public int orbCooldown;
    private int orbHealth = 5;
    private Aspect aspect = Aspect.AIR;
    private int value = 1;
    private Player closestPlayer;

    public AspectOrbEntity(EntityType<? extends AspectOrbEntity> type, Level level) {
        super(type, level);
    }

    public static AspectOrbEntity spawn(Level level, double x, double y, double z, Aspect aspect, int value) {
        AspectOrbEntity orb = ModEntities.ASPECT_ORB.get().create(level);
        orb.setPos(x, y, z);
        orb.setYRot((float) (Math.random() * 360.0));
        orb.setDeltaMovement((Math.random() * 0.2 - 0.1) * 2.0, Math.random() * 0.2 * 2.0, (Math.random() * 0.2 - 0.1) * 2.0);
        orb.aspect = aspect;
        orb.value = value;
        level.addFreshEntity(orb);
        return orb;
    }

    /** Ячейка панели быстрого доступа с жезлом, в который влезет хоть часть этого аспекта; -1, если такого нет. */
    public static int hotbarWandWithRoom(Player player, Aspect aspect, int amount) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof WandItem && WandItem.addVis(stack, aspect, amount, false) < amount) {
                return slot;
            }
        }
        return -1;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public int getValue() {
        return value;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public void tick() {
        super.tick();
        if (orbCooldown > 0) {
            orbCooldown--;
        }
        xo = getX();
        yo = getY();
        zo = getZ();
        setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
        if (level().getFluidState(blockPosition()).is(FluidTags.LAVA)) {
            setDeltaMovement((random.nextFloat() - random.nextFloat()) * 0.2f, 0.2, (random.nextFloat() - random.nextFloat()) * 0.2f);
            playSound(SoundEvents.GENERIC_BURN, 0.4f, 2f + random.nextFloat() * 0.4f);
        }
        if (!level().noCollision(getBoundingBox())) {
            moveTowardsClosestSpace(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2.0, getZ());
        }
        double range = 8.0;
        if (tickCount % 5 == 0 && closestPlayer == null) {
            double distance = Double.MAX_VALUE;
            for (Player player : level().getEntitiesOfClass(Player.class, new AABB(position(), position()).inflate(range))) {
                double d = player.distanceToSqr(this);
                if (d < distance && !player.isSpectator() && hotbarWandWithRoom(player, aspect, value) >= 0) {
                    distance = d;
                    closestPlayer = player;
                }
            }
        }
        if (closestPlayer != null && closestPlayer.isAlive()) {
            double dx = (closestPlayer.getX() - getX()) / range;
            double dy = (closestPlayer.getY() + closestPlayer.getEyeHeight() - getY()) / range;
            double dz = (closestPlayer.getZ() - getZ()) / range;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double pull = 1.0 - distance;
            if (pull > 0 && distance > 0) {
                pull *= pull;
                setDeltaMovement(getDeltaMovement().add(dx / distance * pull * 0.1, dy / distance * pull * 0.1, dz / distance * pull * 0.1));
            }
        }
        move(MoverType.SELF, getDeltaMovement());
        float friction = 0.98f;
        if (onGround()) {
            BlockPos below = BlockPos.containing(getX(), getBoundingBox().minY - 1, getZ());
            friction = level().getBlockState(below).isAir() ? 0.58800006f
                    : level().getBlockState(below).getFriction(level(), below, this) * 0.98f;
        }
        Vec3 motion = getDeltaMovement().multiply(friction, 0.98, friction);
        if (onGround()) {
            motion = motion.multiply(1.0, -0.9, 1.0);
        }
        setDeltaMovement(motion);
        if (++orbAge >= orbMaxAge) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide) {
            return false;
        }
        markHurt();
        orbHealth = (int) (orbHealth - amount);
        if (orbHealth <= 0) {
            discard();
        }
        return false;
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide || orbCooldown != 0 || player.takeXpDelay != 0 || !aspect.isPrimal()) {
            return;
        }
        int slot = hotbarWandWithRoom(player, aspect, value);
        if (slot >= 0) {
            WandItem.addVis(player.getInventory().getItem(slot), aspect, value, true);
            player.takeXpDelay = 2;
            playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.1f, 0.5f * ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.8f));
            discard();
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putShort("Health", (short) orbHealth);
        tag.putShort("Age", (short) orbAge);
        tag.putShort("Value", (short) value);
        tag.putString("Aspect", aspect.getTag());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        orbHealth = tag.getShort("Health") & 255;
        orbAge = tag.getShort("Age");
        value = tag.getShort("Value");
        setAspect(Aspect.byTag(tag.getString("Aspect")));
    }

    private void setAspect(@Nullable Aspect found) {
        if (found != null) {
            aspect = found;
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUtf(aspect.getTag());
        buffer.writeVarInt(orbAge);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        setAspect(Aspect.byTag(buffer.readUtf()));
        orbAge = buffer.readVarInt();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
