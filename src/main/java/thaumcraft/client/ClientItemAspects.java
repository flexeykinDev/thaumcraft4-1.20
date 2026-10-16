package thaumcraft.client;

import net.minecraft.world.item.Item;
import thaumcraft.api.aspects.AspectList;

import java.util.Map;

/** Копия аспектов предметов на клиенте, присланная сервером. */
public final class ClientItemAspects {
    private static final AspectList EMPTY = new AspectList();
    private static volatile Map<Item, AspectList> aspects = Map.of();

    public static void set(Map<Item, AspectList> synced) {
        aspects = synced;
    }

    public static AspectList get(Item item) {
        return aspects.getOrDefault(item, EMPTY);
    }

    private ClientItemAspects() {
    }
}
