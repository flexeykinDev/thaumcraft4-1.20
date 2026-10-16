package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import thaumcraft.common.entity.EldritchMob;
import thaumcraft.common.registry.ModBlockEntities;

import java.util.List;

/**
 * Обелиск Элдрича — перенос BlockEldritch (мета 1, 2 и 3), TileEldritchObelisk и TileEldritchCap оригинала.
 * Основание обелиска рисует весь парящий столб из звёздной пустоты; четыре звена над ним — невидимая опора;
 * навершие — колпак, каких четыре стоит кольцом у алтаря. Существ Элдрича рядом с основанием обелиск лечит и
 * укрепляет. Ничего из этого не добывается: разрушение любой части обрушивает весь обелиск со взрывом.
 */
public class EldritchObeliskBlock extends BaseEntityBlock {
    public enum Part { BASE, SEGMENT, CAP }

    private final Part part;

    public EldritchObeliskBlock(Properties properties, Part part) {
        super(properties);
        this.part = part;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return part == Part.SEGMENT ? RenderShape.INVISIBLE : RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (part) {
            case BASE -> new Obelisk(pos, state);
            case CAP -> new Cap(pos, state);
            default -> null;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return part == Part.BASE ? createTickerHelper(type, ModBlockEntities.ELDRITCH_OBELISK.get(), (l, p, s, obelisk) -> obelisk.tick()) : null;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    /** breakBlock оригинала: вместе с этим блоком рушатся алтарь, колпаки и обелиск вокруг, с хлопком. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!newState.is(this) && !level.isClientSide) {
            collapse(level, pos);
        }
    }

    public static void collapse(Level level, BlockPos pos) {
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
            Block block = level.getBlockState(near).getBlock();
            if (block instanceof EldritchObeliskBlock || block instanceof EldritchAltarBlock) {
                level.setBlock(near, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1f, Level.ExplosionInteraction.NONE);
    }

    /** GenCommon.genObelisk оригинала: основание и четыре звена над ним. */
    public static void place(Level level, BlockPos base) {
        level.setBlock(base, thaumcraft.common.registry.ModBlocks.ELDRITCH_OBELISK.get().defaultBlockState(), 3);
        for (int i = 1; i <= 4; i++) {
            level.setBlock(base.above(i), thaumcraft.common.registry.ModBlocks.ELDRITCH_OBELISK_SEGMENT.get().defaultBlockState(), 3);
        }
    }

    /** TileEldritchObelisk: существам Элдрича в шести блоках — сила и регенерация; к ним тянутся огоньки. */
    public static class Obelisk extends BlockEntity {
        private int counter;

        public Obelisk(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELDRITCH_OBELISK.get(), pos, state);
        }

        void tick() {
            AABB area = new AABB(worldPosition).inflate(6);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, area, e -> e instanceof EldritchMob);
            if (!level.isClientSide) {
                if (counter++ % 20 == 0) {
                    for (LivingEntity mob : near) {
                        if (!mob.hasEffect(MobEffects.REGENERATION)) {
                            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, true));
                            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, true, true));
                        }
                    }
                }
            } else {
                for (LivingEntity mob : near) {
                    thaumcraft.client.fx.Fx.wispTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1 + level.random.nextFloat() * 3f,
                            worldPosition.getZ() + 0.5, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 1f, 5, true, 1f);
                }
            }
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition, worldPosition.offset(1, 5, 1));
        }
    }

    /** TileEldritchCap: навершие без поведения, только рисуется. */
    public static class Cap extends BlockEntity {
        public Cap(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ELDRITCH_CAP.get(), pos, state);
        }
    }
}
