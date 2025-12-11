package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.SpriteBatchRenderer;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.util.GLStateHelper;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.ArrayList;
import java.util.List;

final class SpriteBatch {
    private final List<@NonNull ModelState<?>> models = new ArrayList<>(256);
    private final SpriteBatchRenderer batchRenderer;

    SpriteBatch(SpriteBatchRenderer batchRenderer) {
        this.batchRenderer = batchRenderer;
    }

    void add(@NonNull ModelState<?> model) {
        models.add(model);
    }

    void render(@NonNull SpriteShader shader, @NonNull Sprite sprite, int tex_index, boolean respond, @NonNull SpriteList sprite_list, @NonNull CameraState camera_state) {
        if (models.isEmpty()) return;

        try (var _ = GLStateHelper.cullFace(sprite.culled);
             var _ = GLStateHelper.blend(sprite.modulateColor())) {

            sprite.setupShaderUniforms(shader, tex_index, respond);
            if (sprite.modulateColor()) {
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            }

            for (ModelState<?> model : models) {
                if (Globals.draw_misc) {
                    batchRenderer.getModelViewStack().push();
                    model.transform();
                    shader.setUniformMatrix4(SpriteShader.Uniforms.MODEL_VIEW_MATRIX, false, batchRenderer.getModelViewStack().current());

                    if (respond) {
                        shader.setUniform(SpriteShader.Uniforms.DECAL_COLOR, 1f, 1f, 1f, 1f);
                    } else {
                        float[] tc = model.getTeamColor();
                        shader.setUniform(SpriteShader.Uniforms.DECAL_COLOR, tc[0], tc[1], tc[2], tc.length > 3 ? tc[3] : 1f);
                    }

                    Vector4f color = model.getColor();
                    shader.setUniform(SpriteShader.Uniforms.COLOR, color.x, color.y, color.z, color.w);

                    sprite.renderShader(shader, model.getModel().getAnimation(), model.getModel().getAnimationTicks(), sprite_list);
                    batchRenderer.getModelViewStack().pop();
                }
            }
            
            // Reset texture units
            if (!sprite.modulateColor() && (sprite.hasTeamDecal() || respond)) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
            }
        }
    }

    public void debugRender() {
        if (Globals.isBoundsEnabled(BoundingMode.PLAYERS)) {
            for (ModelState<?> model : models) {
                RenderTools.draw(model.getModel());
            }
        }
    }

    void clear() {
        models.clear();
    }
}
