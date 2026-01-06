package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.form.WarningForm;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.shader.DebugShaderRenderer;
import com.oddlabs.tt.render.shader.FixedFunctionShader;
import com.oddlabs.tt.render.shader.LitShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.scenery.Sky;
import com.oddlabs.tt.scenery.Water;
import com.oddlabs.tt.util.DebugRender;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.viewer.AmbientAudio;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.Selection;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

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
    private final @NonNull Cheat cheat;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final @NonNull Selection selection;
    private final @NonNull EmitterRenderer emitterRenderer;
    private final @NonNull LightningRenderer lightningRenderer;
    private final @NonNull SonicBlastRenderer sonicBlastRenderer;
    private final @NonNull InstancedSpriteRenderer treeSpriteRenderer = new InstancedSpriteRenderer();
    private final @NonNull PostProcessor postProcessor;

    private @Nullable Building selected_building;

    public DefaultRenderer(@NonNull Cheat cheat, @NonNull Player local_player, @NonNull RenderQueues render_queues, @NonNull WorldInfo world_info, @NonNull LandscapeRenderer landscape_renderer, @NonNull Picker picker, @NonNull Selection selection, @NonNull WorldGenerator generator, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.world = local_player.getWorld();
        this.cheat = cheat;
        this.render_queues = render_queues;
        this.picker = picker;
        this.selection = selection;
        this.element_renderer = new ElementRenderer<>(local_player, render_queues, picker, false, sprite_sorter, selection);
        this.tree_renderer = new TreeRenderer(cheat, sprite_sorter, picker.getRespondManager(), treeSpriteRenderer);
        this.landscape_renderer = landscape_renderer;
        this.sky = new Sky(landscape_renderer, generator.getTerrainType(), world_info.detail);
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.water = new Water(world.getHeightMap(), generator.getTerrainType(), sky, modelViewStack, projectionStack);
        this.emitterRenderer = new EmitterRenderer();
        this.lightningRenderer = new LightningRenderer();
        this.sonicBlastRenderer = new SonicBlastRenderer();
        this.postProcessor = new PostProcessor(Settings.getSettings().view_width, Settings.getSettings().view_height);
        DebugRender.setShaderRenderer(new DebugShaderRenderer(new FixedFunctionShader(), modelViewStack, projectionStack));
    }

    private void drawAxes() {
        float center = world.getHeightMap().getMetersPerWorld() / 2f;
        float z = world.getHeightMap().getNearestHeight(center, center);
        DebugRender.drawAxes(center, z);
    }

    public boolean isCheater() {
        return cheat.isEnabled();
    }

    public void setSelectedBuilding(@Nullable Building building) {
        this.selected_building = building;
    }

    private void renderRallyPoint(@NonNull CameraState camera_state) {
        if (selected_building != null && !selected_building.isDead() && selected_building.hasRallyPoint())
            doRenderRallyPoint(camera_state);
    }

    private static final SpriteShader spriteShader = new SpriteShader(); // For rally point

    private void doRenderRallyPoint(@NonNull CameraState camera_state) {
        try (var _ = spriteShader.use();
             var _ = GLStateHelper.blend(true);
             var _ = camera_state.getFog().setup(spriteShader, camera_state.getCurrentZ())) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            Target rally_point = selected_building.getRallyPoint();
            Race race = selected_building.getOwner().getRace();
            SpriteRenderer rally_point_renderer = render_queues.getRenderer(race.getRallyPoint());
            Sprite sprite = rally_point_renderer.getSpriteList().getSprite(0);

            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            spriteShader.setUniform(LitShader.LIGHT_DIR, -1f, 0f, 1f);
            spriteShader.setUniform(LitShader.GLOBAL_AMBIENT, 0.65f, 0.65f, 0.65f);

            sprite.setupShaderUniforms(spriteShader, 0, false);

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
            picker.pickHover(camera, LocalInput.getMouseX(), LocalInput.getMouseY());
        } else {
            picker.resetCurrentHovered();
        }
        // Ensure PostProcessor is sized correctly (hacky place but ensure it runs on resize)
        postProcessor.resize(Settings.getSettings().view_width, Settings.getSettings().view_height);
    }

    @Override
    public @Nullable ToolTip getToolTip() {
        return picker.getCurrentToolTip();
    }

    public @NonNull TreeRenderer getTreeRenderer() {
        return tree_renderer;
    }

    @Override
    public boolean clearColorBuffer() {
        return false; // We handle clearing manually in the FBO pass now
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
    public void startFrame() {
        postProcessor.bindSceneFBO();
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void endFrame() {
        postProcessor.renderComposite();
    }

    @Override
    public void render(@NonNull AmbientAudio ambient, @NonNull CameraState frustum_state, @NonNull GUIRoot gui_root, @NonNull Matrix4f proj, @NonNull Matrix4f modelView) {
        ambient.updateSoundListener(frustum_state, world.getHeightMap());

        modelViewStack.current().set(modelView);
        projectionStack.current().set(proj);

        if (Globals.line_mode || cheat.line_mode) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        }

        if (Globals.draw_sky) {
            sky.render(frustum_state, modelViewStack, projectionStack);
            sky.renderSeaBottom(frustum_state, modelViewStack, projectionStack);
        }

        if (Globals.process_landscape) {
            landscape_renderer.prepareAll(frustum_state, false);
            landscape_renderer.renderAll(frustum_state, modelViewStack, projectionStack);
        }
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
            render_queues.renderShadows(landscape_renderer, modelViewStack, projectionStack);
        }

        if (Globals.process_trees) {
            tree_renderer.renderAll(frustum_state, modelViewStack, projectionStack);
        }
        if (Globals.process_misc) {
            render_queues.renderAll(frustum_state, projectionStack);
            
            // Render trees AFTER opaque units/misc to handle correct sorting for accumulation
            // Trees are blended but depth-write disabled (or handled via separate renderer)
            // Separate renderer ensures they are flushed here.
            treeSpriteRenderer.renderAll(frustum_state, projectionStack);
            
            render_queues.renderPlants(frustum_state, projectionStack);
            
            render_queues.renderNoDetail();
        }

        gui_root.getDelegate().render3D(landscape_renderer, render_queues, frustum_state, modelViewStack, projectionStack);

        if (Globals.debugRenderingEnabled()) {
            renderDebugElements(frustum_state);
        }

        if (Globals.draw_water) {
            water.render(frustum_state, landscape_renderer.getVisiblePatches());
        }

        if (Globals.process_misc)
            render_queues.renderBlends(frustum_state, projectionStack);

        lightningRenderer.render(render_queues, element_renderer.getRenderState().getLightningQueue(), frustum_state, modelViewStack, projectionStack);
        emitterRenderer.render(render_queues, element_renderer.getRenderState().getEmitterQueue(), frustum_state, modelViewStack, projectionStack);
        
        if (world.getRacesResources() != null) {
            sonicBlastRenderer.render(render_queues, element_renderer.getRenderState().getSonicBlastQueue(), frustum_state, modelViewStack, projectionStack, world.getRacesResources().getPoisonTextures()[0]);
        }
        
        renderRallyPoint(frustum_state);

        assert ShaderProgram.activeShader() == null : "Shader still active=" + ShaderProgram.activeShader();

        if (Globals.line_mode || cheat.line_mode) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
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