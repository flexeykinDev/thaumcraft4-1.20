package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModSounds;

/**
 * Фокальный манипулятор (BlockStoneDevice оригинала, мета 13). В него кладут фокус и выбирают улучшение
 * очередного ранга. Плата — уровни опыта сразу и вис из сети реле по ходу дела: чем выше ранг, тем дороже.
 */
public class FocalManipulatorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(0, 8, 0, 16, 16, 16), box(0, 0, 0, 16, 4, 16),
            box(1, 4, 1, 5, 8, 5), box(11, 4, 1, 15, 8, 5), box(1, 4, 11, 5, 8, 15), box(11, 4, 11, 15, 8, 15));

    public FocalManipulatorBlock(Properties properties) {
        super(properties);
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
        return new Manipulator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOCAL_MANIPULATOR.get(), Manipulator::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Manipulator tile && player instanceof ServerPlayer serverPlayer) {
            // без изучения «Фокусной манипуляции» стол не открывается, как в оригинале
            if (thaumcraft.common.research.PlayerKnowledge.get(serverPlayer).isResearchComplete("FOCALMANIPULATION")) {
                NetworkHooks.openScreen(serverPlayer, tile, pos);
            } else {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.researchmissing")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Manipulator tile) {
            Containers.dropContents(level, pos, tile);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** TileFocalManipulator оригинала. */
    public static class Manipulator extends BlockEntity implements Container, MenuProvider {
        /** Уровней опыта за каждый ранг улучшения. */
        public static final int XP_MULT = 8;

        private ItemStack focus = ItemStack.EMPTY;
        /** Сколько вис ещё осталось набрать, в сотых долях. */
        private AspectList aspects = new AspectList();
        /** Полная цена текущего улучшения; ноль — манипулятор свободен. */
        private int size;
        private int upgrade = -1;
        private int rank = -1;
        private int ticks;

        public Manipulator(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FOCAL_MANIPULATOR.get(), pos, state);
        }

        public AspectList getRemaining() {
            return aspects;
        }

        public int getSize() {
            return size;
        }

        public int getUpgrade() {
            return upgrade;
        }

        /** Ранг, на который ляжет следующее улучшение фокуса: от 1 до 5, либо -1, если все пять заняты. */
        public static int nextRank(ItemStack focus) {
            short[] applied = FocusItem.getAppliedUpgrades(focus);
            for (int rank = 1; rank <= 5; rank++) {
                if (applied[rank - 1] == -1) {
                    return rank;
                }
            }
            return -1;
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        public static void tick(Level level, BlockPos pos, BlockState state, Manipulator tile) {
            if (level.isClientSide) {
                // пока идёт работа, над столом вьются искры
                if (tile.size > 0) {
                    thaumcraft.client.fx.Fx.sparkle(pos.getX() + 0.5 + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f,
                            pos.getY() + 1.25 + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f,
                            pos.getZ() + 0.5 + (level.random.nextFloat() - level.random.nextFloat()) * 0.3f,
                            0.7f + level.random.nextFloat() * 0.4f, level.random.nextInt(5), 0f);
                }
                return;
            }
            if (++tile.ticks % 5 != 0 || tile.size <= 0) {
                return;
            }
            boolean complete = false;
            if (tile.aspects.visSize() <= 0 || tile.focus.isEmpty()) {
                // фокус вынули на полпути: работа пропала
                complete = true;
                level.playSound(null, pos, ModSounds.CRAFT_FAIL.get(), SoundSource.BLOCKS, 0.33f, 1f);
            } else {
                for (Aspect aspect : tile.aspects.getAspectsSortedByAmount()) {
                    int drain = VisNetHandler.drainVis(level, pos, aspect, Math.min(100, tile.aspects.getAmount(aspect)));
                    if (drain > 0) {
                        tile.aspects.reduce(aspect, drain);
                        tile.sync();
                    }
                }
                if (tile.aspects.visSize() <= 0) {
                    complete = true;
                    FocusUpgrade type = FocusUpgrade.byId(tile.upgrade);
                    if (type != null) {
                        FocusItem.applyUpgrade(tile.focus, type, tile.rank);
                    }
                    level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
                }
            }
            if (complete) {
                tile.size = 0;
                tile.rank = -1;
                tile.upgrade = -1;
                tile.aspects = new AspectList();
                tile.sync();
            }
        }

        /** Игрок выбрал улучшение и нажал «начать». */
        public boolean startCraft(int id, Player player) {
            if (level == null || size > 0 || !(focus.getItem() instanceof FocusItem item)) {
                return false;
            }
            int next = nextRank(focus);
            FocusUpgrade type = FocusUpgrade.byId(id);
            if (next < 1 || type == null) {
                return false;
            }
            int xp = next * XP_MULT;
            if (player.experienceLevel < xp && !player.getAbilities().instabuild) {
                return false;
            }
            boolean offered = false;
            for (FocusUpgrade possible : item.getPossibleUpgradesByRank(focus, next)) {
                offered |= possible == type;
            }
            if (!offered || !item.canApplyUpgrade(focus, player, type, next)) {
                return false;
            }
            rank = next;
            aspects = type.getVisCost(next);
            size = aspects.visSize();
            upgrade = id;
            if (!player.getAbilities().instabuild) {
                player.giveExperienceLevels(-xp);
            }
            sync();
            level.playSound(null, worldPosition, ModSounds.CRAFT_START.get(), SoundSource.BLOCKS, 0.25f, 1f);
            return true;
        }

        // ---------- одна ячейка под фокус ----------

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return focus.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return focus;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack out = focus.split(amount);
            sync();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack out = focus;
            focus = ItemStack.EMPTY;
            return out;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            focus = stack;
            sync();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof FocusItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            focus = ItemStack.EMPTY;
        }

        @Override
        public Component getDisplayName() {
            return getBlockState().getBlock().getName();
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new FocalManipulatorMenu(id, inventory, this);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.put("Focus", focus.save(new CompoundTag()));
            tag.put("Aspects", aspects.save(new CompoundTag()));
            tag.putInt("size", size);
            tag.putInt("upgrade", upgrade);
            tag.putInt("rank", rank);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            focus = ItemStack.of(tag.getCompound("Focus"));
            aspects = AspectList.load(tag.getCompound("Aspects"));
            size = tag.getInt("size");
            upgrade = tag.getInt("upgrade");
            rank = tag.getInt("rank");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition, worldPosition.offset(1, 2, 1));
        }
    }
}
