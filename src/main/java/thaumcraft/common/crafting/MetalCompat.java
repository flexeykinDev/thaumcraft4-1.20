package thaumcraft.common.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Олово, серебро и свинец. Своих руд у Thaumcraft нет: в оригинале кластеры этих металлов, их очистка и
 * преобразование в тигле и серебряный набалдашник появлялись, только если металл давал другой мод (Config.foundTinOre,
 * foundSilverIngot...). Здесь то же решается по тегам Forge: есть ли в игре слитки (forge:ingots/tin) или руда.
 */
public final class MetalCompat {
    private MetalCompat() {
    }

    public static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, thaumcraft.common.Res.of("forge", path));
    }

    /** Есть ли в игре хоть один предмет с таким тегом (теги уже загружены). */
    public static boolean present(String path) {
        return BuiltInRegistries.ITEM.getTag(tag(path)).map(set -> set.size() > 0).orElse(false);
    }

    public static boolean present(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id)).map(set -> set.size() > 0).orElse(false);
    }

    /** Металл есть в игре: его слитки дал какой-нибудь мод. */
    public static boolean hasMetal(String metal) {
        return present("ingots/" + metal);
    }
}
