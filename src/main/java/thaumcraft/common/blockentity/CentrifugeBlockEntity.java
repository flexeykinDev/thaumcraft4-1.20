package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Алхимическая центрифуга — перенос TileCentrifuge оригинала. Тянет снизу единицу составной эссенции
 * (с силой 128), две секунды крутит её и выдаёт наверх один из двух её компонентов, выбранный случайно;
 * второй теряется. Сигнал красного камня останавливает центрифугу.
 */
public class CentrifugeBlockEntity extends BlockEntity implements EssentiaTransport, thaumcraft.api.aspects.AspectContainer {
    @Nullable
    private Aspect aspectIn;
    @Nullable
    private Aspect aspectOut;
    private int count;
    private int process;
    // только на клиенте
    private float rotationSpeed;
    private float rotation;
    private float rotationOld;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE.get(), pos, state);
    }

    /** Как в оригинале, показан готовый к выдаче аспект. */
    @Override
    public thaumcraft.api.aspects.AspectList getContainedAspects() {
        thaumcraft.api.aspects.AspectList list = new thaumcraft.api.aspects.AspectList();
        if (aspectOut != null) {
            list.add(aspectOut, 1);
        }
        return list;
    }

    public float getRotation(float partialTick) {
        return rotationOld + (rotation - rotationOld) * partialTick;
    }

    private boolean powered() {
        return level != null && level.hasNeighborSignal(worldPosition);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity centrifuge) {
        if (centrifuge.powered()) {
            return;
        }
        if (centrifuge.aspectOut == null && centrifuge.aspectIn == null && ++centrifuge.count % 5 == 0) {
            centrifuge.drawEssentia(level);
        }
        if (centrifuge.process > 0) {
            centrifuge.process--;
        }
        if (centrifuge.aspectOut == null && centrifuge.aspectIn != null && centrifuge.process == 0) {
            centrifuge.aspectOut = level.random.nextBoolean() ? centrifuge.aspectIn.getFirstComponent() : centrifuge.aspectIn.getSecondComponent();
            centrifuge.aspectIn = null;
            centrifuge.sync();
        }
    }

    /** Пока внутри есть эссенция, ротор разгоняется; дважды за оборот слышен стук насоса. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CentrifugeBlockEntity centrifuge) {
        boolean powered = centrifuge.powered();
        if (centrifuge.aspectIn != null && !powered && centrifuge.rotationSpeed < 20f) {
            centrifuge.rotationSpeed += 2f;
        }
        if ((centrifuge.aspectIn == null || powered) && centrifuge.rotationSpeed > 0f) {
            centrifuge.rotationSpeed = Math.max(0f, centrifuge.rotationSpeed - 0.5f);
        }
        int previous = (int) centrifuge.rotation;
        centrifuge.rotationOld = centrifuge.rotation;
        centrifuge.rotation += centrifuge.rotationSpeed;
        if (centrifuge.rotation % 180f <= 20f && previous % 180 >= 160 && centrifuge.rotationSpeed > 0f) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.PUMP.get(), SoundSource.BLOCKS,
                    1f, 1f, false);
        }
        if (centrifuge.rotation > 36000f) {
            centrifuge.rotation -= 36000f;
            centrifuge.rotationOld -= 36000f;
        }
    }

    private void drawEssentia(Level level) {
        EssentiaTransport below = EssentiaTransport.neighbour(level, worldPosition, Direction.DOWN);
        if (below == null || !below.canOutputTo(Direction.UP)) {
            return;
        }
        int suction = getSuctionAmount(Direction.DOWN);
        Aspect wanted = null;
        if (below.getEssentiaAmount(Direction.UP) > 0 && below.getSuctionAmount(Direction.UP) < suction
                && suction >= below.getMinimumSuction()) {
            wanted = below.getEssentiaType(Direction.UP);
        }
        // первичные аспекты делить не на что
        if (wanted != null && !wanted.isPrimal() && below.getSuctionAmount(Direction.UP) < suction
                && below.takeEssentia(wanted, 1, Direction.UP) == 1) {
            aspectIn = wanted;
            process = 39;
            sync();
        }
    }

    @Override
    public boolean isConnectable(Direction side) {
        return side.getAxis().isVertical();
    }

    @Override
    public boolean canInputFrom(Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    public boolean canOutputTo(Direction side) {
        return side == Direction.UP;
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
        return side == Direction.DOWN ? (powered() ? 0 : aspectIn == null ? 128 : 64) : 0;
    }

    @Nullable
    @Override
    public Aspect getEssentiaType(@Nullable Direction side) {
        return aspectOut;
    }

    @Override
    public int getEssentiaAmount(@Nullable Direction side) {
        return aspectOut != null ? 1 : 0;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction side) {
        if (canOutputTo(side) && aspectOut != null && aspect == aspectOut) {
            aspectOut = null;
            sync();
            return amount;
        }
        return 0;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction side) {
        if (aspectIn == null && !aspect.isPrimal()) {
            aspectIn = aspect;
            process = 39;
            sync();
            return 1;
        }
        return 0;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (aspectIn != null) {
            tag.putString("In", aspectIn.getTag());
        }
        if (aspectOut != null) {
            tag.putString("Out", aspectOut.getTag());
        }
        tag.putInt("Process", process);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        aspectIn = Aspect.byTag(tag.getString("In"));
        aspectOut = Aspect.byTag(tag.getString("Out"));
        process = tag.getInt("Process");
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
