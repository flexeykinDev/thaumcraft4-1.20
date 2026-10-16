package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.items.golem.GolemBellItem;
import thaumcraft.common.items.golem.GolemUpgradeItem;
import thaumcraft.common.menu.TrunkMenu;
import thaumcraft.common.registry.ModSounds;

import java.util.UUID;

/**
 * Дорожный сундук (EntityTravelingTrunk оригинала): сундук, который прыжками скачет за хозяином, а отстав на двадцать
 * блоков — переносится к нему. Одно улучшение меняет его нрав: воздух — резвее, земля — четвёртый ряд ячеек,
 * огонь — кусает обидчиков хозяина, вода — неуязвим и открывается только хозяину, порядок — уносит содержимое
 * в предмете, энтропия — втягивает лежащие рядом вещи.
 */
public class TravelingTrunk extends Mob implements OwnableEntity {
    private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(TravelingTrunk.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> STAY = SynchedEntityData.defineId(TravelingTrunk.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER = SynchedEntityData.defineId(TravelingTrunk.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> UPGRADE = SynchedEntityData.defineId(TravelingTrunk.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(TravelingTrunk.class, EntityDataSerializers.INT);

    /** Четыре ряда ячеек есть всегда; четвёртый доступен только с улучшением земли. */
    public final SimpleContainer inventory = new SimpleContainer(36);
    public float lidrot;
    private int jumpDelay;
    private int attackTime;

    public TravelingTrunk(EntityType<? extends TravelingTrunk> type, Level level) {
        super(type, level);
        blocksBuilding = true;
        jumpDelay = random.nextInt(20) + 10;
        setPersistenceRequired();
        moveControl = new Hop(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 75.0).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OPEN, false);
        entityData.define(STAY, false);
        entityData.define(OWNER, "");
        entityData.define(UPGRADE, (byte) -1);
        entityData.define(ANGER, 0);
    }

    // ---------- данные ----------

    public int getUpgrade() {
        return entityData.get(UPGRADE);
    }

    public void setUpgrade(int upgrade) {
        entityData.set(UPGRADE, (byte) upgrade);
    }

    public int getRows() {
        return getUpgrade() == Golem.UPGRADE_EARTH ? 4 : 3;
    }

    public int getAnger() {
        return entityData.get(ANGER);
    }

    public void setAnger(int anger) {
        entityData.set(ANGER, anger);
    }

    public boolean isOpen() {
        return entityData.get(OPEN);
    }

    public void setOpen(boolean open) {
        entityData.set(OPEN, open);
    }

    public boolean getStay() {
        return entityData.get(STAY);
    }

    public void setStay(boolean stay) {
        entityData.set(STAY, stay);
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        try {
            return entityData.get(OWNER).isEmpty() ? null : UUID.fromString(entityData.get(OWNER));
        } catch (IllegalArgumentException broken) {
            return null;
        }
    }

    public void setOwner(Player player) {
        entityData.set(OWNER, player.getUUID().toString());
    }

    /** Сундук с улучшением воды слушается только хозяина. */
    public boolean refuses(Player player) {
        return getUpgrade() == Golem.UPGRADE_WATER && !player.getUUID().equals(getOwnerUUID());
    }

    // ---------- жизнь ----------

    @Override
    public float getSpeed() {
        return 0.1f;
    }

    @Override
    protected float getFlyingSpeed() {
        return getUpgrade() == Golem.UPGRADE_AIR ? 0.04f : 0.03f;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.CACTUS) || getUpgrade() == Golem.UPGRADE_WATER && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.GENERIC_KILL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (getUpgrade() == Golem.UPGRADE_ENTROPY) {
            pullItems();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (isInWater()) {
            setDeltaMovement(getDeltaMovement().add(0, 0.033, 0));
        }
        if (level().isClientSide) {
            // крышка хлопает в прыжке и распахивается, пока сундук открыт
            boolean afloat = isInWater();
            if (!onGround() && getDeltaMovement().y < 0.0 && !afloat) {
                lidrot += 0.015f;
            }
            if ((onGround() || afloat) && !isOpen()) {
                lidrot = Math.max(0f, lidrot - 0.1f);
            }
            if (isOpen()) {
                lidrot += 0.035f;
            }
            lidrot = Math.min(lidrot, isOpen() ? 0.5f : 0.2f);
        } else if (getHealth() < getMaxHealth() && (getUpgrade() == Golem.UPGRADE_WATER || tickCount % 50 == 0)) {
            heal(1f);
        }
    }

    /** Улучшение энтропии: сундук подтягивает к себе вещи с трёх блоков и глотает те, что рядом. */
    private void pullItems() {
        if (!isAlive()) {
            return;
        }
        if (!level().isClientSide) {
            for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.1))) {
                ItemStack stack = item.getItem().copy();
                ItemStack left = insert(stack);
                if (left.getCount() != stack.getCount()) {
                    playSound(SoundEvents.GENERIC_EAT, 0.5f, random.nextFloat() * 0.5f + 0.5f);
                    level().broadcastEntityEvent(this, (byte) 17);
                    if (left.isEmpty()) {
                        item.discard();
                    } else {
                        item.setItem(left);
                    }
                }
            }
        }
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(3.0))) {
            Vec3 pull = new Vec3(item.getX() - getX(), item.getY() - getY() + getBbHeight() * 0.8f, item.getZ() - getZ()).normalize().scale(0.075);
            item.setDeltaMovement(item.getDeltaMovement().subtract(pull));
        }
    }

    /** Кладёт предмет в доступные ряды; возвращает то, что не поместилось. */
    public ItemStack insert(ItemStack stack) {
        int slots = getRows() * 9;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = 0; slot < slots && !stack.isEmpty(); slot++) {
                ItemStack there = inventory.getItem(slot);
                if (pass == 0 && !there.isEmpty() && ItemStack.isSameItemSameTags(there, stack)) {
                    int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(moved);
                    stack.shrink(moved);
                } else if (pass == 1 && there.isEmpty()) {
                    inventory.setItem(slot, stack.copy());
                    stack.setCount(0);
                }
            }
        }
        return stack;
    }

    public void dropAllItems() {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                spawnAtLocation(stack, 0.5f);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            dropAllItems();
        }
        super.die(source);
    }

    /** Мозгов у сундука нет: всё его поведение — в «управлении движением», как у слизня. */
    private static class Hop extends MoveControl {
        private final TravelingTrunk trunk;

        Hop(TravelingTrunk trunk) {
            super(trunk);
            this.trunk = trunk;
        }

        private void face(LivingEntity target) {
            double dx = target.getX() - trunk.getX();
            double dz = target.getZ() - trunk.getZ();
            float wanted = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
            trunk.setYRot(rotlerp(trunk.getYRot(), wanted, 10f));
            trunk.yBodyRot = trunk.getYRot();
            trunk.yHeadRot = trunk.getYRot();
        }

        @Override
        public void tick() {
            if (trunk.getAnger() > 0) {
                trunk.setAnger(trunk.getAnger() - 1);
            }
            if (trunk.attackTime > 0) {
                trunk.attackTime--;
            }
            LivingEntity owner = trunk.getOwner();
            if (owner == null) {
                trunk.setZza(0f);
                trunk.setXxa(0f);
                return;
            }
            Level level = trunk.level();
            // хозяин ушёл дальше двадцати блоков: сундук появляется рядом с ним
            if (!trunk.getStay() && trunk.distanceTo(owner) > 20f) {
                BlockPos base = owner.blockPosition().offset(-2, 0, -2);
                for (int dx = 0; dx <= 4; dx++) {
                    for (int dz = 0; dz <= 4; dz++) {
                        BlockPos spot = base.offset(dx, 0, dz);
                        BlockPos floor = spot.below();
                        boolean edge = dx < 1 || dz < 1 || dx > 3 || dz > 3;
                        if (edge && (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP) || level.getFluidState(floor).is(FluidTags.WATER))
                                && !level.getBlockState(spot).isRedstoneConductor(level, spot)
                                && !level.getBlockState(spot.above()).isRedstoneConductor(level, spot.above())) {
                            level.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, trunk.getSoundSource(), 0.5f, 1f);
                            trunk.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, trunk.getYRot(), trunk.getXRot());
                            trunk.setTarget(null);
                            return;
                        }
                    }
                }
            }
            LivingEntity target = trunk.getTarget();
            if (target != null && !target.isAlive()) {
                trunk.setTarget(null);
                trunk.setAnger(5);
                target = null;
            }
            // улучшение огня: сундук вступается за хозяина
            LivingEntity foe = owner.getLastHurtByMob();
            if (!trunk.getStay() && trunk.getUpgrade() == Golem.UPGRADE_FIRE && trunk.getAnger() == 0 && target == null && foe != null
                    && foe.isAlive() && foe != trunk && foe.distanceTo(trunk) < 16f) {
                trunk.setAnger(600);
                trunk.setTarget(foe);
                target = foe;
            }
            boolean move = false;
            if (trunk.getAnger() > 0 && target != null && target != owner) {
                face(target);
                move = true;
                if (trunk.attackTime <= 0 && trunk.distanceTo(target) < 1.5f && target.getBoundingBox().maxY > trunk.getBoundingBox().minY
                        && target.getBoundingBox().minY < trunk.getBoundingBox().maxY) {
                    trunk.attackTime = 10 + trunk.getRandom().nextInt(5);
                    target.hurt(trunk.damageSources().mobAttack(trunk), 4f);
                    level.broadcastEntityEvent(trunk, (byte) 17);
                    trunk.playSound(SoundEvents.BLAZE_HURT, 0.5f, trunk.getRandom().nextFloat() * 0.1f + 0.9f);
                }
            }
            if (trunk.distanceTo(owner) > 5f && trunk.getAnger() == 0 && !trunk.getStay()) {
                face(owner);
                move = true;
            }
            boolean grounded = trunk.onGround() || trunk.isInWater();
            if (grounded && trunk.jumpDelay-- <= 0 && move) {
                trunk.jumpDelay = (trunk.getRandom().nextInt(10) + 5) / 3;
                trunk.getJumpControl().jump();
                trunk.setXxa(1f - trunk.getRandom().nextFloat() * 2f);
                trunk.setZza(trunk.isInWater() ? 4.5f : 6f);
                trunk.playSound(SoundEvents.CHEST_CLOSE, 0.1f, trunk.getRandom().nextFloat() * 0.1f + 0.9f);
            } else if (trunk.onGround()) {
                trunk.setXxa(0f);
                trunk.setZza(0f);
            }
        }
    }

    // ---------- взаимодействие ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof GolemBellItem) {
            return InteractionResult.PASS;
        }
        if (getUpgrade() == -1 && held.getItem() instanceof GolemUpgradeItem upgrade) {
            if (!level().isClientSide) {
                setUpgrade(upgrade.getUpgrade());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(ModSounds.UPGRADE.get(), 0.5f, 1f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (held.isEdible() && getHealth() < getMaxHealth()) {
            // сундук можно подлечить едой
            if (!level().isClientSide) {
                heal(held.getFoodProperties(this).getNutrition());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(getHealth() == getMaxHealth() ? SoundEvents.PLAYER_BURP : SoundEvents.GENERIC_EAT, 0.5f, random.nextFloat() * 0.5f + 0.5f);
                level().broadcastEntityEvent(this, (byte) 18);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer && !refuses(player)) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, opener) -> new TrunkMenu(id, inventory, this),
                    getDisplayName()), buf -> buf.writeVarInt(getId()).writeVarInt(getRows()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 17) {
            lidrot = 0.15f;
        } else if (id == 18) {
            lidrot = 0.15f;
            level().addParticle(ParticleTypes.HEART, getX() + random.nextFloat() * getBbWidth() * 2f - getBbWidth(),
                    getY() + 0.5 + random.nextFloat() * getBbHeight(), getZ() + random.nextFloat() * getBbWidth() * 2f - getBbWidth(),
                    random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_STEP;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ITEM_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5f;
    }

    // ---------- сохранение ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Stay", getStay());
        tag.putByte("upgrade", (byte) getUpgrade());
        tag.putString("Owner", entityData.get(OWNER));
        tag.put("Inventory", Golem.saveInventory(inventory));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStay(tag.getBoolean("Stay"));
        setUpgrade(tag.contains("upgrade") ? tag.getByte("upgrade") : -1);
        entityData.set(OWNER, tag.getString("Owner"));
        Golem.loadInventory(inventory, tag.getList("Inventory", Tag.TAG_COMPOUND));
    }
}
