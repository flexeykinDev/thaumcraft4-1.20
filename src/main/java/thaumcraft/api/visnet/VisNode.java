package thaumcraft.api.visnet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

/**
 * Звено сети вис (TileVisNode оригинала). Источник — заряженный узел ауры; реле цепляются к источнику или друг
 * к другу и передают вис дальше. Приборы тянут вис из ближайшего звена через {@link VisNetHandler#drainVis}.
 * Количество вис в сети считается в сотых долях.
 */
public interface VisNode {
    /** Связи этого звена с соседями; хранит и обновляет их {@link VisNodeLink}. */
    VisNodeLink visLink();

    BlockPos visPos();

    @Nullable
    Level visLevel();

    /** Звено разрушено или выгружено. */
    boolean isVisNodeRemoved();

    /** На каком расстоянии звено достаёт до соседей и приборов. */
    int getVisRange();

    boolean isVisSource();

    /** Забирает вис: источник отдаёт из своего запаса на этот тик, реле просит у родителя. Возвращает, сколько дали. */
    int consumeVis(Aspect aspect, int amount);

    /** Настройка реле на один аспект: звенья с разной настройкой не соединяются. -1 — без настройки. */
    default byte getAttunement() {
        return -1;
    }

    default void parentChanged() {
    }

    default void triggerConsumeEffect(Aspect aspect) {
    }
}
