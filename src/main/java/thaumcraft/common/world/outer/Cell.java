package thaumcraft.common.world.outer;

/**
 * Клетка лабиринта Запредельных земель (Cell оригинала): одна клетка — один чанк. Выходы на четыре стороны, вверх
 * и вниз и «особенность» — что в этой клетке стоит: 1 — портал, 2–5 — четверти зала хранителя, 6 — комната ключа,
 * 7 — гнездо, 8 — библиотека, 9 и выше — проходы с украшениями.
 */
public class Cell {
    public boolean north;
    public boolean south;
    public boolean east;
    public boolean west;
    public boolean above;
    public boolean below;
    public byte feature;

    public Cell() {
    }

    public Cell(short data) {
        north = (data & 1) != 0;
        south = (data & 2) != 0;
        east = (data & 4) != 0;
        west = (data & 8) != 0;
        above = (data & 16) != 0;
        below = (data & 32) != 0;
        feature = (byte) (data >> 8);
    }

    public short pack() {
        int out = 0;
        if (north) {
            out |= 1;
        }
        if (south) {
            out |= 2;
        }
        if (east) {
            out |= 4;
        }
        if (west) {
            out |= 8;
        }
        if (above) {
            out |= 16;
        }
        if (below) {
            out |= 32;
        }
        out |= feature << 8;
        return (short) out;
    }
}
