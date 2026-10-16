package thaumcraft.common;

import net.minecraft.resources.ResourceLocation;

/**
 * Создание ResourceLocation через конструкторы: ResourceLocation.parse и fromNamespaceAndPath появились
 * только в поздних Forge 47.x, а в NeoForge 1.20.1 (ответвление от 47.1) их нет — так мод собирается под оба.
 */
@SuppressWarnings("removal")
public final class Res {
    private Res() {
    }

    public static ResourceLocation parse(String id) {
        return new ResourceLocation(id);
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
