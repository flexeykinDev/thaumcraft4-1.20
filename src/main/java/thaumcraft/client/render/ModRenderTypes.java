package thaumcraft.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Свои режимы отрисовки. Наследование от RenderType нужно только ради доступа к его защищённым настройкам. */
public final class ModRenderTypes extends RenderType {
    private static final ResourceLocation NODES = Thaumcraft.id("textures/misc/nodes.png");

    /** Свечение узла: цвета складываются с фоном. */
    public static final RenderType NODE_GLOW = node("node_glow", LIGHTNING_TRANSPARENCY, true);
    /** Тёмные слои узла (энтропия, пустота, тёмные и заражённые узлы): обычная прозрачность. */
    public static final RenderType NODE_DARK = node("node_dark", TRANSLUCENT_TRANSPARENCY, true);
    /** То же, но сквозь стены: так узел виден через таумометр. */
    public static final RenderType NODE_GLOW_SEE_THROUGH = node("node_glow_see_through", LIGHTNING_TRANSPARENCY, false);
    public static final RenderType NODE_DARK_SEE_THROUGH = node("node_dark_see_through", TRANSLUCENT_TRANSPARENCY, false);

    /** Стенки портативной дыры: шейдер портала в Край с текстурами оригинала. */
    public static final RenderType HOLE = create(Thaumcraft.MODID + ":hole", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS,
            256, false, false, CompositeState.builder()
                    .setShaderState(RENDERTYPE_END_PORTAL_SHADER)
                    .setTextureState(MultiTextureStateShard.builder()
                            .add(Thaumcraft.id("textures/misc/tunnel.png"), false, false)
                            .add(Thaumcraft.id("textures/misc/particlefield.png"), false, false).build())
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private static final java.util.Map<Integer, RenderType> WARD_OVERLAYS = new java.util.HashMap<>();

    /** Рунная рамка защищённого блока: одна из 47 «соединённых» текстур, цвета складываются с фоном. */
    public static RenderType wardOverlay(int index) {
        return wardOverlay(index, true);
    }

    /** @param depthTest false — рамка видна сквозь блоки (разметка области «Архитектора») */
    public static RenderType wardOverlay(int index, boolean depthTest) {
        return WARD_OVERLAYS.computeIfAbsent(depthTest ? index : -index, i -> create(Thaumcraft.MODID + ":ward_overlay_" + i,
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(Thaumcraft.id("textures/block/warded_glass_" + index + ".png"), false, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLayeringState(depthTest ? NO_LAYERING : THROUGH_BLOCKS)
                        .createCompositeState(false)));
    }

    private static final java.util.Map<ResourceLocation, RenderType> TRANSLUCENT_QUADS = new java.util.HashMap<>();

    /** Плоскость с обычным смешиванием (для «тёмных» аспектов, которые не светятся), видна с обеих сторон. */
    public static RenderType translucentQuad(ResourceLocation texture) {
        return TRANSLUCENT_QUADS.computeIfAbsent(texture, t -> create(Thaumcraft.MODID + ":translucent_" + t.getPath().replace('/', '_'),
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(t, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }

    private static final java.util.Map<ResourceLocation, RenderType> ADDITIVE = new java.util.HashMap<>();

    /** Светящаяся плоскость с любой текстурой: цвета складываются с фоном, видна с обеих сторон. */
    public static RenderType additive(ResourceLocation texture) {
        return ADDITIVE.computeIfAbsent(texture, t -> create(Thaumcraft.MODID + ":additive_" + t.getPath().replace('/', '_'),
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(t, false, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }

    /**
     * Настоящее отключение проверки глубины. Стандартное NO_DEPTH_TEST её не выключает, а лишь оставляет как есть,
     * и на этапе отрисовки мира (где глубина включена) плоскость внутри блока осталась бы скрытой.
     */
    private static final LayeringStateShard THROUGH_BLOCKS = new LayeringStateShard(Thaumcraft.MODID + "_through_blocks",
            com.mojang.blaze3d.systems.RenderSystem::disableDepthTest, com.mojang.blaze3d.systems.RenderSystem::enableDepthTest);

    private static final java.util.Map<ResourceLocation, RenderType> ADDITIVE_SEE_THROUGH = new java.util.HashMap<>();

    /** Как additive, но поверх всего: плоскость видна сквозь блоки. */
    public static RenderType additiveSeeThrough(ResourceLocation texture) {
        return ADDITIVE_SEE_THROUGH.computeIfAbsent(texture, t -> create(Thaumcraft.MODID + ":additive_through_" + t.getPath().replace('/', '_'),
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(t, false, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLayeringState(THROUGH_BLOCKS)
                        .createCompositeState(false)));
    }

    private static final java.util.Map<ResourceLocation, RenderType> ICONS = new java.util.HashMap<>();

    /** Значок поверх всего: обычная прозрачность, виден сквозь блоки (значки аспектов в очках откровения). */
    public static RenderType iconSeeThrough(ResourceLocation texture) {
        return ICONS.computeIfAbsent(texture, t -> create(Thaumcraft.MODID + ":icon_through_" + t.getPath().replace('/', '_'),
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(t, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setLayeringState(THROUGH_BLOCKS)
                        .createCompositeState(false)));
    }

    private static RenderType node(String name, TransparencyStateShard transparency, boolean depthTest) {
        return create(Thaumcraft.MODID + ":" + name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
                256, false, true, CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new TextureStateShard(NODES, false, false))
                        .setTransparencyState(transparency)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
                        .createCompositeState(false));
    }

    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling,
                           boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }
}
