package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.BuildingSiteScanFilter;
import com.oddlabs.tt.render.BuildingSiteRenderer;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.Sprite;
import com.oddlabs.tt.render.SpriteRenderer;
import com.oddlabs.tt.render.shader.LitShader;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class PlacingDelegate extends ControllableCameraDelegate {
    private static final int GRID_RADIUS = 20;
    private static final LandscapeLocation landscape_hit = new LandscapeLocation();

    private final BuildingSiteRenderer site_renderer = new BuildingSiteRenderer();
    private final int building_index;
    private final SpriteShader spriteShader = new SpriteShader();

    public PlacingDelegate(@NonNull WorldViewer viewer, @NonNull CameraState old_camera, int building_index) {
        super(viewer, new GameCamera(viewer, old_camera));
        this.building_index = building_index;
    }

    private @NonNull BuildingTemplate getTemplate() {
        return getViewer().getLocalPlayer().getRace().getBuildingTemplate(building_index);
    }

    public void placeObject() {
        getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit);
        UnitGrid unit_grid = getViewer().getWorld().getUnitGrid();
        int placing_grid_x = UnitGrid.toGridCoordinate(landscape_hit.x);
        int placing_grid_y = UnitGrid.toGridCoordinate(landscape_hit.y);
        if (Building.isPlacingLegal(getViewer().getWorld().getUnitGrid(), getTemplate(), placing_grid_x, placing_grid_y)) {
            Selectable[] peons = getViewer().getSelection().getCurrentSelection().filter(Abilities.BUILD);
            if (peons.length > 0) {
                getViewer().getPeerHub().getPlayerInterface().placeBuilding(peons, building_index, placing_grid_x, placing_grid_y);
            }
            pop();
        }
    }

    @Override
    public void keyPressed(@NonNull KeyboardEvent event) {
        getCamera().keyPressed(event);
        switch (event.getKeyCode()) {
            case ESCAPE -> pop();
            default -> {
                if (event.getKeyCode() != Key.SPACE && event.getKeyCode() != Key.RETURN)
                    super.keyPressed(event);
            }
        }
    }

    @Override
    public void keyReleased(@NonNull KeyboardEvent event) {
        getCamera().keyReleased(event);
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        switch (button) {
            case LEFT -> placeObject();
            case RIGHT -> pop();
            default -> super.mousePressed(button, x, y);
        }
    }

    @Override
    public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues queues, @NonNull CameraState state, @NotNull MatrixStack modelViewStack, @NotNull MatrixStack projectionStack) {
        getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit);
        UnitGrid unit_grid = getViewer().getWorld().getUnitGrid();
        int placing_grid_x = UnitGrid.toGridCoordinate(landscape_hit.x) - (getTemplate().getPlacingSize() - 1);
        int placing_grid_y = UnitGrid.toGridCoordinate(landscape_hit.y) - (getTemplate().getPlacingSize() - 1);
        int placing_center_grid_x = UnitGrid.toGridCoordinate(landscape_hit.x);
        int placing_center_grid_y = UnitGrid.toGridCoordinate(landscape_hit.y);

        float center_x = HeightMap.METERS_PER_UNIT_GRID * (placing_grid_x + (getTemplate().getPlacingSize() - .5f));
        float center_y = HeightMap.METERS_PER_UNIT_GRID * (placing_grid_y + (getTemplate().getPlacingSize() - .5f));

        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(unit_grid, getTemplate(), GRID_RADIUS, false);
        unit_grid.scan(filter, placing_center_grid_x, placing_center_grid_y);
        List<LandscapeTarget> target_list = filter.getResult();
        site_renderer.renderSites(renderer, modelViewStack, projectionStack, target_list, center_x, center_y, 2 * GRID_RADIUS);

        try (var _ = spriteShader.use();
             var _ = state.getFog().setup(spriteShader, state.getCurrentZ())) {
            
            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            spriteShader.setUniform(LitShader.LIGHT_DIR, -1f, 0f, 1f);
            spriteShader.setUniform(LitShader.GLOBAL_AMBIENT, 0.65f, 0.65f, 0.65f);
            spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 1.0f);

            SpriteRenderer built_renderer = queues.getRenderer(getTemplate().getBuiltRenderer());
            Sprite sprite = built_renderer.getSpriteList().getSprite(0);

            sprite.setupShaderUniforms(spriteShader, 0, false);
            spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, true);

            if (Building.isPlacingLegal(unit_grid, getTemplate(), placing_center_grid_x, placing_center_grid_y))
                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, .5f);
            else
                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 0f, 0f, .5f);

            float z = getViewer().getWorld().getHeightMap().getNearestHeight(center_x, center_y);

            modelViewStack.push();
            modelViewStack.translate(center_x, center_y, z);
            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());

            // Pass 1: Depth Prime (No Color, Depth Write)
            try (var _ = new GLStateHelper.DepthMask(true)) {
                GL11.glColorMask(false, false, false, false);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDisable(GL11.GL_BLEND); // No blending for depth pass

                sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
            }

            // Pass 2: Color Render (Color, No Depth Write, Equal Depth)
            try (var _ = new GLStateHelper.DepthMask(false)) {
                GL11.glColorMask(true, true, true, true);
                GL11.glDepthFunc(GL11.GL_EQUAL);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
            }

            modelViewStack.pop();
        } finally {
            // Cleanup
            GL11.glDepthFunc(GL11.GL_LESS);
            GL11.glDisable(GL11.GL_BLEND);
            spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.0f);
            spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, false);
        }
    }
}
