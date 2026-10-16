package thaumcraft.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Малое щупальце (EntityTaintacleSmall оригинала, «Taint Tendril»): вырастает под ногами жертвы большого тентакля,
 * хлещет то, что рядом, и через десять секунд отмирает. Добычи и опыта не даёт.
 */
public class TaintacleSmall extends Taintacle {
    private int lifetime = 200;

    public TaintacleSmall(EntityType<? extends TaintacleSmall> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public boolean isSmall() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && lifetime-- <= 0) {
            hurt(damageSources().magic(), 10f);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Lifetime")) {
            lifetime = tag.getInt("Lifetime");
        }
    }
}
