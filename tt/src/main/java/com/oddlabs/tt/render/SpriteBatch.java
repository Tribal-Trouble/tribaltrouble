package com.oddlabs.tt.render;

import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.SpriteBatchRenderer;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

final class SpriteBatch {
	private static final float[] RESPOND_COLOR = {1f, 1f, 1f};
	
	private final List<ModelState> models = new ArrayList<>(256);
    private final SpriteBatchRenderer batchRenderer;

    private static SpriteShader shader;
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    SpriteBatch(SpriteBatchRenderer batchRenderer) {
        this.batchRenderer = batchRenderer;
    }
	
	void add(@NonNull ModelState model) {
		models.add(model);
	}
	
	void render(@NonNull Sprite sprite, int tex_index, boolean respond, @NonNull SpriteList sprite_list) {
		if (models.isEmpty()) return;

        if (shader == null) {
            shader = new SpriteShader();
        }

        shader.use();
        batchRenderer.getProjectionStack().toBuffer(matrixBuffer);
        shader.setUniformMatrix4(SpriteShader.Uniforms.PROJECTION_MATRIX, false, matrixBuffer);
        shader.setUniform(SpriteShader.Uniforms.LIGHT_DIR, -1f, 0f, 1f); // Matches DefaultRenderer

		sprite.setupShader(shader, tex_index, respond, sprite_list);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);

        if (sprite.modulateColor()) {
            GL11.glEnable(GL11.GL_BLEND);
            // Assuming standard blending for modulate color sprites (transparency)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
		
		for (ModelState model : models) {
			if (Globals.isBoundsEnabled(BoundingMode.PLAYERS))
				RenderTools.draw(model.getModel());
			if (Globals.draw_misc) {
                batchRenderer.getModelViewStack().push();
				model.transform();

                batchRenderer.getModelViewStack().toBuffer(matrixBuffer);
                shader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, matrixBuffer);

                if (respond) {
                    shader.setUniform(SpriteShader.Uniforms.DECAL_COLOR, 1f, 1f, 1f, 1f);
                } else {
                    float[] tc = model.getTeamColor();
                    // getTeamColor returns float[3] usually. Shader needs vec4.
                    shader.setUniform(SpriteShader.Uniforms.DECAL_COLOR, tc[0], tc[1], tc[2], tc.length > 3 ? tc[3] : 1f);
                }

                Vector4f c = model.getColor();
                shader.setUniform(SpriteShader.Uniforms.COLOR, c.x, c.y, c.z, c.w);

				sprite.renderShader(shader, model.getModel().getAnimation(), model.getModel().getAnimationTicks(), sprite_list);
                batchRenderer.getModelViewStack().pop();
			}
		}
		
		sprite.resetShader(shader, respond, sprite.modulateColor());

        GL11.glPopAttrib();
        ShaderProgram.unbind();
	}
	
	void clear() {
		models.clear();
	}
}
