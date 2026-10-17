package thaumcraft.common.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.network.SyncKnowledgePacket;
import thaumcraft.common.registry.ModSounds;

/**
 * Искажение разума — перенос Thaumcraft.addWarpToPlayer и addStickyWarpToPlayer оригинала: запретные знания
 * и сорвавшаяся магия понемногу сводят тауматурга с ума.
 * Чем это оборачивается — см. WarpEvents.
 */
public final class Warp {

    /** @param temporary временное искажение рассеивается само; постоянное можно только прибавлять */
    public static void add(ServerPlayer player, int amount, boolean temporary) {
        if (amount == 0 || !temporary && amount < 0) {
            return;
        }
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        if (temporary) {
            if (amount < 0 && knowledge.getWarpTemp() <= 0) {
                return;
            }
            knowledge.addWarpTemp(amount);
            message(player, amount > 0 ? "tc.addwarptemp" : "tc.removewarptemp", false);
        } else {
            knowledge.addWarpPerm(amount);
            message(player, "tc.addwarp", true);
            thaumcraft.common.ModAdvancements.grant(player, thaumcraft.common.ModAdvancements.WARP);
        }
        knowledge.setWarpCounter(knowledge.getWarpTotal());
        SyncKnowledgePacket.send(player);
    }

    /** Предметы, само создание которых оставляет на создателе единицу «липкого» искажения (addWarpToItem оригинала). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> WARPING = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, thaumcraft.Thaumcraft.id("warping"));

    /** Вызывается, когда игрок создал предмет — в верстаке, магическом верстаке, тигле или алтаре. */
    public static void onCrafted(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player player,
                                 net.minecraft.world.item.ItemStack result) {
        if (player instanceof ServerPlayer server && result.is(WARPING)) {
            addSticky(server, 1);
        }
    }

    public static void addSticky(ServerPlayer player, int amount) {
        PlayerKnowledge knowledge = PlayerKnowledge.get(player);
        if (amount == 0 || amount < 0 && knowledge.getWarpSticky() <= 0) {
            return;
        }
        knowledge.addWarpSticky(amount);
        message(player, amount > 0 ? "tc.addwarpsticky" : "tc.removewarpsticky", amount > 0);
        knowledge.setWarpCounter(knowledge.getWarpTotal());
        SyncKnowledgePacket.send(player);
    }

    /** Как PacketWarpMessage оригинала: лиловый курсив в чате и шёпот, когда искажение растёт всерьёз. */
    private static void message(ServerPlayer player, String key, boolean whisper) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        if (whisper) {
            player.playNotifySound(ModSounds.WHISPERS.get(), SoundSource.PLAYERS, 0.5f, 1f);
        }
    }

    private Warp() {
    }
}
