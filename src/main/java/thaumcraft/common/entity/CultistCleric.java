package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;
import thaumcraft.common.registry.ModSounds;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Клирик культа — перенос EntityCultistCleric оригинала. Издали бросает самонаводящиеся сгустки (как голем-маг)
 * или веер огненных шаров ифрита, вблизи бьёт рукой. Клирики, призванные алтарём Элдрича, стоят вокруг него,
 * воспарив, и тянут к нему лучи обряда, пока их не ранят или не уведут от алтаря.
 */
public class CultistCleric extends Cultist implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> RITUALIST = SynchedEntityData.defineId(CultistCleric.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> ALTAR = SynchedEntityData.defineId(CultistCleric.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS);

    public CultistCleric(EntityType<? extends CultistCleric> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createCultistAttributes().add(Attributes.MAX_HEALTH, 30.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(RITUALIST, false);
        entityData.define(ALTAR, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AltarFocusGoal());
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 20, 40, 24f) {
            @Override
            public boolean canUse() {
                LivingEntity target = getTarget();
                return super.canUse() && target != null && distanceToSqr(target) >= 4.0;
            }
        });
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void equip(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CULTIST_ROBE_HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CULTIST_ROBE_CHESTPLATE.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CULTIST_ROBE_LEGGINGS.get()));
        if (random.nextFloat() < (level().getDifficulty() == Difficulty.HARD ? 0.3f : 0.1f)) {
            setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CULTIST_BOOTS.get()));
        }
    }

    public boolean isRitualist() {
        return entityData.get(RITUALIST);
    }

    public void setRitualist(boolean ritualist) {
        entityData.set(RITUALIST, ritualist);
    }

    /** Алтарь, которому служит клирик (его «дом»). */
    public Optional<BlockPos> getAltar() {
        return entityData.get(ALTAR);
    }

    public void setAltar(BlockPos altar) {
        restrictTo(altar, 8);
        entityData.set(ALTAR, Optional.of(altar));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        double dx = target.getX() - getX();
        double dy = target.getY() + target.getBbHeight() / 2f - (getY() + getBbHeight() / 2f);
        double dz = target.getZ() - getZ();
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (random.nextFloat() > 0.66f) {
            GolemOrb orb = new GolemOrb(level(), this, target, true);
            orb.setPos(orb.getX() + orb.getDeltaMovement().x / 2, orb.getY(), orb.getZ() + orb.getDeltaMovement().z / 2);
            orb.shoot(dx, dy + 2.0, dz, 0.66f, 3f);
            playSound(ModSounds.EG_ATTACK.get(), 1f, 1f + random.nextFloat() * 0.1f);
            level().addFreshEntity(orb);
        } else {
            float spread = Mth.sqrt(power) * 0.5f;
            level().levelEvent(null, 1018, blockPosition(), 0);
            for (int i = 0; i < 3; i++) {
                SmallFireball fireball = new SmallFireball(level(), this, dx + random.nextGaussian() * spread, dy, dz + random.nextGaussian() * spread);
                fireball.setPos(fireball.getX(), getY() + getBbHeight() / 2f + 0.5, fireball.getZ());
                level().addFreshEntity(fireball);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isRitualist() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        setRitualist(false);
        return super.hurt(source, amount);
    }

    /** На клиенте клирик-обрядник смотрит на алтарь. */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isRitualist() && getAltar().isPresent()) {
            BlockPos altar = getAltar().get();
            double dx = altar.getX() + 0.5 - getX();
            double dy = altar.getY() + 1.5 - getEyeY();
            double dz = altar.getZ() + 0.5 - getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
            float pitch = (float) -(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG);
            setXRot(rotlerp(getXRot(), pitch, 10f));
            yHeadRot = rotlerp(yHeadRot, yaw, getMaxHeadXRot());
            // луч обряда от клирика к алтарю (drawFloatyLine в RenderCultist оригинала)
            thaumcraft.client.fx.Fx.floatyBeam(new net.minecraft.world.phys.Vec3(getX(), getY() + getEyeHeight() * 1.2, getZ()),
                    new net.minecraft.world.phys.Vec3(altar.getX() + 0.5, altar.getY() + 1.5, altar.getZ() + 0.5), 0x991133);
        }
    }

    private static float rotlerp(float from, float to, float max) {
        return from + Mth.clamp(Mth.wrapDegrees(to - from), -max, max);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CHANT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Ritualist", isRitualist());
        getAltar().ifPresent(altar -> tag.putLong("Altar", altar.asLong()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setRitualist(tag.getBoolean("Ritualist"));
        if (tag.contains("Altar")) {
            setAltar(BlockPos.of(tag.getLong("Altar")));
        }
    }

    /** AIAltarFocus оригинала: обрядник стоит на месте; отошёл от алтаря дальше четырёх блоков или алтаря нет — обряд кончен. */
    private class AltarFocusGoal extends Goal {
        AltarFocusGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isRitualist() && getAltar().isPresent();
        }

        @Override
        public void tick() {
            BlockPos altar = getAltar().orElse(null);
            if (altar != null && tickCount % 40 == 0 && (altar.distSqr(blockPosition()) > 16.0
                    || !level().getBlockState(altar).is(ModBlocks.ELDRITCH_ALTAR.get()))) {
                setRitualist(false);
            }
        }
    }
}
