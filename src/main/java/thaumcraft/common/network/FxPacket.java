package thaumcraft.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Сервер -> клиент: разовый эффект в мире (PacketFXBlockSparkle, PacketFXBlockZap, PacketFXInfusionSource
 * и блок-события оригинала в одном пакете).
 *
 * @param kind вид эффекта, см. константы
 * @param from откуда (или где)
 * @param to   куда; для точечных эффектов не используется
 * @param a    число, смысл зависит от вида: цвет, количество
 * @param b    второе число
 */
public record FxPacket(int kind, Vec3 from, Vec3 to, int a, int b) {
    /** Блок покрывается искрами: a — цвет (-9999 — магические лиловые), b — сколько. */
    public static final int BLOCK_SPARKLE = 0;
    /** Всплеск пузырей в тигле: a — цвет (0 — белый), b — сила; from.y — уровень жидкости. */
    public static final int CRUCIBLE_BOIL = 1;
    /** Молния из точки в точку: a — тип цвета (0 — лиловая). */
    public static final int BOLT = 2;
    /** Предмет с пьедестала (блок from) крошится и летит в точку to. */
    public static final int ITEM_STREAM = 3;
    /** Струйка эссенции цвета a из точки в точку. */
    public static final int ESSENTIA = 4;
    /** Руны по граням блока from: a — сколько. */
    public static final int RUNES = 5;
    /** Колышущийся луч цвета a из точки в точку (жезл тянет вис из узла). */
    public static final int FLOATY_BEAM = 6;
    /** Начало приступа безумия: тьма по краям экрана и стук сердца (PacketMiscEvent 0 оригинала). */
    public static final int WARP_VIGNETTE = 7;
    /** Туман безумия на a тиков (PacketMiscEvent 1 оригинала). */
    public static final int MIST = 8;

    /** Пар из трубы, в которой столкнулись два потока: a — цвет. */
    public static final int VENT = 9;
    /** Пять искр вокруг точки: a — вид искры, b — разброс в сотых долях блока. */
    public static final int SPARKLES = 10;
    /** Вспышка рунного щита вокруг существа с номером a; b — номер ударившего, -1 — со всех сторон, -2 — снизу, -3 — сверху. */
    public static final int SHIELD = 11;
    /** Заряд рунного щита игрока: a — сколько единиц, b — предел. */
    public static final int RUNIC_CHARGE = 12;
    /** Пузыри по граням блока (PacketFXBlockBubble): a — цвет. */
    public static final int BLOCK_BUBBLE = 13;
    /** Чародейский бур наметил блок: a — сдвиг цели, упакованный по байту на ось. */
    public static final int BORE_TARGET = 14;
    /** Чародейский бур разрушил блок: a — номер состояния блока. */
    public static final int BORE_DUG = 15;
    /** Вспышка (Thaumcraft.proxy.burst оригинала) в точке from: a — размер в сотых (0 — обычный). */
    public static final int BURST = 16;
    /** Вопль стража Элдрича (PacketFXSonic): a — номер существа. */
    public static final int SONIC = 17;
    /** Дуга из существа a в блок from (PacketFXBlockArc); у портала культа она красная, у прочих — тёмно-лиловая. */
    public static final int BLOCK_ARC = 18;

    /** Эффект, который видит только один игрок. */
    public static void sendTo(net.minecraft.server.level.ServerPlayer player, int kind, Vec3 from, Vec3 to, int a, int b) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FxPacket(kind, from, to, a, b));
    }

    public static void send(ServerLevel level, int kind, Vec3 from, Vec3 to, int a, int b) {
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                from.x, from.y, from.z, 32.0, level.dimension())), new FxPacket(kind, from, to, a, b));
    }

    public static void blockSparkle(ServerLevel level, BlockPos pos, int color, int count) {
        send(level, BLOCK_SPARKLE, Vec3.atLowerCornerOf(pos), Vec3.ZERO, color, count);
    }

    public static void bolt(ServerLevel level, Vec3 from, Vec3 to) {
        send(level, BOLT, from, to, 0, 0);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(kind);
        buf.writeDouble(from.x).writeDouble(from.y).writeDouble(from.z);
        buf.writeDouble(to.x).writeDouble(to.y).writeDouble(to.z);
        buf.writeInt(a);
        buf.writeInt(b);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readByte(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt(), buf.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        thaumcraft.client.fx.FxHandler.handle(this);
    }
}
