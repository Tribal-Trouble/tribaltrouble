package com.oddlabs.tt.scenery;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeLeaf;
import com.oddlabs.tt.procedural.GeneratorOcean;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.PatchMesh;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.WaterShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

/**
 * Renders water surfaces.
 */
public final class Water implements AutoCloseable {
    private final @NonNull Sky sky;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final @NonNull HeightMap heightMap;

    private final @NonNull Texture @NonNull [] ocean;

    private final @NonNull WaterShader waterShader;
    private final @NonNull VertexArray skyWaterVao;
    private final @NonNull PatchMesh patchMesh;
    
    // Non-final to allow resizing
    private @NonNull FloatVBO instanceVBO;
    private @NonNull FloatBuffer instanceBuffer;

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
        this.heightMap = heightmap;

        this.sky = sky;
        this.modelViewStack = modelViewStack;
        this.projectionStack = projectionStack;
        this.waterShader = new WaterShader();

        this.skyWaterVao = new VertexArray();
        skyWaterVao.bind();
        setupWaterAttributes(sky.getWaterVertices(), waterShader);
        skyWaterVao.unbind();

        this.patchMesh = new PatchMesh();
        this.instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 2 * Float.BYTES); // Initial capacity
        this.instanceBuffer = BufferUtils.createFloatBuffer(1024 * 2);
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


    public void render(@NonNull RenderContext context, @NonNull CameraState state, @NonNull List<LandscapeLeaf> visiblePatches) {
        updateAnimation();

        try (var _ = waterShader.use();
             var _ = context.withBlendMode(BlendMode.ALPHA);
             var _ = context.withDepthMode(DepthMode.READ_WRITE);
             var _ = context.withCullMode(CullMode.NONE)) {

            context.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
            waterShader.setUniformMatrix4(WaterShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());
            
            waterShader.setUniform(WaterShader.Uniforms.SCROLL_OFFSET_0, scrollOffset0[0], scrollOffset0[1]);
            waterShader.setUniform(WaterShader.Uniforms.SCROLL_OFFSET_1, scrollOffset1[0], scrollOffset1[1]);
            
            waterShader.setUniform(WaterShader.Uniforms.CAMERA_POS, state.getCurrentX(), state.getCurrentY(), state.getCurrentZ());

            context.setTexture(0, ocean[0]);
            waterShader.setUniform(WaterShader.Uniforms.TEXTURE_0, 0);

            if (Globals.draw_detail) {
                context.setTexture(1, ocean[1]);
                waterShader.setUniform(WaterShader.Uniforms.TEXTURE_1, 1);
                waterShader.setUniform(WaterShader.Uniforms.WATER_DETAIL_REPEAT_RATE, Globals.WATER_DETAIL_REPEAT_RATE);
                waterShader.setUniform(WaterShader.Uniforms.ENABLE_DETAIL, true);
            } else {
                waterShader.setUniform(WaterShader.Uniforms.ENABLE_DETAIL, false);
            }

            waterShader.setUniform(WaterShader.Uniforms.WATER_REPEAT_RATE, Globals.WATER_REPEAT_RATE);

            // Render Sky Water (Infinite Plane). u_waterHeight = 0 because Z is baked in.
            waterShader.setUniform(WaterShader.Uniforms.WATER_HEIGHT, 0.0f);
            skyWaterVao.bind();
            sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);
            skyWaterVao.unbind();
            
            // Render Instanced Water Patches. u_waterHeight = seaLevel.
            if (!visiblePatches.isEmpty()) {
                waterShader.setUniform(WaterShader.Uniforms.WATER_HEIGHT, heightMap.getSeaLevelMeters());
                instanceBuffer.clear();
                int count = 0;
                float patchSize = heightMap.getMetersPerPatch();
                
                for (LandscapeLeaf leaf : visiblePatches) {
                    if (heightMap.isBelowSeaLevel(leaf.getPatchX(), leaf.getPatchY())) {
                        if (instanceBuffer.remaining() < 2) {
                            int newCapacity = instanceBuffer.capacity() * 2;
                            FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity);
                            instanceBuffer.flip();
                            newBuffer.put(instanceBuffer);
                            instanceBuffer = newBuffer;
                        }
                        instanceBuffer.put(leaf.getPatchX() * patchSize);
                        instanceBuffer.put(leaf.getPatchY() * patchSize);
                        count++;
                    }
                }
                
                if (count > 0) {
                    instanceBuffer.flip();
                    
                    int requiredBytes = count * 2 * Float.BYTES;
                    if (instanceVBO.capacity() < requiredBytes) {
                        instanceVBO.close();
                        instanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, Math.max(instanceVBO.capacity() * 2, requiredBytes));
                    }
                    
                    instanceVBO.makeCurrent();
                    GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, instanceBuffer);
                    
                    patchMesh.bind();
                    
                    // Setup instance attribute (Location 4: in_InstanceOffset)
                    int offsetLoc = 4;
                    GL20.glEnableVertexAttribArray(offsetLoc);
                    GL20.glVertexAttribPointer(offsetLoc, 2, GL11.GL_FLOAT, false, 0, 0);
                    GL33.glVertexAttribDivisor(offsetLoc, 1);
                    
                    patchMesh.drawInstanced(count);
                    
                    // Cleanup
                    GL33.glVertexAttribDivisor(offsetLoc, 0);
                    GL20.glDisableVertexAttribArray(offsetLoc);
                    
                    patchMesh.unbind();
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                }
            }
            
            context.setActiveTexture(0);
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

    @Override
    public void close() {
        skyWaterVao.close();
        patchMesh.delete();
        instanceVBO.close();
        waterShader.close();
    }
}
