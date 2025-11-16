package com.oddlabs.tt.scenery;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.GeneratorOcean;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.shader.WaterShader;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.vbo.FloatVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 * Renders water surfaces.
 */
public final class Water {
    private final @NonNull Sky sky;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;

    private final @NonNull FloatVBO patch_vertices;
    private final @NonNull Texture @NonNull [] ocean;

    private final @NonNull WaterShader waterShader;

    public Water(@NonNull HeightMap heightmap, Landscape.@NonNull TerrainType terrain, @NonNull Sky sky, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        TextureGenerator ocean_desc = new GeneratorOcean(terrain);
        ocean = Resources.findResource(ocean_desc);
        patch_vertices = makePatchVertices(heightmap);

        this.sky = sky;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.waterShader = new WaterShader();
    }

    public void render() {
        waterShader.use();

        // Save current OpenGL states
        boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        int blendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        int blendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        boolean wasDepthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasDepthMaskEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int originalActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        boolean wasTex0Enabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        boolean wasTex1Enabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);

        try {
            // Set uniforms
            waterShader.setUniformMatrix4(WaterShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.toBuffer());
            waterShader.setUniformMatrix4(WaterShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.toBuffer());

            // Bind textures
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ocean[0].getHandle());
            waterShader.setUniform(WaterShader.Uniforms.TEXTURE_0, 0);

            if (Globals.draw_detail) {
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ocean[1].getHandle());
                waterShader.setUniform(WaterShader.Uniforms.TEXTURE_1, 1);
                waterShader.setUniform(WaterShader.Uniforms.WATER_DETAIL_REPEAT_RATE, Globals.WATER_DETAIL_REPEAT_RATE);
                waterShader.setUniform(WaterShader.Uniforms.ENABLE_DETAIL, true);
            } else {
                waterShader.setUniform(WaterShader.Uniforms.ENABLE_DETAIL, false);
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }

            waterShader.setUniform(WaterShader.Uniforms.WATER_REPEAT_RATE, Globals.WATER_REPEAT_RATE);

            // Apply water-specific states
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);

            // Bind attributes and draw
            // Water from Sky
            GL20.glEnableVertexAttribArray(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION));
            sky.getWaterVertices().vertexAttribPointer(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION), 3, 0, 0L);
            sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);
            GL20.glDisableVertexAttribArray(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION));

            // Water patches
            GL20.glEnableVertexAttribArray(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION));
            patch_vertices.vertexAttribPointer(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION), 3, 0, 0L);
            GL11.glDrawArrays(GL11.GL_QUADS, 0, patch_vertices.capacity() / 3);
            GL20.glDisableVertexAttribArray(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION));
        } finally{
            // Restore original OpenGL states
            if (!wasBlendEnabled) {
                GL11.glDisable(GL11.GL_BLEND);
            }
            GL11.glBlendFunc(blendSrc, blendDst);
            if (!wasDepthTestEnabled) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            }
            GL11.glDepthMask(wasDepthMaskEnabled);

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            if (wasTex0Enabled) {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            } else {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            if (wasTex1Enabled) {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            } else {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }

            GL13.glActiveTexture(originalActiveTexture);

            ShaderProgram.unbind();
        }
    }

    private static @NonNull FloatVBO makePatchVertices(@NonNull HeightMap heightmap) {
        boolean[][] water_patches = new boolean[heightmap.getPatchesPerWorld()][heightmap.getPatchesPerWorld()];
        int count = 0;
        for (int y = 0; y < water_patches.length; y++) {
            for (int x = 0; x < water_patches[y].length; x++) {
                if (heightmap.isBelowSeaLevel(x, y)) {
                    water_patches[y][x] = true;
                    count++;
                }
            }
        }
        int size = count * 4 * 3;
        float[] vertices = new float[size];
        int vertexIndex = 0;

        for (int y = 0; y < water_patches.length; y++) {
            for (int x = 0; x < water_patches[y].length; x++) {
                if (water_patches[y][x]) {
                    vertices[vertexIndex++] = x * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = y * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = heightmap.getSeaLevelMeters();

                    vertices[vertexIndex++] = (x + 1) * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = y * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = heightmap.getSeaLevelMeters();

                    vertices[vertexIndex++] = (x + 1) * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = (y + 1) * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = heightmap.getSeaLevelMeters();

                    vertices[vertexIndex++] = x * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = (y + 1) * heightmap.getMetersPerPatch();
                    vertices[vertexIndex++] = heightmap.getSeaLevelMeters();
                }
            }
        }
        assert vertexIndex == vertices.length;
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertices);
    }
}
