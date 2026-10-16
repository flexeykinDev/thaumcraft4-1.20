package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blockentity.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.items.wands.WandItem;
import thaumcraft.common.registry.ModBlockEntities;

/**
 * Зарядное реле (BlockMetalDevice оригинала, мета 2, TileMagicWorkbenchCharger): реле сети вис, которое ставится
 * над магическим верстаком и подзаряжает лежащий в нём жезл — до 0.05 вис каждой стихии за такт.
 * Сам кристалл висит в верхней половине блока на четырёх опорах.
 */
public class VisChargeRelayBlock extends VisRelayBlock {
    public VisChargeRelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargeRelay(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.VIS_CHARGE_RELAY.get(),
                level.isClientSide ? Relay::clientTick : ChargeRelay::serverTick);
    }

    public static class ChargeRelay extends Relay {
        public ChargeRelay(BlockPos pos, BlockState state) {
            super(ModBlockEntities.VIS_CHARGE_RELAY.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, ChargeRelay relay) {
            Relay.serverTick(level, pos, state, relay);
            if (!(level.getBlockEntity(pos.below()) instanceof ArcaneWorkbenchBlockEntity workbench)) {
                return;
            }
            ItemStack wand = workbench.getWand();
            if (!(wand.getItem() instanceof WandItem)) {
                return;
            }
            boolean charged = false;
            for (Aspect aspect : WandItem.getAspectsWithRoom(wand)) {
                int room = Math.min(5, WandItem.getMaxVis(wand) - WandItem.getVis(wand, aspect));
                if (room > 0) {
                    int got = relay.consumeVis(aspect, room);
                    if (got > 0) {
                        WandItem.storeVis(wand, aspect, WandItem.getVis(wand, aspect) + got);
                        charged = true;
                    }
                }
            }
            if (charged) {
                workbench.setChanged();
            }
        }
    }
}
