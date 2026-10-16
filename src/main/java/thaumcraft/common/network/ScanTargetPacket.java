package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientHudState;

import java.util.function.Supplier;

/**
 * Сервер -> клиент: на что сейчас смотрит игрок с таумометром в руке.
 * Аспекты приходят только для уже изученной цели.
 *
 * @param present false — игрок ни на что не смотрит или убрал таумометр
 */
public record ScanTargetPacket(boolean present, Component name, boolean scanned, AspectList aspects) {

    public static ScanTargetPacket none() {
        return new ScanTargetPacket(false, Component.empty(), false, new AspectList());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(present);
        if (present) {
            buf.writeComponent(name);
            buf.writeBoolean(scanned);
            aspects.write(buf);
        }
    }

    public static ScanTargetPacket decode(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return none();
        }
        return new ScanTargetPacket(true, buf.readComponent(), buf.readBoolean(), AspectList.read(buf));
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ClientHudState.setScanTarget(present ? this : null);
    }
}
