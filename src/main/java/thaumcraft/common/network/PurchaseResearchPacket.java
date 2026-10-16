package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.research.ResearchEntry;
import thaumcraft.common.research.ResearchHelper;
import thaumcraft.common.research.ResearchRegistry;

import java.util.function.Supplier;

/** Клиент -> сервер: игрок нажал в книге на доступное исследование — купить его за очки или получить заметку. */
public record PurchaseResearchPacket(String key) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(key);
    }

    public static PurchaseResearchPacket decode(FriendlyByteBuf buf) {
        return new PurchaseResearchPacket(buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player != null) {
            ResearchEntry entry = ResearchRegistry.get(key);
            if (entry != null && entry.isPurchasable()) {
                ResearchHelper.purchase(player, key);
            } else {
                ResearchHelper.requestNote(player, key);
            }
        }
    }
}
