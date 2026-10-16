package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.api.wands.Wandable;
import thaumcraft.common.blockentity.NodeBlockEntity;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModItems;

import java.util.List;

/**
 * Узел в банке — перенос BlockJar (мета 2), TileJarNode и ItemJarNode оригинала. Узел ауры, запертый жезлом в
 * стеклянный короб с деревянной крышкой, становится банкой: её можно разбить и унести (узел остаётся внутри),
 * поставить где угодно, а щелчком жезла разбить и выпустить узел на волю. В банке узел спит: не растёт и не тратится.
 */
public class NodeJarBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 12, 13);

    public NodeJarBlock(Properties properties) {
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
        return new Jar(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof Jar jar && stack.hasTag()) {
            jar.load(stack.getTag());
            jar.sync();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof Jar jar && !jar.aspects.isEmpty()) {
            return List.of(jar.toItem());
        }
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof Jar jar ? jar.toItem() : new ItemStack(this);
    }

    // ---------- сборка ----------

    /**
     * WandManager.fitNodeJar оригинала: вокруг узла, которого коснулся жезл (или блока рядом с ним), — короб 3×3×3
     * из стекла, накрытый девятью деревянными плитами. Возвращает узел или null.
     */
    @Nullable
    public static BlockPos find(Level level, BlockPos clicked) {
        for (BlockPos origin : BlockPos.betweenClosed(clicked.offset(-2, -3, -2), clicked)) {
            BlockPos node = origin.offset(1, 1, 1);
            if (fits(level, origin.immutable()) && level.getBlockEntity(node) instanceof NodeBlockEntity) {
                return node.immutable();
            }
        }
        return null;
    }

    private static boolean fits(Level level, BlockPos origin) {
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    BlockState state = level.getBlockState(origin.offset(x, y, z));
                    if (y == 3) {
                        if (!state.is(net.minecraft.tags.BlockTags.WOODEN_SLABS)) {
                            return false;
                        }
                    } else if (x == 1 && y == 1 && z == 1) {
                        if (!(level.getBlockEntity(origin.offset(x, y, z)) instanceof NodeBlockEntity)) {
                            return false;
                        }
                    } else if (!state.is(Blocks.GLASS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** replaceNodeJar оригинала: стекло и плиты исчезают, узел становится банкой; в банке он почти всегда тускнеет. */
    public static void assemble(ServerLevel level, BlockPos nodePos) {
        if (!(level.getBlockEntity(nodePos) instanceof NodeBlockEntity node)) {
            return;
        }
        AspectList aspects = new AspectList();
        AspectList base = new AspectList();
        for (Aspect aspect : node.getAspects()) {
            aspects.add(aspect, node.getAmount(aspect));
            base.add(aspect, node.getBase(aspect));
        }
        NodeType type = node.getNodeType();
        NodeModifier modifier = node.getNodeModifier();
        if (level.random.nextFloat() < 0.75f) {
            if (modifier == null) {
                modifier = NodeModifier.PALE;
            } else if (modifier == NodeModifier.BRIGHT) {
                modifier = null;
            } else if (modifier == NodeModifier.PALE) {
                modifier = NodeModifier.FADING;
            }
        }
        BlockPos origin = nodePos.offset(-1, -1, -1);
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(2, 3, 2))) {
            if (!pos.equals(nodePos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.removeBlockEntity(nodePos);
        level.setBlock(nodePos, ModBlocks.NODE_JAR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(nodePos) instanceof Jar jar) {
            jar.aspects = aspects;
            jar.base = base;
            jar.type = type;
            jar.modifier = modifier;
            jar.sync();
        }
        level.blockEvent(nodePos, ModBlocks.NODE_JAR.get(), 9, 0);
        level.playSound(null, nodePos, thaumcraft.common.registry.ModSounds.WAND.get(), SoundSource.BLOCKS, 1f, 1f);
    }

    /** Событие 9: банка с узлом вспыхивает искрами и на мгновение раздувается. */
    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id != 9) {
            return super.triggerEvent(state, level, pos, id, param);
        }
        if (level.isClientSide) {
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 2, 1))) {
                thaumcraft.client.fx.Fx.blockSparkle(near.getX(), near.getY(), near.getZ(), -9999, 5);
            }
            if (level.getBlockEntity(pos) instanceof Jar jar) {
                jar.animate = System.currentTimeMillis() + 1000L;
            }
        }
        return true;
    }

    public static class Jar extends BlockEntity implements AspectContainer, Wandable {
        public AspectList aspects = new AspectList();
        public AspectList base = new AspectList();
        public NodeType type = NodeType.NORMAL;
        @Nullable
        public NodeModifier modifier;
        /** До какого времени банка «раздувается» после сборки (только на клиенте). */
        public long animate;
        @Nullable
        private NodeBlockEntity ghost;

        public Jar(BlockPos pos, BlockState state) {
            super(ModBlockEntities.NODE_JAR.get(), pos, state);
        }

        void sync() {
            setChanged();
            ghost = null;
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        /** Узел для рисования: тот же, что внутри банки, но в мир не добавлен. */
        public NodeBlockEntity ghost() {
            if (ghost == null) {
                ghost = new NodeBlockEntity(worldPosition, ModBlocks.AURA_NODE.get().defaultBlockState());
                fill(ghost);
            }
            return ghost;
        }

        private void fill(NodeBlockEntity node) {
            node.setNodeType(type);
            node.setNodeModifier(modifier);
            node.setAspects(base);
            for (Aspect aspect : base.getAspects()) {
                node.setBase(aspect, base.getAmount(aspect));
            }
            for (Aspect aspect : base.getAspects()) {
                int missing = base.getAmount(aspect) - aspects.getAmount(aspect);
                if (missing > 0) {
                    node.takeFromContainer(aspect, missing);
                }
            }
        }

        public ItemStack toItem() {
            ItemStack stack = new ItemStack(ModItems.NODE_JAR.get());
            CompoundTag tag = new CompoundTag();
            write(tag);
            stack.setTag(tag);
            return stack;
        }

        /** onWandRightClick оригинала: банка разбивается, узел выходит на волю на этом же месте. */
        @Override
        public InteractionResult onWand(UseOnContext context) {
            Level world = context.getLevel();
            if (!world.isClientSide) {
                world.levelEvent(2001, worldPosition, Block.getId(getBlockState()));
                world.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1f, 0.9f + world.random.nextFloat() * 0.2f);
                AspectList keep = aspects;
                AspectList keepBase = base;
                NodeType keepType = type;
                NodeModifier keepModifier = modifier;
                world.removeBlockEntity(worldPosition);
                world.setBlock(worldPosition, ModBlocks.AURA_NODE.get().defaultBlockState(), 3);
                if (world.getBlockEntity(worldPosition) instanceof NodeBlockEntity node) {
                    aspects = keep;
                    base = keepBase;
                    type = keepType;
                    modifier = keepModifier;
                    fill(node);
                    node.nodeChange();
                }
            }
            if (context.getPlayer() != null) {
                context.getPlayer().swing(context.getHand());
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        @Override
        public AspectList getContainedAspects() {
            return aspects;
        }

        private void write(CompoundTag tag) {
            tag.put("Aspects", aspects.save(new CompoundTag()));
            tag.put("AspectsBase", base.save(new CompoundTag()));
            tag.putByte("type", (byte) type.ordinal());
            tag.putByte("modifier", (byte) (modifier == null ? -1 : modifier.ordinal()));
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            write(tag);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            aspects = AspectList.load(tag.getCompound("Aspects"));
            base = AspectList.load(tag.getCompound("AspectsBase"));
            type = NodeType.values()[Math.max(0, Math.min(NodeType.values().length - 1, tag.getByte("type")))];
            int mod = tag.getByte("modifier");
            modifier = mod >= 0 && mod < NodeModifier.values().length ? NodeModifier.values()[mod] : null;
            ghost = null;
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
}
