package thaumcraft.client;

import net.minecraft.world.item.Item;
import thaumcraft.api.aspects.AspectList;

import java.util.Map;

/** Копия аспектов предметов на клиенте, присланная сервером. */
public final class ClientItemAspects {
    private static final AspectList EMPTY = new AspectList();
    private static volatile Map<Item, AspectList> aspects = Map.of();

    /** Кому сообщить о новых данных (JEI достраивает категорию «Аспекты предметов»). */
    public static volatile Runnable listener;

    public static void set(Map<Item, AspectList> synced) {
        aspects = synced;
        Runnable callback = listener;
        if (callback != null) {
            callback.run();
        }
    }

    public static Map<Item, AspectList> all() {
        return aspects;
    }

    public static AspectList get(Item item) {
        return aspects.getOrDefault(item, EMPTY);
    }

    private ClientItemAspects() {
    }
}
