package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Таумический слизень (EntityThaumicSlime оригинала): выползает из луж флюсовой жижи и появляется вместо всего
 * прочего, что умерло от порченого яда. Размер у него — число от 1 до 100: это и здоровье, и сила удара. Слизни
 * сползаются и сливаются в одного большого, а большой плюётся в игрока маленькими, худея на единицу с каждым плевком.
 */
public class ThaumicSlime extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(ThaumicSlime.class, EntityDataSerializers.INT);

    public float targetSquish;
    public float squish;
    public float oSquish;
    private boolean wasOnGround;
    private int jumpDelay;
    /** Первые полсекунды жизни слизень «летит»: брызжет слизью и бьёт чуть дальше. */
    private int launched = 10;
    private int spitCounter = 100;

    public ThaumicSlime(EntityType<? extends ThaumicSlime> type, Level level) {
        super(type, level);
        this.jumpDelay = random.nextInt(20) + 10;
        this.moveControl = new SlimeMove(this);
        setSlimeSize(1 << random.nextInt(3));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0);
    }

    /** Маленький слизень, которым большой плюёт в цель. */
    public static ThaumicSlime launch(ThaumicSlime from, LivingEntity target) {
        ThaumicSlime slime = ModEntities.THAUMIC_SLIME.get().create(from.level());
        if (slime == null) {
            return null;
        }
        slime.setSlimeSize(1);
        double y = (from.getBoundingBox().minY + from.getBoundingBox().maxY) / 2.0;
        double dx = target.getX() - from.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3f - y;
        double dz = target.getZ() - from.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat < 1.0e-7) {
            return null;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float pitch = (float) -(Mth.atan2(dy, flat) * 180.0 / Math.PI);
        slime.moveTo(from.getX() + dx / flat, y, from.getZ() + dz / flat, yaw, pitch);
        dy += flat * 0.2;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        slime.setDeltaMovement((dx / length + slime.random.nextGaussian() * 0.0075) * 1.5,
                (dy / length + slime.random.nextGaussian() * 0.0075) * 1.5, (dz / length + slime.random.nextGaussian() * 0.0075) * 1.5);
        return slime;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SIZE, 1);
    }

    public int getSlimeSize() {
        return entityData.get(SIZE);
    }

    public void setSlimeSize(int size) {
        setSlimeSize(size, true);
    }

    private void setSlimeSize(int size, boolean heal) {
        entityData.set(SIZE, size);
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(size);
        if (heal) {
            setHealth(getMaxHealth());
        }
        this.xpReward = (int) Math.sqrt(size);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    /** Как и в оригинале, у игрока слизень крупнее, чем считает сервер: целиться проще, а в щели он пролезает. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float root = (float) Math.sqrt(getSlimeSize());
        float side = level().isClientSide ? 0.6f * root : 0.25f * root + 0.25f;
        return EntityDimensions.scalable(side, side);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        oSquish = squish;
        squish += (targetSquish - squish) * 0.5f;
        super.tick();
        int root = (int) Math.sqrt(getSlimeSize());
        if (launched > 0) {
            launched--;
            if (level().isClientSide) {
                thaumcraft.client.fx.Fx.slimeJump(this, root, root * (launched + 1));
            }
        }
        if (onGround() && !wasOnGround) {
            if (level().isClientSide) {
                thaumcraft.client.fx.Fx.slimeJump(this, root, root * 8);
            }
            if (getSlimeSize() > 5) {
                playSound(squishSound(), getSoundVolume(), ((random.nextFloat() - random.nextFloat()) * 0.2f + 1f) / 0.8f);
            }
            targetSquish = -0.5f;
        } else if (!onGround() && wasOnGround) {
            targetSquish = 1f;
        }
        wasOnGround = onGround();
        targetSquish *= 0.6f;
    }

    private SoundEvent squishSound() {
        return getSlimeSize() > 3 ? SoundEvents.SLIME_SQUISH : SoundEvents.SLIME_SQUISH_SMALL;
    }

    private SoundEvent jumpSound() {
        return getSlimeSize() > 3 ? SoundEvents.SLIME_JUMP : SoundEvents.SLIME_JUMP_SMALL;
    }

    /** Ближайший слизень, с которым можно слиться: проживший пять секунд и ещё не предельного размера. */
    private ThaumicSlime closestMergable() {
        ThaumicSlime closest = null;
        double distance = Double.MAX_VALUE;
        for (ThaumicSlime slime : level().getEntitiesOfClass(ThaumicSlime.class, new AABB(position(), position()).inflate(16, 8, 16))) {
            double d = distanceToSqr(slime);
            if (slime != this && slime.isAlive() && slime.tickCount > 100 && slime.getSlimeSize() < 100 && d < distance) {
                distance = d;
                closest = slime;
            }
        }
        return closest;
    }

    /** Поворот к цели, не быстрее десяти градусов за тик. */
    private void face(Entity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float want = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        setYRot(getYRot() + Mth.clamp(Mth.wrapDegrees(want - getYRot()), -10f, 10f));
        yHeadRot = getYRot();
        yBodyRot = getYRot();
    }

    @Override
    protected void customServerAiStep() {
        Player player = level().getNearestPlayer(getX(), getY(), getZ(), 16.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (player != null) {
            if (spitCounter > 0) {
                spitCounter--;
            }
            face(player);
            if (distanceTo(player) > 4f && spitCounter <= 0 && getSlimeSize() > 3) {
                spitCounter = 101;
                ThaumicSlime flying = launch(this, player);
                if (flying != null) {
                    level().addFreshEntity(flying);
                }
                playSound(ModSounds.GORE.get(), 1f, ((random.nextFloat() - random.nextFloat()) * 0.2f + 1f) * 0.8f);
                setSlimeSize(getSlimeSize() - 1);
            }
        } else {
            ThaumicSlime other = closestMergable();
            if (other != null) {
                face(other);
                if (distanceTo(other) < getBbWidth() + other.getBbWidth()) {
                    other.setSlimeSize(Math.min(100, other.getSlimeSize() + getSlimeSize()));
                    discard();
                }
            }
        }
        ((SlimeMove) moveControl).hunting = player != null;
    }

    /** Движение прыжками, как у обычного слизня: толчок вперёд в момент прыжка, на земле стоит. */
    private static class SlimeMove extends MoveControl {
        private final ThaumicSlime slime;
        boolean hunting;

        SlimeMove(ThaumicSlime slime) {
            super(slime);
            this.slime = slime;
        }

        @Override
        public void tick() {
            slime.setSpeed(0.1f);
            if (slime.onGround() && slime.jumpDelay-- <= 0) {
                slime.jumpDelay = slime.random.nextInt(16) + 8;
                if (hunting) {
                    slime.jumpDelay /= 3;
                }
                slime.getJumpControl().jump();
                if (slime.getSlimeSize() > 3) {
                    slime.playSound(slime.jumpSound(), slime.getSoundVolume(),
                            ((slime.random.nextFloat() - slime.random.nextFloat()) * 0.2f + 1f) * 0.8f);
                }
                slime.xxa = 1f - slime.random.nextFloat() * 2f;
                slime.zza = (float) Math.sqrt(slime.getSlimeSize());
            } else if (slime.onGround()) {
                slime.xxa = 0f;
                slime.zza = 0f;
            }
        }
    }

    /** Убитый слизень распадается на маленьких: их столько, каков корень из его размера. */
    @Override
    public void remove(RemovalReason reason) {
        int root = (int) Math.sqrt(getSlimeSize());
        if (!level().isClientSide && root > 1 && isDeadOrDying()) {
            for (int k = 0; k < root; k++) {
                float dx = (k % 2 - 0.5f) * root / 4f;
                float dz = (k / 2 - 0.5f) * root / 4f;
                ThaumicSlime small = ModEntities.THAUMIC_SLIME.get().create(level());
                if (small != null) {
                    small.setSlimeSize(1);
                    small.moveTo(getX() + dx, getY() + 0.5, getZ() + dz, random.nextFloat() * 360f, 0f);
                    level().addFreshEntity(small);
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public void playerTouch(Player player) {
        if (level().isClientSide || !isAlive()) {
            return;
        }
        int reach = (int) Math.max(1.0, Math.sqrt(getSlimeSize()));
        if (launched > 0 && reach == 2) {
            reach = 3;
        }
        if (hasLineOfSight(player) && distanceToSqr(player) < 0.8 * reach * 0.8 * reach
                && player.hurt(damageSources().mobAttack(this), getSlimeSize())) {
            playSound(SoundEvents.SLIME_ATTACK, 1f, (random.nextFloat() - random.nextFloat()) * 0.2f + 1f);
        }
    }

    /** Добычу дают только слизни первого и второго размера. */
    @Override
    protected ResourceLocation getDefaultLootTable() {
        return getSlimeSize() < 3 ? getType().getDefaultLootTable() : BuiltInLootTables.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Size", getSlimeSize() - 1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSlimeSize(tag.getInt("Size") + 1, false);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return getSlimeSize() > 3 ? SoundEvents.SLIME_HURT : SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return getSlimeSize() > 3 ? SoundEvents.SLIME_DEATH : SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    protected float getSoundVolume() {
        return 0.1f * (float) Math.sqrt(getSlimeSize());
    }

    @Override
    public int getMaxHeadXRot() {
        return 0;
    }
}
