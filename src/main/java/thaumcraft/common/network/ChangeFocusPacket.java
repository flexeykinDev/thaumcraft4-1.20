package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.items.wands.FocusManager;

import java.util.function.Supplier;

/**
 * Клиент -> сервер: игрок выбрал фокус в круговом меню (PacketFocusChangeToServer оригинала).
 *
 * @param key ключ сортировки выбранного фокуса либо {@link FocusManager#REMOVE}, чтобы снять фокус с жезла
 */
public record ChangeFocusPacket(String key) {
    /** Та же клавиша с колокольчиком големанта в руке снимает все отметки голема (PacketItemKeyToServer оригинала). */
    public static final String BELL_RESET = "bell_reset";

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(key, 64);
    }

    public static ChangeFocusPacket decode(FriendlyByteBuf buf) {
        return new ChangeFocusPacket(buf.readUtf(64));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null && BELL_RESET.equals(key)) {
            if (player.getMainHandItem().getItem() instanceof thaumcraft.common.items.golem.GolemBellItem) {
                thaumcraft.common.items.golem.GolemBellItem.resetMarkers(player.getMainHandItem(), player);
            }
        } else if (player != null) {
            FocusManager.changeFocus(player, key);
        }
    }
}
