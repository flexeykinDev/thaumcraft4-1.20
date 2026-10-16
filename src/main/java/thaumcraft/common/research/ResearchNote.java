package thaumcraft.common.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Данные заметки об исследовании: поле из шестиугольников, по краю которого расставлены аспекты исследования.
 * Игрок выкладывает на свободные клетки свои аспекты так, чтобы все краевые оказались связаны цепочками
 * родственных аспектов. Перенос ResearchNoteData, HexUtils и части ResearchManager оригинала.
 */
public final class ResearchNote {
    /** Клетка свободна. */
    public static final int EMPTY = 0;
    /** Аспект самого исследования: его нельзя убрать, и все такие клетки нужно связать. */
    public static final int FIXED = 1;
    /** Аспект, выложенный игроком. */
    public static final int PLACED = 2;

    private static final int[][] NEIGHBOURS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

    /** Клетка в осевых координатах шестиугольной сетки. */
    public record Hex(int q, int r) {
        public Hex neighbour(int direction) {
            return new Hex(q + NEIGHBOURS[direction][0], r + NEIGHBOURS[direction][1]);
        }

        /** Центр клетки в пикселях при радиусе клетки size. */
        public double pixelX(int size) {
            return size * 1.5 * q;
        }

        public double pixelY(int size) {
            return size * Math.sqrt(3.0) * (r + q / 2.0);
        }

        public static Hex fromPixel(double x, double y, int size) {
            double qq = 2.0 / 3.0 * x / size;
            double rr = (1.0 / 3.0 * Math.sqrt(3.0) * y - 1.0 / 3.0 * x) / size;
            double zz = -qq - rr;
            int rx = (int) Math.round(qq);
            int ry = (int) Math.round(rr);
            int rz = (int) Math.round(zz);
            double dx = Math.abs(rx - qq);
            double dy = Math.abs(ry - rr);
            double dz = Math.abs(rz - zz);
            if (dx > dy && dx > dz) {
                rx = -ry - rz;
            } else if (dy > dz) {
                ry = -rx - rz;
            }
            return new Hex(rx, ry);
        }
    }

    public record Entry(@Nullable Aspect aspect, int type) {
    }

    public String key = "";
    public int color = 0xffffff;
    public boolean complete;
    public final Map<Hex, Entry> hexes = new LinkedHashMap<>();

    // ---------- создание ----------

    /** Новая заметка: поле радиусом по сложности исследования, аспекты исследования равномерно по внешнему кольцу. */
    @Nullable
    public static ResearchNote create(ResearchEntry research, RandomSource random) {
        List<Aspect> aspects = research.aspects.getAspects();
        if (aspects.isEmpty()) {
            return null;
        }
        ResearchNote note = new ResearchNote();
        note.key = research.key;
        Aspect primary = research.aspects.getAspectsSortedByAmount().get(0);
        note.color = primary.getColor();

        int radius = 1 + Math.min(3, research.complexity);
        for (Hex hex : disc(radius)) {
            note.hexes.put(hex, new Entry(null, EMPTY));
        }
        List<Hex> ring = ring(radius);
        float spacing = (float) ring.size() / aspects.size();
        float position = 0f;
        for (Aspect aspect : aspects) {
            note.hexes.put(ring.get(Math.round(position) % ring.size()), new Entry(aspect, FIXED));
            position += spacing;
        }

        // у сложных исследований часть клеток вырезана, но так, чтобы ни один аспект не оказался отрезан
        if (research.complexity > 1) {
            int blanks = research.complexity * 2;
            int attempts = 0;
            while (blanks > 0 && attempts++ < 1000) {
                List<Hex> all = new ArrayList<>(note.hexes.keySet());
                Hex candidate = all.get(random.nextInt(all.size()));
                if (note.hexes.get(candidate).type() != EMPTY || !canRemove(note, candidate)) {
                    continue;
                }
                note.hexes.remove(candidate);
                blanks--;
            }
        }
        return note;
    }

    private static boolean canRemove(ResearchNote note, Hex candidate) {
        for (int n = 0; n < 6; n++) {
            Hex neighbour = candidate.neighbour(n);
            Entry entry = note.hexes.get(neighbour);
            if (entry != null && entry.type() == FIXED) {
                int free = 0;
                for (int q = 0; q < 6; q++) {
                    if (note.hexes.containsKey(neighbour.neighbour(q))) {
                        free++;
                    }
                }
                if (free < 2) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Hex> ring(int radius) {
        Hex hex = new Hex(0, 0);
        for (int k = 0; k < radius; k++) {
            hex = hex.neighbour(4);
        }
        List<Hex> ring = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < radius; j++) {
                ring.add(hex);
                hex = hex.neighbour(i);
            }
        }
        return ring;
    }

    private static List<Hex> disc(int radius) {
        List<Hex> out = new ArrayList<>();
        out.add(new Hex(0, 0));
        for (int k = 1; k <= radius; k++) {
            out.addAll(ring(k));
        }
        return out;
    }

    // ---------- связи ----------

    /** Аспекты родственны, если один из них — прямой компонент другого. */
    public static boolean related(Aspect a, Aspect b) {
        return !a.isPrimal() && (a.getFirstComponent() == b || a.getSecondComponent() == b)
                || !b.isPrimal() && (b.getFirstComponent() == a || b.getSecondComponent() == a);
    }

    /** Клетки, до которых от start можно дойти по родственным аспектам, известным игроку. */
    public Set<Hex> connected(Hex start, PlayerKnowledge knowledge) {
        Set<Hex> visited = new HashSet<>();
        walk(start, knowledge, visited);
        return visited;
    }

    private void walk(Hex hex, PlayerKnowledge knowledge, Set<Hex> visited) {
        visited.add(hex);
        Entry from = hexes.get(hex);
        if (from == null || from.aspect() == null) {
            return;
        }
        for (int n = 0; n < 6; n++) {
            Hex target = hex.neighbour(n);
            Entry to = hexes.get(target);
            if (visited.contains(target) || to == null || to.type() < FIXED || to.aspect() == null) {
                continue;
            }
            if (knowledge.hasDiscovered(from.aspect()) && knowledge.hasDiscovered(to.aspect())
                    && related(from.aspect(), to.aspect())) {
                walk(target, knowledge, visited);
            }
        }
    }

    /**
     * Проверяет, связаны ли все аспекты исследования. Если да, заметка становится открытием: лишние клетки
     * убираются, остаётся только найденная сеть.
     */
    public boolean checkCompletion(PlayerKnowledge knowledge) {
        Hex first = null;
        List<Hex> fixed = new ArrayList<>();
        for (Map.Entry<Hex, Entry> e : hexes.entrySet()) {
            if (e.getValue().type() == FIXED) {
                fixed.add(e.getKey());
                if (first == null) {
                    first = e.getKey();
                }
            }
        }
        if (first == null) {
            return false;
        }
        Set<Hex> reached = connected(first, knowledge);
        if (!reached.containsAll(fixed)) {
            return false;
        }
        hexes.keySet().removeIf(hex -> hexes.get(hex).type() != FIXED && !reached.contains(hex));
        complete = true;
        return true;
    }

    // ---------- хранение в предмете ----------

    @Nullable
    public static ResearchNote read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("key")) {
            return null;
        }
        ResearchNote note = new ResearchNote();
        note.key = tag.getString("key");
        note.color = tag.getInt("color");
        note.complete = tag.getBoolean("complete");
        ListTag grid = tag.getList("hexgrid", Tag.TAG_COMPOUND);
        for (int i = 0; i < grid.size(); i++) {
            CompoundTag cell = grid.getCompound(i);
            Aspect aspect = cell.contains("aspect") ? Aspect.byTag(cell.getString("aspect")) : null;
            note.hexes.put(new Hex(cell.getByte("hexq"), cell.getByte("hexr")), new Entry(aspect, cell.getByte("type")));
        }
        return note;
    }

    public void write(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("key", key);
        tag.putInt("color", color);
        tag.putBoolean("complete", complete);
        ListTag grid = new ListTag();
        hexes.forEach((hex, entry) -> {
            CompoundTag cell = new CompoundTag();
            cell.putByte("hexq", (byte) hex.q());
            cell.putByte("hexr", (byte) hex.r());
            cell.putByte("type", (byte) entry.type());
            if (entry.aspect() != null) {
                cell.putString("aspect", entry.aspect().getTag());
            }
            grid.add(cell);
        });
        tag.put("hexgrid", grid);
    }
}
