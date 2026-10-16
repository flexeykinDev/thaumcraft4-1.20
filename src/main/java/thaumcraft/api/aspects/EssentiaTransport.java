package thaumcraft.api.aspects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * То, что умеет принимать и отдавать эссенцию по трубам — перенос IEssentiaTransport оригинала.
 * Эссенция течёт туда, где сильнее всасывание: банки тянут с силой 32 (с ярлыком — 64), трубы передают
 * всасывание дальше, теряя по единице на блок.
 */
public interface EssentiaTransport {
    /** Можно ли подсоединить трубу с этой стороны. */
    boolean isConnectable(Direction side);

    boolean canInputFrom(Direction side);

    boolean canOutputTo(Direction side);

    void setSuction(@Nullable Aspect aspect, int amount);

    /** Какой аспект здесь всасывается; null — любой. */
    @Nullable
    Aspect getSuctionType(@Nullable Direction side);

    int getSuctionAmount(@Nullable Direction side);

    /** @return сколько удалось забрать */
    int takeEssentia(Aspect aspect, int amount, Direction side);

    /** @return сколько удалось принять */
    int addEssentia(Aspect aspect, int amount, Direction side);

    @Nullable
    Aspect getEssentiaType(@Nullable Direction side);

    int getEssentiaAmount(@Nullable Direction side);

    /** С какой силой нужно тянуть, чтобы забрать эссенцию отсюда. */
    int getMinimumSuction();

    /** ThaumcraftApiHelper.getConnectableTile оригинала: сосед с этой стороны, если к нему можно подсоединиться. */
    @Nullable
    static EssentiaTransport neighbour(Level level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos.relative(side)) instanceof EssentiaTransport transport
                && transport.isConnectable(side.getOpposite()) ? transport : null;
    }
}
