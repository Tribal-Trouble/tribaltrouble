package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.shader.PlacingShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Specialized renderer for drawing the translucent "ghost" building preview during placement.
 * Encapsulates the multi-pass depth-prime technique to prevent self-overlapping transparency artifacts.
 */
public final class PlacingRenderer implements AutoCloseable {
    private final PlacingShader shader = new PlacingShader();
    private final Map<SpriteList, VertexArray> vaos = new WeakHashMap<>();

    public void renderGhost(@NonNull RenderContext context, @NonNull Sprite sprite, @NonNull SpriteList spriteList,
            Color.@NonNull Linear color, @NonNull MatrixStack modelViewStack) {

        VertexArray vao = vaos.computeIfAbsent(spriteList, list -> {
            VertexArray newVao = new VertexArray();
            newVao.bind();

            int texCoordLoc = shader.getAttributeLocation(PlacingShader.Attributes.TEX_COORD);
            int posLoc = shader.getAttributeLocation(PlacingShader.Attributes.POSITION);
            int normLoc = shader.getAttributeLocation(PlacingShader.Attributes.NORMAL);

            list.getIndices().bind();

            if (texCoordLoc >= 0) {
                GL20.glEnableVertexAttribArray(texCoordLoc);
            }
            if (posLoc >= 0) {
                GL20.glEnableVertexAttribArray(posLoc);
            }
            if (normLoc >= 0) {
                GL20.glEnableVertexAttribArray(normLoc);
            }
            newVao.unbind();
            return newVao;
        });

        try (var _ = shader.use()) {
            shader.setUniform(PlacingShader.Uniforms.DESATURATE, 0.3f);

            // Setup uniform state
            context.setTexture(0, sprite.textures[0][Sprite.TEXTURE_NORMAL]);
            shader.setUniform(PlacingShader.Uniforms.TEXTURE_0, 0);

            boolean useLighting = Globals.draw_light && sprite.lighted;
            shader.setUniform(PlacingShader.Uniforms.ENABLE_LIGHTING, useLighting);
            shader.setUniform(PlacingShader.Uniforms.REPLACE_MODE, !useLighting && !sprite.modulate_color);

            if (sprite.modulate_color) {
                shader.setUniform(PlacingShader.Uniforms.MODULATE_COLOR, true);
                shader.setUniform(PlacingShader.Uniforms.ENABLE_TEAM_COLOR, false);
                shader.setUniform(PlacingShader.Uniforms.ALPHA_TEST_VALUE, 0.0f);
            } else {
                shader.setUniform(PlacingShader.Uniforms.MODULATE_COLOR, false);
                shader.setUniform(PlacingShader.Uniforms.ALPHA_TEST_VALUE, 0.3f);
                if (sprite.hasTeamDecal()) {
                    shader.setUniform(PlacingShader.Uniforms.ENABLE_TEAM_COLOR, true);
                    context.setTexture(1, sprite.textures[0][Sprite.TEXTURE_TEAM]);
                    shader.setUniform(PlacingShader.Uniforms.TEXTURE_1, 1);
                } else {
                    shader.setUniform(PlacingShader.Uniforms.ENABLE_TEAM_COLOR, false);
                }
            }

            if (sprite.hasBumpMap(0)) {
                shader.setUniform(PlacingShader.Uniforms.ENABLE_NORMAL_MAP, true);
                context.setTexture(2, sprite.textures[0][Sprite.TEXTURE_BUMP]);
                shader.setUniform(PlacingShader.Uniforms.NORMAL_MAP, 2);
            } else {
                shader.setUniform(PlacingShader.Uniforms.ENABLE_NORMAL_MAP, false);
            }

            shader.setUniform(PlacingShader.Uniforms.MODULATE_COLOR, true);
            shader.setUniform(PlacingShader.Uniforms.ALPHA_TEST_VALUE, 0.5f);
            shader.setUniform(PlacingShader.Uniforms.COLOR, color);
            shader.setUniform(PlacingShader.Uniforms.MODEL_VIEW_MATRIX, modelViewStack.current());

            try (var _ = context.withCullMode(CullMode.BACK)) {
                // Pass 1: Depth Prime (Write Depth, No Color)
                try (var _ = context.withDepthMode(DepthMode.READ_WRITE); var _ = context.withColorMask(false, false,
                        false, false); var _ = context.withBlendMode(BlendMode.NONE)) {
                    drawSprite(sprite, spriteList, vao);
                }

                // Pass 2: Color Render (No Depth Write, Equal Depth)
                try (var _ = context.withDepthMode(DepthMode.READ_ONLY); var _ = context.withColorMask(true, true, true,
                        true); var _ = context.withBlendMode(BlendMode.ALPHA)) {
                    drawSprite(sprite, spriteList, vao);
                }
            } finally {
                shader.setUniform(PlacingShader.Uniforms.DESATURATE, 0.0f);
                shader.setUniform(PlacingShader.Uniforms.MODULATE_COLOR, false);
                shader.setUniform(PlacingShader.Uniforms.ALPHA_TEST_VALUE, 0.3f);
            }
        }
    }

    private void drawSprite(Sprite sprite, SpriteList spriteList, VertexArray vao) {
        int texCoordLoc = shader.getAttributeLocation(PlacingShader.Attributes.TEX_COORD);
        int posLoc = shader.getAttributeLocation(PlacingShader.Attributes.POSITION);
        int normLoc = shader.getAttributeLocation(PlacingShader.Attributes.NORMAL);

        vao.bind();
        try {
            if (texCoordLoc >= 0) {
                spriteList.getTexcoords().vertexAttribPointer(texCoordLoc, 2, 0, sprite.texcoords_offset * 4L);
            }

            int vertex_index = sprite.getVertexOffset(0, 0f);
            int normal_index = sprite.getNormalOffset(vertex_index);

            if (posLoc >= 0) {
                spriteList.getVerticesAndNormals().vertexAttribPointer(posLoc, 3, 0, vertex_index * 4L);
            }

            if (normLoc >= 0) {
                spriteList.getVerticesAndNormals().vertexAttribPointer(normLoc, 3, 0, normal_index * 4L);
            }

            spriteList.getIndices().drawElements(GL11.GL_TRIANGLES, sprite.getTriangleCount() * 3,
                    sprite.indices_offset);
        } finally {
            vao.unbind();
        }
    }

    @Override
    public void close() {
        shader.close();
        for (VertexArray vao : vaos.values()) {
            vao.close();
        }
        vaos.clear();
    }
}
