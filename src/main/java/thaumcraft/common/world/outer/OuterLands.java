package thaumcraft.common.world.outer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;

/** Запредельные земли (dimensionOuterId оригинала): пустота, в которой висят лабиринты Элдрича. */
public final class OuterLands {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Thaumcraft.id("outer_lands"));
    /** Высота пола лабиринта (y = 50 в оригинале). */
    public static final int FLOOR = 50;

    private OuterLands() {
    }

    public static boolean is(Level level) {
        return level.dimension() == LEVEL;
    }
}
