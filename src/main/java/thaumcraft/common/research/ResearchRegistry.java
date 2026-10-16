package thaumcraft.common.research;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Все категории и исследования таумономикона. Сервер читает их из датапаков и пересылает клиенту
 * одним JSON, чтобы книга у игрока совпадала с сервером.
 */
public final class ResearchRegistry {
    public record Category(String key, ResourceLocation icon, ResourceLocation background) {
        public Component getName() {
            return Component.translatable("tc.research_category." + key);
        }
    }

    private static final Gson GSON = new Gson();

    private static Map<String, Category> categories = Map.of();
    private static Map<String, ResearchEntry> entries = Map.of();
    private static String json = "{}";

    public static Collection<Category> categories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    /** Исследования этой игры: без тех, что требуют металлов, которых нет (см. {@link ResearchEntry#available()}). */
    public static Collection<ResearchEntry> entries() {
        List<ResearchEntry> out = new ArrayList<>(entries.size());
        for (ResearchEntry entry : entries.values()) {
            if (entry.requires.isEmpty() || entry.available()) {
                out.add(entry);
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Nullable
    public static ResearchEntry get(String key) {
        ResearchEntry entry = entries.get(key);
        return entry == null || entry.requires.isEmpty() || entry.available() ? entry : null;
    }

    public static List<ResearchEntry> inCategory(String category) {
        List<ResearchEntry> out = new ArrayList<>();
        for (ResearchEntry entry : entries()) {
            if (entry.category.equals(category)) {
                out.add(entry);
            }
        }
        return out;
    }

    /** Текущие данные одним JSON — для отправки клиенту. */
    public static String toJson() {
        return json;
    }

    /** Загружает данные из объединённого JSON: на сервере после чтения датапаков, на клиенте из пакета. */
    public static void load(String text) {
        JsonObject root = GSON.fromJson(text, JsonObject.class);
        Map<String, Category> newCategories = new LinkedHashMap<>();
        Map<String, ResearchEntry> newEntries = new LinkedHashMap<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(root, "categories", new JsonArray())) {
            JsonObject object = element.getAsJsonObject();
            String key = GsonHelper.getAsString(object, "key");
            newCategories.put(key, new Category(key,
                    thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "icon")),
                    thaumcraft.common.Res.parse(GsonHelper.getAsString(object, "background"))));
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(root, "entries", new JsonArray())) {
            try {
                ResearchEntry entry = new ResearchEntry(element.getAsJsonObject());
                newEntries.put(entry.key, entry);
            } catch (RuntimeException e) {
                Thaumcraft.LOGGER.error("Не удалось прочитать исследование {}", element, e);
            }
        }
        categories = newCategories;
        entries = newEntries;
        json = text;
    }

    /** Читает все файлы thaumcraft_research и склеивает их: аддоны дописывают свои категории и исследования. */
    public static class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(GSON, "thaumcraft_research");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            JsonArray allCategories = new JsonArray();
            JsonArray allEntries = new JsonArray();
            files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(file -> {
                JsonObject root = file.getValue().getAsJsonObject();
                allCategories.addAll(GsonHelper.getAsJsonArray(root, "categories", new JsonArray()));
                allEntries.addAll(GsonHelper.getAsJsonArray(root, "entries", new JsonArray()));
            });
            JsonObject merged = new JsonObject();
            merged.add("categories", allCategories);
            merged.add("entries", allEntries);
            load(GSON.toJson(merged));
            Thaumcraft.LOGGER.info("Исследования: {} категорий, {} записей", categories.size(), entries.size());
        }
    }

    private ResearchRegistry() {
    }
}
