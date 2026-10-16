package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.taint.Taint;

/**
 * Тентакль порчи (EntityTaintacle оригинала): трёхметровое щупальце, растущее из порченой земли. С места не сходит,
 * бьёт всё живое, что подойдёт на длину щупальца, а до тех, кто дальше, дотягивается, выращивая у них под ногами
 * малые щупальца. Вне порченой земли чахнет.
 */
public class Taintacle extends Monster implements TaintedMob {
    /** Тот, за кем щупальце следит; каждая сторона (сервер и клиент) находит его сама. */
    protected LivingEntity prey;
    protected int attackCooldown;
    /** Только на клиенте: размах, с которым щупальце хлещет. */
    public float flailIntensity = 1f;

    public Taintacle(EntityType<? extends Taintacle> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 7.0);
    }

    /**
     * getCanSpawnHere оригинала: в темноте, на волокнах порчи или порченой почве в порченой земле, и только если
     * в 24 блоках нет другого тентакля.
     */
    public static boolean canSpawn(EntityType<? extends Taintacle> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                   RandomSource random) {
        BlockState state = level.getBlockState(pos);
        boolean onTaint = (state.is(ModBlocks.TAINT_FIBRES.get()) || state.is(ModBlocks.TAINTED_SOIL.get())) && Taint.isTainted(level, pos);
        return onTaint && level.getEntitiesOfClass(Taintacle.class, new AABB(pos).inflate(24, 8, 24), t -> !t.isSmall()).isEmpty()
                && Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }

    public boolean isSmall() {
        return false;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    /** Щупальце растёт на месте: его может только уронить вниз. */
    @Override
    public void move(MoverType type, Vec3 movement) {
        super.move(type, new Vec3(0, Math.min(0, movement.y), 0));
    }

    /** Ближайшее живое существо, не порождённое порчей, в шести длинах щупальца. */
    protected LivingEntity findPrey() {
        float h = getBbHeight();
        LivingEntity best = null;
        double distance = Double.MAX_VALUE;
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(h * 6, h * 3, h * 6))) {
            if (living instanceof TaintedMob || living instanceof ArmorStand || !living.isAlive()
                    || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            double d = living.distanceToSqr(this);
            if (d < distance) {
                distance = d;
                best = living;
            }
        }
        return best;
    }

    /** Щупальце возбуждено, пока жертва ближе семи его длин. */
    public boolean isAgitated() {
        float reach = getBbHeight() * 7f;
        return prey != null && prey.distanceToSqr(this) < reach * reach;
    }

    @Override
    public void tick() {
        super.tick();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (!level().isClientSide && tickCount % 20 == 0 && !Taint.isTainted(level(), blockPosition())) {
            hurt(damageSources().starve(), 1f);
        }
        if (level().isClientSide) {
            boolean lashing = hurtTime > 0 || swinging || prey != null && prey.distanceTo(this) < getBbHeight();
            if (tickCount > getBbHeight() * 10f && lashing) {
                if (flailIntensity < 3f) {
                    flailIntensity += 0.2f;
                }
            } else if (flailIntensity > 1f) {
                flailIntensity -= 0.2f;
            }
            if (tickCount < getBbHeight() * 10f && onGround()) {
                thaumcraft.client.fx.Fx.tentacleArise(this);
            }
        }
        if (prey == null) {
            prey = findPrey();
        } else if (prey.isAlive() && isAgitated()) {
            // поворачивается следом, не быстрее пяти градусов за тик
            double dx = prey.getX() - getX();
            double dz = prey.getZ() - getZ();
            float want = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
            setYRot(getYRot() + Mth.clamp(Mth.wrapDegrees(want - getYRot()), -5f, 5f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            if (!level().isClientSide && hasLineOfSight(prey)) {
                attack(prey, prey.distanceTo(this));
            }
        } else {
            prey = null;
        }
    }

    protected void attack(LivingEntity target, float distance) {
        if (attackCooldown > 0) {
            return;
        }
        AABB mine = getBoundingBox();
        AABB theirs = target.getBoundingBox();
        if (distance <= getBbHeight() && theirs.maxY > mine.minY && theirs.minY < mine.maxY) {
            attackCooldown = 20;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            doHurtTarget(target);
            playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
        } else if (distance > getBbHeight() && target.onGround() && !isSmall()) {
            spawnTentacles(target);
        }
    }

    /** Под ногами того, до кого щупальце не дотягивается, из порчи вырастает малое щупальце. */
    protected void spawnTentacles(Entity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        // TODO: в оригинале то же происходит и в чужих землях Внешнего мира — там щупальце ещё и заражает землю
        BlockPos pos = BlockPos.containing(target.getX(), target.getBoundingBox().minY, target.getZ());
        if (!Taint.isTainted(level, pos) || !Taint.isTaint(level.getBlockState(pos)) && !Taint.isTaint(level.getBlockState(pos.below()))) {
            return;
        }
        attackCooldown = 40 + random.nextInt(20);
        TaintacleSmall small = ModEntities.TAINTACLE_SMALL.get().create(level);
        if (small != null) {
            small.moveTo(target.getX() + random.nextFloat() - random.nextFloat(), target.getY(),
                    target.getZ() + random.nextFloat() - random.nextFloat(), 0f, 0f);
            level.addFreshEntity(small);
        }
        playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
    }

    /** Тому, кто бьёт издалека, щупальце отвечает сразу. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!isSmall() && attacker != null && distanceTo(attacker) > 16f && !level().isClientSide) {
            spawnTentacles(attacker);
        }
        return super.hurt(source, amount);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ROOTS.get();
    }

    @Override
    public float getVoicePitch() {
        return 1.3f - getBbHeight() / 10f;
    }

    @Override
    protected float getSoundVolume() {
        return getBbHeight() / 8f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TENTACLE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TENTACLE.get();
    }
}
