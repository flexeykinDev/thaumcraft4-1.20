package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.menu.ResearchTableMenu;

import java.util.function.Supplier;

/**
 * Клиент -> сервер: действие за столом исследований. Стол определяется по открытому у игрока окну.
 *
 * @param action PLACE — положить аспект first на клетку (q, r); ERASE — стереть клетку;
 *               COMBINE — соединить аспекты first и second
 */
public record ResearchTablePacket(int action, int q, int r, String first, String second) {
    public static final int PLACE = 0;
    public static final int ERASE = 1;
    public static final int COMBINE = 2;

    public static ResearchTablePacket place(int q, int r, Aspect aspect) {
        return new ResearchTablePacket(PLACE, q, r, aspect.getTag(), "");
    }

    public static ResearchTablePacket erase(int q, int r) {
        return new ResearchTablePacket(ERASE, q, r, "", "");
    }

    public static ResearchTablePacket combine(Aspect first, Aspect second) {
        return new ResearchTablePacket(COMBINE, 0, 0, first.getTag(), second.getTag());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(action);
        buf.writeByte(q);
        buf.writeByte(r);
        buf.writeUtf(first);
        buf.writeUtf(second);
    }

    public static ResearchTablePacket decode(FriendlyByteBuf buf) {
        return new ResearchTablePacket(buf.readByte(), buf.readByte(), buf.readByte(), buf.readUtf(), buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null || !(player.containerMenu instanceof ResearchTableMenu menu) || !menu.stillValid(player)) {
            return;
        }
        Aspect a = Aspect.byTag(first);
        Aspect b = Aspect.byTag(second);
        if (action == PLACE && a != null) {
            menu.getTable().placeAspect(player, q, r, a);
        } else if (action == ERASE) {
            menu.getTable().placeAspect(player, q, r, null);
        } else if (action == COMBINE && a != null && b != null) {
            menu.getTable().combineAspects(player, a, b);
        }
    }
}
