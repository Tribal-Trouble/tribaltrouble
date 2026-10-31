package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.BuildingSiteScanFilter;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/** The translucent building and the site markers shown while a building is being placed. */
@NullMarked
public final class BuildingGhostRenderer {
    private static final int GRID_RADIUS = 20;

    private final BuildingSiteRenderer site_renderer = new BuildingSiteRenderer();
    private final SpriteShader spriteShader = new SpriteShader();

    public void render(World world, Player owner, BuildingTemplate template, int placing_center_grid_x,
            int placing_center_grid_y, LandscapeRenderer renderer, RenderQueues queues,
            MatrixStack modelViewStack, MatrixStack projectionStack) {
        UnitGrid unit_grid = world.getUnitGrid();
        int placing_grid_x = placing_center_grid_x - (template.getPlacingSize() - 1);
        int placing_grid_y = placing_center_grid_y - (template.getPlacingSize() - 1);

        float center_x = HeightMap.METERS_PER_UNIT_GRID * (placing_grid_x + (template.getPlacingSize() - .5f));
        float center_y = HeightMap.METERS_PER_UNIT_GRID * (placing_grid_y + (template.getPlacingSize() - .5f));

        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(owner, template, GRID_RADIUS, false);
        unit_grid.scan(filter, placing_center_grid_x, placing_center_grid_y);
        List<LandscapeTarget> target_list = filter.getResult();

        RenderContext context = Renderer.getRenderer().getRenderContext();
        site_renderer.renderSites(context, renderer, modelViewStack, projectionStack, target_list, center_x, center_y,
                2 * GRID_RADIUS);
        com.oddlabs.tt.util.GLUtils.checkGLError("Placing: After renderSites");

        SpriteRenderer built_renderer = queues.getRenderer(template.getBuiltRenderer());
        Sprite sprite = built_renderer.getSpriteList().getSprite(0);

        try (var _ = spriteShader.use()) {

            spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.5f);
            sprite.setupShaderUniforms(context, spriteShader, 0, false);
            spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, true);
            spriteShader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.5f);

            if (template.isPlacingLegal(owner, placing_center_grid_x, placing_center_grid_y))
                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 1f, 1f, .8f);
            else
                spriteShader.setUniform(SpriteShader.Uniforms.COLOR, 1f, 0f, 0f, .8f);

            float z = world.getHeightMap().getNearestHeight(center_x, center_y);

            modelViewStack.push();
            modelViewStack.translate(center_x, center_y, z);
            spriteShader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());

            try (var _ = context.withCullMode(CullMode.BACK)) {
                // Pass 1: Depth Prime (Write Depth, No Color)
                try (var _ = context.withDepthMode(DepthMode.READ_WRITE); var _ = context.withColorMask(false, false,
                        false, false); var _ = context.withBlendMode(BlendMode.NONE)) {

                    sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
                }

                // Pass 2: Color Render (No Depth Write, Equal Depth)
                try (var _ = context.withDepthMode(DepthMode.READ_ONLY); var _ = context.withColorMask(true, true, true,
                        true); var _ = context.withBlendMode(BlendMode.ALPHA)) {

                    sprite.renderShader(spriteShader, 0, 0f, built_renderer.getSpriteList());
                }
            } finally {
                spriteShader.setUniform(SpriteShader.Uniforms.DESATURATE, 0.0f);
                spriteShader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, false);
                spriteShader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.3f);
            }

            modelViewStack.pop();
        }
    }
}
