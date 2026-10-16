package thaumcraft.common.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * Одно исследование таумономикона (ResearchItem оригинала). Данные читаются из датапаков:
 * data/&lt;namespace&gt;/thaumcraft_research/*.json.
 */
public final class ResearchEntry {
    /**
     * Страница исследования.
     *
     * @param type     text, text_concealed, crafting, arcane, crucible, infusion, ...
     * @param text     ключ перевода текста (для текстовых страниц)
     * @param research для text_concealed: страница видна только после этого исследования
     * @param recipe   id рецепта; null, если рецепт ещё не перенесён
     */
    public record Page(String type, @Nullable String text, @Nullable String research, @Nullable ResourceLocation recipe,
                       List<ResourceLocation> recipes, @Nullable ResourceLocation item, @Nullable Construct construct) {
        public boolean isText() {
            return type.equals("text") || type.equals("text_concealed");
        }
    }

    /**
     * Составная конструкция для страницы-чертежа: что и как поставить в мире, прежде чем коснуться жезлом.
     *
     * @param aspects цена в вис
     * @param blocks  блоки послойно сверху вниз, в слое — от дальнего ряда к ближнему; null — пустое место
     */
    public record Construct(AspectList aspects, int dx, int dy, int dz, List<Optional<ResourceLocation>> blocks) {
    }

    public final String key;
    public final String category;
    /** Цена покупки в очках аспектов (для вторичных исследований) и состав заметки (для первичных). */
    public final AspectList aspects;
    public final int column;
    public final int row;
    public final int complexity;
    @Nullable
    public final ResourceLocation iconItem;
    /** Метки предмета-значка (скипетр, продвинутый голем): SNBT или null. */
    public final String iconNbt;
    @Nullable
    public final ResourceLocation iconTexture;
    public final List<String> parents;
    public final List<String> parentsHidden;
    public final List<String> siblings;
    public final List<Page> pages;
    /** Скрытое исследование открывается подсказкой, когда игрок сканирует такой предмет или такой аспект. */
    public final List<ResourceLocation> itemTriggers;
    public final List<Aspect> aspectTriggers;
    /** Существа, сканирование которых открывает это скрытое исследование. */
    public final List<ResourceLocation> entityTriggers;
    /** Служебное исследование без значка на карте. */
    public final boolean virtual;
    public final boolean special;
    public final boolean stub;
    /** Не показывается, пока игрок не найдёт его в мире. */
    public final boolean lost;
    /** Не показывается, пока не открыты все предпосылки. */
    public final boolean concealed;
    /** Не показывается, пока игрок не получит подсказку. */
    public final boolean hidden;
    public final boolean autoUnlock;
    public final boolean round;
    /** Покупается за очки аспектов прямо в книге, без заметки и стола исследований. */
    public final boolean secondary;
    public final int warp;
    /**
     * Теги предметов, без которых исследования нет в игре: олово, серебро и свинец в оригинале появлялись, только если
     * их давал другой мод (например, forge:ingots/tin).
     */
    public final List<String> requires;

    public ResearchEntry(JsonObject json) {
        key = GsonHelper.getAsString(json, "key");
        category = GsonHelper.getAsString(json, "category");
        aspects = new AspectList();
        if (json.has("aspects")) {
            for (Map.Entry<String, JsonElement> e : GsonHelper.getAsJsonObject(json, "aspects").entrySet()) {
                Aspect aspect = Aspect.byTag(e.getKey());
                if (aspect != null) {
                    aspects.add(aspect, e.getValue().getAsInt());
                }
            }
        }
        column = GsonHelper.getAsInt(json, "column", 0);
        row = GsonHelper.getAsInt(json, "row", 0);
        complexity = GsonHelper.getAsInt(json, "complexity", 1);
        JsonObject icon = json.has("icon") ? GsonHelper.getAsJsonObject(json, "icon") : new JsonObject();
        iconItem = icon.has("item") ? thaumcraft.common.Res.parse(GsonHelper.getAsString(icon, "item")) : null;
        iconNbt = icon.has("nbt") ? GsonHelper.getAsString(icon, "nbt") : null;
        iconTexture = icon.has("texture") ? thaumcraft.common.Res.parse(GsonHelper.getAsString(icon, "texture")) : null;
        parents = strings(json, "parents");
        parentsHidden = strings(json, "parents_hidden");
        siblings = strings(json, "siblings");
        virtual = GsonHelper.getAsBoolean(json, "virtual", false);
        special = GsonHelper.getAsBoolean(json, "special", false);
        stub = GsonHelper.getAsBoolean(json, "stub", false);
        lost = GsonHelper.getAsBoolean(json, "lost", false);
        concealed = GsonHelper.getAsBoolean(json, "concealed", false);
        hidden = GsonHelper.getAsBoolean(json, "hidden", false);
        autoUnlock = GsonHelper.getAsBoolean(json, "autoUnlock", false);
        round = GsonHelper.getAsBoolean(json, "round", false);
        secondary = GsonHelper.getAsBoolean(json, "secondary", false);
        warp = GsonHelper.getAsInt(json, "warp", 0);
        requires = strings(json, "requires");

        List<ResourceLocation> items = new ArrayList<>();
        if (json.has("item_triggers")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "item_triggers")) {
                JsonObject trigger = element.getAsJsonObject();
                if (trigger.has("item")) {
                    items.add(thaumcraft.common.Res.parse(GsonHelper.getAsString(trigger, "item")));
                }
            }
        }
        itemTriggers = List.copyOf(items);
        List<Aspect> triggerAspects = new ArrayList<>();
        for (String tag : strings(json, "aspect_triggers")) {
            Aspect aspect = Aspect.byTag(tag);
            if (aspect != null) {
                triggerAspects.add(aspect);
            }
        }
        aspectTriggers = List.copyOf(triggerAspects);
        List<ResourceLocation> entities = new ArrayList<>();
        for (String id : strings(json, "entity_triggers")) {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed != null) {
                entities.add(parsed);
            }
        }
        entityTriggers = List.copyOf(entities);

        List<Page> list = new ArrayList<>();
        if (json.has("pages")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, "pages")) {
                JsonObject page = element.getAsJsonObject();
                list.add(new Page(GsonHelper.getAsString(page, "type"),
                        page.has("text") ? GsonHelper.getAsString(page, "text") : null,
                        page.has("research") ? GsonHelper.getAsString(page, "research") : null,
                        page.has("recipe") ? thaumcraft.common.Res.parse(GsonHelper.getAsString(page, "recipe")) : null,
                        resources(page, "recipes"),
                        page.has("item") ? ResourceLocation.tryParse(GsonHelper.getAsString(page, "item")) : null,
                        construct(page)));
            }
        }
        pages = List.copyOf(list);
    }

    @Nullable
    private static Construct construct(JsonObject page) {
        if (!page.has("blocks") || !page.has("size")) {
            return null;
        }
        AspectList cost = new AspectList();
        if (page.has("aspects")) {
            for (var entry : GsonHelper.getAsJsonObject(page, "aspects").entrySet()) {
                Aspect aspect = Aspect.byTag(entry.getKey());
                if (aspect != null) {
                    cost.add(aspect, entry.getValue().getAsInt());
                }
            }
        }
        JsonArray size = GsonHelper.getAsJsonArray(page, "size");
        List<Optional<ResourceLocation>> blocks = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(page, "blocks")) {
            blocks.add(element.isJsonNull() ? Optional.empty() : Optional.ofNullable(ResourceLocation.tryParse(element.getAsString())));
        }
        return new Construct(cost, size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(), List.copyOf(blocks));
    }

    private static List<ResourceLocation> resources(JsonObject json, String name) {
        List<ResourceLocation> out = new ArrayList<>();
        if (json.has(name)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(json, name)) {
                out.add(thaumcraft.common.Res.parse(element.getAsString()));
            }
        }
        return List.copyOf(out);
    }

    private static List<String> strings(JsonObject json, String name) {
        List<String> out = new ArrayList<>();
        if (json.has(name)) {
            JsonArray array = GsonHelper.getAsJsonArray(json, name);
            for (JsonElement element : array) {
                out.add(element.getAsString());
            }
        }
        return List.copyOf(out);
    }

    public Component getName() {
        return Component.translatable("tc.research_name." + key);
    }

    public Component getText() {
        return Component.translatable("tc.research_text." + key);
    }

    /** Можно ли купить исследование за очки аспектов прямо в книге. */
    public boolean isPurchasable() {
        return secondary && !aspects.isEmpty();
    }

    /** Есть ли исследование в этой игре: все нужные теги кем-то заполнены (теги к этому времени уже загружены). */
    public boolean available() {
        for (String tag : requires) {
            ResourceLocation id = ResourceLocation.tryParse(tag);
            if (id == null || !thaumcraft.common.crafting.MetalCompat.present(id)) {
                return false;
            }
        }
        return true;
    }
}
