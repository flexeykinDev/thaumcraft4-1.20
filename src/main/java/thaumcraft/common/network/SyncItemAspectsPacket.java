package thaumcraft.common.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.ClientItemAspects;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Сервер -> клиент: аспекты всех предметов, чтобы клиент мог показывать их в подсказках. */
public record SyncItemAspectsPacket(Map<Item, AspectList> aspects) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(aspects.size());
        aspects.forEach((item, list) -> {
            buf.writeId(BuiltInRegistries.ITEM, item);
            buf.writeVarInt(list.size());
            for (Aspect aspect : list.getAspects()) {
                buf.writeUtf(aspect.getTag());
                buf.writeVarInt(list.getAmount(aspect));
            }
        });
    }

    public static SyncItemAspectsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<Item, AspectList> aspects = new IdentityHashMap<>(count);
        for (int i = 0; i < count; i++) {
            Item item = buf.readById(BuiltInRegistries.ITEM);
            AspectList list = new AspectList();
            int size = buf.readVarInt();
            for (int j = 0; j < size; j++) {
                Aspect aspect = Aspect.byTag(buf.readUtf());
                int amount = buf.readVarInt();
                if (aspect != null) {
                    list.add(aspect, amount);
                }
            }
            if (item != null) {
                aspects.put(item, list);
            }
        }
        return new SyncItemAspectsPacket(aspects);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ClientItemAspects.set(aspects);
    }
}
