package thaumcraft.common.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;

/**
 * Фрагмент знаний (ItemResource:9 оригинала): щелчок расходует его и прибавляет к запасу игрока по одному-два очка
 * каждого из шести первичных аспектов. Девять фрагментов складываются в «неизвестное знание».
 */
public class KnowledgeFragmentItem extends Item {
    public KnowledgeFragmentItem() {
        super(new Item.Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            thaumcraft.api.aspects.AspectList gained = new thaumcraft.api.aspects.AspectList();
            for (Aspect aspect : Aspect.primals()) {
                gained.add(aspect, level.random.nextInt(2) + 1);
            }
            thaumcraft.common.research.ResearchHelper.giveAspects(serverPlayer, gained);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
