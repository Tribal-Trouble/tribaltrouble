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
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.util.Random;

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

    private final float[] scrollOffset0 = new float[2];
    private final float[] scrollOffset1 = new float[2];
    private float flowDirection = (float) Math.toRadians(45f);
    private float flowSpeed = 0.001f;
    private float targetFlowDirection = flowDirection;
    private float targetFlowSpeed = flowSpeed;
    private float timeSinceChange = 0f;
    private float changeInterval = 20f;
    private float lastTime = 0f;
    private final Random random = new Random();

    public Water(@NonNull HeightMap heightmap, Landscape.@NonNull TerrainType terrain, @NonNull Sky sky, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        TextureGenerator ocean_desc = new GeneratorOcean(terrain);
        ocean = Resources.findResource(ocean_desc);
        patch_vertices = makePatchVertices(heightmap);

        this.sky = sky;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.waterShader = new WaterShader();

        this.skyWaterVao = new VertexArray();
        skyWaterVao.bind();
        setupWaterAttributes(sky.getWaterVertices(), waterShader);
        skyWaterVao.unbind();

        this.patchWaterVao = new VertexArray();
        patchWaterVao.bind();
        setupWaterAttributes(patch_vertices, waterShader);
        patchWaterVao.unbind();
    }

    public @NonNull WaterShader getShader() {
        return waterShader;
    }

    private void setupWaterAttributes(@NonNull FloatVBO vbo, @NonNull WaterShader shader) {
        int posLoc = shader.getAttributeLocation(WaterShader.Attributes.POSITION);
        vbo.makeCurrent();
        GL20.glEnableVertexAttribArray(posLoc);
        GL20.glVertexAttribPointer(posLoc, 3, GL11.GL_FLOAT, false, 0, 0L);
    }

    public void render(@NonNull CameraState state) {
        updateAnimation();

        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthTestEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);

        try (var _ = waterShader.use();
             var _ = state.getFog().setup(waterShader, state.getCurrentZ())) {

            if (!blendEnabled) GL11.glEnable(GL11.GL_BLEND);
            if (!depthTestEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            waterShader.setUniformMatrix4(WaterShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            waterShader.setUniformMatrix4(WaterShader.Uniforms.PROJECTION_MATRIX, false, projectionStack.current());
            
            waterShader.setUniform(WaterShader.Uniforms.SCROLL_OFFSET_0, scrollOffset0[0], scrollOffset0[1]);
            waterShader.setUniform(WaterShader.Uniforms.SCROLL_OFFSET_1, scrollOffset1[0], scrollOffset1[1]);
            
            waterShader.setUniform(WaterShader.Uniforms.LIGHT_DIR, -1f, 0f, 1f);
            waterShader.setUniform(WaterShader.Uniforms.CAMERA_POS, state.getCurrentX(), state.getCurrentY(), state.getCurrentZ());

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

            skyWaterVao.bind();
            sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);
            
            patchWaterVao.bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, patch_vertices.capacity() / 3);
            
            patchWaterVao.unbind();
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        } finally {
            if (!blendEnabled) GL11.glDisable(GL11.GL_BLEND);
            if (!depthTestEnabled) GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }
    
    private void updateAnimation() {
        float currentTime = LocalEventQueue.getQueue().getTime();
        float dt = currentTime - lastTime;
        if (dt < 0 || dt > 1.0f) dt = 0.016f;
        lastTime = currentTime;

        timeSinceChange += dt;
        if (timeSinceChange > changeInterval) {
            timeSinceChange = 0f;
            
            float mean = 17.5f;
            float stdDev = 5.0f;
            float gaussianValue = (float) random.nextGaussian();
            changeInterval = mean + gaussianValue * stdDev;

            float dirChangeDegrees = (float) random.nextGaussian() * 7.5f;
            targetFlowDirection += (float) Math.toRadians(dirChangeDegrees);

            float speedChange = flowSpeed * (float) random.nextGaussian() * 0.05f;
            targetFlowSpeed = Math.clamp(targetFlowSpeed + speedChange, 0.0005f, 0.002f);
        }

        flowDirection += (targetFlowDirection - flowDirection) * dt * 0.5f;
        flowSpeed += (targetFlowSpeed - flowSpeed) * dt * 0.5f;

        float dx = (float) Math.cos(flowDirection) * flowSpeed * dt;
        float dy = (float) Math.sin(flowDirection) * flowSpeed * dt;

        scrollOffset0[0] += dx;
        scrollOffset0[1] += dy;
        
        scrollOffset1[0] += dx * 1.2f; 
        scrollOffset1[1] += dy * 0.8f;
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
        int size = count * 6 * 3;
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

                    vertices[vertexIndex++] = x0; vertices[vertexIndex++] = y0; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x1; vertices[vertexIndex++] = y0; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x1; vertices[vertexIndex++] = y1; vertices[vertexIndex++] = z;

                    vertices[vertexIndex++] = x0; vertices[vertexIndex++] = y0; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x1; vertices[vertexIndex++] = y1; vertices[vertexIndex++] = z;
                    vertices[vertexIndex++] = x0; vertices[vertexIndex++] = y1; vertices[vertexIndex++] = z;
                }
            }
        }
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertices);
    }

    @Override
    public void close() {
        skyWaterVao.close();
        patchWaterVao.close();
        patch_vertices.close();
        waterShader.close();
    }
}
