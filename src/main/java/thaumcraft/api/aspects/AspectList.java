package thaumcraft.api.aspects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Набор аспектов с количествами. Используется для предметов, узлов, рецептов, банок и т.д. */
public class AspectList {
    private final Map<Aspect, Integer> aspects = new LinkedHashMap<>();

    public AspectList add(Aspect aspect, int amount) {
        if (amount > 0) {
            aspects.merge(aspect, amount, Integer::sum);
        }
        return this;
    }

    public AspectList add(AspectList other) {
        other.aspects.forEach(this::add);
        return this;
    }

    /** Берёт максимум по каждому аспекту вместо суммы. */
    public AspectList merge(Aspect aspect, int amount) {
        if (amount > 0) {
            aspects.merge(aspect, amount, Math::max);
        }
        return this;
    }

    /** Убирает количество; аспект исчезает, когда его не остаётся. Возвращает false, если не хватило. */
    public boolean reduce(Aspect aspect, int amount) {
        int current = getAmount(aspect);
        if (current < amount) {
            return false;
        }
        if (current == amount) {
            aspects.remove(aspect);
        } else {
            aspects.put(aspect, current - amount);
        }
        return true;
    }

    public AspectList remove(Aspect aspect) {
        aspects.remove(aspect);
        return this;
    }

    public int getAmount(Aspect aspect) {
        return aspects.getOrDefault(aspect, 0);
    }

    public boolean contains(Aspect aspect) {
        return aspects.containsKey(aspect);
    }

    public int size() {
        return aspects.size();
    }

    public boolean isEmpty() {
        return aspects.isEmpty();
    }

    /** Суммарное количество всех аспектов. */
    public int visSize() {
        int total = 0;
        for (int amount : aspects.values()) {
            total += amount;
        }
        return total;
    }

    public List<Aspect> getAspects() {
        return new ArrayList<>(aspects.keySet());
    }

    public List<Aspect> getAspectsSortedByAmount() {
        List<Aspect> out = getAspects();
        out.sort(Comparator.comparingInt(this::getAmount).reversed());
        return out;
    }

    public List<Aspect> getAspectsSortedByName() {
        List<Aspect> out = getAspects();
        out.sort(Comparator.comparing(Aspect::getTag));
        return out;
    }

    public AspectList copy() {
        return new AspectList().add(this);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(aspects.size());
        aspects.forEach((aspect, amount) -> {
            buf.writeUtf(aspect.getTag());
            buf.writeVarInt(amount);
        });
    }

    public static AspectList read(FriendlyByteBuf buf) {
        AspectList out = new AspectList();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++) {
            Aspect aspect = Aspect.byTag(buf.readUtf());
            int amount = buf.readVarInt();
            if (aspect != null) {
                out.add(aspect, amount);
            }
        }
        return out;
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        aspects.forEach((aspect, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("key", aspect.getTag());
            entry.putInt("amount", amount);
            list.add(entry);
        });
        tag.put("Aspects", list);
        return tag;
    }

    public static AspectList load(CompoundTag tag) {
        AspectList out = new AspectList();
        ListTag list = tag.getList("Aspects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Aspect aspect = Aspect.byTag(entry.getString("key"));
            if (aspect != null) {
                out.add(aspect, entry.getInt("amount"));
            }
        }
        return out;
    }
}
