package thaumcraft.common.world.outer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import thaumcraft.common.blocks.StrangeCrystalsBlock;
import thaumcraft.common.registry.ModBlocks;

import java.util.Random;

/**
 * Мир глазами генераторов комнат Запредельных земель (GenCommon, GenPortal, GenPassage... оригинала). Генераторы
 * перенесены почти дословно, поэтому им оставлен старый способ называть блоки — «блок и мета» (см. {@link Old});
 * здесь они переводятся в блоки 1.20. Работает и при создании чанка, и в живом мире (зал хранителя достраивается,
 * когда открывают замок).
 */
public final class OuterWorld {
    /** Блоки 1.7.10, которыми пользуются генераторы комнат. */
    public enum Old { AIR, BEDROCK, WEB, MOB_SPAWNER, ELDRITCH, ELDRITCH_NOTHING, ELDRITCH_PORTAL, COSMETIC_SOLID, STAIRS_ELDRITCH, SLAB_STONE,
        AIRY, CRYSTAL, LOOT_URN, LOOT_CRATE, TAINT, TAINT_FIBRES, OTHER }

    /** ForgeDirection оригинала: те же номера (0 низ, 1 верх, 2 север, 3 юг, 4 запад, 5 восток) и смещения. */
    public enum Dir {
        DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0), UNKNOWN(0, 0, 0);

        public static final Dir[] VALID_DIRECTIONS = {DOWN, UP, NORTH, SOUTH, WEST, EAST};
        public final int offsetX;
        public final int offsetY;
        public final int offsetZ;

        Dir(int x, int y, int z) {
            offsetX = x;
            offsetY = y;
            offsetZ = z;
        }

        public Dir getOpposite() {
            return switch (this) {
                case DOWN -> UP;
                case UP -> DOWN;
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case EAST -> WEST;
                default -> UNKNOWN;
            };
        }

        public static Dir getOrientation(int i) {
            return i >= 0 && i < 6 ? VALID_DIRECTIONS[i] : UNKNOWN;
        }

        public Direction toDirection() {
            return this == UNKNOWN ? Direction.UP : Direction.from3DDataValue(ordinal());
        }
    }

    /** ChunkCoordinates оригинала: точка, которую можно класть в списки и сравнивать. */
    public static final class ChunkCoordinates {
        public final int posX;
        public final int posY;
        public final int posZ;

        public ChunkCoordinates(int x, int y, int z) {
            posX = x;
            posY = y;
            posZ = z;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ChunkCoordinates c && c.posX == posX && c.posY == posY && c.posZ == posZ;
        }

        @Override
        public int hashCode() {
            return (posX * 31 + posY) * 31 + posZ;
        }
    }

    public final ServerLevelAccessor level;
    public final Random rand;

    public OuterWorld(ServerLevelAccessor level, Random rand) {
        this.level = level;
        this.rand = rand;
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    // ---------- блоки ----------

    /** Блок 1.20 для «блока и меты» оригинала. */
    public static BlockState state(Old block, int meta) {
        return switch (block) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            case WEB -> Blocks.COBWEB.defaultBlockState();
            case MOB_SPAWNER -> Blocks.SPAWNER.defaultBlockState();
            case ELDRITCH -> switch (meta) {
                case 0 -> ModBlocks.ELDRITCH_ALTAR.get().defaultBlockState();
                case 1 -> ModBlocks.ELDRITCH_OBELISK.get().defaultBlockState();
                case 2 -> ModBlocks.ELDRITCH_OBELISK_SEGMENT.get().defaultBlockState();
                case 3 -> ModBlocks.ELDRITCH_CAPSTONE.get().defaultBlockState();
                case 4 -> ModBlocks.GLOWING_CRUSTED_STONE.get().defaultBlockState();
                case 5 -> ModBlocks.GLYPHED_STONE.get().defaultBlockState();
                case 7 -> ModBlocks.ANCIENT_DOORWAY.get().defaultBlockState();
                case 8 -> ModBlocks.ANCIENT_LOCK.get().defaultBlockState();
                case 9 -> ModBlocks.CRUSTED_OPENING.get().defaultBlockState();
                case 10 -> ModBlocks.RUNED_STONE.get().defaultBlockState();
                default -> ModBlocks.GLOWING_CRUSTED_STONE.get().defaultBlockState();
            };
            case ELDRITCH_NOTHING -> ModBlocks.ELDRITCH_VOID.get().defaultBlockState();
            case ELDRITCH_PORTAL -> ModBlocks.ELDRITCH_PORTAL.get().defaultBlockState();
            case COSMETIC_SOLID -> switch (meta) {
                case 0 -> ModBlocks.OBSIDIAN_TILE.get().defaultBlockState();
                case 1 -> ModBlocks.OBSIDIAN_TOTEM.get().defaultBlockState();
                case 12 -> ModBlocks.ANCIENT_ROCK.get().defaultBlockState();
                case 13 -> ModBlocks.ANCIENT_STONE_SEALED.get().defaultBlockState();
                case 14 -> ModBlocks.CRUSTED_STONE.get().defaultBlockState();
                case 15 -> ModBlocks.ANCIENT_PEDESTAL.get().defaultBlockState();
                default -> ModBlocks.ANCIENT_STONE.get().defaultBlockState();
            };
            case STAIRS_ELDRITCH -> {
                Direction facing = switch (meta & 3) {
                    case 0 -> Direction.EAST;
                    case 1 -> Direction.WEST;
                    case 2 -> Direction.SOUTH;
                    default -> Direction.NORTH;
                };
                yield ModBlocks.ANCIENT_STONE_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, facing)
                        .setValue(StairBlock.HALF, (meta & 4) != 0 ? Half.TOP : Half.BOTTOM);
            }
            case SLAB_STONE -> ((meta & 7) == 0 ? ModBlocks.ARCANE_STONE_SLAB : ModBlocks.ANCIENT_STONE_SLAB).get().defaultBlockState()
                    .setValue(SlabBlock.TYPE, (meta & 8) != 0 ? SlabType.TOP : SlabType.BOTTOM);
            case AIRY -> meta == 11 ? ModBlocks.SAPPING_FIELD.get().defaultBlockState() : ModBlocks.IMPASSABLE.get().defaultBlockState();
            case CRYSTAL -> ModBlocks.STRANGE_CRYSTALS.get().defaultBlockState();
            case LOOT_URN -> (meta == 2 ? ModBlocks.LOOT_URN_RARE : meta == 1 ? ModBlocks.LOOT_URN_UNCOMMON : ModBlocks.LOOT_URN_COMMON).get()
                    .defaultBlockState();
            case LOOT_CRATE -> (meta == 2 ? ModBlocks.LOOT_CRATE_RARE : meta == 1 ? ModBlocks.LOOT_CRATE_UNCOMMON : ModBlocks.LOOT_CRATE_COMMON)
                    .get().defaultBlockState();
            case TAINT -> (meta == 1 ? ModBlocks.TAINTED_SOIL : ModBlocks.TAINT_CRUST).get().defaultBlockState();
            case TAINT_FIBRES -> (meta == 1 ? ModBlocks.TAINT_GRASS : ModBlocks.TAINT_FIBRES).get().defaultBlockState();
            case OTHER -> Blocks.STONE.defaultBlockState();
        };
    }

    /** Какой это блок по меркам оригинала. */
    public static Old kind(BlockState state) {
        Block b = state.getBlock();
        if (state.isAir()) {
            return Old.AIR;
        }
        if (b == Blocks.BEDROCK) {
            return Old.BEDROCK;
        }
        if (b instanceof thaumcraft.common.blocks.EldritchVoidBlock) {
            return Old.ELDRITCH_NOTHING;
        }
        if (b == ModBlocks.ELDRITCH_PORTAL.get()) {
            return Old.ELDRITCH_PORTAL;
        }
        if (b == ModBlocks.ELDRITCH_ALTAR.get() || b == ModBlocks.ELDRITCH_OBELISK.get() || b == ModBlocks.ELDRITCH_OBELISK_SEGMENT.get()
                || b == ModBlocks.ELDRITCH_CAPSTONE.get() || b == ModBlocks.GLOWING_CRUSTED_STONE.get() || b == ModBlocks.GLYPHED_STONE.get()
                || b == ModBlocks.ANCIENT_DOORWAY.get() || b == ModBlocks.ANCIENT_LOCK.get() || b == ModBlocks.CRUSTED_OPENING.get()
                || b == ModBlocks.RUNED_STONE.get()) {
            return Old.ELDRITCH;
        }
        if (b == ModBlocks.ANCIENT_STONE.get() || b == ModBlocks.ANCIENT_ROCK.get() || b == ModBlocks.ANCIENT_STONE_SEALED.get()
                || b == ModBlocks.CRUSTED_STONE.get() || b == ModBlocks.ANCIENT_PEDESTAL.get()) {
            return Old.COSMETIC_SOLID;
        }
        if (b == ModBlocks.ANCIENT_STONE_STAIRS.get()) {
            return Old.STAIRS_ELDRITCH;
        }
        return Old.OTHER;
    }

    public void setBlock(int x, int y, int z, Old block, int meta, int flags) {
        level.setBlock(pos(x, y, z), state(block, meta), 2);
    }

    public void setBlock(int x, int y, int z, Old block) {
        setBlock(x, y, z, block, 0, 3);
    }

    public void setBlockToAir(int x, int y, int z) {
        level.setBlock(pos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
    }

    /** Строгий вариант для блоков с направлением: страннные кристаллы растут от опоры в сторону dir. */
    public void setCrystal(int x, int y, int z, Dir dir) {
        level.setBlock(pos(x, y, z), ModBlocks.STRANGE_CRYSTALS.get().defaultBlockState().setValue(StrangeCrystalsBlock.FACING, dir.toDirection()), 2);
    }

    public Old getBlock(int x, int y, int z) {
        return kind(level.getBlockState(pos(x, y, z)));
    }

    public BlockState getState(int x, int y, int z) {
        return level.getBlockState(pos(x, y, z));
    }

    public boolean isAirBlock(int x, int y, int z) {
        return level.getBlockState(pos(x, y, z)).isAir();
    }

    /** isOpaqueCube оригинала: целый непрозрачный куб (Пустота тоже считается таким). */
    public boolean isOpaqueCube(int x, int y, int z) {
        BlockPos p = pos(x, y, z);
        BlockState state = level.getBlockState(p);
        return state.getBlock() instanceof thaumcraft.common.blocks.EldritchVoidBlock || state.isSolidRender(level, p);
    }

    public BlockEntity getTileEntity(int x, int y, int z) {
        return level.getBlockEntity(pos(x, y, z));
    }

    public void notifyBlockChange(int x, int y, int z, Old block) {
    }

    public void spawnEntityInWorld(Entity entity) {
        level.addFreshEntity(entity);
    }

    public Difficulty difficulty() {
        return level.getDifficulty();
    }

    /** Utils.setBiomeAt оригинала: столбец 4×4 вокруг точки становится заданным биомом. */
    public void setBiomeAt(int x, int z, ResourceKey<Biome> biome) {
        if (level instanceof ServerLevel server) {
            thaumcraft.common.world.ModBiomes.set(server, pos(x, 64, z), biome);
            return;
        }
        if (level instanceof WorldGenLevel gen) {
            Holder<Biome> holder = gen.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome);
            ChunkAccess chunk = gen.getChunk(pos(x, 64, z));
            int qx = QuartPos.fromBlock(x);
            int qz = QuartPos.fromBlock(z);
            chunk.fillBiomesFromNoise((bx, by, bz, sampler) -> bx == qx && bz == qz ? holder : chunk.getNoiseBiome(bx, by, bz),
                    gen.getLevel().getChunkSource().randomState().sampler());
        }
    }

    // ---------- BlockUtils и MathHelper оригинала ----------

    /** Сколько граней блока открыты (сосед — не целый непрозрачный куб). */
    public int countExposedSides(int x, int y, int z) {
        int count = 0;
        for (Dir dir : Dir.VALID_DIRECTIONS) {
            if (!isOpaqueCube(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)) {
                count++;
            }
        }
        return count;
    }

    /** isBlockAdjacentToAtleast(..., blockEldritch, любая мета, 1): есть ли рядом хоть один блок Элдрича. */
    public boolean isBlockAdjacentToAtleast(int x, int y, int z, Old block, int count) {
        int found = 0;
        for (Dir dir : Dir.VALID_DIRECTIONS) {
            if (getBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ) == block && ++found >= count) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdjacentToSolidBlock(int x, int y, int z) {
        for (Dir dir : Dir.VALID_DIRECTIONS) {
            BlockPos p = pos(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
            if (level.getBlockState(p).isFaceSturdy(level, p, dir.getOpposite().toDirection())) {
                return true;
            }
        }
        return false;
    }

    public static int getRandomIntegerInRange(Random random, int min, int max) {
        return min >= max ? min : random.nextInt(max - min + 1) + min;
    }
}
