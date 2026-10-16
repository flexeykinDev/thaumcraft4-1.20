package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.visnet.VisNetHandler;
import thaumcraft.api.wands.FocusUpgrade;
import thaumcraft.api.wands.Wandable;
import thaumcraft.common.aspects.ItemAspectManager;
import thaumcraft.common.items.tools.ElementalPickaxeItem;
import thaumcraft.common.items.tools.ToolHelper;
import thaumcraft.common.items.wands.FocusItem;
import thaumcraft.common.items.wands.foci.ExcavationFocus;
import thaumcraft.common.menu.ArcaneBoreMenu;
import thaumcraft.common.network.FxPacket;
import thaumcraft.common.registry.ModBlockEntities;
import thaumcraft.common.registry.ModBlocks;
import thaumcraft.common.registry.ModEnchantments;
import thaumcraft.common.registry.ModSounds;
import thaumcraft.common.research.ResearchHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Чародейский бур — перенос blockWoodenDevice:5 и TileArcaneBore оригинала. Ставится на основание (или под него),
 * в него вкладывают фокус раскопок и кирку; запитанный красным камнем, бур прогрызает перед собой тоннель,
 * обходя круг по спирали лучом. Фокус задаёт ширину («Расширение»), удачу («Сокровище»), шёлковое касание и
 * лозоходство, кирка — свою удачу, скорость («Эффективность») и шёлковое касание; кирка с «Починкой» чинится,
 * беря вис из сети. Пердицио по трубе в основание (или из сети вис) разгоняет бур вчетверо. Жезлом бур
 * поворачивают в любую из шести сторон.
 */
public class ArcaneBoreBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** Висит ли бур под основанием (иначе стоит на нём). */
    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");

    public ArcaneBoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING);
    }

    /** Бур ставится только сверху или снизу основания; копать он будет туда, куда смотрит поставивший. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        if (side.getAxis() != Direction.Axis.Y) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(HANGING, side == Direction.DOWN)
                .setValue(FACING, context.getNearestLookingDirection().getOpposite());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    private static BlockPos basePos(BlockState state, BlockPos pos) {
        return state.getValue(HANGING) ? pos.above() : pos.below();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(basePos(state, pos)).is(ModBlocks.ARCANE_BORE_BASE.get());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos from) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbour, level, pos, from);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof thaumcraft.common.items.wands.WandItem) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof Bore bore && player instanceof net.minecraft.server.level.ServerPlayer server) {
            NetworkHooks.openScreen(server, bore, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Bore bore) {
            net.minecraft.world.Containers.dropContents(level, pos, bore);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Bore(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ARCANE_BORE.get(), Bore::tick);
    }

    public static class Bore extends BlockEntity implements Container, MenuProvider, Wandable {
        public final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
        public final int maxRadius = 2;
        // анимация головки (rotX/rotZ — поворот к стороне раскопок, vRad — отклонение луча, topRotation — вертушка)
        public int rotX;
        public int rotZ;
        public int tarX;
        public int tarZ;
        public int speedX;
        public int speedZ;
        public float vRadX;
        public float vRadZ;
        private float tRadX;
        private float tRadZ;
        private float mRadX;
        private float mRadZ;
        public int topRotation;
        public int beamlength;
        public boolean hasFocus;
        public boolean hasPickaxe;
        public int fortune;
        public int speed;
        public int area;
        // раскопка
        private int spiral;
        private float currentRadius;
        private float radInc;
        private int lastX;
        private int lastY;
        private int lastZ;
        private boolean toDig;
        private BlockPos dig = BlockPos.ZERO;
        @Nullable
        private BlockState digState;
        private int count;
        private int paused = 100;
        private int maxPause = 100;
        private float speedyTime;
        private long repairCounter;
        private long soundDelay;
        private AspectList repairCost = new AspectList();
        private final AspectList repairVis = new AspectList();
        private boolean first = true;

        public Bore(BlockPos pos, BlockState state) {
            super(ModBlockEntities.ARCANE_BORE.get(), pos, state);
        }

        public Direction facing() {
            return getBlockState().getValue(FACING);
        }

        public boolean hanging() {
            return getBlockState().getValue(HANGING);
        }

        @Nullable
        private ArcaneBoreBaseBlock.Base base() {
            return level.getBlockEntity(basePos(getBlockState(), worldPosition)) instanceof ArcaneBoreBaseBlock.Base base ? base : null;
        }

        private ItemStack focus() {
            return items.get(0);
        }

        private ItemStack pickaxe() {
            return items.get(1);
        }

        /** Свойства бура от фокуса и кирки (markDirty оригинала). */
        private void recalc() {
            fortune = 0;
            area = 0;
            speed = 0;
            hasFocus = focus().getItem() instanceof ExcavationFocus;
            if (hasFocus) {
                fortune = FocusItem.getUpgradeLevel(focus(), FocusUpgrade.TREASURE);
                area = FocusItem.getUpgradeLevel(focus(), FocusUpgrade.ENLARGE);
                speed += FocusItem.getUpgradeLevel(focus(), FocusUpgrade.POTENCY);
            }
            hasPickaxe = pickaxe().getItem() instanceof PickaxeItem;
            if (hasPickaxe) {
                fortune = Math.max(fortune, pickaxe().getEnchantmentLevel(Enchantments.BLOCK_FORTUNE));
                speed += pickaxe().getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY);
            }
        }

        public boolean nativeClusters() {
            return pickaxe().getItem() instanceof ElementalPickaxeItem || hasFocus && FocusItem.isUpgradedWith(focus(), FocusUpgrade.DOWSING);
        }

        public boolean silkTouch() {
            return pickaxe().getEnchantmentLevel(Enchantments.SILK_TOUCH) > 0 || hasFocus && FocusItem.isUpgradedWith(focus(), FocusUpgrade.SILK_TOUCH);
        }

        /** Кирка вот-вот сломается — бур бережёт её и стоит. */
        public boolean pickaxeWorn() {
            return hasPickaxe && pickaxe().getDamageValue() + 1 >= pickaxe().getMaxDamage();
        }

        private boolean itemsValid() {
            return hasFocus && hasPickaxe && pickaxe().isDamageableItem() && !pickaxeWorn();
        }

        private boolean powered() {
            return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(basePos(getBlockState(), worldPosition));
        }

        /** setOrientation оригинала: головка разворачивается к новой стороне. */
        private void aim(boolean instant) {
            switch (facing()) {
                case DOWN -> {
                    tarZ = 180;
                    tarX = 0;
                }
                case UP -> {
                    tarZ = 0;
                    tarX = 0;
                }
                case NORTH -> {
                    tarZ = 90;
                    tarX = 270;
                }
                case SOUTH -> {
                    tarZ = 90;
                    tarX = 90;
                }
                case WEST -> {
                    tarZ = 90;
                    tarX = 0;
                }
                case EAST -> {
                    tarZ = 90;
                    tarX = 180;
                }
            }
            if (instant) {
                rotX = tarX;
                rotZ = tarZ;
            }
            lastX = 0;
            lastY = 0;
            lastZ = 0;
            toDig = false;
            radInc = 0f;
            paused = 100;
            tRadX = 0f;
            tRadZ = 0f;
            mRadX = 0f;
            mRadZ = 0f;
            dig = BlockPos.ZERO;
        }

        @Override
        public InteractionResult onWand(UseOnContext context) {
            if (level != null && !level.isClientSide) {
                level.setBlock(worldPosition, getBlockState().setValue(FACING, context.getClickedFace()), 3);
                aim(false);
                level.playSound(null, worldPosition, ModSounds.TOOL.get(), SoundSource.BLOCKS, 0.5f, 0.9f + level.random.nextFloat() * 0.2f);
                setChanged();
            }
            context.getPlayer().swing(context.getHand());
            return InteractionResult.sidedSuccess(level != null && level.isClientSide);
        }

        @Override
        public void setBlockState(BlockState state) {
            Direction old = getBlockState().getValue(FACING);
            super.setBlockState(state);
            if (old != state.getValue(FACING)) {
                aim(false);
            }
        }

        static void tick(Level level, BlockPos pos, BlockState state, Bore bore) {
            if (bore.first) {
                bore.recalc();
                bore.aim(true);
                bore.first = false;
            }
            if (!level.isClientSide && bore.speedyTime < 20f) {
                bore.speedyTime += VisNetHandler.drainVis(level, pos, Aspect.ENTROPY, 100) / 5f;
                ArcaneBoreBaseBlock.Base base = bore.base();
                if (bore.speedyTime < 20f && base != null && base.drawEssentia()) {
                    bore.speedyTime += 20f;
                }
            }
            bore.turnHead();
            if (bore.powered() && bore.itemsValid()) {
                bore.dig();
            } else if (level.isClientSide) {
                if (bore.topRotation % 90 != 0) {
                    bore.topRotation += Math.min(10, 90 - bore.topRotation % 90);
                }
                bore.vRadX *= 0.9f;
                bore.vRadZ *= 0.9f;
            }
            if (!level.isClientSide && bore.hasPickaxe) {
                bore.repair((ServerLevel) level);
            }
        }

        /** Головка плавно доворачивается к цели (rotX/rotZ к tarX/tarZ). */
        private void turnHead() {
            if (rotX < tarX) {
                rotX += speedX;
                if (rotX < tarX) {
                    speedX++;
                } else {
                    speedX = speedX / 3;
                }
            } else if (rotX > tarX) {
                rotX += speedX;
                if (rotX > tarX) {
                    speedX--;
                } else {
                    speedX = speedX / 3;
                }
            } else {
                speedX = 0;
            }
            if (rotZ < tarZ) {
                rotZ += speedZ;
                if (rotZ < tarZ) {
                    speedZ++;
                } else {
                    speedZ = speedZ / 3;
                }
            } else if (rotZ > tarZ) {
                rotZ += speedZ;
                if (rotZ > tarZ) {
                    speedZ--;
                } else {
                    speedZ = speedZ / 3;
                }
            } else {
                speedZ = 0;
            }
        }

        /** Кирка с «Починкой»: раз в две секунды чинится, если накоплено вис из сети (doRepair оригинала). */
        private void repair(ServerLevel level) {
            ItemStack pick = pickaxe();
            if (repairCounter++ % 40L == 0L && pick.isDamaged()) {
                int grade = Math.min(2, EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.REPAIR.get(), pick));
                if (grade > 0) {
                    AspectList primals = ResearchHelper.reduceToPrimals(ItemAspectManager.INSTANCE.getAspects(pick));
                    if (primals.isEmpty()) {
                        return;
                    }
                    repairCost = new AspectList();
                    for (Aspect aspect : primals.getAspects()) {
                        repairCost.merge(aspect, (int) Math.sqrt(primals.getAmount(aspect) * 2) * grade);
                    }
                    boolean paid = true;
                    for (Aspect aspect : repairCost.getAspects()) {
                        if (repairVis.getAmount(aspect) < repairCost.getAmount(aspect)) {
                            paid = false;
                            break;
                        }
                    }
                    if (paid) {
                        for (Aspect aspect : repairCost.getAspects()) {
                            repairVis.reduce(aspect, repairCost.getAmount(aspect));
                        }
                        pick.setDamageValue(Math.max(0, pick.getDamageValue() - grade));
                        setChanged();
                    }
                }
            }
            if (!repairCost.isEmpty() && repairCounter % 5L == 0L) {
                for (Aspect aspect : repairCost.getAspects()) {
                    if (repairVis.getAmount(aspect) < repairCost.getAmount(aspect)) {
                        repairVis.add(aspect, VisNetHandler.drainVis(level, worldPosition, aspect, repairCost.getAmount(aspect)));
                    }
                }
            }
        }

        private void dig() {
            if (rotX != tarX || rotZ != tarZ) {
                if (level.isClientSide) {
                    if (topRotation % 90 != 0) {
                        topRotation += Math.min(10, 90 - topRotation % 90);
                    }
                    vRadX *= 0.9f;
                    vRadZ *= 0.9f;
                }
                return;
            }
            if (!level.isClientSide) {
                if (--count > 0) {
                    return;
                }
                boolean dug = false;
                if (toDig) {
                    toDig = false;
                    dug = breakTarget((ServerLevel) level);
                }
                findNextBlockToDig();
                if (dug && speedyTime > 0f) {
                    speedyTime--;
                }
            } else {
                animate();
            }
        }

        private FakePlayer fakePlayer(ServerLevel level) {
            return FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("41c82c87-7afb-4024-ba57-13d2c99cae77"),
                    "FakeThaumcraftBore"));
        }

        /** Разрушение намеченного блока: добыча — в сундук у носика основания или на землю перед ним. */
        private boolean breakTarget(ServerLevel level) {
            BlockState state = level.getBlockState(dig);
            if (state.isAir() || state.getDestroySpeed(level, dig) < 0f) {
                return false;
            }
            FakePlayer player = fakePlayer(level);
            int tfortune = fortune;
            boolean silk = silkTouch();
            ItemStack tool = pickaxe().copy();
            if (silk) {
                tfortune = 0;
                tool.enchant(Enchantments.SILK_TOUCH, 1);
            } else if (tfortune > tool.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE)) {
                tool.enchant(Enchantments.BLOCK_FORTUNE, tfortune);
            }
            FxPacket.send(level, FxPacket.BORE_DUG, Vec3.atLowerCornerOf(worldPosition), Vec3.ZERO, Block.getId(state), 0);
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, dig, level.getBlockEntity(dig), player, tool));
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(dig).inflate(1.0))) {
                drops.add(item.getItem().copy());
                item.discard();
            }
            ArcaneBoreBaseBlock.Base base = base();
            for (ItemStack drop : drops) {
                ItemStack out = drop.copy();
                if (!silk && nativeClusters()) {
                    out = ToolHelper.specialMiningResult(out, 0.2f + tfortune * 0.075f, level.random);
                }
                if (base == null) {
                    continue;
                }
                Direction nozzle = base.facing();
                BlockEntity chest = level.getBlockEntity(base.getBlockPos().relative(nozzle));
                if (chest != null) {
                    IItemHandler handler = chest.getCapability(ForgeCapabilities.ITEM_HANDLER, nozzle.getOpposite()).orElse(null);
                    if (handler != null) {
                        out = ItemHandlerHelper.insertItemStacked(handler, out, false);
                    }
                }
                if (!out.isEmpty()) {
                    BlockPos basePos = base.getBlockPos();
                    ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5 + nozzle.getStepX() * 0.66,
                            basePos.getY() + 0.4, worldPosition.getZ() + 0.5 + nozzle.getStepZ() * 0.66, out);
                    entity.setDeltaMovement(0.075 * nozzle.getStepX(), 0.025, 0.075 * nozzle.getStepZ());
                    level.addFreshEntity(entity);
                }
            }
            state.spawnAfterBreak(level, dig, tool, true);
            pickaxe().hurtAndBreak(1, player, broken -> { });
            if (pickaxe().isEmpty()) {
                items.set(1, ItemStack.EMPTY);
                recalc();
            }
            level.removeBlock(dig, false);
            if (base != null) {
                lightTunnel(level, base);
            }
            setChanged();
            return true;
        }

        /** Чародейская лампа у основания расставляет огоньки вдоль прорытого тоннеля. */
        private void lightTunnel(ServerLevel level, ArcaneBoreBaseBlock.Base base) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos lampPos = base.getBlockPos().relative(side);
                if (!(level.getBlockEntity(lampPos) instanceof ArcaneLampBlock.Lamp)) {
                    continue;
                }
                Direction face = facing();
                int d = level.random.nextInt(32) * 2;
                int x = worldPosition.getX() + face.getStepX() + face.getStepX() * d;
                int y = worldPosition.getY() + face.getStepY() + face.getStepY() * d;
                int z = worldPosition.getZ() + face.getStepZ() + face.getStepZ() * d;
                int p = d / 2 % 4;
                int shift = p == 0 ? 3 : p != 1 && p != 3 ? -3 : 0;
                if (face.getStepX() != 0) {
                    z += shift;
                } else {
                    x += shift;
                }
                if (p == 3 && face.getStepY() == 0) {
                    y -= 2;
                }
                BlockPos light = new BlockPos(x, y, z);
                if (level.getBlockState(light).isAir() && !level.getBlockState(light).is(ModBlocks.LAMP_LIGHT.get())
                        && level.getMaxLocalRawBrightness(light) < 15) {
                    level.setBlock(light, ModBlocks.LAMP_LIGHT.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(light) instanceof ArcaneLampBlock.Light lit) {
                        lit.source = lampPos;
                    }
                }
                break;
            }
        }

        private static Vec3 rotateX(Vec3 v, float angle) {
            float c = Mth.cos(angle);
            float s = Mth.sin(angle);
            return new Vec3(v.x, v.y * c + v.z * s, v.z * c - v.y * s);
        }

        private static Vec3 rotateY(Vec3 v, float angle) {
            float c = Mth.cos(angle);
            float s = Mth.sin(angle);
            return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
        }

        private static Vec3 rotateZ(Vec3 v, float angle) {
            float c = Mth.cos(angle);
            float s = Mth.sin(angle);
            return new Vec3(v.x * c + v.y * s, v.y * c - v.x * s, v.z);
        }

        /** Следующий блок на спирали перед буром (findNextBlockToDig оригинала). */
        private void findNextBlockToDig() {
            Direction face = facing();
            if (radInc == 0f) {
                radInc = (maxRadius + area) / 360f;
            }
            int x = lastX;
            int y = lastY;
            int z = lastZ;
            while (x == lastX && z == lastZ && y == lastY) {
                spiral += 2;
                if (spiral >= 360) {
                    spiral -= 360;
                }
                currentRadius += radInc;
                if (currentRadius > maxRadius + area || currentRadius < -(maxRadius + area)) {
                    radInc *= -1f;
                }
                Vec3 source = new Vec3(worldPosition.getX() + face.getStepX() + 0.5, worldPosition.getY() + face.getStepY() + 0.5,
                        worldPosition.getZ() + face.getStepZ() + 0.5);
                Vec3 target = new Vec3(0.0, currentRadius, 0.0);
                target = rotateZ(target, spiral / 180f * Mth.PI);
                target = rotateY(target, Mth.HALF_PI * face.getStepX());
                target = rotateX(target, Mth.HALF_PI * face.getStepY());
                Vec3 result = source.add(target);
                x = Mth.floor(result.x);
                y = Mth.floor(result.y);
                z = Mth.floor(result.z);
            }
            lastX = x;
            lastY = y;
            lastZ = z;
            x += face.getStepX();
            y += face.getStepY();
            z += face.getStepZ();
            for (int depth = 0; depth < 64; depth++) {
                x += face.getStepX();
                y += face.getStepY();
                z += face.getStepZ();
                BlockPos at = new BlockPos(x, y, z);
                if (!level.isLoaded(at)) {
                    break;
                }
                BlockState state = level.getBlockState(at);
                if (state.getDestroySpeed(level, at) < 0f) {
                    break;
                }
                if (state.isAir() || state.getCollisionShape(level, at).isEmpty()) {
                    continue;
                }
                dig = at;
                count = Math.max(10 - speed, (int) (state.getDestroySpeed(level, at) * 2f) - speed * 2);
                if (speedyTime < 1f) {
                    count *= 4;
                }
                toDig = true;
                // по дороге к цели луч может упереться в блок поближе — тогда копать его
                Vec3 from = new Vec3(worldPosition.getX() + 0.5 + face.getStepX(), worldPosition.getY() + 0.5 + face.getStepY(),
                        worldPosition.getZ() + 0.5 + face.getStepZ());
                BlockHitResult hit = level.clip(new ClipContext(from, Vec3.atCenterOf(at), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockState near = level.getBlockState(hit.getBlockPos());
                    if (near.getDestroySpeed(level, hit.getBlockPos()) > -1f && !near.getCollisionShape(level, hit.getBlockPos()).isEmpty()) {
                        count = Math.max(10 - speed, (int) (near.getDestroySpeed(level, hit.getBlockPos()) * 2f) - speed * 2);
                        if (speedyTime < 1f) {
                            count *= 4;
                        }
                        dig = hit.getBlockPos();
                    }
                }
                int dx = dig.getX() - worldPosition.getX() + 64;
                int dy = dig.getY() - worldPosition.getY() + 64;
                int dz = dig.getZ() - worldPosition.getZ() + 64;
                if (level instanceof ServerLevel server) {
                    FxPacket.send(server, FxPacket.BORE_TARGET, Vec3.atLowerCornerOf(worldPosition), Vec3.ZERO,
                            (dx & 255) << 16 | (dy & 255) << 8 | dz & 255, 0);
                }
                break;
            }
        }

        /** На клиенте: новая цель для луча (getDigEvent оригинала). */
        public void setDigTarget(int packed) {
            if (level == null || !level.isClientSide) {
                return;
            }
            dig = worldPosition.offset((packed >> 16 & 255) - 64, (packed >> 8 & 255) - 64, (packed & 255) - 64);
            toDig = true;
            digState = level.getBlockState(dig);
        }

        /** Звук и крошки разрушенного блока. */
        public void dugFx(BlockState state) {
            Direction face = facing();
            level.playLocalSound(dig.getX() + 0.5, dig.getY() + 0.5, dig.getZ() + 0.5, state.getSoundType().getBreakSound(), SoundSource.BLOCKS,
                    (state.getSoundType().getVolume() + 1f) / 2f, state.getSoundType().getPitch() * 0.8f, false);
            for (int i = 0; i < 10; i++) {
                thaumcraft.client.fx.Fx.boreDig(dig, worldPosition.relative(face), state);
            }
        }

        /** Клиентская часть: луч обходит круг, головка крутится, гул бура. */
        private void animate() {
            Direction face = facing();
            paused++;
            if (paused < maxPause && soundDelay < System.currentTimeMillis()) {
                soundDelay = System.currentTimeMillis() + 1200L + level.random.nextInt(100);
                level.playLocalSound(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, ModSounds.RUMBLE.get(),
                        SoundSource.BLOCKS, 0.25f, 0.9f + level.random.nextFloat() * 0.2f, false);
            }
            if (beamlength > 0 && paused > maxPause) {
                beamlength--;
            }
            if (toDig) {
                paused = 0;
                beamlength = 64;
                BlockState state = level.getBlockState(dig);
                maxPause = 10 + Math.max(10 - speed, (int) (state.getDestroySpeed(level, dig) * 2f) - speed * 2);
                if (speedyTime <= 0f) {
                    maxPause *= 4;
                }
                toDig = false;
                double xd = worldPosition.getX() + 0.5 - (dig.getX() + 0.5);
                double yd = worldPosition.getY() + 0.5 - (dig.getY() + 0.5);
                double zd = worldPosition.getZ() + 0.5 - (dig.getZ() + 0.5);
                double flat = Math.sqrt(xd * xd + zd * zd);
                float rx = (float) (Math.atan2(zd, xd) * 180.0 / Math.PI);
                float rz = (float) (-(Math.atan2(yd, flat) * 180.0 / Math.PI)) + 90f;
                tRadX = Mth.wrapDegrees((float) rotX) + rx;
                if (face == Direction.EAST) {
                    if (tRadX > 180f) {
                        tRadX -= 360f;
                    }
                    if (tRadX < -180f) {
                        tRadX += 360f;
                    }
                }
                tRadZ = rz - rotZ;
                if (face.getAxis() == Direction.Axis.Y) {
                    tRadZ += 180f;
                    if (vRadX - tRadX >= 180f) {
                        vRadX -= 360f;
                    }
                    if (vRadX - tRadX <= -180f) {
                        vRadX += 360f;
                    }
                }
                mRadX = Math.abs((vRadX - tRadX) / 6f);
                mRadZ = Math.abs((vRadZ - tRadZ) / 6f);
            }
            if (paused < maxPause) {
                vRadX += vRadX < tRadX ? mRadX : vRadX > tRadX ? -mRadX : 0f;
                vRadZ += vRadZ < tRadZ ? mRadZ : vRadZ > tRadZ ? -mRadZ : 0f;
            } else {
                vRadX *= 0.9f;
                vRadZ *= 0.9f;
            }
            mRadX *= 0.9f;
            mRadZ *= 0.9f;
            float vx = rotX + 90 - vRadX;
            float vz = rotZ + 90 - vRadZ;
            float dX = Mth.sin(vx / 180f * Mth.PI) * Mth.cos(vz / 180f * Mth.PI);
            float dZ = Mth.cos(vx / 180f * Mth.PI) * Mth.cos(vz / 180f * Mth.PI);
            float dY = Mth.sin(vz / 180f * Mth.PI);
            Vec3 from = new Vec3(worldPosition.getX() + 0.5 + dX, worldPosition.getY() + 0.5 + dY, worldPosition.getZ() + 0.5 + dZ);
            Vec3 to = new Vec3(worldPosition.getX() + 0.5 + dX * beamlength, worldPosition.getY() + 0.5 + dY * beamlength,
                    worldPosition.getZ() + 0.5 + dZ * beamlength);
            int impact = 0;
            Vec3 end = to;
            if (beamlength > 0) {
                BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    end = hit.getLocation();
                    impact = 5;
                    BlockState hitState = level.getBlockState(hit.getBlockPos());
                    if (!hitState.isAir()) {
                        thaumcraft.client.fx.Fx.boreDig(hit.getBlockPos(), worldPosition.relative(face), hitState);
                    }
                }
            }
            topRotation += beamlength / 6;
            Vec3 centre = Vec3.atCenterOf(worldPosition);
            thaumcraft.client.fx.Fx.boreBeam(worldPosition.asLong() * 2, centre, end, 1, 65382, true, impact > 0 ? 2f : 0f, impact);
            thaumcraft.client.fx.Fx.boreBeam(worldPosition.asLong() * 2 + 1, centre, end, 2, 16746581, false, impact > 0 ? 2f : 0f, impact);
            if (digState != null && level.getBlockState(dig).isAir() && !digState.isAir()) {
                dugFx(digState);
                digState = null;
            }
        }

        // ---------- предметы ----------

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack result = ContainerHelper.removeItem(items, slot, amount);
            setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return ContainerHelper.takeItem(items, slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            super.setChanged();
            recalc();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            items.clear();
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("block.thaumcraft.arcane_bore");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new ArcaneBoreMenu(id, inventory, this);
        }

        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            ContainerHelper.saveAllItems(tag, items);
            tag.putShort("SpeedyTime", (short) speedyTime);
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            items.clear();
            ContainerHelper.loadAllItems(tag, items);
            speedyTime = tag.getShort("SpeedyTime");
            recalc();
        }

        @Override
        public CompoundTag getUpdateTag() {
            CompoundTag tag = super.getUpdateTag();
            ContainerHelper.saveAllItems(tag, items);
            return tag;
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public AABB getRenderBoundingBox() {
            return new AABB(worldPosition).inflate(1.0);
        }
    }
}
