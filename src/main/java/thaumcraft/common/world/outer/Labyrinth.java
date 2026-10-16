package thaumcraft.common.world.outer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Карта лабиринтов Запредельных земель — MazeHandler, MazeThread и labyrinth.dat оригинала: какая клетка лабиринта
 * лежит в каком чанке. Лабиринт создаётся, когда в алтарь Элдрича вставляют око, и ложится в Запредельных землях
 * на те же координаты чанков, что и алтарь в обычном мире: портал — ровно над алтарём.
 */
public class Labyrinth extends SavedData {
    private static final String NAME = "thaumcraft_labyrinth";
    private final Map<Long, Short> cells = new HashMap<>();

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }

    public static Labyrinth get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Labyrinth::load, Labyrinth::new, NAME);
    }

    private static Labyrinth load(CompoundTag tag) {
        Labyrinth maze = new Labyrinth();
        ListTag list = tag.getList("cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag cell = list.getCompound(i);
            maze.cells.put(key(cell.getInt("x"), cell.getInt("z")), cell.getShort("cell"));
        }
        return maze;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        cells.forEach((key, value) -> {
            if (value > 0) {
                CompoundTag cell = new CompoundTag();
                cell.putInt("x", (int) (key >> 32));
                cell.putInt("z", (int) (long) key);
                cell.putShort("cell", value);
                list.add(cell);
            }
        });
        tag.put("cells", list);
        return tag;
    }

    @Nullable
    public synchronized Cell get(int chunkX, int chunkZ) {
        Short value = cells.get(key(chunkX, chunkZ));
        return value == null ? null : new Cell(value);
    }

    public synchronized boolean mazesInRange(int chunkX, int chunkZ, int w, int h) {
        for (int x = -w; x <= w; x++) {
            for (int z = -h; z <= h; z++) {
                if (cells.containsKey(key(chunkX + x, chunkZ + z))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** MazeThread.run оригинала: строит лабиринт w×h клеток с порталом в чанке (x, z). */
    public synchronized void generate(int x, int z, int w, int h, long seed) {
        int[][] corners = {{0, 0}, {-w, -h}, {w, h}, {-w, h}, {w, -h}};
        for (int[] c : corners) {
            cells.put(key(x + c[0], z + c[1]), (short) 0);
        }
        MazeGenerator gen = new MazeGenerator(w, h, seed++);
        while (!gen.generate()) {
            gen = new MazeGenerator(w, h, seed++);
        }
        int col = x - (1 + w / 2);
        int row = z - (1 + h / 2);
        for (int a = 0; a < w; a++) {
            for (int b = 0; b < h; b++) {
                if (gen.grid[b][a] > 0) {
                    cells.put(key(a + col, b + row), (short) gen.grid[b][a]);
                }
            }
        }
        for (int[] c : corners) {
            long k = key(x + c[0], z + c[1]);
            if (cells.getOrDefault(k, (short) 0) == 0) {
                cells.remove(k);
            }
        }
        setDirty();
    }
}
