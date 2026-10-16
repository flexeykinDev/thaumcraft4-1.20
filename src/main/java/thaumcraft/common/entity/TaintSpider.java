package thaumcraft.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import thaumcraft.api.entities.TaintedMob;

/**
 * Порченый паучок (EntityTaintSpider оригинала, «Tainted Crawler»): крошечный паук, который высыпается из лопнувшей
 * споры и из «заражённых» особых мобов. Пять единиц здоровья, укус на две.
 */
public class TaintSpider extends Spider implements TaintedMob {
    public TaintSpider(EntityType<? extends TaintSpider> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }
}
