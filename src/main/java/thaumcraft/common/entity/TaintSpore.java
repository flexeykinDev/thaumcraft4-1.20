package thaumcraft.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEntities;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.taint.Taint;
import thaumcraft.common.taint.TaintPlantBlock;

/**
 * Спора порчи (EntityTaintSpore оригинала): пузырь, который созревает на споровом стебле и растёт раз в минуту,
 * до десятого размера. Неподвижна и беззащитна, но стоит её задеть, убить или лишить стебля — лопается и высыпает
 * порченых паучков, тем больше, чем она крупнее. Вне порченой земли чахнет.
 */
public class TaintSpore extends Monster implements TaintedMob {
    private static final EntityDataAccessor<Byte> SIZE = SynchedEntityData.defineId(TaintSpore.class, EntityDataSerializers.BYTE);

    protected int growth;
    /** Только на клиенте: видимый размер плавно догоняет настоящий. */
    public float displaySize;
    /** Только на клиенте: сколько мошек роя уже кружит вокруг. */
    public int swarmCount;

    public TaintSpore(EntityType<? extends TaintSpore> type, Level level) {
        super(type, level);
        setSporeSize(2);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SIZE, (byte) 1);
    }

    public int getSporeSize() {
        return entityData.get(SIZE);
    }

    public void setSporeSize(int size) {
        entityData.set(SIZE, (byte) size);
        refreshDimensions();
        this.xpReward = size;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    /**
     * Спора растёт вверх и вширь от своего основания. Игра по умолчанию раздувает существо из центра и опускает его,
     * отчего подросшая спора проваливалась бы в стебель.
     */
    @Override
    public void refreshDimensions() {
        double x = getX();
        double y = getY();
        double z = getZ();
        super.refreshDimensions();
        setPos(x, y, z);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float size = Math.max(0.15f * getSporeSize(), 0.5f);
        return EntityDimensions.scalable(size, size);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Стебель под спорой: спора на нём сидит неподвижно. */
    protected boolean onStalk() {
        BlockPos below = BlockPos.containing(getX(), getBoundingBox().minY - 1, getZ());
        BlockState state = level().getBlockState(below);
        return state.is(ModBlocks.SPORE_STALK.get()) && state.getValue(TaintPlantBlock.RIPE);
    }

    /** Спора не ходит: её может только уронить вниз, когда под ней нет стебля. */
    @Override
    public void move(MoverType type, Vec3 movement) {
        if (!onStalk()) {
            super.move(type, new Vec3(0, Math.min(0, movement.y), 0));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 20 == 0 && !Taint.isTainted(level(), blockPosition())) {
            hurt(damageSources().starve(), 1f);
        }
        sporeTick();
    }

    protected void sporeTick() {
        if (getSporeSize() < 10 && growth++ == 1200) {
            setSporeSize(getSporeSize() + 1);
            growth = 0;
        }
        if (level().isClientSide) {
            if (displaySize < getSporeSize()) {
                displaySize += 0.02f;
            }
            if (swarmCount < getSporeSize() / 3) {
                swarmCount++;
                thaumcraft.client.fx.Fx.swarm(getId(), 0.1f, 10f, 0f);
            }
            return;
        }
        // без стебля спора лопается сразу, на стебле — когда погибает
        if (!onStalk() || deathTime > 0) {
            burst();
        }
    }

    @Override
    public void playerTouch(Player player) {
        if (!level().isClientSide && isAlive() && !player.isSpectator()) {
            burst();
        }
    }

    /** Спора лопается: паучки, брызги, стебель снова пуст. */
    protected void burst() {
        if (level().isClientSide || isRemoved()) {
            return;
        }
        playSound(ModSounds.GORE.get(), 1f, 0.9f + random.nextFloat() * 0.1f);
        int count = getSporeSize() / 3 + random.nextInt(getSporeSize() / 2 + 1);
        for (int i = 0; i < count; i++) {
            TaintSpider spider = ModEntities.TAINT_SPIDER.get().create(level());
            if (spider != null) {
                spider.moveTo(getX() + random.nextFloat() - random.nextFloat(), getY() + random.nextFloat(),
                        getZ() + random.nextFloat() - random.nextFloat(), random.nextFloat() * 360f, 0f);
                level().addFreshEntity(spider);
            }
        }
        BlockPos below = BlockPos.containing(getX(), getBoundingBox().minY - 1, getZ());
        BlockState state = level().getBlockState(below);
        if (state.is(ModBlocks.SPORE_STALK.get()) && state.getValue(TaintPlantBlock.RIPE)) {
            level().setBlock(below, state.setValue(TaintPlantBlock.RIPE, false), 3);
        }
        level().broadcastEntityEvent(this, (byte) 7);
        discard();
    }

    /** Лиловые брызги слизи вокруг споры. */
    protected void sploosh(int amount) {
        for (int i = 0; i < amount; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float spread = random.nextFloat() * 0.5f + 0.5f;
            level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
                    getX() + Math.sin(angle) * spread, getY() + random.nextFloat() * getBbHeight(), getZ() + Math.cos(angle) * spread,
                    0, 0.05, 0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 7) {
            sploosh(50);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Size", getSporeSize() - 1);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSporeSize(tag.getInt("Size") + 1);
    }

    @Override
    protected float getSoundVolume() {
        return 0.1f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SWARM.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GORE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GORE.get();
    }
}
