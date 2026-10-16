package thaumcraft.common.world.outer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Генератор лабиринта — перенос MazeGenerator оригинала (растущее дерево проходов). В середине — портал, в одном
 * из углов — зал хранителя 2×2, в тупиках — комната ключа, гнёзда и библиотеки, по проходам — украшения.
 */
public class MazeGenerator {
    public static final int N = 1;
    public static final int S = 2;
    public static final int E = 4;
    public static final int W = 8;
    private final int width;
    private final int height;
    private final Random rand;
    public final int[][] grid;

    public MazeGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.rand = new Random(seed);
        this.grid = new int[height][width];
    }

    static int opposite(int dir) {
        return switch (dir) {
            case N -> S;
            case S -> N;
            case E -> W;
            case W -> E;
            default -> -99;
        };
    }

    static int dx(int dir) {
        return switch (dir) {
            case E -> 1;
            case W -> -1;
            case N, S -> 0;
            default -> -99;
        };
    }

    static int dy(int dir) {
        return switch (dir) {
            case N -> -1;
            case S -> 1;
            case E, W -> 0;
            default -> -99;
        };
    }

    private boolean inside(int x, int y) {
        return 0 < x && x < width - 1 && 0 < y && y < height - 1;
    }

    private int nextIndex(int ceil) {
        float r = rand.nextFloat();
        if (r <= 0.45f) {
            return ceil - 1;
        }
        return r <= 0.9f ? rand.nextInt(ceil) : 0;
    }

    private record Loc(int x, int y) {
    }

    public boolean generate() {
        int bx = 0;
        int by = 0;
        switch (rand.nextInt(4)) {
            case 1 -> {
                bx = width - 2;
                by = height - 2;
            }
            case 2 -> bx = width - 2;
            case 3 -> by = height - 2;
            default -> {
            }
        }
        grid[by][bx] = 512;
        grid[by][bx + 1] = 768;
        grid[by + 1][bx] = 1024;
        grid[by + 1][bx + 1] = 1280;
        int px = 1 + width / 2;
        int py = 1 + height / 2;
        grid[py][px] = 256;
        List<Loc> cells = new ArrayList<>();
        int l = (width + height) / 4;
        for (int z = 0; z < l; z++) {
            int w = 1 + rand.nextInt(3);
            if (w > 2) {
                l--;
            }
            int qq = rand.nextInt(width - w);
            int ww = rand.nextInt(height - w);
            for (int a = qq; a < qq + w; a++) {
                for (int b = ww; b < ww + w; b++) {
                    if (grid[b][a] == 0) {
                        grid[b][a] = -1;
                    }
                }
            }
        }
        List<Integer> directions = Arrays.asList(N, S, E, W);
        Collections.shuffle(directions, rand);
        int xx = px + dx(directions.get(0));
        int yy = py + dy(directions.get(0));
        grid[py][px] |= directions.get(0);
        if (grid[yy][xx] < 0) {
            grid[yy][xx] = 0;
        }
        grid[yy][xx] |= opposite(directions.get(0));
        cells.add(new Loc(xx, yy));
        boolean success = false;
        while (!cells.isEmpty()) {
            int index = nextIndex(cells.size());
            int x = cells.get(index).x;
            int y = cells.get(index).y;
            Collections.shuffle(directions, rand);
            boolean carved = false;
            for (int dir : directions) {
                int nx = x + dx(dir);
                int ny = y + dy(dir);
                if (inside(nx, ny)) {
                    if (grid[ny][nx] == 0) {
                        grid[y][x] |= dir;
                        grid[ny][nx] |= opposite(dir);
                        cells.add(new Loc(nx, ny));
                        carved = true;
                    }
                    if (carved) {
                        success = true;
                        break;
                    }
                }
            }
            if (!carved) {
                cells.remove(index);
            }
        }
        if (!success) {
            return false;
        }
        for (int aa = 0; aa < height; aa++) {
            for (int bb = 0; bb < width; bb++) {
                if (grid[aa][bb] < 0) {
                    grid[aa][bb] = 0;
                }
            }
        }
        Collections.shuffle(directions, rand);
        for (int dir : directions) {
            int nx = px + dx(dir);
            int ny = py + dy(dir);
            if (inside(nx, ny) && grid[ny][nx] > 0 && rand.nextBoolean()) {
                grid[ny][nx] |= opposite(dir);
                grid[py][px] |= dir;
            }
        }
        Collections.shuffle(directions, rand);
        boolean connected = false;
        outer:
        for (int ax = 0; ax < 2; ax++) {
            for (int ay = 0; ay < 2; ay++) {
                for (int dir : directions) {
                    int nx = bx + ax + dx(dir);
                    int ny = by + ay + dy(dir);
                    if (inside(nx, ny) && grid[ny][nx] > 0 && new Cell((short) grid[ny][nx]).feature == 0) {
                        grid[ny][nx] |= opposite(dir);
                        grid[by + ay][bx + ax] |= dir;
                        connected = true;
                        break outer;
                    }
                }
            }
        }
        if (!connected) {
            List<Integer> directions2 = Arrays.asList(N, S, E, W);
            Collections.shuffle(directions2, rand);
            success = false;
            search:
            for (int ax = 0; ax < 2; ax++) {
                for (int ay = 0; ay < 2; ay++) {
                    for (int dir2 : directions2) {
                        int qx = bx + ax + dx(dir2);
                        int qy = by + ay + dy(dir2);
                        if (inside(qx, qy) && grid[qy][qx] == 0) {
                            cells.add(new Loc(qx, qy));
                            while (!cells.isEmpty()) {
                                int index = nextIndex(cells.size());
                                int x = cells.get(index).x;
                                int y = cells.get(index).y;
                                Collections.shuffle(directions, rand);
                                boolean carved = false;
                                for (int dir : directions) {
                                    int nx = x + dx(dir);
                                    int ny = y + dy(dir);
                                    if (inside(nx, ny)) {
                                        if (grid[ny][nx] == 0) {
                                            grid[y][x] |= dir | 25344;
                                            grid[ny][nx] |= opposite(dir) | 25344;
                                            cells.add(new Loc(nx, ny));
                                            carved = true;
                                        } else if (new Cell((short) grid[ny][nx]).feature == 0) {
                                            grid[y][x] |= dir;
                                            grid[ny][nx] |= opposite(dir);
                                            grid[qy][qx] |= opposite(dir2);
                                            grid[by + ay][bx + ax] |= dir2;
                                            success = true;
                                            break search;
                                        }
                                        if (carved) {
                                            break;
                                        }
                                    }
                                }
                                if (!carved) {
                                    cells.remove(index);
                                }
                            }
                        }
                    }
                }
            }
            if (!success) {
                return false;
            }
        }
        for (int aa = 0; aa < height; aa++) {
            for (int bb = 0; bb < width; bb++) {
                Cell c = new Cell((short) grid[aa][bb]);
                if (c.feature == 99) {
                    c.feature = 0;
                    grid[aa][bb] = c.pack();
                }
            }
        }
        List<int[]> deadEnds = new ArrayList<>();
        for (int aa = 0; aa < height; aa++) {
            for (int bb = 0; bb < width; bb++) {
                Cell c = new Cell((short) grid[aa][bb]);
                int exits = (c.north ? 1 : 0) + (c.south ? 1 : 0) + (c.east ? 1 : 0) + (c.west ? 1 : 0);
                if (exits == 1 && c.feature == 0) {
                    deadEnds.add(new int[]{aa, bb});
                }
            }
        }
        if (deadEnds.isEmpty()) {
            return false;
        }
        int r = rand.nextInt(deadEnds.size());
        int[] key = deadEnds.remove(r);
        Cell keyCell = new Cell((short) grid[key[0]][key[1]]);
        keyCell.feature = 6;
        grid[key[0]][key[1]] = keyCell.pack();
        if (!deadEnds.isEmpty()) {
            int placed = 0;
            while (placed < deadEnds.size() / 2) {
                int pick = rand.nextInt(deadEnds.size());
                int[] loc = deadEnds.get(pick);
                Cell c = new Cell((short) grid[loc[0]][loc[1]]);
                if (c.feature == 0) {
                    c.feature = (byte) (7 + rand.nextInt(3));
                    grid[loc[0]][loc[1]] = c.pack();
                    deadEnds.remove(pick);
                    placed++;
                }
            }
        }
        for (int aa = 0; aa < height; aa++) {
            for (int bb = 0; bb < width; bb++) {
                Cell c = new Cell((short) grid[aa][bb]);
                if (c.feature == 0 && (c.north || c.south || c.west || c.east) && rand.nextInt(25) == 0) {
                    c.feature = switch (rand.nextInt(8)) {
                        case 0 -> 8;
                        case 1 -> 10;
                        case 2, 3 -> 11;
                        case 4, 5 -> 12;
                        case 6 -> 13;
                        default -> 14;
                    };
                    grid[aa][bb] = c.pack();
                }
            }
        }
        return true;
    }
}
