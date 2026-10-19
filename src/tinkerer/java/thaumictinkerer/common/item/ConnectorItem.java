package thaumictinkerer.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumictinkerer.common.block.TransvectorBlock;

/**
 * Связующий щуп (ItemConnector оригинала): первым щелчком запоминает трансвектор, вторым привязывает к нему блок-цель.
 * Два интерфейса между собой не связать; цель должна быть в пределах досягаемости трансвектора.
 */
public class ConnectorItem extends Item {
    public ConnectorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (!stack.hasTag() || !stack.getTag().contains("Linking")) {
            if (level.getBlockEntity(clicked) instanceof TransvectorBlock.Tile) {
                stack.getOrCreateTag().putLong("Linking", clicked.asLong());
                ping(level, clicked);
                player.sendSystemMessage(Component.translatable("ttmisc.connector.set"));
            } else {
                player.sendSystemMessage(Component.translatable("ttmisc.connector.notinterf"));
            }
            return InteractionResult.SUCCESS;
        }
        BlockPos source = BlockPos.of(stack.getTag().getLong("Linking"));
        stack.getTag().remove("Linking");
        if (!(level.getBlockEntity(source) instanceof TransvectorBlock.Tile transvector)) {
            player.sendSystemMessage(Component.translatable("ttmisc.connector.notpresent"));
            return InteractionResult.SUCCESS;
        }
        if (transvector instanceof TransvectorBlock.Interface.Tile && level.getBlockEntity(clicked) instanceof TransvectorBlock.Interface.Tile) {
            player.sendSystemMessage(Component.translatable("ttmisc.connector.interffail"));
            return InteractionResult.SUCCESS;
        }
        if (!transvector.inRange(clicked)) {
            player.sendSystemMessage(Component.translatable("ttmisc.connector.toofar"));
            return InteractionResult.SUCCESS;
        }
        transvector.setTarget(clicked);
        ping(level, clicked);
        player.sendSystemMessage(Component.translatable("ttmisc.connector.complete"));
        return InteractionResult.SUCCESS;
    }

    private static void ping(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.8f, 1f);
    }
}
