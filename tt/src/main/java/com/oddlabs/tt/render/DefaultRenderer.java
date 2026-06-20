package com.oddlabs.tt.render;


import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.shader.DebugMeshShader;
import com.oddlabs.tt.render.shader.DebugShaderRenderer;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.state.GlobalUniforms;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.scenery.Sky;
import com.oddlabs.tt.scenery.Water;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.model.Target;
import com.oddlabs.tt.gui.ToolTip;
import com.oddlabs.tt.viewer.AmbientAudio;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.util.function.Consumer;

/**
 * The primary world renderer responsible for coordinating the rendering
 * of the landscape, units, buildings, and transient effects.
 */
public final class DefaultRenderer implements UIRenderer, AutoCloseable {
    private final @NonNull Picker picker;
    private final @NonNull Water water;
    private final @NonNull Sky sky;
    private final @NonNull LandscapeRenderer landscape_renderer;
    private final @NonNull World world;
    private final @NonNull ElementRenderer<?> element_renderer;
    private final @NonNull TreeRenderer tree_renderer;
    private final SpriteSorter sprite_sorter = new SpriteSorter();
    private final @NonNull RenderQueues render_queues;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final @NonNull Selection selection;
    private final @NonNull LightningRenderer lightningRenderer;
    private final @NonNull SonicBlastRenderer sonicBlastRenderer;
    private final InstancedSpriteRenderer treeSpriteRenderer = new InstancedSpriteRenderer();
    private final @NonNull PostProcessor postProcessor;
    private final @Nullable Cheat cheat;

    private final GlobalUniforms globalUniforms = new GlobalUniforms();

    private @Nullable Building selected_building;

    public DefaultRenderer(@Nullable Cheat cheat, @NonNull Player local_player, @NonNull RenderQueues render_queues,
            @NonNull WorldInfo<Texture> world_info, @NonNull LandscapeRenderer landscape_renderer,
            @NonNull Picker picker,
            @NonNull Selection selection, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.world = local_player.getWorld();
        this.cheat = cheat;
        this.render_queues = render_queues;
        this.picker = picker;
        this.selection = selection;
        this.element_renderer = new ElementRenderer<>(local_player, render_queues, picker, false, sprite_sorter,
                selection);
        this.tree_renderer = new TreeRenderer(cheat, sprite_sorter, picker.getRespondManager(), treeSpriteRenderer);
        this.landscape_renderer = landscape_renderer;
        this.sky = new Sky(landscape_renderer, world_info.terrain(), world_info.detail(), world_info.detailNormal());
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.water = new Water(world.getHeightMap(), world_info.terrain(), sky, modelViewStack);
        this.landscape_renderer.setWater(this.water);
        this.lightningRenderer = new LightningRenderer();
        this.sonicBlastRenderer = new SonicBlastRenderer();
        var context = Renderer.getRenderer().getRenderContext();
        this.postProcessor = new PostProcessor(context.getViewportWidth(), context.getViewportHeight());
        DebugRender.setShaderRenderer(new DebugShaderRenderer(new DebugMeshShader(), modelViewStack, projectionStack));
    }

    private void drawAxes() {
        float center = world.getHeightMap().getMetersPerWorld() / 2f;
        float z = world.getHeightMap().getNearestHeight(center, center);
        DebugRender.drawAxes(center, z);
    }

    @Override
    public boolean isCheater() {
        return cheat != null && cheat.isEnabled();
    }

    public void setSelectedBuilding(@Nullable Building building) {
        this.selected_building = building;
    }

    private void renderRallyPoint(@NonNull RenderContext context, @NonNull CameraState camera_state) {
        if (selected_building != null && !selected_building.isDead() && selected_building.hasRallyPoint())
            doRenderRallyPoint(context, camera_state,
                    selected_building.getRallyPoint(), VisualRegistry.getInstance().getRallyPoint(selected_building
                            .getOwner().getRaceInfo().getRaceType()),
                    SelectableVisitor.getTeamColor(selected_building));
    }

    private void doRenderRallyPoint(@NonNull RenderContext context, @NonNull CameraState camera_state,
            @NonNull Target rally_point, @NonNull SpriteKey rally_sprite, Color.@NonNull Linear teamColor) {

        SpriteRenderer rally_point_renderer = render_queues.getRenderer(rally_sprite);

        float x = rally_point.getPositionX();
        float y = rally_point.getPositionY();
        float z = world.getHeightMap().getNearestHeight(rally_point.getPositionX(), rally_point.getPositionY());
        if (rally_point instanceof Building rally_building) {
            var rally = rally_building.getTemplate().getRally();
            x += rally.x();
            y += rally.y();
            z += rally.z();
        }

        Matrix4f modelMatrix = new Matrix4f();
        float dx = camera_state.getCurrentX() - x;
        float dy = camera_state.getCurrentY() - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.1f) {
            float angle = (float) Math.atan2(dy / len, dx / len);
            modelMatrix.translation(x, y, z).rotate(angle, 0f, 0f, 1f);
        } else {
            modelMatrix.translation(x, y, z);
        }

        rally_point_renderer.addInstance(
                0, // spriteIndex
                0, // animation
                0f, // animTicks
                false, // respond
                true,  // blend
                true,  // depthWrite
                true,  // depthTest
                modelMatrix,
                Color.Linear.WHITE,
                teamColor
        );
    }

    @Override
    public void pickHover(boolean can_hover_behind, @NonNull CameraState camera, int x, int y) {
        if (can_hover_behind) {
            var localInput = Renderer.getLocalInput();
            picker.pickHoverPhysical(camera, localInput.getMouseX(), localInput.getMouseY());
        } else {
            picker.resetCurrentHovered();
        }
    }

    @Override
    public @Nullable ToolTip getToolTip() {
        return picker.getCurrentToolTip();
    }

    private @NonNull TreeRenderer getTreeRenderer() {
        return tree_renderer;
    }

    private void renderDebugElements(@NonNull CameraState frustum_state) {
        if (Globals.draw_axes) drawAxes();
        landscape_renderer.debugRender(frustum_state);
        lightningRenderer.debugRender(element_renderer.getRenderState().getLightningQueue());
        render_queues.getEmitterRenderer().debugRender(element_renderer.getRenderState().getEmitterQueue());
        tree_renderer.debugRender(tree_renderer.getRenderLists(), tree_renderer.getRespondRenderLists());

        if (Globals.isBoundsEnabled(BoundingMode.REGIONS))
            world.getUnitGrid().debugRenderRegions(frustum_state.getCurrentX(), frustum_state.getCurrentY());
        if (Globals.isBoundsEnabled(BoundingMode.OCCUPATION)) picker.debugRender();
        if (Globals.isBoundsEnabled(BoundingMode.UNIT_GRID)) {
            world.getUnitGrid().debugRender(frustum_state.getCurrentX(), frustum_state.getCurrentY());
            for (Object obj : selection.getCurrentSelection().getSet()) {
                if (obj instanceof Unit unit) unit.debugRender();
            }
        }
        DebugRender.flush();
    }

    @Override
    public void startFrame(@NonNull RenderContext context) {
        postProcessor.bindSceneFBO(context);
        context.clear(true, true);
    }

    @Override
    public void endFrame(@NonNull RenderContext context, @NonNull Consumer<@NonNull RenderContext> guiRenderCallback) {
        postProcessor.renderComposite(context, guiRenderCallback);
    }

    @Override
    public void render(@NonNull RenderContext context, @NonNull AmbientAudio ambient,
            @NonNull CameraState frustum_state, @NonNull GUIRoot gui_root) {
        treeSpriteRenderer.clear();
        render_queues.getInstancedRenderer().clear();

        postProcessor.resize(frustum_state.getWidth(), frustum_state.getHeight());
        postProcessor.bindSceneFBO(context);
        context.setDrawBuffers(true); // Ensure both Color and Mask are cleared
        context.setColorMask(true, true, true, true);
        context.setDepthMask(true);
        context.setDepthTest(true);
        context.setDepthFunc(GL11.GL_LEQUAL);
        context.clear(true, true);

        context.setViewport(0, 0, frustum_state.getWidth(), frustum_state.getHeight());

        // Update Global UBO
        try (var stack = MemoryStack.stackPush()) {
            java.nio.ByteBuffer buf = stack.malloc(512);
            globalUniforms.update(frustum_state, Renderer.getRenderer().getEventQueue().getTime(),
                    world.getHeightMap().getSeaLevelMeters(), water, buf);
            buf.flip();
            context.updateGlobalState(buf);
        }

        ambient.updateSoundListener(frustum_state, world.getHeightMap());
        modelViewStack.current().set(frustum_state.getModelView());
        projectionStack.current().set(frustum_state.getProjectionMatrix());

        if (Globals.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        // Sky & Landscape don't write to mask -> Disable Mask Buffer
        context.setDrawBuffers(false);

        if (Globals.draw_sky) {
            sky.render(context, frustum_state, modelViewStack, projectionStack);
            sky.renderSeaBottom(context, frustum_state, modelViewStack, projectionStack);
        }

        if (Globals.process_landscape) {
            landscape_renderer.prepareAll(frustum_state, false);
            landscape_renderer.render(context, frustum_state, modelViewStack, projectionStack);
        }
        // Trees & Units write to mask -> Enable Mask Buffer
        context.setDrawBuffers(true);

        if (Globals.process_trees) {
            tree_renderer.setup(frustum_state);
            tree_renderer.visit(world.getTreeRoot());
        }
        if (Globals.process_misc) {
            element_renderer.setup(frustum_state);
            element_renderer.visit(world.getElementRoot());
        }

        // Process transient effects (smoke, lightning, fragments) immediately after visitation.
        if (Globals.process_misc) {
            var renderState = element_renderer.getRenderState();
            render_queues.getEmitterRenderer().prepare(render_queues, renderState.getEmitterQueue(), frustum_state,
                    modelViewStack);
            lightningRenderer.prepare(renderState.getLightningQueue());
            sonicBlastRenderer.prepare(renderState.getSonicBlastQueue());
        }
        sprite_sorter.distributeModels();
        if (Globals.process_shadows) {
            render_queues.renderShadows(context, landscape_renderer, modelViewStack, projectionStack);
            if (Globals.process_trees) {
                tree_renderer.renderShadows((SelectableShadowRenderer) render_queues.getDefaultShadowRenderer());
            }
        }

        if (Globals.process_trees) {
            tree_renderer.render(context, frustum_state, modelViewStack, projectionStack);
        }
        if (Globals.process_misc) {
            render_queues.renderAll(context, frustum_state, projectionStack);

            // Render trees AFTER opaque units/misc.
            // Trees use Alpha-to-Coverage with Depth-Write enabled.
            // Separate renderer ensures they are flushed here.
            treeSpriteRenderer.renderAll(context, frustum_state, projectionStack);

            render_queues.renderPlants(context, frustum_state, projectionStack);

            render_queues.renderNoDetail();
        }

        gui_root.getDelegate().render3D(landscape_renderer, render_queues, frustum_state, modelViewStack,
                projectionStack);

        if (Globals.debugRenderingEnabled()) {
            renderDebugElements(frustum_state);
        }

        // Enable Mask Buffer for Water interaction (occluding submerged unit outlines)
        context.setDrawBuffers(true);

        if (Globals.draw_water) {
            water.render(context, frustum_state, landscape_renderer.getVisiblePatches());
        }

        if (Globals.process_misc)
            render_queues.renderBlends(context, frustum_state, projectionStack);

        // Water & Particles don't write to mask -> Disable Mask Buffer
        context.setDrawBuffers(false);

        // Copy depth buffer for Soft Particles (smoke/effects)
        postProcessor.copyDepthBuffer();

        // Render transient effects (smoke, lightning) AFTER all other scene objects.
        // This ensures they are depth-tested against the complete scene (including water and blended units).
        lightningRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack);
        render_queues.renderParticles(context, frustum_state, modelViewStack, projectionStack, postProcessor
                .getDepthCopyTexture());
        sonicBlastRenderer.render(context, render_queues, frustum_state, modelViewStack, projectionStack);
        // Rally point uses SpriteShader (Mask) -> Enable
        context.setDrawBuffers(true);
        renderRallyPoint(context, frustum_state);
        render_queues.getInstancedRenderer().renderAll(context, frustum_state, projectionStack);

        assert ShaderProgram.activeShader() == null : "Shader still active=" + ShaderProgram.activeShader();

        if (Globals.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }

        // Ensure Mask is enabled for GUI clearing
        context.setDrawBuffers(true);

        if (Globals.debugRenderingEnabled()) {
            context.validate();
        }
    }

    private boolean closed = false;

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            lightningRenderer.close();
            sonicBlastRenderer.close();
            sky.close();
            water.close();
            tree_renderer.close();
            treeSpriteRenderer.close();
            postProcessor.close();
        }
    }
}
