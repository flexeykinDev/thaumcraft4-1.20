package thaumcraft.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import thaumcraft.Thaumcraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Камни Запредельных земель, у которых вид грани зависит от места (getIcon(IBlockAccess...) оригинала):
 * <ul>
 * <li>RANDOM — древний камень и рунный камень: на каждой грани одна из четырёх текстур, выбранная тем же
 * зерном, что в оригинале (строка координат и номер грани), так что стены пёстрые, но не мерцают;</li>
 * <li>PARITY — древняя скала: четыре текстуры складываются в узор 2×2 по чётности координат;</li>
 * <li>INSET — светящийся покрытый коркой и испещрённый глифами камень (BlockEldritchRenderer): там, где у блока
 * нет сплошного соседа, его грань утоплена на два пикселя, и он выглядит вставленным в кладку.</li>
 * </ul>
 * Описание блока в предмете и частицы берутся из обычной модели, её же модель оборачивает.
 */
public class EldritchStoneModel extends BakedModelWrapper<BakedModel> {
    public enum Mode { RANDOM, PARITY, INSET }

    private static final ModelProperty<BlockPos> POS = new ModelProperty<>();
    private static final ModelProperty<Integer> OPEN = new ModelProperty<>();
    private static final float INSET = 2f / 16f;

    private final Mode mode;
    private final String[] textures;

    public EldritchStoneModel(BakedModel original, Mode mode, String... textures) {
        super(original);
        this.mode = mode;
        this.textures = textures;
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Thaumcraft.id("block/" + name));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state,
                                           @NotNull ModelData data) {
        ModelData.Builder builder = ModelData.builder().with(POS, pos.immutable());
        if (mode == Mode.INSET) {
            int open = 0;
            for (Direction side : Direction.values()) {
                BlockPos other = pos.relative(side);
                if (!level.getBlockState(other).isFaceSturdy(level, other, side.getOpposite())) {
                    open |= 1 << side.ordinal();
                }
            }
            builder.with(OPEN, open);
        }
        return builder.build();
    }

    /** Номер текстуры грани: те же формулы, что в BlockCosmeticSolid и BlockEldritch оригинала (грани нумеруются так же). */
    private int index(BlockPos pos, Direction side) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (mode == Mode.PARITY) {
            return switch (side.getAxis()) {
                case Y -> Math.abs(x % 2) + Math.abs(z % 2) * 2;
                case Z -> Math.abs(x % 2) + Math.abs(y % 2) * 2;
                case X -> Math.abs(z % 2) + Math.abs(y % 2) * 2;
            };
        }
        String key = x + "" + y + "" + z;
        Random random = new Random(Math.abs(key.hashCode() * 100) + 1);
        return random.nextInt(12345 + side.ordinal()) % 4;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource random,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        BlockPos pos = data.get(POS);
        if (mode == Mode.INSET) {
            if (side != null) {
                return List.of();
            }
            Integer open = data.get(OPEN);
            int mask = open == null ? 0x3f : open;
            float[] min = {0f, 0f, 0f};
            float[] max = {1f, 1f, 1f};
            for (Direction face : Direction.values()) {
                if ((mask & 1 << face.ordinal()) != 0) {
                    int axis = face.getAxis().ordinal();
                    if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                        max[axis] -= INSET;
                    } else {
                        min[axis] += INSET;
                    }
                }
            }
            TextureAtlasSprite sprite = sprite(textures[0]);
            List<BakedQuad> quads = new ArrayList<>();
            for (Direction face : Direction.values()) {
                if ((mask & 1 << face.ordinal()) != 0) {
                    quads.add(quad(face, sprite, min, max));
                }
            }
            return quads;
        }
        if (side == null) {
            return List.of();
        }
        int i = pos == null ? 0 : index(pos, side);
        return List.of(quad(side, sprite(textures[i]), new float[]{0f, 0f, 0f}, new float[]{1f, 1f, 1f}));
    }

    /** Грань коробки min..max; текстура обрезается по размеру коробки, как у RenderBlocks оригинала. */
    static BakedQuad quad(Direction side, TextureAtlasSprite sprite, float[] min, float[] max) {
        float[][] corners = switch (side) {
            case DOWN -> new float[][]{{min[0], min[1], max[2]}, {min[0], min[1], min[2]}, {max[0], min[1], min[2]}, {max[0], min[1], max[2]}};
            case UP -> new float[][]{{min[0], max[1], min[2]}, {min[0], max[1], max[2]}, {max[0], max[1], max[2]}, {max[0], max[1], min[2]}};
            case NORTH -> new float[][]{{max[0], max[1], min[2]}, {max[0], min[1], min[2]}, {min[0], min[1], min[2]}, {min[0], max[1], min[2]}};
            case SOUTH -> new float[][]{{min[0], max[1], max[2]}, {min[0], min[1], max[2]}, {max[0], min[1], max[2]}, {max[0], max[1], max[2]}};
            case WEST -> new float[][]{{min[0], max[1], min[2]}, {min[0], min[1], min[2]}, {min[0], min[1], max[2]}, {min[0], max[1], max[2]}};
            case EAST -> new float[][]{{max[0], max[1], max[2]}, {max[0], min[1], max[2]}, {max[0], min[1], min[2]}, {max[0], max[1], min[2]}};
        };
        BakedQuad[] out = new BakedQuad[1];
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer(quad -> out[0] = quad);
        builder.setDirection(side);
        builder.setSprite(sprite);
        builder.setShade(true);
        builder.setTintIndex(-1);
        for (float[] c : corners) {
            float u;
            float v;
            switch (side.getAxis()) {
                case Y -> {
                    u = c[0];
                    v = side == Direction.UP ? c[2] : 1f - c[2];
                }
                case Z -> {
                    u = side == Direction.SOUTH ? c[0] : 1f - c[0];
                    v = 1f - c[1];
                }
                default -> {
                    u = side == Direction.WEST ? c[2] : 1f - c[2];
                    v = 1f - c[1];
                }
            }
            builder.vertex(c[0], c[1], c[2]).color(1f, 1f, 1f, 1f).uv(sprite.getU(u * 16f), sprite.getV(v * 16f)).uv2(0, 0)
                    .normal(side.getStepX(), side.getStepY(), side.getStepZ()).endVertex();
        }
        return out[0];
    }
}
