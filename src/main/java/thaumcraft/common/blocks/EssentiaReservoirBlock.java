package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectContainer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.EssentiaTransport;
import thaumcraft.api.wands.Wandable;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModSounds;

/**
 * Резервуар эссенции — перенос BlockEssentiaReservoir и TileEssentiaReservoir оригинала. Держит до 256 единиц
 * эссенции любых аспектов вперемешку; труба подключается к одной грани (жезлом её выбирают: щелчок — грань напротив,
 * крадучись — та, по которой щёлкнули). Сам тянет эссенцию из трубы, а отдаёт только тот аспект, который у него
 * просят; служит источником для матрицы наполнения и зеркал эссенции. Разбитый полный резервуар взрывается скверной.
 */
public class EssentiaReservoirBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    public EssentiaReservoirBlock(Properties properties) {
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
        return new Reservoir(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ESSENTIA_RESERVOIR.get(), (l, p, s, reservoir) -> reservoir.tick());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Reservoir reservoir) {
            int size = reservoir.essentia.visSize();
            return Mth.floor(size / (float) Reservoir.MAX * 14f) + (size > 0 ? 1 : 0);
        }
        return 0;
    }

    /** breakBlock оригинала: каждые 16 единиц эссенции оборачиваются клоком скверны, и резервуар хлопает. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!newState.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof Reservoir reservoir) {
            int size = reservoir.essentia.visSize() / 16;
            if (size > 0) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1f, Level.ExplosionInteraction.NONE);
                int placed = 0;
                for (int a = 0; a < 50; a++) {
                    BlockPos near = pos.offset(level.random.nextInt(5) - level.random.nextInt(5), level.random.nextInt(5) - level.random.nextInt(5),
                            level.random.nextInt(5) - level.random.nextInt(5));
                    if (level.isEmptyBlock(near)) {
                        level.setBlock(near, (near.getY() < pos.getY() ? ModBlocks.FLUX_GOO : ModBlocks.FLUX_GAS).get().defaultBlockState()
                                .setValue(FluxBlock.LEVEL, 7), 3);
                        if (placed++ >= size) {
                            break;
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    public static class Reservoir extends BlockEntity implements EssentiaTransport, AspectContainer, Wandable {
        public static final int MAX = 256;
        public AspectList essentia = new AspectList();
        public Direction facing = Direction.DOWN;
        private int count;
        // цвет жидкости на клиенте плавно перетекает от аспекта к аспекту
        public float cr = 1f;
        public float cg = 1f;
        public float cb = 1f;
        private float tri;
        private float tgi;
        private float tbi;
        @Nullable
        public Aspect displayAspect;

        public Reservoir(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ESSENTIA_RESERVOIR.get(), pos, state);
        }

        void tick() {
            count++;
            if (!level.isClientSide) {
                if (count % 5 == 0 && essentia.visSize() < MAX) {
                    fill();
                }
                return;
            }
            int size = essentia.visSize();
            if (size <= 0) {
                return;
            }
            if (level.random.nextInt(Math.max(1, 500 - size)) == 0) {
                level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, ModSounds.CREAK.get(),
                        SoundSource.BLOCKS, 1f, 1.4f + level.random.nextFloat() * 0.2f, false);
            }
            if (count % 20 == 0 && essentia.size() > 0) {
                displayAspect = essentia.getAspects().get(count / 20 % essentia.size());
                int c = displayAspect.getColor();
                tri = (cr - (c >> 16 & 255) / 255f) / 20f;
                tgi = (cg - (c >> 8 & 255) / 255f) / 20f;
                tbi = (cb - (c & 255) / 255f) / 20f;
            }
            if (displayAspect != null) {
                cr -= tri;
                cg -= tgi;
                cb -= tbi;
            }
        }

        /** fillReservoir оригинала: по единице из подключённой трубы, если наша тяга сильнее. */
        private void fill() {
            EssentiaTransport source = EssentiaTransport.neighbour(level, worldPosition, facing);
            if (source == null || !source.canOutputTo(facing.getOpposite())) {
                return;
            }
            Direction back = facing.getOpposite();
            if (source.getEssentiaAmount(back) > 0 && source.getSuctionAmount(back) < getSuctionAmount(facing)
                    && getSuctionAmount(facing) >= source.getMinimumSuction()) {
                Aspect type = source.getEssentiaType(back);
                if (type != null) {
                    add(type, source.takeEssentia(type, 1, back));
                }
            }
        }

        /** @return сколько не поместилось */
        public int add(Aspect aspect, int amount) {
            if (amount <= 0) {
                return amount;
            }
            int space = MAX - essentia.visSize();
            int added = Math.min(space, amount);
            if (added > 0) {
                essentia.add(aspect, added);
                sync();
            }
            return amount - added;
        }

        /** Источник эссенции (IAspectSource оригинала). */
        public boolean take(Aspect aspect, int amount) {
            if (essentia.getAmount(aspect) < amount) {
                return false;
            }
            essentia.reduce(aspect, amount);
            sync();
            return true;
        }

        private void sync() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public InteractionResult onWand(UseOnContext context) {
            if (level != null && !level.isClientSide) {
                Direction side = context.getClickedFace();
                facing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? side : side.getOpposite();
                sync();
            }
            if (context.getPlayer() != null) {
                context.getPlayer().swing(context.getHand());
            }
            return InteractionResult.sidedSuccess(level != null && level.isClientSide);
        }

        @Override
        public AspectList getContainedAspects() {
            return essentia;
        }

        @Override
        public boolean isConnectable(Direction side) {
            return side == facing;
        }

        @Override
        public boolean canInputFrom(Direction side) {
            return side == facing;
        }

        @Override
        public boolean canOutputTo(Direction side) {
            return side == facing;
        }

        @Override
        public void setSuction(@Nullable Aspect aspect, int amount) {
        }

        @Override
        public Aspect getSuctionType(@Nullable Direction side) {
            return null;
        }

        @Override
        public int getSuctionAmount(@Nullable Direction side) {
            return essentia.visSize() < MAX ? 24 : 0;
        }

        @Override
        public int takeEssentia(Aspect aspect, int amount, Direction side) {
            return canOutputTo(side) && take(aspect, amount) ? amount : 0;
        }

        @Override
        public int addEssentia(Aspect aspect, int amount, Direction side) {
            return canInputFrom(side) ? amount - add(aspect, amount) : 0;
        }

        /** Как в оригинале: свой аспект резервуар называет только «вообще», а не трубе, — сам он ничего не выталкивает. */
        @Override
        public Aspect getEssentiaType(@Nullable Direction side) {
            return essentia.visSize() > 0 && side == null ? essentia.getAspects().get(0) : null;
        }

        @Override
        public int getEssentiaAmount(@Nullable Direction side) {
            return essentia.visSize();
        }

        @Override
        public int getMinimumSuction() {
            return 24;
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            essentia.save(tag);
            tag.putByte("face", (byte) facing.get3DDataValue());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            essentia = AspectList.load(tag);
            if (essentia.visSize() > MAX) {
                essentia = new AspectList();
            }
            facing = Direction.from3DDataValue(tag.getByte("face"));
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
