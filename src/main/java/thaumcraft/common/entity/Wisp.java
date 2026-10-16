package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.WispEssenceItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.world.ModBiomes;

import java.util.ArrayList;
import java.util.List;

/**
 * Висп (EntityWisp оригинала): летающий сгусток одного аспекта. Бесцельно дрейфует, изредка замечает игрока поблизости
 * и бьёт его молнией раз в секунду-две — по стоящему точнее и больнее, чем по бегущему. Из убитого выпадает
 * эфирная эссенция его аспекта.
 */
public class Wisp extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(Wisp.class, EntityDataSerializers.STRING);
    private static final Aspect[] EERIE = {Aspect.DARKNESS, Aspect.UNDEAD, Aspect.ENTROPY, Aspect.ELDRITCH, Aspect.POISON, Aspect.DEATH};

    private int courseChangeCooldown;
    private Vec3 waypoint = Vec3.ZERO;
    @Nullable
    private LivingEntity prey;
    private int aggroCooldown;
    private int attackCounter;

    public Wisp(EntityType<? extends Wisp> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    /** getCanSpawnHere оригинала: в темноте, не на мирной сложности и не больше восьми виспов в шестнадцати блоках. */
    public static boolean canSpawn(EntityType<Wisp> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) {
            return false;
        }
        int light = level.getLevel().isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
        return light <= random.nextInt(8) && level.getEntitiesOfClass(Wisp.class, new AABB(pos).inflate(16.0)).size() < 8;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TYPE, "");
    }

    @Nullable
    public Aspect getAspect() {
        return Aspect.byTag(entityData.get(TYPE));
    }

    public void setAspect(Aspect aspect) {
        entityData.set(TYPE, aspect.getTag());
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof LivingEntity living) {
            prey = living;
            aggroCooldown = 200;
        }
        if (source.getEntity() instanceof LivingEntity living) {
            prey = living;
            aggroCooldown = 200;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        level().broadcastEntityEvent(this, (byte) 60);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) {
            thaumcraft.client.fx.Fx.burst(getX(), getY() + 0.45, getZ(), 1f);
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            return;
        }
        if (tickCount <= 1) {
            thaumcraft.client.fx.Fx.burst(getX(), getY() + 0.45, getZ(), 1f);
        }
        Aspect aspect = getAspect();
        if (aspect != null && random.nextBoolean()) {
            int color = aspect.getColor();
            thaumcraft.client.fx.Fx.wispColor(getX() + (random.nextFloat() - random.nextFloat()) * 0.7f,
                    getY() + 0.45 + (random.nextFloat() - random.nextFloat()) * 0.7f, getZ() + (random.nextFloat() - random.nextFloat()) * 0.7f,
                    0.1f, (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, false, 0.02f);
        }
    }

    /** Аспект виспа: в жутких землях — из тёмных, в прочих местах обычно первичный и изредка составной. */
    private void chooseAspect() {
        if (ModBiomes.is(level(), blockPosition(), ModBiomes.EERIE)) {
            setAspect(EERIE[random.nextInt(EERIE.length)]);
            return;
        }
        List<Aspect> pool = new ArrayList<>();
        boolean compound = random.nextInt(10) == 0;
        for (Aspect aspect : Aspect.all()) {
            if (aspect.isPrimal() != compound) {
                pool.add(aspect);
            }
        }
        setAspect(pool.get(random.nextInt(pool.size())));
    }

    @Override
    protected void customServerAiStep() {
        if (getAspect() == null) {
            chooseAspect();
        }
        double dx = waypoint.x - getX();
        double dy = waypoint.y - getY();
        double dz = waypoint.z - getZ();
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq < 1.0 || distanceSq > 3600.0) {
            waypoint = new Vec3(getX() + (random.nextFloat() * 2f - 1f) * 16.0, getY() + (random.nextFloat() * 2f - 1f) * 16.0,
                    getZ() + (random.nextFloat() * 2f - 1f) * 16.0);
        }
        if (courseChangeCooldown-- <= 0) {
            courseChangeCooldown += random.nextInt(5) + 2;
            double distance = Math.sqrt(distanceSq);
            if (isCourseTraversable(distance)) {
                setDeltaMovement(getDeltaMovement().add(dx / distance * 0.1, dy / distance * 0.1, dz / distance * 0.1));
            } else {
                waypoint = position();
            }
        }

        if (prey != null && !prey.isAlive()) {
            prey = null;
        }
        aggroCooldown--;
        // сам по себе висп замечает игрока редко: в среднем раз в пятьдесят секунд
        if (random.nextInt(1000) == 0 && (prey == null || aggroCooldown-- <= 0)) {
            prey = level().getNearestPlayer(getX(), getY(), getZ(), 16.0, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
            if (prey != null) {
                aggroCooldown = 50;
            }
        }

        if (prey != null && prey.distanceToSqr(this) < 256.0) {
            double px = prey.getX() - getX();
            double pz = prey.getZ() - getZ();
            setYRot(-((float) Mth.atan2(px, pz)) * 180f / (float) Math.PI);
            yBodyRot = getYRot();
            if (hasLineOfSight(prey)) {
                attackCounter++;
                if (attackCounter == 20) {
                    zap(prey);
                    attackCounter = -20 + random.nextInt(20);
                }
            } else if (attackCounter > 0) {
                attackCounter--;
            }
        } else {
            Vec3 motion = getDeltaMovement();
            setYRot(-((float) Mth.atan2(motion.x, motion.z)) * 180f / (float) Math.PI);
            yBodyRot = getYRot();
            if (attackCounter > 0) {
                attackCounter--;
            }
        }
    }

    /** Молния: по неподвижной цели попадает в двух случаях из трёх и бьёт на единицу сильнее, по движущейся — в двух из пяти. */
    private void zap(LivingEntity target) {
        playSound(ModSounds.ZAP.get(), 1f, 1.1f);
        if (level() instanceof ServerLevel server) {
            FxPacket.send(server, FxPacket.BOLT, getBoundingBox().getCenter(), target.getBoundingBox().getCenter(), 0, 0);
        }
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 motion = target.getDeltaMovement();
        boolean moving = Math.abs(motion.x) > 0.1 || Math.abs(motion.y) > 0.1 || Math.abs(motion.z) > 0.1;
        if (!moving) {
            if (random.nextFloat() < 0.66f) {
                target.hurt(damageSources().mobAttack(this), damage + 1f);
            }
        } else if (random.nextFloat() < 0.4f) {
            target.hurt(damageSources().mobAttack(this), damage);
        }
    }

    /** Путь свободен, и точка назначения не в жидкости и не выше десяти блоков над землёй. */
    private boolean isCourseTraversable(double distance) {
        Vec3 step = waypoint.subtract(position()).scale(1.0 / distance);
        AABB box = getBoundingBox();
        for (int i = 1; i < distance; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) {
                return false;
            }
        }
        BlockPos target = BlockPos.containing(waypoint);
        if (!level().getFluidState(target).isEmpty()) {
            return false;
        }
        for (int a = 0; a < 11; a++) {
            if (!level().isEmptyBlock(target.below(a))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        Aspect aspect = getAspect();
        if (aspect != null) {
            spawnAtLocation(WispEssenceItem.of(ModItems.WISP_ESSENCE.get(), aspect));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Type", entityData.get(TYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(TYPE, tag.getString("Type"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WISP_LIVE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WISP_DEAD.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.25f;
    }
}
