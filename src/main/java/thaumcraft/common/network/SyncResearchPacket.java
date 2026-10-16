package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.research.ResearchRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Сервер -> клиент: всё дерево исследований одним сжатым JSON. */
public record SyncResearchPacket(String json) {

    public void encode(FriendlyByteBuf buf) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(json.getBytes(StandardCharsets.UTF_8));
            }
            buf.writeByteArray(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static SyncResearchPacket decode(FriendlyByteBuf buf) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(buf.readByteArray()))) {
            return new SyncResearchPacket(new String(gzip.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ResearchRegistry.load(json);
    }
}
