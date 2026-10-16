package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.blocks.TubeBlock;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Труба для эссенции — перенос TileTube оригинала и его разновидностей (вентиль, фильтр, сужение, обратный клапан).
 * Труба держит одну единицу эссенции. Раз в два тика она узнаёт у соседей, кто сильнее всасывает, и перенимает это
 * всасывание, ослабив на единицу; раз в пять тиков пустая труба забирает единицу у соседа со всасыванием слабее.
 * Если встречаются два равных всасывания разных аспектов, трубу «рвёт»: она шипит и выпускает пар.
 */
public class TubeBlockEntity extends BlockEntity implements EssentiaTransport, TubeBlock.Sided, thaumcraft.api.aspects.AspectContainer {
    private final boolean[] openSides = {true, true, true, true, true, true};
    /** Куда направлен обратный клапан (и куда смотрит ручка вентиля). */
    private Direction facing = Direction.NORTH;
    @Nullable
    private Aspect essentiaType;
    private int essentiaAmount;
    @Nullable
    private Aspect suctionType;
    private int suction;
    private int venting;
    private int count;
    // вентиль
    private boolean allowFlow = true;
    private boolean wasPowered;
    // фильтр
    @Nullable
    private Aspect aspectFilter;

    public TubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUBE.get(), pos, state);
    }

    /** Труба с фильтром показывает свой аспект, без количества (TileTubeFilter оригинала). */
    @Override
    public thaumcraft.api.aspects.AspectList getContainedAspects() {
        thaumcraft.api.aspects.AspectList list = new thaumcraft.api.aspects.AspectList();
        if (kind() == TubeBlock.Kind.FILTER && aspectFilter != null) {
            list.add(aspectFilter, 1);
        }
        return list;
    }

    @Override
    public boolean showAspectAmounts() {
        return false;
    }

    private TubeBlock.Kind kind() {
        return getBlockState().getBlock() instanceof TubeBlock tube ? tube.kind : TubeBlock.Kind.NORMAL;
    }

    @Override
    public Direction getFacing() {
        return facing;
    }

    @Override
    public boolean isOpen(Direction side) {
        return openSides[side.get3DDataValue()];
    }

    @Override
    public boolean isFlowAllowed() {
        return allowFlow;
    }

    @Nullable
    public Aspect getAspectFilter() {
        return aspectFilter;
    }

    public void setAspectFilter(@Nullable Aspect aspect) {
        aspectFilter = aspect;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // только на клиенте: поворот рукояти вентиля
    private float valveRotation;
    private float valveRotationOld;

    public float getValveRotation(float partialTick) {
        return valveRotationOld + (valveRotation - valveRotationOld) * partialTick;
    }

    /** Закрытый вентиль докручивается до 360 градусов, открытый возвращается к нулю — по 20 градусов за тик. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TubeBlockEntity tube) {
        tube.valveRotationOld = tube.valveRotation;
        if (!tube.allowFlow && tube.valveRotation < 360f) {
            tube.valveRotation += 20f;
        } else if (tube.allowFlow && tube.valveRotation > 0f) {
            tube.valveRotation -= 20f;
        }
    }

    // ---------- работа ----------

    public static void serverTick(Level level, BlockPos pos, BlockState state, TubeBlockEntity tube) {
        if (tube.venting > 0) {
            tube.venting--;
        }
        if (tube.count == 0) {
            tube.count = level.random.nextInt(10);
        }
        TubeBlock.Kind kind = tube.kind();
        if (kind == TubeBlock.Kind.VALVE && tube.count % 5 == 0) {
            tube.checkValve(level);
        }
        if (tube.venting > 0) {
            // пар из «порванной» трубы
            if (level instanceof ServerLevel server && tube.venting % 2 == 0) {
                FxPacket.send(server, FxPacket.VENT, Vec3.atCenterOf(pos), Vec3.ZERO,
                        tube.suctionType == null ? 0xaaaaaa : tube.suctionType.getColor(), 0);
            }
            return;
        }
        if (++tube.count % 2 == 0) {
            tube.calculateSuction(level);
            tube.checkVenting(level);
            if (tube.essentiaType != null && tube.essentiaAmount == 0) {
                tube.essentiaType = null;
            }
        }
        if (tube.count % 5 == 0 && tube.suction > 0) {
            tube.equalizeWithNeighbours(level);
        }
    }

    /** Сигнал красного камня закрывает вентиль; когда сигнал пропадает, вентиль открывается. */
    private void checkValve(Level level) {
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (wasPowered && !powered && !allowFlow) {
            allowFlow = true;
            level.playSound(null, worldPosition, ModSounds.SQUEEK.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.random.nextFloat() * 0.2f);
            sync();
        }
        if (!wasPowered && powered && allowFlow) {
            allowFlow = false;
            level.playSound(null, worldPosition, ModSounds.SQUEEK.get(), SoundSource.BLOCKS, 0.7f, 0.9f + level.random.nextFloat() * 0.2f);
            sync();
        }
        if (wasPowered != powered) {
            // соседние трубы должны перерисовать отводы к закрытому вентилю
            TubeBlock.refresh(level, worldPosition);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        wasPowered = powered;
    }

    /** Перенимает самое сильное всасывание соседей. Фильтр пропускает только свой аспект, сужение делит силу пополам. */
    private void calculateSuction(Level level) {
        TubeBlock.Kind kind = kind();
        Aspect filter = kind == TubeBlock.Kind.FILTER ? aspectFilter : null;
        boolean restrict = kind == TubeBlock.Kind.RESTRICT;
        boolean directional = kind == TubeBlock.Kind.ONEWAY;
        suction = 0;
        suctionType = null;
        if (kind == TubeBlock.Kind.VALVE && !allowFlow) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (directional && facing != side.getOpposite() || !isConnectable(side)) {
                continue;
            }
            EssentiaTransport other = EssentiaTransport.neighbour(level, worldPosition, side);
            if (other == null) {
                continue;
            }
            Aspect otherType = other.getSuctionType(side.getOpposite());
            boolean passesFilter = filter == null || otherType == null || otherType == filter;
            boolean matchesContents = essentiaAmount <= 0 || otherType == null || essentiaType == null || essentiaType == otherType;
            if (passesFilter && matchesContents) {
                int suck = other.getSuctionAmount(side.getOpposite());
                if (suck > 0 && suck > suction + 1) {
                    setSuction(otherType == null ? filter : otherType, restrict ? suck / 2 : suck - 1);
                }
            }
        }
    }

    private void checkVenting(Level level) {
        for (Direction side : Direction.values()) {
            if (!isConnectable(side)) {
                continue;
            }
            EssentiaTransport other = EssentiaTransport.neighbour(level, worldPosition, side);
            if (other == null) {
                continue;
            }
            int suck = other.getSuctionAmount(side.getOpposite());
            if (suction > 0 && (suck == suction || suck == suction - 1) && suctionType != other.getSuctionType(side.getOpposite())) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1f, 1f + level.random.nextFloat() * 0.1f);
                venting = 40;
            }
        }
    }

    private void equalizeWithNeighbours(Level level) {
        if (essentiaAmount > 0) {
            return;
        }
        boolean directional = kind() == TubeBlock.Kind.ONEWAY;
        for (Direction side : Direction.values()) {
            if (directional && facing == side.getOpposite() || !isConnectable(side)) {
                continue;
            }
            EssentiaTransport other = EssentiaTransport.neighbour(level, worldPosition, side);
            if (other == null || !other.canOutputTo(side.getOpposite())) {
                continue;
            }
            Aspect otherEssentia = other.getEssentiaType(side.getOpposite());
            if ((suctionType == null || suctionType == otherEssentia || otherEssentia == null)
                    && suction > other.getSuctionAmount(side.getOpposite()) && suction >= other.getMinimumSuction()) {
                Aspect wanted = suctionType != null ? suctionType : otherEssentia != null ? otherEssentia : other.getEssentiaType(null);
                if (wanted == null) {
                    continue;
                }
                if (addEssentia(wanted, other.takeEssentia(wanted, 1, side.getOpposite()), side) > 0) {
                    if (level.random.nextInt(100) == 0) {
                        level.playSound(null, worldPosition, ModSounds.CREAK.get(), SoundSource.BLOCKS, 1f, 1.3f + level.random.nextFloat() * 0.2f);
                    }
                    setChanged();
                    return;
                }
            }
        }
    }

    // ---------- жезл ----------

    @Override
    public void setOpen(Direction side, boolean open) {
        openSides[side.get3DDataValue()] = open;
        sync();
    }

    /** Жезлом по середине — повернуть к следующему подключённому соседу. */
    public void rotate() {
        if (level == null) {
            return;
        }
        int start = facing.get3DDataValue();
        for (int a = start + 1; a < start + 7; a++) {
            Direction candidate = Direction.from3DDataValue(a % 6);
            if (level.getBlockEntity(worldPosition.relative(candidate.getOpposite())) instanceof EssentiaTransport
                    && isConnectable(candidate.getOpposite())) {
                facing = candidate;
                break;
            }
        }
        sync();
        TubeBlock.refresh(level, worldPosition);
    }

    // ---------- транспорт ----------

    @Override
    public boolean isConnectable(Direction side) {
        if (kind() == TubeBlock.Kind.VALVE && !allowFlow) {
            return false;
        }
        return openSides[side.get3DDataValue()];
    }

    @Override
    public boolean canInputFrom(Direction side) {
        return isConnectable(side);
    }

    @Override
    public boolean canOutputTo(Direction side) {
        return isConnectable(side);
    }

    @Override
    public void setSuction(@Nullable Aspect aspect, int amount) {
        suctionType = aspect;
        suction = amount;
    }

    @Nullable
    @Override
    public Aspect getSuctionType(@Nullable Direction side) {
        return suctionType;
    }

    @Override
    public int getSuctionAmount(@Nullable Direction side) {
        return suction;
    }

    @Nullable
    @Override
    public Aspect getEssentiaType(@Nullable Direction side) {
        return essentiaType;
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return essentiaAmount;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction side) {
        if (canOutputTo(side) && essentiaType == aspect && essentiaAmount > 0 && amount > 0) {
            essentiaAmount--;
            if (essentiaAmount <= 0) {
                essentiaType = null;
            }
            setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction side) {
        if (canInputFrom(side) && essentiaAmount == 0 && amount > 0) {
            essentiaType = aspect;
            essentiaAmount++;
            setChanged();
            return 1;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // ---------- сохранение ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (essentiaType != null) {
            tag.putString("Type", essentiaType.getTag());
        }
        tag.putInt("Amount", essentiaAmount);
        tag.putByte("Facing", (byte) facing.get3DDataValue());
        byte[] sides = new byte[6];
        for (int i = 0; i < 6; i++) {
            sides[i] = (byte) (openSides[i] ? 1 : 0);
        }
        tag.putByteArray("Open", sides);
        if (suctionType != null) {
            tag.putString("SuctionType", suctionType.getTag());
        }
        tag.putInt("Suction", suction);
        tag.putBoolean("Flow", allowFlow);
        tag.putBoolean("Powered", wasPowered);
        if (aspectFilter != null) {
            tag.putString("Filter", aspectFilter.getTag());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        essentiaType = Aspect.byTag(tag.getString("Type"));
        essentiaAmount = tag.getInt("Amount");
        facing = Direction.from3DDataValue(tag.getByte("Facing"));
        byte[] sides = tag.getByteArray("Open");
        if (sides.length == 6) {
            for (int i = 0; i < 6; i++) {
                openSides[i] = sides[i] == 1;
            }
        }
        suctionType = Aspect.byTag(tag.getString("SuctionType"));
        suction = tag.getInt("Suction");
        allowFlow = !tag.contains("Flow") || tag.getBoolean("Flow");
        wasPowered = tag.getBoolean("Powered");
        aspectFilter = Aspect.byTag(tag.getString("Filter"));
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
