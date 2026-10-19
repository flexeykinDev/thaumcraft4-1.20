package thaumictinkerer.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * TileCamo оригинала: блок, который можно замаскировать под другой полный блок. Щелчок блоком в руке — маскировка,
 * пустой рукой — снять её. У маскируемого блока есть свойство camo: под маскировкой своя модель не рисуется,
 * вместо неё отрисовщик показывает выбранный блок.
 */
public class CamoBlockEntity extends BlockEntity {
    public static final BooleanProperty CAMO = BooleanProperty.create("camo");

    @Nullable
    private BlockState camo;

    public CamoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public BlockState getCamo() {
        return camo;
    }

    /** Общий щелчок по маскируемому блоку; PASS — щелчок не про маскировку. */
    public static InteractionResult useCamo(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof CamoBlockEntity tile)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        BlockState camo = null;
        if (!held.isEmpty()) {
            if (!(held.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }
            camo = blockItem.getBlock().defaultBlockState();
            if (!PlatformBlock.canDisguiseAs(camo, level, pos)) {
                return InteractionResult.PASS;
            }
        }
        if (!level.isClientSide) {
            tile.camo = camo;
            level.setBlock(pos, state.setValue(CAMO, camo != null), Block.UPDATE_ALL);
            tile.setChanged();
            level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (camo != null) {
            tag.put("Camo", NbtUtils.writeBlockState(camo));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        camo = tag.contains("Camo") ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Camo")) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
