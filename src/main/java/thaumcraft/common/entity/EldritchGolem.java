package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.blocks.LootBlock;
import thaumcraft.common.champion.Champions;
import thaumcraft.common.registry.ModSounds;

/**
 * Голем Элдрича — перенос EntityEldritchGolem оригинала, босс одного из залов лабиринта: исполинский закованный
 * в плащ голем (250 здоровья, 6 брони, огонь его не берёт). Пять секунд пробуждается, бьёт кулаками с
 * подбрасыванием, давит под ногами хрупкое (урны и ящики), а получив смертельный удар в первый раз, лишается
 * головы со взрывом, снова встаёт и дальше копит заряд, дымится, искрит дугами в пол и стреляет из шеи очередями
 * сфер, а удары его теперь отбрасывают.
 */
public class EldritchGolem extends ThaumcraftBoss implements EldritchMob, RangedAttackMob {
    private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(EldritchGolem.class, EntityDataSerializers.BOOLEAN);
    private int beamCharge;
    private boolean chargingBeam;
    private int attackTimer;
    private boolean rangedAdded;
    // дуга в пол (на клиенте)
    public int arcing;
    public int ax;
    public int ay;
    public int az;

    public EldritchGolem(EntityType<? extends EldritchGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HEADLESS, false);
    }

    public boolean isHeadless() {
        return entityData.get(HEADLESS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** makeHeadless оригинала: без головы голем учится стрелять очередями сфер. */
    private void makeHeadless() {
        if (!rangedAdded) {
            rangedAdded = true;
            goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 5, 5, 24f) {
                @Override
                public boolean canUse() {
                    LivingEntity target = getTarget();
                    return super.canUse() && target != null && distanceToSqr(target) >= 9.0;
                }
            });
        }
    }

    @Override
    public void generateName() {
        int mod = Champions.get(this);
        if (mod >= 0) {
            setCustomName(Component.translatable("entity.Thaumcraft.EldritchGolem.name", Champions.MODS[mod].title()));
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        spawnTimer = 100;
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions size) {
        return isHeadless() ? 3.33f : 3.0f;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HEADLESS.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1f, 1f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackTimer > 0) {
            attackTimer--;
        }
        Vec3 motion = getDeltaMovement();
        if (motion.x * motion.x + motion.z * motion.z > 2.5000003E-7 && random.nextInt(5) == 0) {
            BlockPos below = BlockPos.containing(getX(), getY() - 0.2, getZ());
            BlockState state = level().getBlockState(below);
            if (!state.isAir()) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(below), getX() + (random.nextFloat() - 0.5) * getBbWidth(),
                        getBoundingBox().minY + 0.1, getZ() + (random.nextFloat() - 0.5) * getBbWidth(), 4.0 * (random.nextFloat() - 0.5), 0.5,
                        (random.nextFloat() - 0.5) * 4.0);
            }
            if (!level().isClientSide && state.getBlock() instanceof LootBlock) {
                level().destroyBlock(below, true);
            }
        }
        if (!level().isClientSide) {
            // топчет хрупкое на пути: всё, что ломается почти мгновенно
            BlockPos ahead = BlockPos.containing(getX() + motion.x, getBoundingBox().minY, getZ() + motion.z);
            float hardness = level().getBlockState(ahead).getDestroySpeed(level(), ahead);
            if (hardness >= 0f && hardness <= 0.15f && !level().getBlockState(ahead).isAir()) {
                level().destroyBlock(ahead, true);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > getHealth() && !isHeadless() && !isInvulnerableTo(source) && spawnTimer == 0) {
            entityData.set(HEADLESS, true);
            spawnTimer = 100;
            double xx = Mth.cos(getYRot() % 360f / 180f * Mth.PI) * 0.75;
            double zz = Mth.sin(getYRot() % 360f / 180f * Mth.PI) * 0.75;
            level().explode(this, getX() + xx, getY() + getEyeHeight(), getZ() + zz, 2f, Level.ExplosionInteraction.NONE);
            makeHeadless();
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (attackTimer > 0) {
            return false;
        }
        attackTimer = 10;
        level().broadcastEntityEvent(this, (byte) 4);
        boolean hurt = target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f);
        if (hurt) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.2, 0));
            if (isHeadless()) {
                target.push(-Mth.sin(getYRot() * Mth.DEG_TO_RAD) * 1.5, 0.1, Mth.cos(getYRot() * Mth.DEG_TO_RAD) * 1.5);
            }
            target.hurtMarked = true;
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!hasLineOfSight(target) || chargingBeam || beamCharge <= 0) {
            return;
        }
        beamCharge -= 15 + random.nextInt(5);
        getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2f, target.getZ(), 30f, 30f);
        Vec3 look = getViewVector(1f);
        GolemOrb orb = new GolemOrb(level(), this, target, false);
        orb.setPos(orb.getX() + look.x, orb.getY(), orb.getZ() + look.z);
        Vec3 lead = target.getDeltaMovement();
        orb.shoot(target.getX() + lead.x - getX(), target.getY() - getY() - target.getBbHeight() / 2f, target.getZ() + lead.z - getZ(), 0.66f, 5f);
        playSound(ModSounds.EG_ATTACK.get(), 1f, 1f + random.nextFloat() * 0.1f);
        level().addFreshEntity(orb);
    }

    public int getAttackTimer() {
        return attackTimer;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 4 -> {
                attackTimer = 10;
                playSound(SoundEvents.IRON_GOLEM_ATTACK, 1f, 1f);
            }
            case 18 -> spawnTimer = 150;
            case 19 -> {
                if (arcing == 0) {
                    float radius = 2f + random.nextFloat() * 2f;
                    double radians = Math.toRadians(random.nextInt(360));
                    int bx = Mth.floor(getX() + radius * Math.cos(radians));
                    int by = Mth.floor(getY());
                    int bz = Mth.floor(getZ() + radius * Math.sin(radians));
                    for (int c = 0; c < 5 && level().isEmptyBlock(new BlockPos(bx, by, bz)); by--) {
                        c++;
                    }
                    if (level().isEmptyBlock(new BlockPos(bx, by + 1, bz)) && !level().isEmptyBlock(new BlockPos(bx, by, bz))) {
                        ax = bx;
                        ay = by;
                        az = bz;
                        arcing = 8 + random.nextInt(5);
                        level().playLocalSound(getX(), getY(), getZ(), ModSounds.JACOBS.get(), SoundSource.HOSTILE, 0.8f,
                                1f + (random.nextFloat() - random.nextFloat()) * 0.05f, false);
                    }
                }
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        if (getSpawnTimer() == 150) {
            level().broadcastEntityEvent(this, (byte) 18);
        }
        if (getSpawnTimer() > 0) {
            heal(2f);
        }
        super.tick();
        if (level().isClientSide) {
            if (isHeadless()) {
                // дым и искры из обрубка шеи, дуги в пол
                setXRot(0f);
                float f1 = Mth.cos(-yBodyRot * Mth.DEG_TO_RAD - Mth.PI);
                float f2 = Mth.sin(-yBodyRot * Mth.DEG_TO_RAD - Mth.PI);
                Vec3 v = new Vec3(f2 * -1f, 0, f1 * -1f);
                float eye = getEyeHeight();
                if (random.nextInt(20) == 0) {
                    float a = (random.nextFloat() - random.nextFloat()) / 2f;
                    float b = (random.nextFloat() - random.nextFloat()) / 2f;
                    thaumcraft.client.fx.Fx.spark(getX() + v.x + a, getY() + eye - 0.25f, getZ() + v.z + b, 0.3f, 0.65f + random.nextFloat() * 0.1f,
                            1f, 1f, 0.8f);
                }
                thaumcraft.client.fx.Fx.vent(getX() + v.x * 0.66, getY() + eye - 0.75f, getZ() + v.z * 0.66, 0, 0.001, 0, 5592405, 4f);
                if (arcing > 0) {
                    thaumcraft.client.fx.Fx.arc(getX(), getY() + getBbHeight() / 2f, getZ(), ax + 0.5, ay + 1, az + 0.5, 0.65f + random.nextFloat() * 0.1f,
                            1f, 1f, 1f - arcing / 10f);
                    arcing--;
                }
            }
        } else {
            if (isHeadless() && beamCharge <= 0) {
                chargingBeam = true;
            }
            if (isHeadless() && chargingBeam) {
                beamCharge++;
                level().broadcastEntityEvent(this, (byte) 19);
                if (beamCharge == 150) {
                    chargingBeam = false;
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("headless", isHeadless());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(HEADLESS, tag.getBoolean("headless"));
        if (isHeadless()) {
            makeHeadless();
        }
    }
}
