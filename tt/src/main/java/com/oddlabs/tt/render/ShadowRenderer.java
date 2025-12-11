package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.shader.ShadowShader;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateHelper;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

abstract class ShadowRenderer {
    private final ShadowShader shader = new ShadowShader();

    protected @NonNull GLState setupShadows(@NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        GLState shaderState = shader.use();
        shader.setUniformMatrix4(ShadowShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
        shader.setUniformMatrix4(ShadowShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -1.0f);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        var blendState = GLStateHelper.blend(true);
        var depthMaskState = new GLStateHelper.DepthMask(false);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        return () -> {
            depthMaskState.close();
            blendState.close();
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glDepthFunc(GL11.GL_LESS); // Restore default
            shaderState.close();
        };
    }

    protected void setShadowColor(float r, float g, float b, float a) {
        shader.setUniform(ShadowShader.Uniforms.COLOR, r, g, b, a);
    }

    protected void bindShadowTexture(@NonNull Texture texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
        shader.setUniform(ShadowShader.Uniforms.TEXTURE, 0);
    }

    protected final void renderShadow(@NonNull LandscapeRenderer renderer, float shadow_size, float f_x, float f_y) {
        assert shadow_size >= 0;

        float texture_scale = 1f / shadow_size;
        float half_shadow_size = shadow_size * 0.5f;
        float texture_x = f_x - half_shadow_size;
        float texture_y = f_y - half_shadow_size;

        float offsetX = -texture_x * texture_scale;
        float offsetY = -texture_y * texture_scale;
        shader.setUniform(ShadowShader.Uniforms.SHADOW_PARAMS, texture_scale, texture_scale, offsetX, offsetY);

        int meters_per_grid_unit = HeightMap.METERS_PER_UNIT_GRID;
        int grid_units_per_patch = renderer.getHeightMap().getGridUnitsPerPatch();
        int grid_start_x = Math.max(0, (int) (texture_x / meters_per_grid_unit));
        int grid_start_y = Math.max(0, (int) (texture_y / meters_per_grid_unit));
        int max_grid_index = renderer.getHeightMap().getGridUnitsPerWorld() - 1;
        int grid_end_x = Math.min(max_grid_index, (int) ((f_x + half_shadow_size) / meters_per_grid_unit));
        int grid_end_y = Math.min(max_grid_index, (int) ((f_y + half_shadow_size) / meters_per_grid_unit));
        int patch_start_x = grid_start_x / grid_units_per_patch;
        int patch_start_y = grid_start_y / grid_units_per_patch;
        int patch_end_x = grid_end_x / grid_units_per_patch;
        int patch_end_y = grid_end_y / grid_units_per_patch;

        for (int patch_y = patch_start_y; patch_y <= patch_end_y; patch_y++) {
            for (int patch_x = patch_start_x; patch_x <= patch_end_x; patch_x++) {
                int local_start_x = Math.max(grid_start_x, patch_x * grid_units_per_patch) & (grid_units_per_patch - 1);
                int local_start_y = Math.max(grid_start_y, patch_y * grid_units_per_patch) & (grid_units_per_patch - 1);
                int local_end_x = Math.min(grid_end_x, (patch_x + 1) * grid_units_per_patch - 1) & (grid_units_per_patch - 1);
                int local_end_y = Math.min(grid_end_y, (patch_y + 1) * grid_units_per_patch - 1) & (grid_units_per_patch - 1);
                renderer.renderShadow(shader, patch_x, patch_y, local_start_x, local_start_y, local_end_x, local_end_y);
            }
        }
    }
}
