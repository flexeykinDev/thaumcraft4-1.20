package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.blockentity.PedestalBlockEntity;
import thaumcraft.common.network.FxPacket;

/** Клиентская сторона FxPacket: превращает пакет в частицы. */
public final class FxHandler {
    public static void handle(FxPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        RandomSource random = mc.level.random;
        switch (packet.kind()) {
            case FxPacket.BLOCK_SPARKLE -> Fx.blockSparkle((int) packet.from().x, (int) Math.floor(packet.from().y),
                    (int) packet.from().z, packet.a(), packet.b());
            case FxPacket.CRUCIBLE_BOIL -> {
                BlockPos pos = BlockPos.containing(packet.from());
                Fx.crucibleBoil(pos.getX(), pos.getY(), pos.getZ(), (float) (packet.from().y - pos.getY()), packet.a(), packet.b());
            }
            case FxPacket.BOLT -> {
                // b = 1 — молния фокуса разряда, иначе обычная молния узла
                if (packet.b() == 1) {
                    // ближайший к началу молнии игрок с жезлом: молния выходит из вершины его жезла
                    net.minecraft.world.phys.Vec3 from = packet.from();
                    net.minecraft.world.entity.player.Player caster = mc.level.getNearestPlayer(from.x, from.y, from.z, 2.0, false);
                    net.minecraft.world.phys.Vec3 tip = caster == null ? null
                            : thaumcraft.client.render.WandItemRenderer.tipOf(caster.getId());
                    if (tip != null) {
                        from = tip;
                    }
                    Fx.shockBolt(from.x, from.y, from.z, packet.to().x, packet.to().y, packet.to().z, packet.a());
                } else {
                    Fx.bolt(packet.from().x, packet.from().y, packet.from().z, packet.to().x, packet.to().y, packet.to().z, packet.a());
                }
            }
            case FxPacket.ITEM_STREAM -> {
                BlockPos pos = BlockPos.containing(packet.from());
                ItemStack stack = mc.level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal ? pedestal.getItem() : ItemStack.EMPTY;
                if (!stack.isEmpty()) {
                    Fx.itemStream(stack, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, packet.to().x, packet.to().y, packet.to().z);
                }
            }
            case FxPacket.ESSENTIA -> {
                // несколько огоньков вразброс, как след эссенции (FXEssentiaTrail оригинала)
                for (int i = 0; i < 4; i++) {
                    Fx.wispTo(packet.from().x + (random.nextFloat() - 0.5) * 0.3, packet.from().y + (random.nextFloat() - 0.5) * 0.3,
                            packet.from().z + (random.nextFloat() - 0.5) * 0.3, packet.to().x, packet.to().y, packet.to().z,
                            0.25f + random.nextFloat() * 0.15f, packet.a());
                }
            }
            case FxPacket.RUNES -> {
                for (int i = 0; i < packet.a(); i++) {
                    Fx.blockRunes(Math.floor(packet.from().x), Math.floor(packet.from().y), Math.floor(packet.from().z),
                            0.3f + random.nextFloat() * 0.7f, 0f, 0.3f + random.nextFloat() * 0.7f, 15, 0f);
                }
            }
            case FxPacket.FLOATY_BEAM -> Fx.floatyBeam(packet.from(), packet.to(), packet.a());
            case FxPacket.SHIELD -> Fx.shield(packet.a(), packet.b());
            case FxPacket.RUNIC_CHARGE -> {
                thaumcraft.client.gui.RunicOverlay.charge = packet.a();
                thaumcraft.client.gui.RunicOverlay.max = packet.b();
            }
            case FxPacket.BLOCK_BUBBLE -> {
                float r = (packet.a() >> 16 & 255) / 255f;
                float g = (packet.a() >> 8 & 255) / 255f;
                float b = (packet.a() & 255) / 255f;
                double x = packet.from().x;
                double y = packet.from().y;
                double z = packet.from().z;
                Fx.crucibleBubble(x, y + random.nextFloat(), z + random.nextFloat(), r, g, b);
                Fx.crucibleBubble(x + 1, y + random.nextFloat(), z + random.nextFloat(), r, g, b);
                Fx.crucibleBubble(x + random.nextFloat(), y + random.nextFloat(), z, r, g, b);
                Fx.crucibleBubble(x + random.nextFloat(), y + random.nextFloat(), z + 1, r, g, b);
            }
            case FxPacket.BORE_TARGET, FxPacket.BORE_DUG -> {
                if (mc.level.getBlockEntity(BlockPos.containing(packet.from())) instanceof thaumcraft.common.blocks.ArcaneBoreBlock.Bore bore) {
                    if (packet.kind() == FxPacket.BORE_TARGET) {
                        bore.setDigTarget(packet.a());
                    } else {
                        bore.dugFx(net.minecraft.world.level.block.Block.stateById(packet.a()));
                    }
                }
            }
            case FxPacket.BURST -> Fx.burst(packet.from().x, packet.from().y, packet.from().z, packet.a() == 0 ? 1f : packet.a() / 100f);
            case FxPacket.WARP_VIGNETTE -> thaumcraft.client.ClientWarp.startVignette();
            case FxPacket.SONIC -> Fx.sonic(packet.a());
            case FxPacket.BLOCK_ARC -> {
                net.minecraft.world.entity.Entity source = mc.level.getEntity(packet.a());
                if (source != null) {
                    float r = 0.3f - random.nextFloat() * 0.1f;
                    float g = 0f;
                    float b = 0.5f + random.nextFloat() * 0.2f;
                    if (packet.b() == 1) {
                        r = 0.5f + random.nextFloat() * 0.2f;
                        b = 0f;
                    }
                    Fx.arc(source.getX(), source.getBoundingBox().minY + source.getBbHeight() / 2f, source.getZ(), packet.from().x + 0.5,
                            packet.from().y + 1, packet.from().z + 0.5, r, g, b, 0.5f);
                }
            }
            case FxPacket.MIST -> thaumcraft.client.ClientWarp.startMist(packet.a());
            case FxPacket.SPARKLES -> {
                float spread = packet.b() / 100f;
                for (int i = 0; i < 5; i++) {
                    Fx.sparkle(packet.from().x + (random.nextFloat() - random.nextFloat()) * spread,
                            packet.from().y + (random.nextFloat() - random.nextFloat()) * spread,
                            packet.from().z + (random.nextFloat() - random.nextFloat()) * spread,
                            2f + random.nextFloat(), packet.a(), 0.05f + random.nextFloat() * 0.05f);
                }
            }
            case FxPacket.VENT -> {
                // FXVent оригинала: клуб пара в цвете аспекта; без направления — в случайную сторону, как из порванной трубы
                net.minecraft.world.phys.Vec3 dir = packet.to();
                if (dir.lengthSqr() == 0) {
                    float pitch = random.nextFloat() * 360f;
                    float yaw = random.nextFloat() * 360f;
                    double fx = -net.minecraft.util.Mth.sin(yaw * net.minecraft.util.Mth.DEG_TO_RAD) * net.minecraft.util.Mth.cos(pitch * net.minecraft.util.Mth.DEG_TO_RAD);
                    double fz = net.minecraft.util.Mth.cos(yaw * net.minecraft.util.Mth.DEG_TO_RAD) * net.minecraft.util.Mth.cos(pitch * net.minecraft.util.Mth.DEG_TO_RAD);
                    double fy = -net.minecraft.util.Mth.sin(pitch * net.minecraft.util.Mth.DEG_TO_RAD);
                    dir = new net.minecraft.world.phys.Vec3(fx / 5.0, fy / 5.0, fz / 5.0);
                }
                Fx.vent(packet.from().x, packet.from().y, packet.from().z, dir.x, dir.y, dir.z, packet.a(), packet.b() == 0 ? 1f : packet.b() / 100f);
            }
            default -> {
            }
        }
    }

    private FxHandler() {
    }
}
