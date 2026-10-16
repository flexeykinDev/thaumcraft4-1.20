package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.items.VisAmuletItem;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Пьедестал подзарядки (blockStoneDevice:5 и TileWandPedestal оригинала): жезл или амулет вис, поставленный на него,
 * сам тянет вис из узлов ауры в восьми блоках вокруг — по единице четыре раза в секунду. Со стабилизирующим
 * фокусом сверху берёт и составные аспекты узла, превращая их в первичные.
 */
public class WandPedestalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 4, 16), box(2, 4, 2, 14, 8, 14), box(4, 8, 4, 12, 16, 12));

    public WandPedestalBlock(Properties properties) {
        super(properties);
    }

    public static boolean accepts(ItemStack stack) {
        return stack.getItem() instanceof WandItem || stack.getItem() instanceof VisAmuletItem;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Charger(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.WAND_PEDESTAL.get(), Charger::serverTick);
    }

    /** Занятый пьедестал отдаёт предмет, пустой принимает жезл или амулет вис из руки. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Charger charger)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        // фокус пьедестала ставится сверху, а не кладётся на него
        if (held.is(ModBlocks.WAND_PEDESTAL_FOCUS.get().asItem())) {
            return InteractionResult.PASS;
        }
        if (!charger.getItem().isEmpty()) {
            if (!level.isClientSide) {
                ItemStack taken = charger.getItem();
                charger.setItem(ItemStack.EMPTY);
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f,
                        ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1f) * 1.5f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (accepts(held)) {
            if (!level.isClientSide) {
                charger.setItem(player.getAbilities().instabuild ? held.copyWithCount(1) : held.split(1));
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f,
                        ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1f) * 1.6f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Charger charger) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), charger.getItem());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Компаратор показывает, насколько заряжен жезл: от 1 у пустого до 15 у полного. */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Charger charger && charger.getItem().getItem() instanceof WandItem) {
            ItemStack wand = charger.getItem();
            int total = 0;
            for (Aspect aspect : Aspect.primals()) {
                total += WandItem.getVis(wand, aspect);
            }
            return Mth.floor(total / (WandItem.getMaxVis(wand) * 6f) * 14f) + 1;
        }
        return 0;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    public static class Charger extends BlockEntity implements WorldlyContainer, AspectContainer {
        private static final int[] SLOTS = {0};
        private ItemStack item = ItemStack.EMPTY;
        private int counter;
        private boolean somethingChanged;
        @Nullable
        private List<BlockPos> nodes;

        public Charger(BlockPos pos, BlockState state) {
            super(ModBlockEntities.WAND_PEDESTAL.get(), pos, state);
        }

        public ItemStack getItem() {
            return item;
        }

        public void setItem(ItemStack stack) {
            item = stack;
            sync();
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }

        private void findNodes(Level level) {
            nodes = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-8, -8, -8), worldPosition.offset(8, 8, 8))) {
                if (level.getBlockEntity(pos) instanceof NodeBlockEntity) {
                    nodes.add(pos.immutable());
                }
            }
        }

        /** ResearchManager.reduceToPrimals оригинала: составной аспект раскладывается на первичные. */
        private static void reduceToPrimals(Aspect aspect, int amount, AspectList out) {
            if (aspect.isPrimal()) {
                out.add(aspect, amount);
            } else {
                reduceToPrimals(aspect.getFirstComponent(), amount, out);
                reduceToPrimals(aspect.getSecondComponent(), amount, out);
            }
        }

        private void give(Aspect aspect) {
            if (item.getItem() instanceof VisAmuletItem amulet) {
                amulet.addVis(item, aspect, 1, true);
            } else {
                WandItem.addVis(item, aspect, 1, true);
            }
        }

        /** Берёт единицу вис из первого подходящего узла. @return удалось ли */
        private boolean drain(ServerLevel level, List<Aspect> room, int min, boolean focus) {
            for (BlockPos pos : nodes) {
                if (!(level.getBlockEntity(pos) instanceof NodeBlockEntity node) || node.isEnergized()) {
                    continue;
                }
                Aspect taken = null;
                Aspect given = null;
                for (Aspect aspect : room) {
                    if (node.getAmount(aspect) > min) {
                        taken = aspect;
                        given = aspect;
                        break;
                    }
                }
                if (taken == null && focus) {
                    search:
                    for (Aspect aspect : node.getAspects()) {
                        if (aspect.isPrimal() || node.getAmount(aspect) <= min) {
                            continue;
                        }
                        AspectList primals = new AspectList();
                        reduceToPrimals(aspect, 1, primals);
                        for (Aspect wanted : room) {
                            if (primals.getAmount(wanted) > 0) {
                                taken = aspect;
                                given = wanted;
                                break search;
                            }
                        }
                    }
                }
                if (taken != null && node.takeFromContainer(taken, 1)) {
                    give(given);
                    node.nodeChange();
                    somethingChanged = true;
                    // колышущийся луч от узла к предмету над пьедесталом
                    FxPacket.send(level, FxPacket.FLOATY_BEAM, Vec3.atCenterOf(pos),
                            new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.65, worldPosition.getZ() + 0.5), taken.getColor(), 0);
                    return true;
                }
            }
            return false;
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Charger charger) {
            if (charger.nodes == null) {
                charger.findNodes(level);
            }
            charger.counter++;
            boolean recalc = false;
            if (charger.counter % 20 == 0 && charger.somethingChanged && !charger.item.isEmpty()) {
                charger.sync();
                charger.somethingChanged = false;
            }
            if (charger.counter % 5 == 0 && !charger.nodes.isEmpty() && !charger.item.isEmpty()) {
                boolean focus = level.getBlockState(pos.above()).is(ModBlocks.WAND_PEDESTAL_FOCUS.get());
                ItemStack item = charger.item;
                List<Aspect> room = List.of();
                // узел не опустошается до конца; жезл с железными набалдашниками или деревянным стержнем берёт и последнее
                int min = 1;
                if (item.getItem() instanceof WandItem) {
                    if (WandItem.getCap(item).getTag().equals("iron") || WandItem.getRod(item).getTag().equals("wood")) {
                        min = 0;
                    }
                    room = WandItem.getAspectsWithRoom(item);
                } else if (item.getItem() instanceof VisAmuletItem amulet) {
                    room = amulet.getAspectsWithRoom(item);
                }
                if (!room.isEmpty() && !charger.drain((ServerLevel) level, room, min, focus)) {
                    recalc = true;
                }
            }
            if (charger.counter % 100 == 0 && (recalc || charger.nodes.isEmpty())) {
                charger.findNodes(level);
            }
        }

        /** Сквозь очки откровения виден запас предмета в целых единицах вис. */
        @Nullable
        @Override
        public AspectList getContainedAspects() {
            AspectList out = new AspectList();
            for (Aspect aspect : Aspect.primals()) {
                if (item.getItem() instanceof WandItem) {
                    out.add(aspect, WandItem.getVis(item, aspect) / 100);
                } else if (item.getItem() instanceof VisAmuletItem) {
                    out.add(aspect, VisAmuletItem.getVis(item, aspect) / 100);
                }
            }
            return out;
        }

        // ---------- воронки и трубы: один предмет, только жезл или амулет ----------

        @Override
        public int[] getSlotsForFace(Direction side) {
            return SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return item.isEmpty() && accepts(stack);
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return true;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return accepts(stack);
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return item.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return item;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (item.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = item;
            setItem(ItemStack.EMPTY);
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = item;
            item = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            setItem(stack);
        }

        @Override
        public boolean stillValid(Player player) {
            return level != null && level.getBlockEntity(worldPosition) == this
                    && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64.0;
        }

        @Override
        public void clearContent() {
            setItem(ItemStack.EMPTY);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!item.isEmpty()) {
                tag.put("Item", item.save(new CompoundTag()));
            }
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            item = tag.contains("Item") ? ItemStack.of(tag.getCompound("Item")) : ItemStack.EMPTY;
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        /** Предмет рисуется над блоком. */
        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(2.0);
        }
    }
}
