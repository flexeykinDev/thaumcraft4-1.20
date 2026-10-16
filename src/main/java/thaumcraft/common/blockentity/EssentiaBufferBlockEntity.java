package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.blocks.BellowsBlock;
import thaumcraft.common.blocks.TubeBlock;
import thaumcraft.common.registry.ModBlockEntities;

import java.util.List;

/**
 * Буфер эссенции — перенос TileTubeBuffer оригинала. Держит до 8 единиц любых аспектов вперемешку. Сам тянет
 * совсем слабо (сила 1), а с мехами рядом — по 32 за каждые. Каждую сторону можно «придушить» жезлом с Shift:
 * один раз — сторона перестаёт получать тягу мехов, второй — перестаёт тянуть вовсе.
 */
public class EssentiaBufferBlockEntity extends BlockEntity implements EssentiaTransport, TubeBlock.Sided, thaumcraft.api.aspects.AspectContainer {
    public static final int MAX_AMOUNT = 8;

    private AspectList aspects = new AspectList();
    private final boolean[] openSides = {true, true, true, true, true, true};
    private final byte[] chokedSides = new byte[6];
    private int count;
    private int bellows = -1;

    public EssentiaBufferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENTIA_BUFFER.get(), pos, state);
    }

    @Override
    public AspectList getContainedAspects() {
        return aspects;
    }

    public AspectList getAspects() {
        return aspects;
    }

    /** 0 — сторона работает в полную силу, 1 — без тяги мехов, 2 — не тянет вовсе. */
    public int getChoke(Direction side) {
        return chokedSides[side.get3DDataValue()];
    }

    /** Жезл с Shift: следующая степень «придушенности» стороны. */
    public void cycleChoke(Direction side) {
        int index = side.get3DDataValue();
        chokedSides[index] = (byte) ((chokedSides[index] + 1) % 3);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssentiaBufferBlockEntity buffer) {
        buffer.count++;
        if (buffer.bellows < 0 || buffer.count % 20 == 0) {
            buffer.bellows = BellowsBlock.count(level, pos, List.of(Direction.values()));
        }
        if (buffer.count % 5 == 0 && buffer.aspects.visSize() < MAX_AMOUNT) {
            buffer.fillBuffer(level);
        }
    }

    private void fillBuffer(Level level) {
        for (Direction side : Direction.values()) {
            EssentiaTransport other = EssentiaTransport.neighbour(level, worldPosition, side);
            if (other == null || !isConnectable(side)) {
                continue;
            }
            int suction = getSuctionAmount(side);
            if (other.getEssentiaAmount(side.getOpposite()) > 0 && other.getSuctionAmount(side.getOpposite()) < suction
                    && suction >= other.getMinimumSuction()) {
                Aspect aspect = other.getEssentiaType(side.getOpposite());
                if (aspect != null && other.takeEssentia(aspect, 1, side.getOpposite()) == 1) {
                    aspects.add(aspect, 1);
                    setChanged();
                }
                return;
            }
        }
    }

    // ---------- стороны ----------

    @Override
    public boolean isOpen(Direction side) {
        return openSides[side.get3DDataValue()];
    }

    @Override
    public void setOpen(Direction side, boolean open) {
        openSides[side.get3DDataValue()] = open;
        sync();
    }

    @Override
    public Direction getFacing() {
        return Direction.NORTH;
    }

    @Override
    public boolean isFlowAllowed() {
        return true;
    }

    // ---------- транспорт ----------

    @Override
    public boolean isConnectable(Direction side) {
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
    }

    @Nullable
    @Override
    public Aspect getSuctionType(@Nullable Direction side) {
        return null;
    }

    @Override
    public int getSuctionAmount(@Nullable Direction side) {
        int choke = side == null ? 0 : chokedSides[side.get3DDataValue()];
        return choke == 2 ? 0 : bellows > 0 && choke != 1 ? bellows * 32 : 1;
    }

    /** Буфер отдаёт что попало: каждый раз называет случайный из лежащих в нём аспектов. */
    @Nullable
    @Override
    public Aspect getEssentiaType(@Nullable Direction side) {
        List<Aspect> present = aspects.getAspects();
        return present.isEmpty() || level == null ? null : present.get(level.random.nextInt(present.size()));
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return aspects.visSize();
    }

    /** Не отдаёт эссенцию слабому соседу, если с другой стороны её тянут сильнее. */
    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction side) {
        if (!canOutputTo(side) || level == null) {
            return 0;
        }
        EssentiaTransport asking = EssentiaTransport.neighbour(level, worldPosition, side);
        int suction = asking == null ? 0 : asking.getSuctionAmount(side.getOpposite());
        for (Direction other : Direction.values()) {
            if (other == side || !canOutputTo(other)) {
                continue;
            }
            EssentiaTransport rival = EssentiaTransport.neighbour(level, worldPosition, other);
            if (rival == null) {
                continue;
            }
            int rivalSuction = rival.getSuctionAmount(other.getOpposite());
            Aspect rivalType = rival.getSuctionType(other.getOpposite());
            if ((rivalType == aspect || rivalType == null) && suction < rivalSuction && getSuctionAmount(other) < rivalSuction) {
                return 0;
            }
        }
        int taken = Math.min(amount, aspects.getAmount(aspect));
        if (taken > 0 && aspects.reduce(aspect, taken)) {
            setChanged();
            return taken;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction side) {
        if (!canInputFrom(side) || amount != 1 || aspects.visSize() >= MAX_AMOUNT) {
            return 0;
        }
        aspects.add(aspect, 1);
        setChanged();
        return 1;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // ---------- сохранение ----------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Aspects", aspects.save(new CompoundTag()));
        byte[] sides = new byte[6];
        for (int i = 0; i < 6; i++) {
            sides[i] = (byte) (openSides[i] ? 1 : 0);
        }
        tag.putByteArray("Open", sides);
        tag.putByteArray("Choke", chokedSides.clone());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        aspects = AspectList.load(tag.getCompound("Aspects"));
        byte[] sides = tag.getByteArray("Open");
        if (sides.length == 6) {
            for (int i = 0; i < 6; i++) {
                openSides[i] = sides[i] == 1;
            }
        }
        byte[] choke = tag.getByteArray("Choke");
        if (choke.length == 6) {
            System.arraycopy(choke, 0, chokedSides, 0, 6);
        }
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
