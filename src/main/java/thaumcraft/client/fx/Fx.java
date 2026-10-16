package thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Эффекты оригинала — те же вызовы, что были в ClientProxy (wispFX, sparkle, crucibleBubble, blockRunes...),
 * с теми же числами. Вызывать только на клиенте.
 */
public final class Fx {
    /** Цвета «типов» FXWisp и FXSparkle оригинала: 0 лиловый, 1 жёлтый, 2 синий, 3 зелёный, 4 красный, 5 тёмный, 6 белый. */
    private static float[] typeColor(RandomSource random, int type, boolean wisp) {
        return switch (type) {
            case 0 -> new float[]{0.75f + random.nextFloat() * 0.25f, 0.25f + random.nextFloat() * 0.25f, 0.75f + random.nextFloat() * 0.25f};
            case 1 -> new float[]{0.5f + random.nextFloat() * 0.3f, 0.5f + random.nextFloat() * 0.3f, 0.2f};
            case 2 -> new float[]{0.2f, 0.2f, 0.7f + random.nextFloat() * 0.3f};
            case 3 -> new float[]{0.2f, 0.7f + random.nextFloat() * 0.3f, 0.2f};
            case 4 -> new float[]{0.7f + random.nextFloat() * 0.3f, 0.2f, 0.2f};
            case 5 -> new float[]{random.nextFloat() * 0.1f + 0.001f, random.nextFloat() * 0.1f, random.nextFloat() * 0.1f};
            case 6 -> new float[]{0.8f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f, 0.8f + random.nextFloat() * 0.2f};
            // тип 7 у огонька оранжевый, у искры бирюзовый
            default -> wisp ? new float[]{0.7f + random.nextFloat() * 0.3f, 0.5f + random.nextFloat() * 0.2f, 0.3f + random.nextFloat() * 0.1f}
                    : new float[]{0.2f, 0.5f + random.nextFloat() * 0.3f, 0.6f + random.nextFloat() * 0.3f};
        };
    }

    private static ClientLevel level() {
        return Minecraft.getInstance().level;
    }

    private static void add(Particle particle) {
        // как в оригинале, дальше 50 блоков частицы не создаются
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(particle.getBoundingBox().getCenter()) < 2500.0) {
            mc.particleEngine.add(particle);
        }
    }

    /** Настройка «Частицы»: при минимальных редкие эффекты пропускаются (particleCount оригинала). */
    private static int particleCount(int base) {
        return switch (Minecraft.getInstance().options.particles().get()) {
            case ALL -> base * 2;
            case DECREASED -> base;
            case MINIMAL -> 0;
        };
    }

    // ---------- огоньки ----------

    /** wispFX2: огонёк на месте. */
    public static void wisp(double x, double y, double z, float size, int type, boolean shrink, boolean noClip, float gravity) {
        float[] c = typeColor(level().random, type, true);
        TcParticle p = TcParticle.wisp(level(), x, y, z, size, c[0], c[1], c[2]).gravity(gravity).shrink(shrink);
        if (noClip) {
            p.noClip();
        }
        if (type == 5) {
            p.normalBlend();
        }
        add(p);
    }

    /** wispFX3: огонёк, летящий из точки в точку. */
    public static void wispTo(double x, double y, double z, double tx, double ty, double tz, float size, int type, boolean shrink,
                              float gravity) {
        float[] c = typeColor(level().random, type, true);
        TcParticle p = TcParticle.wisp(level(), x, y, z, size, c[0], c[1], c[2]).target(tx, ty, tz).gravity(gravity).shrink(shrink);
        if (type == 5) {
            p.normalBlend();
        }
        add(p);
    }

    /** Огонёк заданного цвета, летящий в точку: так течёт эссенция и вис. */
    public static void wispTo(double x, double y, double z, double tx, double ty, double tz, float size, int color) {
        add(TcParticle.wisp(level(), x, y, z, size, (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f)
                .target(tx, ty, tz).noClip());
    }

    // ---------- искры ----------

    /** sparkle: одиночная искра; как в оригинале, появляется не каждый раз. */
    public static void sparkle(double x, double y, double z, float size, int type, float gravity) {
        if (level().random.nextInt(6) < particleCount(2)) {
            float[] c = typeColor(level().random, type, false);
            TcParticle p = TcParticle.sparkle(level(), x, y, z, size, c[0], c[1], c[2], 6).gravity(gravity).noClip();
            if (type == 5) {
                p.normalBlend();
            }
            add(p);
        }
    }

    /** blockSparkle: блок на миг покрывается искрами. Тип -9999 — лиловые искры магии. */
    /** furnaceLavaFx оригинала: брызги лавы из решётки адской печи. */
    public static void furnaceLava(net.minecraft.core.BlockPos pos, int facingX, int facingZ) {
        var random = Minecraft.getInstance().level.random;
        double x = pos.getX() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.3 + facingX;
        double z = pos.getZ() + 0.5 + (random.nextFloat() - random.nextFloat()) * 0.3 + facingZ;
        float qx = facingX == 0 ? (random.nextFloat() - random.nextFloat()) * 0.5f : facingX * random.nextFloat();
        float qz = facingZ == 0 ? (random.nextFloat() - random.nextFloat()) * 0.5f : facingZ * random.nextFloat();
        var particle = Minecraft.getInstance().particleEngine.createParticle(net.minecraft.core.particles.ParticleTypes.LAVA, x, pos.getY() + 0.3, z,
                0, 0, 0);
        if (particle != null) {
            particle.setParticleSpeed(0.15 * qx, 0.2 * random.nextFloat(), 0.15 * qz);
        }
    }

    public static void blockSparkle(int x, int y, int z, int color, int count) {
        RandomSource random = level().random;
        float r = color == -9999 ? 0.33f + random.nextFloat() * 0.67f : (color >> 16 & 255) / 255f;
        float g = color == -9999 ? 0.33f + random.nextFloat() * 0.67f : (color >> 8 & 255) / 255f;
        float b = color == -9999 ? 0.33f + random.nextFloat() * 0.67f : (color & 255) / 255f;
        for (int i = 0; i < particleCount(count); i++) {
            float fr = color == -9999 ? 0.33f + random.nextFloat() * 0.67f : r;
            // искры по граням блока
            double px = x + random.nextFloat();
            double py = y + random.nextFloat();
            double pz = z + random.nextFloat();
            switch (random.nextInt(6)) {
                case 0 -> py = y - 0.03;
                case 1 -> py = y + 1.03;
                case 2 -> pz = z - 0.03;
                case 3 -> pz = z + 1.03;
                case 4 -> px = x - 0.03;
                default -> px = x + 1.03;
            }
            add(TcParticle.sparkle(level(), px, py, pz, 1.75f, fr, g, b, 3 + random.nextInt(3)).noClip());
        }
    }

    // ---------- тигель ----------

    /** Густой пузырь варева (FXSlimyBubble оригинала). */
    public static void slimyBubble(double x, double y, double z, float size, float r, float g, float b) {
        TcParticle p = TcParticle.slimy(level(), x, y, z, size);
        p.setColor(r, g, b);
        add(p);
    }

    public static void crucibleBubble(double x, double y, double z, float r, float g, float b) {
        TcParticle p = TcParticle.bubble(level(), x, y, z, 1);
        p.setColor(r, g, b);
        add(p);
    }

    public static void crucibleFroth(double x, double y, double z) {
        TcParticle p = TcParticle.bubble(level(), x, y, z, -4).froth(false);
        p.setColor(0.5f, 0.5f, 0.7f);
        add(p);
    }

    public static void crucibleFrothDown(double x, double y, double z) {
        TcParticle p = TcParticle.bubble(level(), x, y, z, -4).froth(true);
        p.setColor(0.5f, 0.5f, 0.7f);
        add(p);
    }

    /** crucibleBoil: всплеск пузырей, когда тигель что-то сварил или его вылили. */
    public static void crucibleBoil(int x, int y, int z, float height, int color, int power) {
        RandomSource random = level().random;
        for (int q = 0; q < 10; q++) {
            for (int i = 0; i < particleCount(1); i++) {
                TcParticle p = TcParticle.bubble(level(), x + 0.2 + random.nextFloat() * 0.6, y + 0.1 + height,
                        z + 0.2 + random.nextFloat() * 0.6, 3);
                if (color == 0) {
                    p.setColor(1f, 1f, 1f);
                } else {
                    p.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
                }
                p.bubbleSpeed = 0.003 * power;
                add(p);
            }
        }
    }

    // ---------- руны и инфузия ----------

    /** blockRunes: руны, плывущие по граням блока. */
    public static void blockRunes(double x, double y, double z, float r, float g, float b, int duration, float gravity) {
        add(TcParticle.rune(level(), x + 0.5, y + 0.5, z + 0.5, r, g, b, duration).gravity(gravity));
    }

    /** drawInfusionParticles: крошки предмета, летящие с пьедестала в матрицу. */
    public static void itemStream(ItemStack stack, double x, double y, double z, double tx, double ty, double tz) {
        RandomSource random = level().random;
        for (int i = 0; i < 3; i++) {
            double speed = 0.08 + random.nextFloat() * 0.04;
            double dx = tx - x;
            double dy = ty - y;
            double dz = tz - z;
            double length = Math.max(0.01, Math.sqrt(dx * dx + dy * dy + dz * dz));
            level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack),
                    x + random.nextGaussian() * 0.05, y + random.nextGaussian() * 0.05, z + random.nextGaussian() * 0.05,
                    dx / length * speed, dy / length * speed + 0.06, dz / length * speed);
        }
        // лиловые искры по тому же пути (FXBoreSparkle оригинала)
        add(TcParticle.sparkle(level(), x, y, z, 1.2f, 0.4f + random.nextFloat() * 0.2f, 0.2f, 0.6f + random.nextFloat() * 0.3f, 4)
                .noClip().target(tx, ty, tz));
    }

    /** nodeBolt оригинала: молния между двумя точками. Тип задаёт цвет: 0 лиловая, 1 жёлтая, 2 синяя, 3 зелёная, 4 красная. */
    public static void bolt(double x, double y, double z, double tx, double ty, double tz, int type) {
        long seed = level().random.nextLong();
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(x, y, z);
        net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(tx, ty, tz);
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, seed, type, false));
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, seed, type, true));
    }

    /** Тонкая молния заданной толщины (молнии летательной упряжи). */
    public static void thinBolt(double x, double y, double z, double tx, double ty, double tz, int type, float width) {
        long seed = level().random.nextLong();
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(x, y, z);
        net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(tx, ty, tz);
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, seed, type, false, 10, 4f, 5, width));
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, seed, type, true, 10, 4f, 5, width));
    }

    /** Молния фокуса разряда (ItemFocusShock.shootLightning): короткая, почти прямая и толстая. */
    public static void shockBolt(double x, double y, double z, double tx, double ty, double tz, int type) {
        long seed = level().random.nextLong();
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(x, y, z);
        net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(tx, ty, tz);
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, seed, type, false, 6, 0.5f, 8, 0.125f));
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, seed, type, true, 6, 0.5f, 8, 0.125f));
    }

    private static final java.util.Map<Long, WandBeamParticle> BORE_BEAMS = new java.util.HashMap<>();

    /** beamBore оригинала: луч чародейского бура; key — какой из двух лучей какого бура. */
    public static void boreBeam(long key, net.minecraft.world.phys.Vec3 origin, net.minecraft.world.phys.Vec3 target, int type, int color,
                                boolean reverse, float endMod, int impact) {
        WandBeamParticle beam = BORE_BEAMS.get(key);
        if (beam == null || !beam.isAlive()) {
            beam = new WandBeamParticle(level(), origin, target, type, color, reverse);
            BORE_BEAMS.put(key, beam);
            Minecraft.getInstance().particleEngine.add(beam);
        }
        beam.update(target, endMod, impact);
    }

    /**
     * boreDigFx оригинала: крошка разрушаемого блока летит от него к буру (иногда вместо крошки — искра).
     */
    public static void boreDig(net.minecraft.core.BlockPos from, net.minecraft.core.BlockPos to, net.minecraft.world.level.block.state.BlockState state) {
        RandomSource random = level().random;
        double x = from.getX() + random.nextFloat();
        double y = from.getY() + random.nextFloat();
        double z = from.getZ() + random.nextFloat();
        double tx = to.getX() + 0.5;
        double ty = to.getY() + 0.5;
        double tz = to.getZ() + 0.5;
        if (random.nextInt(10) == 0) {
            wispTo(x, y, z, tx, ty, tz, 0.3f, 0xFFD700);
            return;
        }
        net.minecraft.client.particle.TerrainParticle crumb = new net.minecraft.client.particle.TerrainParticle(level(), x, y, z,
                (tx - x) / 20.0, (ty - y) / 20.0, (tz - z) / 20.0, state) {
            {
                gravity = 0f;
                lifetime = 20;
                xd = (tx - x) / 20.0;
                yd = (ty - y) / 20.0;
                zd = (tz - z) / 20.0;
                hasPhysics = false;
            }

            @Override
            public void tick() {
                xo = this.x;
                yo = this.y;
                zo = this.z;
                if (age++ >= lifetime) {
                    remove();
                    return;
                }
                move(xd, yd, zd);
            }
        };
        add(crumb);
    }

    private static final java.util.Map<Integer, WandBeamParticle> BEAMS = new java.util.HashMap<>();

    /** beamCont оригинала: луч от жезла игрока к точке; пока его вызывают каждый тик, луч один и тот же. */
    public static void wandBeam(int playerId, double tx, double ty, double tz, int type, int color, boolean reverse, float endMod,
                                int impact) {
        if (!(level().getEntity(playerId) instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        net.minecraft.world.phys.Vec3 target = new net.minecraft.world.phys.Vec3(tx, ty, tz);
        WandBeamParticle beam = BEAMS.get(playerId);
        if (beam == null || !beam.isAlive()) {
            beam = new WandBeamParticle(level(), player, target, type, color, reverse);
            BEAMS.put(playerId, beam);
            Minecraft.getInstance().particleEngine.add(beam);
        }
        beam.update(target, endMod, impact);
    }

    /** excavateFX оригинала: трещины на блоке, который сверлит луч; progress от 0 до 9, отрицательный — убрать. */
    public static void blockCracks(int playerId, int x, int y, int z, int progress) {
        Minecraft.getInstance().levelRenderer.destroyBlockProgress(playerId, new net.minecraft.core.BlockPos(x, y, z), progress);
    }

    /** blockWard оригинала: вспышка рунного щита на грани защищённого блока. */
    public static void blockWard(int x, int y, int z, int side, float fx, float fy, float fz) {
        Minecraft.getInstance().particleEngine.add(new BlockWardParticle(level(), x, y, z,
                net.minecraft.core.Direction.from3DDataValue(side), fx, fy, fz));
    }

    private static final java.util.Map<Long, PowerBeamParticle> POWER_BEAMS = new java.util.HashMap<>();

    /** beamPower оригинала: луч между звеньями сети вис; key — кто его держит (координаты реле). */
    public static void powerBeam(long key, double x, double y, double z, double tx, double ty, double tz, float r, float g, float b,
                                 boolean pulse) {
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(x, y, z);
        net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(tx, ty, tz);
        PowerBeamParticle beam = POWER_BEAMS.get(key);
        if (beam == null || !beam.isAlive()) {
            beam = new PowerBeamParticle(level(), from, to, r, g, b);
            POWER_BEAMS.put(key, beam);
            Minecraft.getInstance().particleEngine.add(beam);
        }
        beam.update(from, to, r, g, b, pulse);
    }

    /** FXSpark оригинала: электрическая искра заданного размера и цвета. */
    public static void spark(double x, double y, double z, float scale, float r, float g, float b, float alpha) {
        Minecraft.getInstance().particleEngine.add(new SparkParticle(level(), x, y, z, scale, r, g, b, alpha));
    }

    /**
     * Вспышка рунного щита (PacketFXShield оригинала): полусфера повёрнута к ударившему; если бьют со всех сторон —
     * две полусферы, сверху и снизу.
     */
    public static void shield(int entityId, int targetId) {
        net.minecraft.world.entity.Entity entity = level().getEntity(entityId);
        if (entity == null) {
            return;
        }
        float yaw = 0f;
        float pitch = 90f;
        if (targetId >= 0) {
            net.minecraft.world.entity.Entity attacker = level().getEntity(targetId);
            if (attacker != null) {
                double dx = entity.getX() - attacker.getX();
                double dy = entity.getBoundingBox().getCenter().y - attacker.getBoundingBox().getCenter().y;
                double dz = entity.getZ() - attacker.getZ();
                double flat = Math.sqrt(dx * dx + dz * dz);
                yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
                pitch = (float) -(Math.atan2(dy, flat) * 180.0 / Math.PI);
            }
            add(new ShieldParticle(level(), entity, 8, yaw, pitch));
        } else if (targetId == -1) {
            add(new ShieldParticle(level(), entity, 8, 0f, 90f));
            add(new ShieldParticle(level(), entity, 8, 0f, 270f));
        } else if (targetId == -2) {
            add(new ShieldParticle(level(), entity, 8, 0f, 270f));
        } else {
            add(new ShieldParticle(level(), entity, 8, 0f, 90f));
        }
    }

    /** swarmParticleFX оригинала: ещё одна мошка, кружащая возле существа. */
    public static void swarm(int entityId, float speed, float turnSpeed, float pull) {
        net.minecraft.world.entity.Entity entity = level().getEntity(entityId);
        if (entity != null) {
            add(new SwarmParticle(level(), entity, speed, turnSpeed, pull));
        }
    }

    /** Мошки существа улетают прочь и гаснут. */
    public static void releaseSwarm(int entityId) {
        SwarmParticle.release(entityId);
    }

    /** smokeSpiral оригинала: клуб дыма, летящий по спирали радиуса radius; start — начальный угол, floor — уровень земли. */
    public static void smokeSpiral(double x, double y, double z, float radius, int start, int floor, int color) {
        SmokeSpiralParticle p = new SmokeSpiralParticle(level(), x, y, z, radius, start, floor);
        p.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
        add(p);
    }

    /** drawGenericParticles оригинала: кадры из листа частиц, по кругу (loop) или один раз за жизнь частицы. */
    public static void generic(double x, double y, double z, double dx, double dy, double dz, float r, float g, float b, float alpha,
                               boolean loop, int start, int count, int step, int age, int delay, float scale) {
        add(new GenericParticle(level(), x, y, z, dx, dy, dz, r, g, b, alpha, loop, start, count, step, age, delay, scale));
    }

    /** drawFloatyLine оригинала: колышущаяся лента света между узлом и жезлом. */
    public static void floatyBeam(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, int color) {
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level(), from, to, color));
    }

    private Fx() {
    }

    /** Огонёк произвольного цвета (FXWisp оригинала с явным цветом). */
    public static void wispColor(double x, double y, double z, float size, float r, float g, float b, boolean shrink, float gravity) {
        add(TcParticle.wisp(level(), x, y, z, size, r, g, b).gravity(gravity).shrink(shrink));
    }

    /** drawVentParticles оригинала: клуб пара цвета color, летящий в сторону (dx, dy, dz). */
    public static void vent(double x, double y, double z, double dx, double dy, double dz, int color, float size) {
        add(TcParticle.vent(level(), x, y, z, dx, dy, dz, color, size));
    }

    /** FXWispEG оригинала: тёмный дымок под стражем Элдрича. */
    public static void eldritchWisp(double x, double y, double z, net.minecraft.world.entity.Entity follow) {
        add(TcParticle.eldritch(level(), x, y, z, follow));
    }

    /** arcLightning оригинала: искра в цели и дуга до неё. */
    public static void arc(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float heightGain) {
        add(TcParticle.sparkle(level(), tx, ty, tz, 3f, r, g, b, 2).gravity(0f).noClip());
        Minecraft.getInstance().particleEngine.add(new ArcParticle(level(), x, y, z, tx, ty, tz, r, g, b, heightGain));
    }

    /** FXSonic оригинала: вопль стража Элдрича. */
    public static void sonic(int entityId) {
        net.minecraft.world.entity.Entity entity = level().getEntity(entityId);
        if (entity != null) {
            add(new SonicParticle(level(), entity, 10));
        }
    }

    /** burst оригинала: белая вспышка-кольцо. */
    public static void burst(double x, double y, double z, float size) {
        add(new BurstParticle(level(), x, y, z, size));
    }

    // ---------- слизь порчи ----------

    private static SplooshParticle splat(double x, double y, double z) {
        RandomSource random = level().random;
        return random.nextBoolean() ? new SplooshParticle(level(), x, y, z, 0.6f, 0f, 0.3f, 0.4f)
                : new SplooshParticle(level(), x, y, z, 0.3f, 0f, 0.3f, 0.6f);
    }

    /** splooshFX: лиловые брызги кольцом вокруг только что появившегося порченого существа. */
    public static void sploosh(net.minecraft.world.entity.Entity entity) {
        RandomSource random = level().random;
        for (int i = particleCount(10); i > 0; i--) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float spread = random.nextFloat() * 0.5f + 0.5f;
            add(splat(entity.getX() + Math.sin(angle) * spread, entity.getY() + random.nextFloat() * entity.getBbHeight(),
                    entity.getZ() + Math.cos(angle) * spread));
        }
    }

    /** taintsplosionFX: слизь, разлетающаяся во все стороны от взорвавшегося порченого крипера. */
    public static void taintsplosion(net.minecraft.world.entity.Entity entity) {
        RandomSource random = level().random;
        for (int i = particleCount(100); i > 0; i--) {
            double dx = Math.random() * 2.0 - 1.0;
            double dy = Math.random() * 2.0 - 1.0;
            double dz = Math.random() * 2.0 - 1.0;
            double speed = (Math.random() + Math.random() + 1.0) * 0.15 * 0.964 / Math.max(1.0e-4, Math.sqrt(dx * dx + dy * dy + dz * dz));
            add(splat(entity.getX(), entity.getY() + random.nextFloat() * entity.getBbHeight(), entity.getZ())
                    .motion(dx * speed, dy * speed + 0.1, dz * speed));
        }
    }

    /** tentacleAriseFX: щупальце пробивается из земли — летят слизь и комья того, из чего оно растёт. */
    public static void tentacleArise(net.minecraft.world.entity.Entity entity) {
        RandomSource random = level().random;
        float h = entity.getBbHeight();
        net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(entity.getX(), entity.getY() - 1, entity.getZ());
        net.minecraft.world.level.block.state.BlockState ground = level().getBlockState(below);
        for (int j = 0; j < 2f * h; j++) {
            float angle = random.nextFloat() * (float) Math.PI * h;
            float spread = (random.nextFloat() * 0.5f + 0.5f) * h * 0.25f;
            add(new SplooshParticle(level(), entity.getX() + Math.sin(angle) * spread, entity.getY(), entity.getZ() + Math.cos(angle) * spread,
                    0.4f, 0f, 0.4f, 0.5f));
            if (!ground.isAir()) {
                angle = random.nextFloat() * (float) Math.PI * h;
                spread = (random.nextFloat() * 0.5f + 0.5f) * h * 0.25f;
                level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, ground)
                        .setPos(below), entity.getX() + Math.sin(angle) * spread, entity.getY(), entity.getZ() + Math.cos(angle) * spread, 0, 0, 0);
            }
        }
    }

    /** slimeJumpFX: ярко-лиловые брызги вокруг прыгнувшего или упавшего таумического слизня. */
    public static void slimeJump(net.minecraft.world.entity.Entity entity, int size, int count) {
        RandomSource random = level().random;
        double y = (entity.getBoundingBox().minY + entity.getBoundingBox().maxY) / 2.0;
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float spread = (random.nextFloat() * 0.5f + 0.5f) * size * 0.5f;
            add(new SplooshParticle(level(), entity.getX() + Math.sin(angle) * spread, y, entity.getZ() + Math.cos(angle) * spread,
                    0.7f, 0f, 1f, 0.4f));
        }
    }
}
