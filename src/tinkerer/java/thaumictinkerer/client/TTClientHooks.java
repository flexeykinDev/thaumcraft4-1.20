package thaumictinkerer.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import thaumcraft.client.fx.Fx;
import thaumictinkerer.client.render.RepairerRenderer;
import thaumictinkerer.common.block.GasBlock;

/** То, что общий код вызывает только на клиенте: частицы и отрисовка предметов. */
public final class TTClientHooks {
    private TTClientHooks() {
    }

    /** blockSparkle оригинала над силовым полем: искра в случайной точке блока. */
    public static void forcefieldSparkle(BlockPos pos, RandomSource random) {
        Fx.sparkle(pos.getX() + random.nextFloat(), pos.getY() + random.nextFloat(), pos.getZ() + random.nextFloat(), 1f, 2, 0f);
    }

    /** randomDisplayTick газов оригинала: изредка искра или тёмный огонёк. */
    public static void gasParticles(GasBlock.Kind kind, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (kind) {
            case LIGHT -> {
                if (random.nextFloat() < 0.0075f) {
                    Fx.sparkle(x, y, z, 1f, 1, random.nextFloat() / 2f);
                }
            }
            case SHADOW -> {
                if (random.nextFloat() < 0.0075f) {
                    Fx.wisp(x, y, z, 0.125f, 5, true, true, -0.02f);
                }
            }
            case NITOR -> {
                if (random.nextFloat() < 0.03f) {
                    Fx.sparkle(x, y, z, 1f, 4, random.nextFloat() / 2f);
                }
            }
        }
    }

    /** placeParticle оригинала: газ рассеялся — на его месте вспышка. */
    public static void gasRemoved(GasBlock.Kind kind, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (kind == GasBlock.Kind.SHADOW) {
            Fx.wisp(x, y, z, 0.125f, 5, true, true, -0.02f);
        } else {
            Fx.sparkle(x, y, z, 1f, kind == GasBlock.Kind.NITOR ? 4 : 1, 0f);
        }
    }

    public static IClientItemExtensions magnetItem(boolean mobs) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = thaumictinkerer.client.render.MagnetRenderer.item(mobs);
                }
                return renderer;
            }
        };
    }

    /** Предмет восстановителя рисуется той же моделью, что и блок. */
    public static IClientItemExtensions repairerItem() {
        return new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RepairerRenderer.ITEM;
            }
        };
    }
}
