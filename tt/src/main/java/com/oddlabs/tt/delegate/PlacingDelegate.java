package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
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
import java.util.logging.Logger;

public final class PlacingDelegate extends ControllableCameraDelegate {
    private static final Logger logger = Logger.getLogger(PlacingDelegate.class.getName());
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
        if (!getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit)) {
            logger.info("placeObject: Pick failed (off map?)");
            return;
        }
        UnitGrid unit_grid = getViewer().getWorld().getUnitGrid();
        int placing_grid_x = UnitGrid.toGridCoordinate(landscape_hit.x);
        int placing_grid_y = UnitGrid.toGridCoordinate(landscape_hit.y);
        if (Building.isPlacingLegal(getViewer().getWorld().getUnitGrid(), getTemplate(), placing_grid_x, placing_grid_y)) {
            var peons = getViewer().getSelection().getCurrentSelection().filter(Abilities.BUILD);
            if (peons.length > 0) {
                logger.info("placeObject: Placing building at " + placing_grid_x + "," + placing_grid_y);
                getViewer().getPeerHub().getPlayerInterface().placeBuilding(peons, building_index, placing_grid_x, placing_grid_y);
            } else {
                logger.info("placeObject: No peons selected");
            }
            logger.info("placeObject: Popping delegate");
            pop();
        } else {
            logger.info("placeObject: Placement illegal");
        }
    }

	@Override
	public void handleInput(@NonNull InputEvent event) {
        if (event.consumeAction(GameAction.UI_ACTIVATE)) {
            if (event.getPhase() == InputPhase.RELEASED) {
                placeObject();
            }
            event.consume();
            return;
        }

		if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
			if (event.consumeAction(GameAction.UI_CANCEL)) {
				pop();
				event.consume();
				return;
			}
		}

		super.handleInput(event);
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
        if (!getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit)) return;
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
        com.oddlabs.tt.util.GLUtils.checkGLError("Placing: After renderSites");

        SpriteRenderer built_renderer = queues.getRenderer(getTemplate().getBuiltRenderer());
        Sprite sprite = built_renderer.getSpriteList().getSprite(0);

        try (var _ = spriteShader.use();
             var _ = state.getFog().setup(spriteShader, state)) {
            
                            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
                            spriteShader.setUniform(LitShader.LIGHT_DIR, -1f, 0f, 1f);
                            spriteShader.setUniform(LitShader.GLOBAL_AMBIENT, 0.4f, 0.4f, 0.4f);
                            spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.5f);
            sprite.setupShaderUniforms(spriteShader, 0, false);
            spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, true);
            spriteShader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.5f);

            if (Building.isPlacingLegal(unit_grid, getTemplate(), placing_center_grid_x, placing_center_grid_y))
                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, .8f);
            else
                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 0f, 0f, .8f);

            float z = getViewer().getWorld().getHeightMap().getNearestHeight(center_x, center_y);

            modelViewStack.push();
            modelViewStack.translate(center_x, center_y, z);
            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());

            try {
                try (var _ = GLStateHelper.cullFace(true)) {
                    GL11.glCullFace(GL11.GL_BACK);

                    // Pass 1: Depth Prime (Write Depth, No Color)
                    try (var _ = new GLStateHelper.DepthMask(true);
                         var _ = new GLStateHelper.ColorMask(false, false, false, false)) {
                        GL11.glEnable(GL11.GL_DEPTH_TEST);
                        GL11.glDepthFunc(GL11.GL_LEQUAL);
                        GL11.glDisable(GL11.GL_BLEND);
                        sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
                    }

                    // Pass 2: Color Render (No Depth Write, Equal Depth)
                    try (var _ = new GLStateHelper.DepthMask(false);
                         var _ = new GLStateHelper.ColorMask(true, true, true, true)) {
                        GL11.glEnable(GL11.GL_DEPTH_TEST);
                        GL11.glDepthFunc(GL11.GL_EQUAL);
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
                    }
                }
            } finally {
                spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.0f);
                spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, false);
                spriteShader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.3f);
            }

            modelViewStack.pop();
        } finally {
            // Cleanup Global State
            GL11.glDepthFunc(GL11.GL_LESS);
            GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
