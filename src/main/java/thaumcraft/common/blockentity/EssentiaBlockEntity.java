package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Сосуд с эссенцией одного аспекта — общая часть перегонного куба (TileAlembic, 32 единицы)
 * и запечатанной банки (TileJarFillable, 64 единицы). Ярлык закрепляет за сосудом один аспект.
 * К трубам куб подключается с боков и сверху и только отдаёт; банка подключается сверху и тянет эссенцию в себя.
 */
public class EssentiaBlockEntity extends BlockEntity implements EssentiaTransport, thaumcraft.api.aspects.AspectContainer {
    public static final int ALEMBIC_CAPACITY = 32;
    public static final int JAR_CAPACITY = 64;

    private final int capacity;
    @Nullable
    private Aspect aspect;
    private int amount;
    /** Аспект с ярлыка: сосуд не примет ничего другого. */
    @Nullable
    private Aspect filter;
    /** Сторона, на которую наклеен ярлык. */
    private Direction labelSide = Direction.NORTH;
    private int count;

    protected EssentiaBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.capacity = capacity;
    }

    public static EssentiaBlockEntity alembic(BlockPos pos, BlockState state) {
        return new EssentiaBlockEntity(ModBlockEntities.ALEMBIC.get(), pos, state, ALEMBIC_CAPACITY);
    }

    public static EssentiaBlockEntity jar(BlockPos pos, BlockState state) {
        return new EssentiaBlockEntity(ModBlockEntities.JAR.get(), pos, state, JAR_CAPACITY);
    }

    @Override
    public thaumcraft.api.aspects.AspectList getContainedAspects() {
        thaumcraft.api.aspects.AspectList list = new thaumcraft.api.aspects.AspectList();
        if (aspect != null) {
            list.add(aspect, amount);
        }
        return list;
    }

    public boolean isJar() {
        return capacity == JAR_CAPACITY;
    }

    /** Банка пустоты (TileJarFillableVoid оригинала): принимает эссенцию и полной, излишек пропадает. */
    public boolean isVoid() {
        return getBlockState().is(thaumcraft.common.registry.ModBlocks.VOID_JAR.get());
    }

    public int getCapacity() {
        return capacity;
    }

    /** Аспект содержимого; null, если сосуд пуст. */
    @Nullable
    public Aspect getAspect() {
        return amount > 0 ? aspect : null;
    }

    public int getAmount() {
        return amount;
    }

    @Nullable
    public Aspect getFilter() {
        return filter;
    }

    public Direction getLabelSide() {
        return labelSide;
    }

    /** Наклеивает ярлык (или снимает, если aspect == null). */
    public void setFilter(@Nullable Aspect aspect, Direction side) {
        filter = aspect;
        labelSide = side.getAxis().isHorizontal() ? side : Direction.NORTH;
        sync();
    }

    /** Пустой сосуд примет любой аспект, начатый — только свой, с ярлыком — только аспект ярлыка. */
    public boolean accepts(Aspect candidate) {
        return (filter == null || filter == candidate) && (amount == 0 || aspect == candidate);
    }

    /** @return сколько не поместилось */
    public int add(Aspect added, int count) {
        if (!accepts(added) || count <= 0) {
            return count;
        }
        int moved = Math.min(count, capacity - amount);
        if (moved > 0) {
            aspect = added;
            amount += moved;
            sync();
        }
        return isVoid() ? 0 : count - moved;
    }

    /** Забирает ровно столько, сколько просили, либо ничего. */
    public boolean take(Aspect taken, int count) {
        if (amount < count || aspect != taken) {
            return false;
        }
        amount -= count;
        if (amount == 0) {
            aspect = null;
        }
        sync();
        return true;
    }

    public void clear() {
        amount = 0;
        aspect = null;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    // ---------- трубы ----------

    /** Банка раз в пять тиков тянет единицу эссенции из трубы над собой (fillJar оригинала). */
    public static void jarTick(Level level, BlockPos pos, BlockState state, EssentiaBlockEntity jar) {
        if (++jar.count % 5 != 0 || jar.amount >= jar.capacity && !jar.isVoid()) {
            return;
        }
        EssentiaTransport above = EssentiaTransport.neighbour(level, pos, Direction.UP);
        if (above == null || !above.canOutputTo(Direction.DOWN)) {
            return;
        }
        int suction = jar.getSuctionAmount(Direction.UP);
        Aspect wanted = null;
        if (jar.filter != null) {
            wanted = jar.filter;
        } else if (jar.aspect != null && jar.amount > 0) {
            wanted = jar.aspect;
        } else if (above.getEssentiaAmount(Direction.DOWN) > 0 && above.getSuctionAmount(Direction.DOWN) < suction
                && suction >= above.getMinimumSuction()) {
            wanted = above.getEssentiaType(Direction.DOWN);
        }
        if (wanted != null && above.getSuctionAmount(Direction.DOWN) < suction) {
            jar.add(wanted, above.takeEssentia(wanted, 1, Direction.DOWN));
        }
    }

    @Override
    public boolean isConnectable(Direction side) {
        return isJar() ? side == Direction.UP : side != Direction.DOWN;
    }

    @Override
    public boolean canInputFrom(Direction side) {
        return isJar() && side == Direction.UP;
    }

    @Override
    public boolean canOutputTo(Direction side) {
        return isConnectable(side);
    }

    @Override
    public void setSuction(@Nullable Aspect suctionAspect, int suctionAmount) {
    }

    @Nullable
    @Override
    public Aspect getSuctionType(@Nullable Direction side) {
        return !isJar() ? null : filter != null ? filter : getAspect();
    }

    @Override
    public int getSuctionAmount(@Nullable Direction side) {
        if (isVoid()) {
            // тянет всегда, но слабее обычной банки с ярлыком: сперва наполняются они
            return filter != null && amount < capacity ? 48 : 32;
        }
        return isJar() && amount < capacity ? (filter != null ? 64 : 32) : 0;
    }

    @Override
    public int takeEssentia(Aspect taken, int count, Direction side) {
        return canOutputTo(side) && take(taken, count) ? count : 0;
    }

    @Override
    public int addEssentia(Aspect added, int count, Direction side) {
        return canInputFrom(side) ? count - add(added, count) : 0;
    }

    @Nullable
    @Override
    public Aspect getEssentiaType(@Nullable Direction side) {
        return getAspect();
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return amount;
    }

    @Override
    public int getMinimumSuction() {
        return !isJar() ? 0 : isVoid() ? (filter != null ? 48 : 32) : filter != null ? 64 : 32;
    }

    // ---------- сохранение ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // пустой сосуд без ярлыка ничего не пишет, чтобы пустые банки складывались в стопку
        if (amount > 0 && aspect != null) {
            tag.putString("Aspect", aspect.getTag());
            tag.putShort("Amount", (short) amount);
        }
        if (filter != null) {
            tag.putString("Filter", filter.getTag());
            tag.putByte("LabelSide", (byte) labelSide.get3DDataValue());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        aspect = Aspect.byTag(tag.getString("Aspect"));
        amount = aspect == null ? 0 : Math.min(capacity, tag.getShort("Amount"));
        filter = Aspect.byTag(tag.getString("Filter"));
        labelSide = tag.contains("LabelSide") ? Direction.from3DDataValue(tag.getByte("LabelSide")) : Direction.NORTH;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
