package thaumcraft.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Небо Запредельных земель — WorldProviderOuter оригинала: неба нет (ни солнца, ни облаков), всё тонет в густом
 * лилово-сером тумане (цвет 10518688, приглушённый до 15%).
 */
public class OuterLandsEffects extends DimensionSpecialEffects {
    private static final int FOG = 10518688;

    public OuterLandsEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return new Vec3((FOG >> 16 & 255) / 255.0 * 0.15, (FOG >> 8 & 255) / 255.0 * 0.15, (FOG & 255) / 255.0 * 0.15);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        return null;
    }
}
