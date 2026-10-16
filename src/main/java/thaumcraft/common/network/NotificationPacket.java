package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.ClientHudState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Сервер -> клиент: всплывающие уведомления у края экрана, с иконкой аспекта или без. */
public record NotificationPacket(List<Entry> entries) {

    public record Entry(@Nullable Aspect aspect, Component text) {
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUtf(entry.aspect() == null ? "" : entry.aspect().getTag());
            buf.writeComponent(entry.text());
        }
    }

    public static NotificationPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Aspect aspect = Aspect.byTag(buf.readUtf());
            entries.add(new Entry(aspect, buf.readComponent()));
        }
        return new NotificationPacket(entries);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        for (Entry entry : entries) {
            ClientHudState.addNotification(entry.aspect(), entry.text());
        }
    }
}
