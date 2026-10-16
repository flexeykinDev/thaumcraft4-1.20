package thaumcraft.common.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.research.PlayerKnowledge;

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
        if (!level.isClientSide) {
            PlayerKnowledge knowledge = PlayerKnowledge.get(player);
            for (Aspect aspect : Aspect.primals()) {
                knowledge.addPool(aspect, level.random.nextInt(2) + 1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
