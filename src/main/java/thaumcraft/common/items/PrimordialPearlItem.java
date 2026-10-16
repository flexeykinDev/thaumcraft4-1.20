package thaumcraft.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.blocks.FluxBlock;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.List;

/**
 * Первозданная жемчужина (ItemEldritchObject:3 оригинала). Раздавленная о узел ауры, взрывается и перекраивает его:
 * сложные аспекты могут ослабнуть, первоначальные — окрепнуть, в узел добавляются недостающие первоначала, тусклые
 * узлы светлеют. Изучивший «Первозданный узел» получает больше и взрывом страдает меньше. Вокруг разливается скверна.
 */
public class PrimordialPearlItem extends Item {
    public PrimordialPearlItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ItemEldritchObject.text.5").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.ItemEldritchObject.text.6").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof NodeBlockEntity node) || context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        context.getPlayer().swing(context.getHand());
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        stack.shrink(1);
        boolean research = PlayerKnowledge.get(context.getPlayer()).isResearchComplete("PRIMNODE");
        var random = level.random;
        for (Aspect aspect : node.getAspects()) {
            int base = node.getBase(aspect);
            if (!aspect.isPrimal()) {
                if (random.nextBoolean()) {
                    node.setBase(aspect, base - 1);
                }
            } else {
                node.setBase(aspect, base - 2 + random.nextInt(research ? 9 : 6));
            }
        }
        for (Aspect aspect : Aspect.primals()) {
            int base = node.getBase(aspect);
            int roll = random.nextInt(research ? 4 : 3);
            if (roll > 0 && roll > base) {
                node.setBase(aspect, roll);
                node.addToContainer(aspect, 1);
            }
        }
        NodeModifier modifier = node.getNodeModifier();
        if (modifier == NodeModifier.FADING && random.nextBoolean()) {
            node.setNodeModifier(NodeModifier.PALE);
        } else if (modifier == NodeModifier.PALE && random.nextBoolean()) {
            node.setNodeModifier(null);
        } else if (modifier == null && random.nextInt(5) == 0) {
            node.setNodeModifier(NodeModifier.BRIGHT);
        }
        node.nodeChange();
        level.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3f + random.nextFloat() * (research ? 3 : 5),
                Level.ExplosionInteraction.TNT);
        for (int a = 0; a < 33; a++) {
            BlockPos near = pos.offset(random.nextInt(6) - random.nextInt(6), random.nextInt(6) - random.nextInt(6),
                    random.nextInt(6) - random.nextInt(6));
            if (level.isEmptyBlock(near)) {
                BlockState flux = (near.getY() < pos.getY() ? ModBlocks.FLUX_GOO : ModBlocks.FLUX_GAS).get().defaultBlockState()
                        .setValue(FluxBlock.LEVEL, 7);
                level.setBlock(near, flux, 3);
            }
        }
        return InteractionResult.CONSUME;
    }
}
