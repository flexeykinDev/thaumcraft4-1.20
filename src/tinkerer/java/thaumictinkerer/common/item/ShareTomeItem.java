package thaumictinkerer.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.List;

/**
 * Том обмена знаниями (ItemShareBook оригинала). Чистый том записывает все исследования того, кто им щёлкнул;
 * любой другой игрок, щёлкнув записанным томом, получает их себе. Если автор в игре, берутся его исследования
 * на этот момент, иначе — записанные в томе.
 */
public class ShareTomeItem extends Item {
    public ShareTomeItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Nullable
    private static String owner(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("player") ? stack.getTag().getString("player") : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        String owner = owner(stack);
        if (owner == null) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("player", player.getGameProfile().getName());
            ListTag research = new ListTag();
            for (String key : PlayerKnowledge.get(player).getCompletedResearch()) {
                research.add(StringTag.valueOf(key));
            }
            tag.put("research", research);
            player.sendSystemMessage(Component.translatable("ttmisc.shareTome.write"));
            return InteractionResultHolder.success(stack);
        }
        List<String> research;
        ServerPlayer author = serverPlayer.server.getPlayerList().getPlayerByName(owner);
        if (author != null) {
            research = List.copyOf(PlayerKnowledge.get(author).getCompletedResearch());
        } else {
            ListTag list = stack.getTag().getList("research", Tag.TAG_STRING);
            research = new java.util.ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                research.add(list.getString(i));
            }
        }
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        for (String key : research) {
            knowledge.completeResearch(key);
        }
        SyncKnowledgePacket.send(serverPlayer);
        player.sendSystemMessage(Component.translatable("ttmisc.shareTome.sync"));
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String owner = owner(stack);
        tooltip.add((owner == null ? Component.translatable("ttmisc.shareTome.noAssign")
                : Component.translatable("ttmisc.shareTome.playerName", owner)).withStyle(ChatFormatting.GRAY));
    }
}
