package thaumcraft.common.research;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Всё, что игрок знает: открытые аспекты с очками исследований и список изученного таумометром.
 * Хранится на игроке, переживает смерть, копия отправляется на клиент.
 */
public class PlayerKnowledge {
    public static final Capability<PlayerKnowledge> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    /** Открытые аспекты и очки исследований по каждому. Первичные открыты с самого начала. */
    private final Map<Aspect, Integer> aspects = new LinkedHashMap<>();
    /** Ключи изученных объектов: "item:id", "block:id", "entity:id...". */
    private final Set<String> scanned = new HashSet<>();
    /** Ключи завершённых исследований; "@ключ" — скрытое исследование, на которое игрок получил подсказку. */
    private final Set<String> research = new HashSet<>();

    public PlayerKnowledge() {
        for (Aspect primal : Aspect.primals()) {
            aspects.put(primal, 0);
        }
    }

    public static PlayerKnowledge get(Player player) {
        return player.getCapability(CAPABILITY).orElseThrow(
                () -> new IllegalStateException("У игрока нет данных Thaumcraft"));
    }

    /** Знания игрока на этой стороне: на сервере настоящие, на клиенте — присланная сервером копия. */
    public static PlayerKnowledge forSide(Player player) {
        return player.level().isClientSide ? thaumcraft.client.ClientKnowledge.get() : get(player);
    }

    public boolean hasDiscovered(Aspect aspect) {
        return aspects.containsKey(aspect);
    }

    /** Составной аспект можно понять, только зная оба его компонента. */
    public boolean hasDiscoveredParents(Aspect aspect) {
        return aspect.isPrimal()
                || hasDiscovered(aspect.getFirstComponent()) && hasDiscovered(aspect.getSecondComponent());
    }

    /** @return true, если аспект открыт впервые */
    public boolean discover(Aspect aspect) {
        return aspects.putIfAbsent(aspect, 0) == null;
    }

    public int getPool(Aspect aspect) {
        return aspects.getOrDefault(aspect, 0);
    }

    public void addPool(Aspect aspect, int amount) {
        aspects.merge(aspect, amount, (a, b) -> Math.max(0, a + b));
    }

    public Map<Aspect, Integer> getAspects() {
        return Collections.unmodifiableMap(aspects);
    }

    public boolean hasScanned(String key) {
        return scanned.contains(key);
    }

    public void addScanned(String key) {
        scanned.add(key);
    }

    public boolean isResearchComplete(String key) {
        return research.contains(key);
    }

    /** @return true, если исследование завершено впервые */
    public boolean completeResearch(String key) {
        return research.add(key);
    }

    /** Все завершённые исследования, включая подсказки «@…» (только для чтения: том обмена знаниями переписывает их). */
    public Set<String> getCompletedResearch() {
        return java.util.Collections.unmodifiableSet(research);
    }

    public void clearResearch() {
        research.clear();
    }

    // ---------- искажение (warp) ----------
    /** Постоянное искажение: от запретных исследований, не уходит никогда. */
    private int warpPerm;
    /** «Липкое» искажение: уходит только особыми средствами (мыло здравомыслия). */
    private int warpSticky;
    /** Временное искажение: постепенно рассеивается само. */
    private int warpTemp;

    /** Счётчик искажения: сбрасывается на полную сумму при каждом новом искажении и тает с каждым приступом. */
    private int warpCounter;

    public int getWarpCounter() {
        return warpCounter;
    }

    public void setWarpCounter(int value) {
        warpCounter = Math.max(0, value);
    }

    public int getWarpPerm() {
        return warpPerm;
    }

    public int getWarpSticky() {
        return warpSticky;
    }

    public int getWarpTemp() {
        return warpTemp;
    }

    public int getWarpTotal() {
        return warpPerm + warpSticky + warpTemp;
    }

    public void addWarpPerm(int amount) {
        warpPerm = Math.max(0, warpPerm + amount);
    }

    public void addWarpSticky(int amount) {
        warpSticky = Math.max(0, warpSticky + amount);
    }

    public void addWarpTemp(int amount) {
        warpTemp = Math.max(0, warpTemp + amount);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("WarpPerm", warpPerm);
        tag.putInt("WarpSticky", warpSticky);
        tag.putInt("WarpTemp", warpTemp);
        tag.putInt("WarpCounter", warpCounter);
        CompoundTag pool = new CompoundTag();
        aspects.forEach((aspect, amount) -> pool.putInt(aspect.getTag(), amount));
        tag.put("Aspects", pool);
        ListTag list = new ListTag();
        for (String key : scanned) {
            list.add(StringTag.valueOf(key));
        }
        tag.put("Scanned", list);
        ListTag researchList = new ListTag();
        for (String key : research) {
            researchList.add(StringTag.valueOf(key));
        }
        tag.put("Research", researchList);
        return tag;
    }

    public void load(CompoundTag tag) {
        warpPerm = tag.getInt("WarpPerm");
        warpSticky = tag.getInt("WarpSticky");
        warpTemp = tag.getInt("WarpTemp");
        warpCounter = tag.getInt("WarpCounter");
        aspects.clear();
        for (Aspect primal : Aspect.primals()) {
            aspects.put(primal, 0);
        }
        CompoundTag pool = tag.getCompound("Aspects");
        for (String key : pool.getAllKeys()) {
            Aspect aspect = Aspect.byTag(key);
            if (aspect != null) {
                aspects.put(aspect, pool.getInt(key));
            }
        }
        scanned.clear();
        ListTag list = tag.getList("Scanned", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            scanned.add(list.getString(i));
        }
        research.clear();
        ListTag researchList = tag.getList("Research", Tag.TAG_STRING);
        for (int i = 0; i < researchList.size(); i++) {
            research.add(researchList.getString(i));
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerKnowledge knowledge = new PlayerKnowledge();
        private final LazyOptional<PlayerKnowledge> optional = LazyOptional.of(() -> knowledge);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return knowledge.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            knowledge.load(tag);
        }
    }
}
