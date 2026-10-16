package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.items.wands.Architect;

import java.util.function.Supplier;

/** Клиент -> сервер: нажата клавиша «Переключатель жезла» (PacketItemKeyToServer оригинала с кодом 1). */
public record ToggleMiscPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ToggleMiscPacket decode(FriendlyByteBuf buf) {
        return new ToggleMiscPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) {
            net.minecraft.world.item.ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof thaumcraft.common.items.tools.ElementalShovelItem) {
                // у лопаты землекопа клавиша меняет, как лягут девять блоков: плашмя, стеной или полом
                thaumcraft.common.items.tools.ElementalShovelItem.setOrientation(held,
                        (byte) (thaumcraft.common.items.tools.ElementalShovelItem.getOrientation(held) + 1));
                return;
            }
            Architect.toggle(held, player);
        }
    }
}
