package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;

/**
 * Пустота — перенос BlockEldritchNothing и TileEldritchNothing оригинала: то, из чего сложен «скелет» лабиринта
 * Запредельных земель за его каменными стенами. Неразрушима, её нельзя выделить; пока она скрыта, её не видно, а
 * стоит открыть её грань (разбить стену) — сквозь неё проступает звёздная бездна. Всё, что в неё влезет (кроме
 * игроков в творческом режиме), бездна рвёт на части — по 8 урона.
 * <p>
 * Мета 0 и 1 оригинала здесь — два блока: скрытая Пустота (простой блок, таких в лабиринте десятки тысяч) и
 * открытая ({@link Open}, с блок-сущностью, которая рисует бездну). Они сами превращаются друг в друга, когда
 * рядом что-то ломают или ставят.
 */
public class EldritchVoidBlock extends Block {
    private static final VoxelShape COLLISION = Block.box(2, 2, 2, 14, 14, 14);

    public EldritchVoidBlock(Properties properties) {
        super(properties);
    }

    /** Открыта ли грань: соседа нет или он не целый непрозрачный куб (другая Пустота считается непрозрачной). */
    public static boolean faceOpen(BlockGetter level, BlockPos pos, Direction side) {
        BlockPos other = pos.relative(side);
        BlockState state = level.getBlockState(other);
        return !(state.getBlock() instanceof EldritchVoidBlock) && !state.isSolidRender(level, other);
    }

    private static boolean exposed(BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (faceOpen(level, pos, side)) {
                return true;
            }
        }
        return false;
    }

    private static BlockState form(BlockGetter level, BlockPos pos) {
        return (exposed(level, pos) ? ModBlocks.ELDRITCH_VOID_OPEN : ModBlocks.ELDRITCH_VOID).get().defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return form(level, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        BlockState now = form(level, pos);
        if (!now.is(state.getBlock())) {
            level.setBlock(pos, now, 3);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Её не выделить взглядом. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.tickCount > 20 && !(entity instanceof Player player && player.isCreative())) {
            entity.hurt(level.damageSources().fellOutOfWorld(), 8f);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.SpawnPlacements.Type type,
                                net.minecraft.world.entity.EntityType<?> entityType) {
        return false;
    }

    /** Открытая Пустота: сквозь открытые грани видна бездна. */
    public static class Open extends EldritchVoidBlock implements EntityBlock {
        public Open(Properties properties) {
            super(properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Void(pos, state);
        }
    }

    public static class Void extends BlockEntity {
        public Void(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELDRITCH_VOID.get(), pos, state);
        }
    }
}
