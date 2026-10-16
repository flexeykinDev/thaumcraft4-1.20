package thaumcraft.common.taint;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.entities.TaintedMob;
import thaumcraft.common.entity.ThaumicSlime;
import thaumcraft.common.registry.ModEffects;
import thaumcraft.common.registry.ModEntities;

/**
 * Смерть от порчи (EventHandlerEntity.livingTick оригинала): всё живое, что умирает с порченым ядом в крови, не
 * оставляет тела и добычи, а превращается. Крипер, овца, корова, свинья, курица и житель — в свои порченые подобия,
 * всё остальное — в таумического слизня, тем крупнее, чем здоровее была жертва.
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
public final class TaintEvents {
    private TaintEvents() {
    }

    private static EntityType<?> taintedForm(LivingEntity victim) {
        if (victim instanceof Creeper) {
            return ModEntities.TAINTED_CREEPER.get();
        } else if (victim instanceof Sheep) {
            return ModEntities.TAINTED_SHEEP.get();
        } else if (victim instanceof Cow) {
            return ModEntities.TAINTED_COW.get();
        } else if (victim instanceof Pig) {
            return ModEntities.TAINTED_PIG.get();
        } else if (victim instanceof Chicken) {
            return ModEntities.TAINTED_CHICKEN.get();
        } else if (victim instanceof Villager) {
            return ModEntities.TAINTED_VILLAGER.get();
        }
        return ModEntities.THAUMIC_SLIME.get();
    }

    /**
     * Сферы аспектов из убитого: если существо недавно ранил игрок, каждый первичный аспект, из которых оно состоит,
     * с шансом один к двум вылетает сферой — от одной единицы до всего, что в существе было.
     */
    private static void dropAspectOrbs(LivingEntity victim) {
        if (!(victim.getKillCredit() instanceof Player)) {
            return;
        }
        thaumcraft.api.aspects.AspectList primals = thaumcraft.common.research.ResearchHelper.reduceToPrimals(
                thaumcraft.common.aspects.ItemAspectManager.INSTANCE.getEntityAspects(victim));
        net.minecraft.util.RandomSource random = victim.getRandom();
        for (thaumcraft.api.aspects.Aspect aspect : primals.getAspects()) {
            if (random.nextBoolean()) {
                thaumcraft.common.entity.AspectOrbEntity.spawn(victim.level(), victim.getX(), victim.getY(), victim.getZ(), aspect,
                        1 + random.nextInt(primals.getAmount(aspect)));
            }
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim instanceof ArmorStand) {
            return;
        }
        if (victim instanceof TaintedMob || !victim.hasEffect(ModEffects.TAINT_POISON.get())) {
            dropAspectOrbs(victim);
            return;
        }
        Entity born = taintedForm(victim).create(victim.level());
        if (born == null) {
            return;
        }
        if (born instanceof ThaumicSlime slime) {
            slime.setSlimeSize((int) (1f + Math.min(victim.getMaxHealth() / 10f, 6f)));
        }
        born.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0f);
        victim.level().addFreshEntity(born);
        // игрок умирает как обычно, остальные исчезают бесследно
        if (!(victim instanceof Player)) {
            victim.discard();
        }
    }
}
