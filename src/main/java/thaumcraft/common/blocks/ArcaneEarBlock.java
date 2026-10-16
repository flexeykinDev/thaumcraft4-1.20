package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.NoteBlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Чародейское ухо — перенос blockWoodenDevice:1 и TileSensor оригинала. Ухо настраивается щелчком на одну из 25 нот
 * и слушает нотные блоки в 64 блоках вокруг: услышав свою ноту на своём инструменте, оно на полсекунды даёт
 * сигнал красного камня. Инструмент уха, как у нотного блока, задаёт блок под ним.
 */
public class ArcaneEarBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(4, 3, 4, 12, 16, 12),
            Block.box(4, 8, 1, 12, 16, 3), Block.box(5, 8, 3, 11, 15, 4), Block.box(1, 8, 4, 3, 16, 12), Block.box(3, 8, 5, 4, 15, 11),
            Block.box(4, 8, 13, 12, 16, 15), Block.box(5, 8, 12, 11, 15, 13), Block.box(13, 8, 4, 15, 16, 12), Block.box(12, 8, 5, 13, 15, 11));
    /** Сыгранные в этот такт ноты по мирам (noteBlockEvents оригинала). */
    private static final Map<Level, List<Heard>> HEARD = new WeakHashMap<>();

    private record Heard(BlockPos pos, NoteBlockInstrument instrument, int note) {
    }

    public ArcaneEarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Sensor(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ARCANE_EAR.get(), Sensor::tick);
    }

    /** Щелчок поднимает ноту на полтона и даёт её услышать. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Sensor sensor) {
            sensor.note = (byte) ((sensor.note + 1) % 25);
            sensor.setChanged();
            sensor.trigger(true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Звук и нотка над ухом: id — инструмент (или -1, когда ухо лишь услышало ноту), param — нота. */
    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id >= 0 && id < NoteBlockInstrument.values().length) {
            NoteBlockInstrument instrument = NoteBlockInstrument.values()[id];
            level.playSeededSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, instrument.getSoundEvent(), SoundSource.RECORDS,
                    3f, NoteBlock.getPitchFromNote(param), level.random.nextLong());
        }
        level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, param / 24.0, 0.0, 0.0);
        return true;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    public static class Sensor extends BlockEntity {
        public byte note;
        private int signal;

        public Sensor(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ARCANE_EAR.get(), pos, state);
        }

        private NoteBlockInstrument instrument() {
            return level.getBlockState(worldPosition.below()).instrument();
        }

        /** triggerNote: нота звучит, только если над ухом пусто. */
        void trigger(boolean sound) {
            if (level.getBlockState(worldPosition.above()).isAir()) {
                level.blockEvent(worldPosition, getBlockState().getBlock(), sound ? instrument().ordinal() : -1, note);
            }
        }

        private void power(boolean on) {
            BlockState state = getBlockState();
            if (state.getValue(POWERED) != on) {
                level.setBlock(worldPosition, state.setValue(POWERED, on), 3);
                level.updateNeighborsAt(worldPosition.below(), state.getBlock());
            }
        }

        static void tick(Level level, BlockPos pos, BlockState state, Sensor sensor) {
            if (sensor.signal > 0 && --sensor.signal == 0) {
                sensor.power(false);
            }
            List<Heard> heard = HEARD.get(level);
            if (heard == null) {
                return;
            }
            NoteBlockInstrument instrument = sensor.instrument();
            for (Heard note : heard) {
                if (note.instrument() == instrument && note.note() == sensor.note && note.pos().getCenter().distanceToSqr(pos.getCenter()) <= 4096.0) {
                    sensor.trigger(false);
                    sensor.signal = 10;
                    sensor.power(true);
                    break;
                }
            }
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            tag.putByte("note", note);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            note = (byte) Math.max(0, Math.min(24, tag.getByte("note")));
        }
    }

    @Mod.EventBusSubscriber(modid = Thaumcraft.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void noteBlock(NoteBlockEvent.Play event) {
            if (event.getLevel() instanceof Level level && !level.isClientSide) {
                HEARD.computeIfAbsent(level, key -> new ArrayList<>())
                        .add(new Heard(event.getPos().immutable(), event.getInstrument(), event.getVanillaNoteId()));
            }
        }

        /** Ноты слышны только в тот такт, когда прозвучали. */
        @SubscribeEvent
        public static void levelTick(TickEvent.LevelTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.level.isClientSide) {
                HEARD.remove(event.level);
            }
        }
    }
}
