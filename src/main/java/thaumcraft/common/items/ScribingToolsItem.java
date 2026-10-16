package thaumcraft.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blockentity.ResearchTableBlockEntity;
import thaumcraft.common.blocks.ResearchTableBlock;
import thaumcraft.common.registry.ModBlocks;

/**
 * Письменные принадлежности (ItemInkwell оригинала): чернил хватает на 100 действий. Нужны, чтобы получать
 * заметки из таумономикона и работать за столом исследований. Клик ими по одному из двух стоящих рядом
 * столов превращает пару в стол исследований.
 */
public class ScribingToolsItem extends Item {

    public ScribingToolsItem() {
        super(new Item.Properties().stacksTo(1).durability(100));
    }

    public static boolean hasInk(ItemStack stack) {
        return stack.getItem() instanceof ScribingToolsItem && stack.getDamageValue() < stack.getMaxDamage();
    }

    /** Чернила не «ломают» предмет: пустую чернильницу можно заправить. */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !level.getBlockState(pos).is(ModBlocks.TABLE.get())) {
            return InteractionResult.PASS;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos other = pos.relative(direction);
            if (!level.getBlockState(other).is(ModBlocks.TABLE.get())) {
                continue;
            }
            if (!level.isClientSide) {
                BlockState main = ModBlocks.RESEARCH_TABLE.get().defaultBlockState()
                        .setValue(ResearchTableBlock.FACING, direction).setValue(ResearchTableBlock.MAIN, true);
                BlockState side = main.setValue(ResearchTableBlock.FACING, direction.getOpposite())
                        .setValue(ResearchTableBlock.MAIN, false);
                level.setBlock(pos, main, 3);
                level.setBlock(other, side, 3);
                if (level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
                    table.getItems().set(ResearchTableBlockEntity.TOOLS_SLOT, context.getItemInHand().copy());
                    table.contentsChanged();
                    player.setItemInHand(context.getHand(), ItemStack.EMPTY);
                }
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
