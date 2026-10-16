package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

/**
 * Паук разума (EntityMindSpider оригинала): крошечный паук, порождение безумия. Бывает настоящим (кусается)
 * и видением: видение видит только тот, кому оно явилось, вреда оно не причиняет и через минуту исчезает.
 */
public class MindSpider extends Spider {
    private static final EntityDataAccessor<Boolean> HARMLESS = SynchedEntityData.defineId(MindSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(MindSpider.class, EntityDataSerializers.STRING);

    private int lifeSpan = Integer.MAX_VALUE;

    public MindSpider(EntityType<? extends MindSpider> type, Level level) {
        super(type, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HARMLESS, false);
        entityData.define(VIEWER, "");
    }

    /** Имя игрока, которому явилось видение; пустая строка — паука видят все. */
    public String getViewer() {
        return entityData.get(VIEWER);
    }

    public void setViewer(String player) {
        entityData.set(VIEWER, player);
    }

    public boolean isHarmless() {
        return entityData.get(HARMLESS);
    }

    public void setHarmless(boolean harmless) {
        if (harmless) {
            lifeSpan = 1200;
        }
        entityData.set(HARMLESS, harmless);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > lifeSpan) {
            discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !isHarmless() && super.doHurtTarget(target);
    }

    @Override
    public int getExperienceReward() {
        return isHarmless() ? 0 : super.getExperienceReward();
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("harmless", isHarmless());
        tag.putString("viewer", getViewer());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHarmless(tag.getBoolean("harmless"));
        setViewer(tag.getString("viewer"));
    }
}
