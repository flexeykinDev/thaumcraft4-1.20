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
        CHANNEL.registerMessage(1, ArmorStatus.class, ArmorStatus::write, ArmorStatus::read, ArmorStatus::handle);
        CHANNEL.registerMessage(2, WarpTeleport.class, WarpTeleport::write, WarpTeleport::read, WarpTeleport::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, Beam.class, Beam::write, Beam::read, Beam::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendWarpTeleport(net.minecraft.core.BlockPos gate, int index) {
        CHANNEL.sendToServer(new WarpTeleport(gate, index));
    }

    /** PacketWarpGateTeleport: игрок на вратах выбрал направление. */
    public record WarpTeleport(net.minecraft.core.BlockPos gate, int index) {
        void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(gate);
            buf.writeVarInt(index);
        }

        static WarpTeleport read(FriendlyByteBuf buf) {
            return new WarpTeleport(buf.readBlockPos(), buf.readVarInt());
        }

        void handle(Supplier<NetworkEvent.Context> context) {
            ServerPlayer sender = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (sender != null && sender.level().getBlockEntity(gate) instanceof thaumictinkerer.common.block.WarpGateBlock.Tile tile) {
                    tile.teleport(sender, index);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    /** Путь теневого луча — точки, где рассыпать тёмные искры. */
    public static void sendBeam(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 near, java.util.List<net.minecraft.world.phys.Vec3> points) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(near.x, near.y, near.z, 64, level.dimension())),
                new Beam(points));
    }

    public record Beam(java.util.List<net.minecraft.world.phys.Vec3> points) {
        void write(FriendlyByteBuf buf) {
            buf.writeVarInt(points.size());
            for (net.minecraft.world.phys.Vec3 point : points) {
                buf.writeFloat((float) point.x);
                buf.writeFloat((float) point.y);
                buf.writeFloat((float) point.z);
            }
        }

        static Beam read(FriendlyByteBuf buf) {
            int size = Math.min(buf.readVarInt(), 512);
            java.util.List<net.minecraft.world.phys.Vec3> points = new java.util.ArrayList<>();
            for (int i = 0; i < size; i++) {
                points.add(new net.minecraft.world.phys.Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
            }
            return new Beam(points);
        }

        void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> thaumictinkerer.client.TTClientHooks.beam(points)));
            context.get().setPacketHandled(true);
        }
    }

    /** Силы пробуждённой брони включены или выключены (PacketToggleArmor): клиент сообщает клавишу, сервер — сохранённое. */
    public static void sendArmorStatus(ServerPlayer player, boolean enabled) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ArmorStatus(enabled));
    }

    public static void sendArmorStatusToServer(boolean enabled) {
        CHANNEL.sendToServer(new ArmorStatus(enabled));
    }

    public record ArmorStatus(boolean enabled) {
        void write(FriendlyByteBuf buf) {
            buf.writeBoolean(enabled);
        }

        static ArmorStatus read(FriendlyByteBuf buf) {
            return new ArmorStatus(buf.readBoolean());
        }

        void handle(Supplier<NetworkEvent.Context> context) {
            ServerPlayer sender = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (sender != null) {
                    thaumictinkerer.common.item.kami.IchorArmor.setEnabled(sender, enabled);
                } else {
                    thaumictinkerer.common.item.kami.IchorArmor.clientEnabled = enabled;
                }
            });
            context.get().setPacketHandled(true);
        }
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
