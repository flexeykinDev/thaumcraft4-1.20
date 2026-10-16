package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.champion.Champions;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

/**
 * Предводитель Багрового культа — перенос EntityCultistLeader оригинала, босс, которого призывает портал культа.
 * Носит латы предводителя и багровый клинок (на лёгкой сложности — пустотный меч), издали бросает самонаводящиеся
 * сгустки, вблизи рубит; культисты рядом с ним регенерируют. Имя — один из пятнадцати титулов и свойство чемпиона.
 */
public class CultistLeader extends ThaumcraftBoss implements RangedAttackMob {
    private static final String[] TITLES = {"Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse", "Dodorol", "Ebardo", "Ferrando",
            "Fertus", "Guillen", "Larpe", "Obano", "Zelipe"};
    private static final EntityDataAccessor<Integer> TITLE = SynchedEntityData.defineId(CultistLeader.class, EntityDataSerializers.INT);

    public CultistLeader(EntityType<? extends CultistLeader> type, Level level) {
        super(type, level);
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 125.0).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TITLE, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 40, 24f) {
            @Override
            public boolean canUse() {
                LivingEntity target = getTarget();
                return super.canUse() && target != null && distanceToSqr(target) >= 256.0;
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Cultist.HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CULTIST_LEADER_HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CULTIST_LEADER_CHESTPLATE.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CULTIST_LEADER_LEGGINGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CULTIST_BOOTS.get()));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(level.getDifficulty() == Difficulty.EASY ? ModItems.VOID_SWORD.get()
                : ModItems.CRIMSON_BLADE.get()));
        float tension = difficulty.getSpecialMultiplier();
        if (random.nextFloat() < 0.5f * tension) {
            setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(random, getMainHandItem(), (int) (7f + tension * random.nextInt(22)), false));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0f);
        }
        entityData.set(TITLE, random.nextInt(TITLES.length));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void generateName() {
        int mod = Champions.get(this);
        if (mod >= 0) {
            setCustomName(Component.translatable("entity.Thaumcraft.CultistLeader.name", TITLES[entityData.get(TITLE) % TITLES.length],
                    Champions.MODS[mod].title()));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        for (Cultist cultist : level().getEntitiesOfClass(Cultist.class, new AABB(blockPosition()).inflate(8))) {
            if (!cultist.hasEffect(MobEffects.REGENERATION)) {
                cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!hasLineOfSight(target)) {
            return;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(target.getX(), target.getY() + target.getBbHeight() / 2f, target.getZ(), 30f, 30f);
        GolemOrb orb = new GolemOrb(level(), this, target, true);
        orb.setPos(orb.getX() + orb.getDeltaMovement().x / 2, orb.getY(), orb.getZ() + orb.getDeltaMovement().z / 2);
        orb.shoot(target.getX() - getX(), target.getY() + target.getBbHeight() / 2f - (getY() + getBbHeight() / 2f) + 2.0, target.getZ() - getZ(),
                0.66f, 3f);
        playSound(ModSounds.EG_ATTACK.get(), 1f, 1f + random.nextFloat() * 0.1f);
        level().addFreshEntity(orb);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof Cultist || other instanceof CultistLeader || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof Cultist) && !(target instanceof CultistLeader) && super.canAttack(target);
    }

    /** Как в оригинале: из предводителя выпадает только редкий мешок (без жемчужины). */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        spawnAtLocation(new ItemStack(ModItems.LOOT_BAG_RARE.get()), 1.5f);
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
