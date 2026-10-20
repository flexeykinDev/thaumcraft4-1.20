package thaumictinkerer.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import thaumictinkerer.ThaumicTinkerer;

import java.util.Optional;
import java.util.function.Supplier;

/** Сетевой канал аддона: пока только число сердец души для полоски на экране (PacketSoulHearts). */
public final class TTNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ThaumicTinkerer.id("main"), () -> VERSION,
            VERSION::equals, VERSION::equals);

    private TTNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(0, SoulHearts.class, SoulHearts::write, SoulHearts::read, SoulHearts::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendSoulHearts(ServerPlayer player, int hearts) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SoulHearts(hearts));
    }

    public record SoulHearts(int hearts) {
        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(hearts);
        }

        static SoulHearts read(FriendlyByteBuf buf) {
            return new SoulHearts(buf.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> thaumictinkerer.common.item.kami.SoulHearts.clientHearts = hearts);
            context.get().setPacketHandled(true);
        }
    }
}
