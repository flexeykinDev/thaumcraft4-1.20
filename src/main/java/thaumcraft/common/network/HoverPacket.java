package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import thaumcraft.common.items.armor.HoverHarnessItem;
import thaumcraft.common.registry.ModSounds;

import java.util.function.Supplier;

/** Клиент -> сервер: нажата клавиша парения (H). Включить парение можно, только если в упряжи есть Potentia. */
public record HoverPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static HoverPacket decode(FriendlyByteBuf buf) {
        return new HoverPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null) {
            return;
        }
        ItemStack harness = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(harness.getItem() instanceof HoverHarnessItem)) {
            return;
        }
        boolean hover = HoverHarnessItem.isHovering(player);
        if (!hover && HoverHarnessItem.getFuel(harness) <= 0) {
            return;
        }
        HoverHarnessItem.setHovering(harness, !hover);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), !hover ? ModSounds.COMBINE.get() : ModSounds.HHOFF.get(),
                SoundSource.PLAYERS, 0.33f, 1f);
    }
}
