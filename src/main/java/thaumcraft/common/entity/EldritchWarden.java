package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.champion.Champions;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.Warp;

/**
 * Хранитель-надзиратель Элдрича — перенос EntityEldritchWarden оригинала, босс одного из залов лабиринта. Стражу
 * подобен, но выше в полтора раза, со светящимся глазом под капюшоном; поднимается из пола 7,5 секунд в столбе
 * дыма. Держит запас защиты в две трети здоровья и восполняет его; когда защита иссякнет, он уходит в «бурю
 * полей»: неуязвимый, переносится в середину зала и волнами раскидывает вокруг истощающие поля. Под ногами он
 * всегда оставляет такие поля. Бьёт сферами Элдрича, а иногда воплем: отбрасывает, иссушает, ослабляет и насылает
 * 3–5 временного искажения. Имя — одно из пятнадцати древних имён и свойство чемпиона.
 */
public class EldritchWarden extends ThaumcraftBoss implements RangedAttackMob, EldritchMob {
    private static final String[] TITLES = {"Aphoom-Zhah", "Basatan", "Chaugnar Faugn", "Mnomquah", "Nyogtha", "Oorn", "Shaikorth", "Rhan-Tegoth",
            "Rhogog", "Shudde M'ell", "Vulthoom", "Yag-Kosha", "Yibb-Tstll", "Zathog", "Zushakon"};
    private static final EntityDataAccessor<Integer> TITLE = SynchedEntityData.defineId(EldritchWarden.class, EntityDataSerializers.INT);
    private boolean fieldFrenzy;
    private int fieldFrenzyCounter;
    private boolean lastBlast;
    public float armLiftL;
    public float armLiftR;

    public EldritchWarden(EntityType<? extends EldritchWarden> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TITLE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24f) {
            @Override
            public boolean canUse() {
                LivingEntity target = getTarget();
                return super.canUse() && target != null && distanceToSqr(target) >= 9.0;
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Cultist.class, true));
    }

    @Override
    public void generateName() {
        int mod = Champions.get(this);
        if (mod >= 0) {
            setCustomName(Component.translatable("entity.Thaumcraft.EldritchWarden.name", TITLES[entityData.get(TITLE) % TITLES.length],
                    Champions.MODS[mod].title()));
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        spawnTimer = 150;
        entityData.set(TITLE, random.nextInt(TITLES.length));
        setAbsorptionAmount(getAbsorptionAmount() + (float) (getAttributeBaseValue(Attributes.MAX_HEALTH) * 0.66));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected void customServerAiStep() {
        if (fieldFrenzyCounter == 0) {
            super.customServerAiStep();
        }
        if (hurtTime <= 0 && tickCount % 25 == 0) {
            int cap = (int) (getAttributeBaseValue(Attributes.MAX_HEALTH) * 0.66);
            if (getAbsorptionAmount() < cap) {
                setAbsorptionAmount(getAbsorptionAmount() + 1f);
            }
        }
    }

    @Override
    public void tick() {
        if (getSpawnTimer() == 150) {
            level().broadcastEntityEvent(this, (byte) 18);
        }
        super.tick();
        if (!level().isClientSide) {
            return;
        }
        if (armLiftL > 0f) {
            armLiftL -= 0.05f;
        }
        if (armLiftR > 0f) {
            armLiftR -= 0.05f;
        }
        double x = getX() + (random.nextFloat() - random.nextFloat()) * 0.2f;
        double z = getZ() + (random.nextFloat() - random.nextFloat()) * 0.2f;
        thaumcraft.client.fx.Fx.eldritchWisp(x, getY() + 0.25 * getBbHeight(), z, this);
        if (spawnTimer > 0) {
            // поднимается из пола в столбе дыма
            float height = Math.max(1f, getBbHeight() * ((150 - spawnTimer) / 150f));
            for (int a = 0; a < 33; a++) {
                thaumcraft.client.fx.Fx.smokeSpiral(getX(), getBoundingBox().minY + height / 2f, getZ(), height, random.nextInt(360),
                        Mth.floor(getBoundingBox().minY) - 1, 2232623);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        // под ногами — истощающее поле
        for (int l = 0; l < 4; l++) {
            BlockPos at = BlockPos.containing(getX() + (l % 2 * 2 - 1) * 0.25f, getY(), getZ() + (l / 2 % 2 * 2 - 1) * 0.25f);
            if (level().isEmptyBlock(at)) {
                level().setBlock(at, ModBlocks.SAPPING_FIELD.get().defaultBlockState(), 3);
            }
        }
        if (fieldFrenzyCounter > 0) {
            if (fieldFrenzyCounter == 150) {
                teleportHome();
            }
            performFieldFrenzy();
        }
    }

    /** Буря полей: десятью волнами всё ближе к себе раскидывает круги истощающих полей. */
    private void performFieldFrenzy() {
        if (fieldFrenzyCounter < 121 && fieldFrenzyCounter % 10 == 0) {
            level().broadcastEntityEvent(this, (byte) 17);
            double radius = (150 - fieldFrenzyCounter) / 8.0;
            int d = 1 + fieldFrenzyCounter / 8;
            int i = Mth.floor(getX());
            int j = Mth.floor(getY());
            int k = Mth.floor(getZ());
            for (int q = 0; q < 180 / d; q++) {
                double radians = Math.toRadians(q * 2 * d);
                int dx = (int) (radius * Math.cos(radians));
                int dz = (int) (radius * Math.sin(radians));
                BlockPos at = new BlockPos(i + dx, j, k + dz);
                if (level().isEmptyBlock(at) && level().getBlockState(at.below()).isRedstoneConductor(level(), at.below())) {
                    level().setBlock(at, ModBlocks.SAPPING_FIELD.get().defaultBlockState(), 3);
                    level().scheduleTick(at, ModBlocks.SAPPING_FIELD.get(), 250 + random.nextInt(150));
                    if (random.nextFloat() < 0.3f) {
                        FxPacket.send((ServerLevel) level(), FxPacket.BLOCK_ARC, Vec3.atLowerCornerOf(at), Vec3.ZERO, getId(), 0);
                    } else {
                        FxPacket.blockSparkle((ServerLevel) level(), at, 8388736, 1);
                    }
                }
            }
            playSound(ModSounds.ZAP.get(), 1f, 0.9f + random.nextFloat() * 0.1f);
        }
        fieldFrenzyCounter--;
    }

    /** teleportHome оригинала: переносится к середине своего зала, как эндермен. */
    private void teleportHome() {
        if (!hasRestriction()) {
            return;
        }
        BlockPos home = getRestrictCenter();
        double ox = getX();
        double oy = getY();
        double oz = getZ();
        int i = home.getX();
        int j = home.getY();
        int k = home.getZ();
        for (int tries = 20; tries > 0; tries--) {
            BlockPos at = new BlockPos(i, j, k);
            if (level().getBlockState(at.below()).blocksMotion() && !level().getBlockState(at).blocksMotion()) {
                setPos(i + 0.5, j + 0.1, k + 0.5);
                if (level().noCollision(this)) {
                    // событие 46 — те же 128 частиц портала по пути, что у эндермена
                    level().broadcastEntityEvent(this, (byte) 46);
                    level().playSound(null, ox, oy, oz, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1f, 1f);
                    playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
                    return;
                }
                setPos(ox, oy, oz);
            }
            i = home.getX() + random.nextInt(8) - random.nextInt(8);
            k = home.getZ() + random.nextInt(8) - random.nextInt(8);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return fieldFrenzyCounter > 0 || source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!level().isClientSide && hurt && !fieldFrenzy && getAbsorptionAmount() <= 0f) {
            fieldFrenzy = true;
            fieldFrenzyCounter = 150;
        }
        return hurt;
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions size) {
        return 3.1f;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (random.nextFloat() > 0.2f) {
            EldritchOrb orb = new EldritchOrb(level(), this);
            lastBlast = !lastBlast;
            level().broadcastEntityEvent(this, (byte) (lastBlast ? 16 : 15));
            int turn = lastBlast ? 90 : 180;
            double xx = Mth.cos((getYRot() + turn) % 360f / 180f * Mth.PI) * 0.5;
            double zz = Mth.sin((getYRot() + turn) % 360f / 180f * Mth.PI) * 0.5;
            orb.setPos(orb.getX() - xx, orb.getY() - 0.13, orb.getZ() - zz);
            Vec3 lead = target.getDeltaMovement();
            orb.shoot(target.getX() + lead.x - getX(), target.getY() - getY() - target.getBbHeight() / 2f, target.getZ() + lead.z - getZ(), 1f, 2f);
            playSound(ModSounds.EG_ATTACK.get(), 2f, 1f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        } else if (hasLineOfSight(target)) {
            FxPacket.send((ServerLevel) level(), FxPacket.SONIC, position(), Vec3.ZERO, getId(), 0);
            target.push(-Mth.sin(getYRot() * Mth.DEG_TO_RAD) * 1.5, 0.1, Mth.cos(getYRot() * Mth.DEG_TO_RAD) * 1.5);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
            if (target instanceof ServerPlayer player) {
                Warp.add(player, 3 + random.nextInt(3), true);
            }
            playSound(ModSounds.EG_SCREECH.get(), 4f, 1f + random.nextFloat() * 0.1f);
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
            case 18 -> spawnTimer = 150;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean canAttackType(EntityType<?> type) {
        return type != thaumcraft.common.registry.ModEntities.ELDRITCH_GUARDIAN.get() && super.canAttackType(type);
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("title", (byte) (int) entityData.get(TITLE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(TITLE, (int) tag.getByte("title"));
    }
}
