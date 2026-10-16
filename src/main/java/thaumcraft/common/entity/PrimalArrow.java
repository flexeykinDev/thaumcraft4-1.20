package thaumcraft.common.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModItems;

/**
 * Первозданная стрела — перенос EntityPrimalArrow оригинала. Воздушная и упорядоченная бьют магией сквозь броню
 * (вторая ещё и сильно ослабляет), огненная поджигает, водяная надолго замедляет, земляная бьёт в полтора раза
 * сильнее и отбрасывает, энтропийная иссушает.
 */
public class PrimalArrow extends AbstractArrow {
    public static final String[] NAMES = {"air", "fire", "water", "earth", "order", "entropy"};
    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(PrimalArrow.class, EntityDataSerializers.BYTE);
    private static final ResourceKey<DamageType> AIR = ResourceKey.create(Registries.DAMAGE_TYPE, Thaumcraft.id("airarrow"));
    private static final ResourceKey<DamageType> FIRE = ResourceKey.create(Registries.DAMAGE_TYPE, Thaumcraft.id("firearrow"));
    private static final ResourceKey<DamageType> ORDER = ResourceKey.create(Registries.DAMAGE_TYPE, Thaumcraft.id("orderarrow"));

    public PrimalArrow(EntityType<? extends PrimalArrow> type, Level level) {
        super(type, level);
        setBaseDamage(2.1);
    }

    public PrimalArrow(Level level, LivingEntity shooter, int type) {
        super(ModEntities.PRIMAL_ARROW.get(), shooter, level);
        setBaseDamage(2.1);
        entityData.set(TYPE, (byte) type);
        setKnockback(0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(TYPE, (byte) 0);
    }

    /** Сколько тактов стрела уже торчит в блоке (для угасания огонька). */
    public int groundTime() {
        return inGroundTime;
    }

    public int getArrowType() {
        return entityData.get(TYPE);
    }

    /** Урон земляной стрелы — в полтора раза больше, упорядоченной и энтропийной — 0.8 от обычного (getDamage оригинала). */
    private double damageFactor() {
        return switch (getArrowType()) {
            case 3 -> 1.5;
            case 4, 5 -> 0.8;
            default -> 1.0;
        };
    }

    /** Сила отбрасывания (у AbstractArrow поле закрыто, а попадание здесь своё); земляная стрела — на ступень сильнее. */
    private int knockback;

    @Override
    public void setKnockback(int knockback) {
        this.knockback = knockback + (getArrowType() == 3 ? 1 : 0);
        super.setKnockback(this.knockback);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.PRIMAL_ARROWS.get(Math.min(getArrowType(), 5)).get());
    }

    /**
     * Попадание — как у обычной стрелы 1.20 (сила удара от скорости, критический выстрел, отбрасывание, чары лука),
     * но со своим видом урона и действием стихии. Стрелы арбалета с «Пронзанием» ведут себя как обычные.
     */
    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (getPierceLevel() > 0) {
            super.onHitEntity(hit);
            return;
        }
        Entity target = hit.getEntity();
        int type = getArrowType();
        float speed = (float) getDeltaMovement().length();
        int damage = Mth.ceil(Mth.clamp(speed * getBaseDamage() * damageFactor(), 0.0, Integer.MAX_VALUE));
        if (isCritArrow()) {
            damage = (int) Math.min(random.nextInt(damage / 2 + 2) + (long) damage, Integer.MAX_VALUE);
        }
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living) {
            living.setLastHurtMob(target);
        }
        boolean enderman = target.getType() == EntityType.ENDERMAN;
        int fireBefore = target.getRemainingFireTicks();
        // горящая стрела поджигает (водяная не горит), огненная — и без того
        int fire = isOnFire() && type != 2 ? 5 : 0;
        if (type == 1) {
            fire += 5;
        }
        if (fire > 0 && !enderman) {
            target.setSecondsOnFire(fire);
        }
        if (target.hurt(damageSource(), damage)) {
            if (enderman) {
                return;
            }
            if (target instanceof LivingEntity living) {
                if (!level().isClientSide) {
                    living.setArrowCount(living.getArrowCount() + 1);
                    switch (type) {
                        case 2 -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4), getEffectSource());
                        case 4 -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 4), getEffectSource());
                        case 5 -> living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100), getEffectSource());
                        default -> {
                        }
                    }
                }
                if (knockback > 0) {
                    double resist = Math.max(0.0, 1.0 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                    Vec3 push = getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize().scale(knockback * 0.6 * resist);
                    if (push.lengthSqr() > 0.0) {
                        living.push(push.x, 0.1, push.z);
                    }
                }
                if (!level().isClientSide && owner instanceof LivingEntity shooter) {
                    EnchantmentHelper.doPostHurtEffects(living, shooter);
                    EnchantmentHelper.doPostDamageEffects(shooter, living);
                }
                doPostHurtEffects(living);
                if (owner != null && living != owner && living instanceof net.minecraft.world.entity.player.Player
                        && owner instanceof net.minecraft.server.level.ServerPlayer shooter && !isSilent()) {
                    shooter.connection.send(new net.minecraft.network.protocol.game.ClientboundGameEventPacket(
                            net.minecraft.network.protocol.game.ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0f));
                }
            }
            playSound(getHitGroundSoundEvent(), 1f, 1.2f / (random.nextFloat() * 0.2f + 0.9f));
            discard();
        } else {
            target.setRemainingFireTicks(fireBefore);
            setDeltaMovement(getDeltaMovement().scale(-0.1));
            setYRot(getYRot() + 180f);
            yRotO += 180f;
            if (!level().isClientSide && getDeltaMovement().lengthSqr() < 1.0e-7) {
                if (pickup == Pickup.ALLOWED) {
                    spawnAtLocation(getPickupItem(), 0.1f);
                }
                discard();
            }
        }
    }

    /** Свой вид урона у воздушной, огненной и упорядоченной стрел (DamageSourceIndirectThaumcraftEntity оригинала). */
    public DamageSource damageSource() {
        Entity owner = getOwner();
        ResourceKey<DamageType> key = switch (getArrowType()) {
            case 0 -> AIR;
            case 1 -> FIRE;
            case 4 -> ORDER;
            default -> null;
        };
        if (key == null) {
            return damageSources().arrow(this, owner == null ? this : owner);
        }
        return new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), this,
                owner == null ? this : owner);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("type", (byte) getArrowType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(TYPE, tag.getByte("type"));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
