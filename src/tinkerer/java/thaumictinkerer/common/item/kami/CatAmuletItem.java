package thaumictinkerer.common.item.kami;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import thaumictinkerer.ThaumicTinkerer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Амулет кошки (ItemCatAmulet): существа в 10 блоках от носящего навсегда перестают сами искать игроков, а криперы начинают
 * бояться игроков, как кошек, и гасят запал. Ответить на удар существо по-прежнему может.
 */
@Mod.EventBusSubscriber(modid = ThaumicTinkerer.MODID)
public class CatAmuletItem extends Item implements ICurioItem {
    /** Метка «напуган амулетом»: переживает перезаход, при загрузке существа его ИИ правится снова. */
    private static final String TAG = "thaumictinkerer:cat_scared";
    private static final Field TARGET_TYPE = ObfuscationReflectionHelper.findField(NearestAttackableTargetGoal.class, "f_26048_");

    public CatAmuletItem() {
        super(new Item.Properties().stacksTo(1).rarity(Kami.RARITY));
    }

    @Override
    public void curioTick(SlotContext context, ItemStack stack) {
        LivingEntity wearer = context.entity();
        if (wearer.level().isClientSide) {
            return;
        }
        for (Mob mob : wearer.level().getEntitiesOfClass(Mob.class, new AABB(wearer.getX() - 10, wearer.getY() - 4, wearer.getZ() - 10,
                wearer.getX() + 10, wearer.getY() + 4, wearer.getZ() + 10))) {
            if (!mob.getPersistentData().getBoolean(TAG)) {
                mob.getPersistentData().putBoolean(TAG, true);
                scare(mob);
            }
            if (mob instanceof Creeper creeper) {
                creeper.setSwellDir(-1);
                creeper.setTarget(null);
            } else if (mob.getTarget() instanceof Player) {
                mob.setTarget(null);
            }
        }
    }

    /** messWithGetTargetAI и messWithRunAwayAI оригинала. */
    private static void scare(Mob mob) {
        List<WrappedGoal> remove = new ArrayList<>();
        for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
            try {
                if (goal.getGoal() instanceof NearestAttackableTargetGoal<?> target && TARGET_TYPE.get(target) == Player.class) {
                    remove.add(goal);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        remove.forEach(goal -> mob.targetSelector.removeGoal(goal.getGoal()));
        if (mob instanceof Creeper creeper) {
            creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>((PathfinderMob) creeper, Player.class, 6f, 1.0, 1.2));
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob && mob.getPersistentData().getBoolean(TAG)) {
            scare(mob);
        }
    }
}
