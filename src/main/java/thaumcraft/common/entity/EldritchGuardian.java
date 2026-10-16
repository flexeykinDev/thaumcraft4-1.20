package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.WispEssenceItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.Warp;
import thaumcraft.common.world.outer.OuterLands;

/**
 * Страж Элдрича — перенос EntityEldritchGuardian оригинала: полупрозрачный исполин в капюшоне, нежить Запредельных
 * земель. Издали мечет попеременно из обеих рук сферы Элдрича, а иногда издаёт вопль — иссушение на 20 секунд и
 * искажение. Магия ранит его вдвое слабее. В обычном мире, пока он рядом, игроков окутывает туман, а сам он
 * почти невидим; в Запредельных землях он виден целиком и держит запас защиты (поглощение до половины здоровья).
 */
public class EldritchGuardian extends Monster implements RangedAttackMob, EldritchMob {
    public float armLiftL;
    public float armLiftR;
    private boolean lastBlast;

    public EldritchGuardian(EntityType<? extends EldritchGuardian> type, Level level) {
        super(type, level);
        xpReward = 20;
        setCanPickUpLoot(false);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24f) {
            @Override
            public boolean canUse() {
                LivingEntity target = getTarget();
                return super.canUse() && target != null && distanceToSqr(target) >= 64.0;
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            amount /= 2f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (armLiftL > 0f) {
                armLiftL -= 0.05f;
            }
            if (armLiftR > 0f) {
                armLiftR -= 0.05f;
            }
            double x = getX() + (random.nextFloat() - random.nextFloat()) * 0.2f;
            double z = getZ() + (random.nextFloat() - random.nextFloat()) * 0.2f;
            thaumcraft.client.fx.Fx.eldritchWisp(x, getY() + 0.22 * getBbHeight(), z, this);
        } else if (!OuterLands.is(level()) && (tickCount == 1 || tickCount % 100 == 0) && level().getDifficulty() != Difficulty.EASY) {
            // в обычном мире рядом со стражем сгущается туман
            double range = level().getDifficulty() == Difficulty.HARD ? 576.0 : 256.0;
            for (Player player : level().players()) {
                if (player.isAlive() && player.distanceToSqr(this) < range && player instanceof ServerPlayer server) {
                    FxPacket.sendTo(server, FxPacket.MIST, Vec3.ZERO, Vec3.ZERO, 200, 0);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            int difficulty = level().getDifficulty().getId();
            if (getMainHandItem().isEmpty() && isOnFire() && random.nextFloat() < difficulty * 0.3f) {
                target.setSecondsOnFire(2 * difficulty);
            }
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.EG_IDLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.EG_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    protected float getSoundVolume() {
        return 1.5f;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (random.nextBoolean()) {
            spawnAtLocation(WispEssenceItem.of(ModItems.WISP_ESSENCE.get(), Aspect.UNDEAD), 1f);
        }
        if (random.nextBoolean()) {
            spawnAtLocation(WispEssenceItem.of(ModItems.WISP_ESSENCE.get(), Aspect.ELDRITCH), 1f);
        }
        // dropRareDrop оригинала — око Элдрича
        if (recentlyHit && random.nextInt(200) - looting < 5) {
            spawnAtLocation(ModItems.ELDRITCH_EYE.get());
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        if (OuterLands.is(level.getLevel())) {
            setAbsorptionAmount(getAbsorptionAmount() + (float) (getAttributeBaseValue(Attributes.MAX_HEALTH) / 2));
        }
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (OuterLands.is(level()) && hurtTime <= 0 && tickCount % 25 == 0) {
            float cap = (float) (getAttributeBaseValue(Attributes.MAX_HEALTH) / 2);
            if (getAbsorptionAmount() < cap) {
                setAbsorptionAmount(getAbsorptionAmount() + 1f);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case 15 -> armLiftL = 0.5f;
            case 16 -> armLiftR = 0.5f;
            case 17 -> {
                armLiftL = 0.9f;
                armLiftR = 0.9f;
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasRestriction();
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions size) {
        return 2.1f;
    }

    /** Не больше одного стража на 32 блока; свет не мешает. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        return level.getEntitiesOfClass(EldritchGuardian.class, new AABB(blockPosition()).inflate(32, 16, 32)).isEmpty();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (random.nextFloat() > 0.1f) {
            EldritchOrb orb = new EldritchOrb(level(), this);
            lastBlast = !lastBlast;
            level().broadcastEntityEvent(this, (byte) (lastBlast ? 16 : 15));
            int turn = lastBlast ? 90 : 180;
            double xx = Mth.cos((getYRot() + turn) % 360f / 180f * Mth.PI) * 0.5;
            double yy = 0.057777777 * getBbHeight();
            double zz = Mth.sin((getYRot() + turn) % 360f / 180f * Mth.PI) * 0.5;
            orb.setPos(orb.getX() - xx, orb.getY() - yy, orb.getZ() - zz);
            Vec3 lead = target.getDeltaMovement();
            orb.shoot(target.getX() + lead.x - getX(), target.getY() - getY() - target.getBbHeight() / 2f, target.getZ() + lead.z - getZ(), 1f, 2f);
            playSound(ModSounds.EG_ATTACK.get(), 2f, 1f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        } else if (hasLineOfSight(target)) {
            FxPacket.send((net.minecraft.server.level.ServerLevel) level(), FxPacket.SONIC, position(), Vec3.ZERO, getId(), 0);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            if (target instanceof ServerPlayer player) {
                Warp.add(player, 1 + random.nextInt(3), true);
            }
            playSound(ModSounds.EG_SCREECH.get(), 3f, 1f + random.nextFloat() * 0.1f);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof EldritchMob || super.isAlliedTo(other);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) {
            BlockPos home = getRestrictCenter();
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
            tag.putInt("HomeD", (int) getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) {
            restrictTo(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), tag.getInt("HomeD"));
        }
    }
}
