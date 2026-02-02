package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.shader.DebugMeshShader;
import com.oddlabs.tt.render.shader.DebugShaderRenderer;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.GlobalUniforms;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.scenery.Sky;
import com.oddlabs.tt.scenery.Water;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.AmbientAudio;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.Selection;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

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
    private final @NonNull EmitterRenderer emitterRenderer;
    private final @NonNull LightningRenderer lightningRenderer;
    private final @NonNull SonicBlastRenderer sonicBlastRenderer;
    private final @NonNull InstancedSpriteRenderer treeSpriteRenderer = new InstancedSpriteRenderer();
    private final @NonNull PostProcessor postProcessor;
    private final @Nullable Cheat cheat;
    
    private final GlobalUniforms globalUniforms = new GlobalUniforms();
    private final Vector3f sunDirection = new Vector3f(-1f, 0f, 1f).normalize();
    private final Vector3f globalAmbient = new Vector3f(0.4f, 0.4f, 0.4f);

    private @Nullable Building selected_building;

    private void setDrawBuffers(boolean mask) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.IntBuffer buffers;
            if (mask) {
                buffers = stack.mallocInt(2).put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1);
            } else {
                buffers = stack.mallocInt(1).put(GL30.GL_COLOR_ATTACHMENT0);
            }
            buffers.flip();
            GL20.glDrawBuffers(buffers);
        }
    }

    public DefaultRenderer(@Nullable Cheat cheat, @NonNull Player local_player, @NonNull RenderQueues render_queues, @NonNull WorldInfo world_info, @NonNull LandscapeRenderer landscape_renderer, @NonNull Picker picker, @NonNull Selection selection, @NonNull WorldGenerator generator, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.world = local_player.getWorld();
        this.cheat = cheat;
        this.render_queues = render_queues;
        this.picker = picker;
        this.selection = selection;
        this.element_renderer = new ElementRenderer<>(local_player, render_queues, picker, false, sprite_sorter, selection);
        this.tree_renderer = new TreeRenderer(cheat, sprite_sorter, picker.getRespondManager(), treeSpriteRenderer);
        this.landscape_renderer = landscape_renderer;
        this.sky = new Sky(landscape_renderer, generator.getTerrainType(), world_info.detail());
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.water = new Water(world.getHeightMap(), generator.getTerrainType(), sky, modelViewStack, projectionStack);
        this.emitterRenderer = new EmitterRenderer();
        this.lightningRenderer = new LightningRenderer();
        this.sonicBlastRenderer = new SonicBlastRenderer();
        var window = Renderer.getRenderer().getWindow();
        this.postProcessor = new PostProcessor(window.getWidth(), window.getHeight());
        DebugRender.setShaderRenderer(new DebugShaderRenderer(new DebugMeshShader(), modelViewStack, projectionStack));
    }

    private void drawAxes() {
        float center = world.getHeightMap().getMetersPerWorld() / 2f;
        float z = world.getHeightMap().getNearestHeight(center, center);
        DebugRender.drawAxes(center, z);
    }

    public boolean isCheater() {
        return cheat != null && cheat.isEnabled();
    }

    public void setSelectedBuilding(@Nullable Building building) {
        this.selected_building = building;
    }

    private void renderRallyPoint(@NonNull RenderContext context, @NonNull CameraState camera_state) {
        if (selected_building != null && !selected_building.isDead() && selected_building.hasRallyPoint())
            doRenderRallyPoint(context, camera_state);
    }

    private static final SpriteShader spriteShader = new SpriteShader(); // For rally point

    private void doRenderRallyPoint(@NonNull RenderContext context, @NonNull CameraState camera_state) {
        try (var _ = spriteShader.use();
             var _ = context.withBlendMode(BlendMode.ALPHA)) {

            Target rally_point = selected_building.getRallyPoint();
            Race race = selected_building.getOwner().getRace();
            SpriteRenderer rally_point_renderer = render_queues.getRenderer(race.getRallyPoint());
            Sprite sprite = rally_point_renderer.getSpriteList().getSprite(0);

            sprite.setupShaderUniforms(context, spriteShader, 0, false);

            float x = rally_point.getPositionX();
            float y = rally_point.getPositionY();
            float z = world.getHeightMap().getNearestHeight(rally_point.getPositionX(), rally_point.getPositionY());
            if (rally_point instanceof Building rally_building) {
                x += rally_building.getTemplate().getRallyX();
                y += rally_building.getTemplate().getRallyY();
                z += rally_building.getTemplate().getRallyZ();
            }

            modelViewStack.push();
            float dx = camera_state.getCurrentX() - x;
            float dy = camera_state.getCurrentY() - y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.1f) {
                RenderTools.translateAndRotate(x, y, z, dx / len, dy / len, modelViewStack);
            } else {
                modelViewStack.translate(x, y, z);
            }

            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());

            var teamColor = SelectableVisitor.getTeamColor(selected_building);
            spriteShader.setUniform(SpriteShader.Uniforms.DECAL_COLOR, teamColor.x(), teamColor.y(), teamColor.z(), 1f);
            spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, 1f);

            sprite.renderShader(spriteShader, 0, 0f, rally_point_renderer.getSpriteList());

            modelViewStack.pop();
        }
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

    public @NonNull TreeRenderer getTreeRenderer() {
        return tree_renderer;
    }

    private void renderDebugElements(@NonNull CameraState frustum_state) {
        if (Globals.draw_axes) drawAxes();
        landscape_renderer.debugRender(frustum_state);
        lightningRenderer.debugRender(element_renderer.getRenderState().getLightningQueue());
        emitterRenderer.debugRender(element_renderer.getRenderState().getEmitterQueue());
        tree_renderer.debugRender(tree_renderer.getRenderLists(), tree_renderer.getRespondRenderLists());

        if (Globals.isBoundsEnabled(BoundingMode.REGIONS)) world.getUnitGrid().debugRenderRegions(frustum_state.getCurrentX(), frustum_state.getCurrentY());
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
        postProcessor.bindSceneFBO();
        context.clear(true, true);
    }

    @Override
    public void endFrame(@NonNull RenderContext context) {
        postProcessor.renderComposite(context);
    }

    @Override
    public void render(@NonNull RenderContext context, @NonNull AmbientAudio ambient, @NonNull CameraState frustum_state, @NonNull GUIRoot gui_root) {
        if (postProcessor.resize(frustum_state.getWidth(), frustum_state.getHeight())) {
            postProcessor.bindSceneFBO();
            context.clear(true, true);
        }
        
        // Update Global UBO
        globalUniforms.update(frustum_state, sunDirection, globalAmbient, LocalEventQueue.getQueue().getTime());
        context.updateGlobalState(globalUniforms.getBuffer());

        ambient.updateSoundListener(frustum_state, world.getHeightMap());
        modelViewStack.current().set(frustum_state.getModelView());
        projectionStack.current().set(frustum_state.getProjectionMatrix());

        if (Globals.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        // Sky & Landscape don't write to mask -> Disable Mask Buffer
        setDrawBuffers(false);

        if (Globals.draw_sky) {
            sky.render(context, frustum_state, modelViewStack, projectionStack);
            sky.renderSeaBottom(context, frustum_state, modelViewStack, projectionStack);
        }

        if (Globals.process_landscape) {
            landscape_renderer.prepareAll(frustum_state, false);
            landscape_renderer.render(context, frustum_state, modelViewStack, projectionStack);
        }
        // Trees & Units write to mask -> Enable Mask Buffer
        setDrawBuffers(true);

        if (Globals.process_trees) {
            tree_renderer.setup(frustum_state);
            world.getTreeRoot().visit(tree_renderer);
        }
        if (Globals.process_misc) {
            element_renderer.setup(frustum_state);
            world.getElementRoot().visit(element_renderer);
        }

        sprite_sorter.distributeModels();

        if (Globals.process_shadows) {
            render_queues.renderShadows(context, landscape_renderer, modelViewStack, projectionStack);
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

        gui_root.getDelegate().render3D(landscape_renderer, render_queues, frustum_state, modelViewStack, projectionStack);

        if (Globals.debugRenderingEnabled()) {
            renderDebugElements(frustum_state);
        }

        // Water & Particles don't write to mask -> Disable Mask Buffer
        setDrawBuffers(false);

        if (Globals.draw_water) {
            water.render(context, frustum_state, landscape_renderer.getVisiblePatches());
        }

        if (Globals.process_misc)
            render_queues.renderBlends(context, frustum_state, projectionStack);

        lightningRenderer.render(context, render_queues, element_renderer.getRenderState().getLightningQueue(), frustum_state, modelViewStack, projectionStack);
        emitterRenderer.render(context, render_queues, element_renderer.getRenderState().getEmitterQueue(), frustum_state, modelViewStack, projectionStack);

        if (world.getRacesResources() != null) {
            sonicBlastRenderer.render(context, render_queues, element_renderer.getRenderState().getSonicBlastQueue(), frustum_state, modelViewStack, projectionStack, world.getRacesResources().getPoisonTextures()[0]);
        }

        // Rally point uses SpriteShader (Mask) -> Enable
        setDrawBuffers(true);
        renderRallyPoint(context, frustum_state);

        assert ShaderProgram.activeShader() == null : "Shader still active=" + ShaderProgram.activeShader();

        if (Globals.line_mode || (cheat != null && cheat.line_mode)) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        }

        // Ensure Mask is enabled for GUI clearing
        setDrawBuffers(true);

        if (Globals.debugRenderingEnabled()) {
            context.validate();
        }
    }

    @Override
    public void close() {
        emitterRenderer.close();
        lightningRenderer.close();
        sonicBlastRenderer.close();
        sky.close();
        water.close();
        treeSpriteRenderer.close();
        postProcessor.close();
    }
}
