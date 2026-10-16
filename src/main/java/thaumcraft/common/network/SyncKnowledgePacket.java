package thaumcraft.common.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import thaumcraft.client.ClientKnowledge;
import thaumcraft.common.research.PlayerKnowledge;

import java.util.function.Supplier;

/** Сервер -> клиент: полная копия знаний игрока. */
public record SyncKnowledgePacket(CompoundTag data) {

    public static void send(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncKnowledgePacket(PlayerKnowledge.get(player).save()));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
    }

    public static SyncKnowledgePacket decode(FriendlyByteBuf buf) {
        CompoundTag data = buf.readNbt();
        return new SyncKnowledgePacket(data == null ? new CompoundTag() : data);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ClientKnowledge.get().load(data);
    }
}
