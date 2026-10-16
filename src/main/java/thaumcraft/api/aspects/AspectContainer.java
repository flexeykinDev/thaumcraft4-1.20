package thaumcraft.api.aspects;

import org.jetbrains.annotations.Nullable;

/** Блок, содержимое которого видно сквозь очки откровения (IAspectContainer оригинала в части показа). */
public interface AspectContainer {
    /** Что показать над блоком; null или пустой список — ничего. */
    @Nullable
    AspectList getContainedAspects();

    /** Подписывать ли значки количеством: у фильтра трубы, например, показан только сам аспект. */
    default boolean showAspectAmounts() {
        return true;
    }
}
