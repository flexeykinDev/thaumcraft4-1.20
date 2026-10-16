package thaumcraft.common.entity.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Отметка колокольчика (Marker оригинала): блок, его сторона и цвет, с которыми голему велено работать.
 * Цвет -1 — «любой».
 */
public record GolemMarker(BlockPos pos, String dimension, byte side, byte color) {

    /** Та же отметка, если не считать цвета: отметка без цвета подходит к любому. */
    public boolean equalsFuzzy(GolemMarker other) {
        return pos.equals(other.pos) && dimension.equals(other.dimension) && side == other.side && (color == other.color || color == -1);
    }

    public static ListTag save(List<GolemMarker> markers) {
        ListTag list = new ListTag();
        for (GolemMarker marker : markers) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", marker.pos.getX());
            tag.putInt("y", marker.pos.getY());
            tag.putInt("z", marker.pos.getZ());
            tag.putString("dim", marker.dimension);
            tag.putByte("side", marker.side);
            tag.putByte("color", marker.color);
            list.add(tag);
        }
        return list;
    }

    public static List<GolemMarker> load(ListTag list) {
        List<GolemMarker> markers = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            markers.add(new GolemMarker(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")), tag.getString("dim"),
                    tag.getByte("side"), tag.getByte("color")));
        }
        return markers;
    }

    public static List<GolemMarker> load(CompoundTag holder, String key) {
        return load(holder.getList(key, Tag.TAG_COMPOUND));
    }
}
