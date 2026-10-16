package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.research.ScanManager;

import java.util.function.Supplier;

/**
 * Клиент -> сервер: сканирование в инвентаре (порт аддона Thaumcraft Inventory Scanning).
 * Игрок держит таумометр на курсоре и наводит его на предмет в слоте либо на свою фигурку.
 *
 * @param slot номер слота в открытом контейнере; {@link #SELF} — сам игрок
 */
public record ScanSlotPacket(int slot) {
    public static final int SELF = -1;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot + 1);
    }

    public static ScanSlotPacket decode(FriendlyByteBuf buf) {
        return new ScanSlotPacket(buf.readVarInt() - 1);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null || !(player.containerMenu.getCarried().getItem() instanceof ThaumometerItem)) {
            return;
        }
        if (slot == SELF) {
            ScanManager.completeScan(player, ScanManager.targetForEntity(player));
            return;
        }
        if (slot < 0 || slot >= player.containerMenu.slots.size()) {
            return;
        }
        Slot target = player.containerMenu.slots.get(slot);
        // результат крафта сканировать нельзя: предмета ещё не существует
        if (target.hasItem() && target.mayPickup(player) && !(target instanceof ResultSlot)) {
            ScanManager.completeScan(player, ScanManager.targetForItem(target.getItem(), player.position()));
        }
    }
}
