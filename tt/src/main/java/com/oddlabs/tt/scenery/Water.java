package com.oddlabs.tt.scenery;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.GeneratorOcean;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.WaterShader;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.util.GLStateHelper;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.tt.vbo.VertexArrays;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 * Renders water surfaces.
 */
public final class Water implements AutoCloseable {
    private final @NonNull Sky sky;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;

    private final @NonNull FloatVBO patch_vertices;
    private final @NonNull Texture @NonNull [] ocean;

    private final @NonNull WaterShader waterShader;
    private final @NonNull VertexArray skyWaterVao;
    private final @NonNull VertexArray patchWaterVao;

    public Water(@NonNull HeightMap heightmap, Landscape.@NonNull TerrainType terrain, @NonNull Sky sky, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        TextureGenerator ocean_desc = new GeneratorOcean(terrain);
        ocean = Resources.findResource(ocean_desc);
        patch_vertices = makePatchVertices(heightmap);

        this.sky = sky;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.waterShader = new WaterShader();

        // Setup skyWaterVao
        this.skyWaterVao = VertexArrays.create();
        if (VertexArrays.isSupported()) {
            skyWaterVao.bind();
            setupWaterAttributes(sky.getWaterVertices(), waterShader);
            skyWaterVao.unbind();
        }

        // Setup patchWaterVao
        this.patchWaterVao = VertexArrays.create();
        if (VertexArrays.isSupported()) {
            patchWaterVao.bind();
            setupWaterAttributes(patch_vertices, waterShader);
            patchWaterVao.unbind();
        }
    }

    public @NonNull WaterShader getShader() {
        return waterShader;
    }

    private void setupWaterAttributes(@NonNull FloatVBO vbo, @NonNull WaterShader shader) {
        int posLoc = shader.getAttributeLocation(WaterShader.Attributes.POSITION);
        if (posLoc != -1) {
            vbo.vertexAttribPointer(posLoc, 3, 0, 0L);
            GL20.glEnableVertexAttribArray(posLoc);
        }
    }

    public void render(@NonNull CameraState state) {
        try (var _ = waterShader.use();
             var _ = state.getFog().setup(waterShader, state.getCurrentZ());
             var _ = GLStateHelper.blend(true);
             var _ = GLStateHelper.depthTest(true)) {

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            waterShader.setUniformMatrix4(WaterShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            waterShader.setUniformMatrix4(WaterShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            waterShader.setUniform(WaterShader.Uniforms.TIME, LocalEventQueue.getQueue().getTime());

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ocean[0].getHandle());
            waterShader.setUniform(WaterShader.Uniforms.TEXTURE_0, 0);

            if (Globals.draw_detail) {
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, ocean[1].getHandle());
                waterShader.setUniform(WaterShader.Uniforms.TEXTURE_1, 1);
                waterShader.setUniform(WaterShader.Uniforms.WATER_DETAIL_REPEAT_RATE, Globals.WATER_DETAIL_REPEAT_RATE);
                waterShader.setUniform(WaterShader.Uniforms.ENABLE_DETAIL, true);
            } else {
                waterShader.setUniform(WaterShader.Uniforms.ENABLE_DETAIL, false);
            }

            waterShader.setUniform(WaterShader.Uniforms.WATER_REPEAT_RATE, Globals.WATER_REPEAT_RATE);

            // Render water surface from Sky
            if (VertexArrays.isSupported()) {
                skyWaterVao.bind();
            } else {
                setupWaterAttributes(sky.getWaterVertices(), waterShader);
            }
            sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);
            if (VertexArrays.isSupported()) {
                skyWaterVao.unbind();
            } else {
                GL20.glDisableVertexAttribArray(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION));
            }


            // Render water patches
            if (VertexArrays.isSupported()) {
                patchWaterVao.bind();
            } else {
                setupWaterAttributes(patch_vertices, waterShader);
            }
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, patch_vertices.capacity() / 3);
            if (VertexArrays.isSupported()) {
                patchWaterVao.unbind();
            } else {
                GL20.glDisableVertexAttribArray(waterShader.getAttributeLocation(WaterShader.Attributes.POSITION));
            }

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
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
        int size = count * 6 * 3; // 6 vertices per quad, 3 floats per vertex
        float[] vertices = new float[size];
        int vertexIndex = 0;

        for (int y = 0; y < water_patches.length; y++) {
            for (int x = 0; x < water_patches[y].length; x++) {
                if (water_patches[y][x]) {
                    float x0 = x * heightmap.getMetersPerPatch();
                    float y0 = y * heightmap.getMetersPerPatch();
                    float x1 = (x + 1) * heightmap.getMetersPerPatch();
                    float y1 = (y + 1) * heightmap.getMetersPerPatch();
                    float z = heightmap.getSeaLevelMeters();

                    // Triangle 1
                    vertices[vertexIndex++] = x0; vertices[vertexIndex++] = y0; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x1; vertices[vertexIndex++] = y0; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x1; vertices[vertexIndex++] = y1; vertices[vertexIndex++] = z;

                    // Triangle 2
                    vertices[vertexIndex++] = x0; vertices[vertexIndex++] = y0; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x1; vertices[vertexIndex++] = y1; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x0; vertices[vertexIndex++] = y1; vertices[vertexIndex++] = z;
                }
            }
        }
        assert vertexIndex == vertices.length;
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertices);
    }

    @Override
    public void close() {
        if (VertexArrays.isSupported()) {
            skyWaterVao.delete();
            patchWaterVao.delete();
        }
    }
}
