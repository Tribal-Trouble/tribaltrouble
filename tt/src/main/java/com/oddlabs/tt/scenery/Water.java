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
import org.joml.Matrix4f;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Renders water surfaces.
 */
public final class Water implements AutoCloseable {
    /** Depth scale (in meters) over which Native (Tropical) water alpha transitions. */
    private static final float NATIVE_DEPTH_SCALE = 1.0f;
    /** Minimum alpha (transparency) of Native water at the shoreline. */
    private static final float NATIVE_MIN_ALPHA = 0.05f;
    /** Maximum alpha (transparency) of Native water in deep ocean. */
    private static final float NATIVE_MAX_ALPHA = 0.60f;

    /** Depth scale (in meters) over which Viking (Northern) water alpha transitions. */
    private static final float VIKING_DEPTH_SCALE = 2.0f;
    /** Minimum alpha (transparency) of Viking water at the shoreline. */
    private static final float VIKING_MIN_ALPHA = 0.05f;
    /** Maximum alpha (transparency) of Viking water in deep ocean. */
    private static final float VIKING_MAX_ALPHA = 0.60f;

    public static final int WAVE_COUNT = 3;
    public static final float WAVE_AMPLITUDE_BASE = 0.15f;
    public static final float WAVE_STEEPNESS_BASE = 0.5f;

    public static final float WAVE_AMPLITUDE_SCALE_2 = 0.53f;
    public static final float WAVE_AMPLITUDE_SCALE_3 = 0.27f;

    public static final float VIKING_AMPLITUDE_MULTIPLIER = 1.5f;
    public static final float VIKING_STEEPNESS_MULTIPLIER = 1.2f;
    public static final float VIKING_WAVE_SPEED = 0.8f;

    public static final float NATIVE_WAVE_SPEED = 0.4f;

    public static final float WAVE_DIR_X_1 = 1.0f;
    public static final float WAVE_DIR_X_2 = 0.707f;
    public static final float WAVE_DIR_X_3 = -0.5f;
    public static final float WAVE_DIR_Y_1 = 0.0f;
    public static final float WAVE_DIR_Y_2 = 0.707f;
    public static final float WAVE_DIR_Y_3 = 0.866f;

    public static final float NATIVE_WAVE_LEN_1 = 60.0f;
    public static final float NATIVE_WAVE_LEN_2 = 35.0f;
    public static final float NATIVE_WAVE_LEN_3 = 18.0f;

    public static final float VIKING_WAVE_LEN_1 = 50.0f;
    public static final float VIKING_WAVE_LEN_2 = 28.0f;
    public static final float VIKING_WAVE_LEN_3 = 14.0f;

    private static final float[] WAVE_DIRS_X = new float[]{WAVE_DIR_X_1, WAVE_DIR_X_2, WAVE_DIR_X_3};
    private static final float[] WAVE_DIRS_Y = new float[]{WAVE_DIR_Y_1, WAVE_DIR_Y_2, WAVE_DIR_Y_3};

    private final Landscape.@NonNull TerrainType terrain;
    private final @NonNull Sky sky;
    private final @NonNull MatrixStack modelViewStack;
    private final @NonNull MatrixStack projectionStack;
    private final @NonNull HeightMap heightMap;

    private final @NonNull Texture @NonNull [] ocean;

    private final @NonNull WaterShader waterShader;
    private final @NonNull VertexArray skyWaterVao;
    private final @NonNull PatchMesh patchMesh;

    private final @NonNull BitSet oceanPatches;

    // Non-final to allow resizing
    private @NonNull FloatVBO oceanInstanceVBO;
    private @NonNull FloatBuffer oceanInstanceBuffer;
    private @NonNull FloatVBO inlandInstanceVBO;
    private @NonNull FloatBuffer inlandInstanceBuffer;

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

    private float waveTime = 0f;
    private final float @NonNull [] waveAmplitudes;
    private final float @NonNull [] waveSteepness;
    private final float @NonNull [] waveLengths;
    private final float waveSpeed;

    public Water(@NonNull HeightMap heightmap, Landscape.@NonNull TerrainType terrain, @NonNull Sky sky,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        this.terrain = terrain;
        waveAmplitudes = switch (terrain) {
            case VIKING ->
                new float[]{WAVE_AMPLITUDE_BASE * VIKING_AMPLITUDE_MULTIPLIER, WAVE_AMPLITUDE_BASE * VIKING_AMPLITUDE_MULTIPLIER * WAVE_AMPLITUDE_SCALE_2, WAVE_AMPLITUDE_BASE * VIKING_AMPLITUDE_MULTIPLIER * WAVE_AMPLITUDE_SCALE_3
                };
            case NATIVE ->
                new float[]{WAVE_AMPLITUDE_BASE, WAVE_AMPLITUDE_BASE * WAVE_AMPLITUDE_SCALE_2, WAVE_AMPLITUDE_BASE * WAVE_AMPLITUDE_SCALE_3
                };
        };
        waveSteepness = switch (terrain) {
            case VIKING ->
                new float[]{WAVE_STEEPNESS_BASE * VIKING_STEEPNESS_MULTIPLIER, WAVE_STEEPNESS_BASE * VIKING_STEEPNESS_MULTIPLIER, WAVE_STEEPNESS_BASE * VIKING_STEEPNESS_MULTIPLIER
                };
            case NATIVE -> new float[]{WAVE_STEEPNESS_BASE, WAVE_STEEPNESS_BASE, WAVE_STEEPNESS_BASE
            };
        };
        waveLengths = switch (terrain) {
            case VIKING -> new float[]{VIKING_WAVE_LEN_1, VIKING_WAVE_LEN_2, VIKING_WAVE_LEN_3};
            case NATIVE -> new float[]{NATIVE_WAVE_LEN_1, NATIVE_WAVE_LEN_2, NATIVE_WAVE_LEN_3};
        };
        waveSpeed = switch (terrain) {
            case VIKING -> VIKING_WAVE_SPEED;
            case NATIVE -> NATIVE_WAVE_SPEED;
        };
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
        this.oceanInstanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 3 * Float.BYTES); // Initial capacity
        this.oceanInstanceBuffer = BufferUtils.createFloatBuffer(1024 * 3);
        this.inlandInstanceVBO = new FloatVBO(GL15.GL_STREAM_DRAW, 1024 * 3 * Float.BYTES);
        this.inlandInstanceBuffer = BufferUtils.createFloatBuffer(1024 * 3);

        // Flood fill from the map border to distinguish ocean patches from enclosed inland water
        int patchesPerWorld = heightmap.getPatchesPerWorld();
        this.oceanPatches = new BitSet(patchesPerWorld * patchesPerWorld);
        Queue<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < patchesPerWorld; x++) {
            if (heightmap.isBelowSeaLevel(x, 0)) {
                int index = x; // y is 0
                oceanPatches.set(index);
                queue.add(new int[]{x, 0});
            }
            if (heightmap.isBelowSeaLevel(x, patchesPerWorld - 1)) {
                int index = (patchesPerWorld - 1) * patchesPerWorld + x;
                oceanPatches.set(index);
                queue.add(new int[]{x, patchesPerWorld - 1});
            }
        }
        for (int y = 1; y < patchesPerWorld - 1; y++) {
            if (heightmap.isBelowSeaLevel(0, y)) {
                int index = y * patchesPerWorld; // x is 0
                oceanPatches.set(index);
                queue.add(new int[]{0, y});
            }
            if (heightmap.isBelowSeaLevel(patchesPerWorld - 1, y)) {
                int index = y * patchesPerWorld + (patchesPerWorld - 1);
                oceanPatches.set(index);
                queue.add(new int[]{patchesPerWorld - 1, y});
            }
        }

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int currX = current[0];
            int currY = current[1];

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    int nx = currX + dx;
                    int ny = currY + dy;
                    if (nx >= 0 && nx < patchesPerWorld && ny >= 0 && ny < patchesPerWorld) {
                        int index = ny * patchesPerWorld + nx;
                        if (!oceanPatches.get(index) && heightmap.isBelowSeaLevel(nx, ny)) {
                            oceanPatches.set(index);
                            queue.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }
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


    public void render(@NonNull RenderContext context, @NonNull CameraState state,
            @NonNull List<LandscapeLeaf> visiblePatches, @Nullable Texture reflectionTexture,
            @Nullable Matrix4f reflectionVP) {
        updateAnimation();

        try (var _ = waterShader.use(); var _ = context.withBlendMode(BlendMode.ALPHA); var _ = context.withDepthMode(
                DepthMode.READ_WRITE); var _ = context.withCullMode(CullMode.NONE)) {

            context.setBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            waterShader.setUniformMatrix4(WaterShader.Uniforms.MODEL_VIEW_MATRIX, false, modelViewStack.current());

            waterShader.setUniform(WaterShader.Uniforms.SCROLL_OFFSET_0, scrollOffset0[0], scrollOffset0[1]);
            waterShader.setUniform(WaterShader.Uniforms.SCROLL_OFFSET_1, scrollOffset1[0], scrollOffset1[1]);

            waterShader.setUniform(WaterShader.Uniforms.CAMERA_POS, state.getCurrentX(), state.getCurrentY(),
                    state.getCurrentZ());

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

            if (reflectionTexture != null && reflectionVP != null) {
                context.setTexture(2, reflectionTexture);
                waterShader.setUniform(WaterShader.Uniforms.REFLECTION_TEXTURE, 2);
                waterShader.setUniformMatrix4(WaterShader.Uniforms.REFLECTION_VP, false, reflectionVP);
                waterShader.setUniform(WaterShader.Uniforms.HAS_REFLECTION, true);
            } else {
                waterShader.setUniform(WaterShader.Uniforms.HAS_REFLECTION, false);
            }

            waterShader.setUniform(WaterShader.Uniforms.WATER_REPEAT_RATE, Globals.WATER_REPEAT_RATE);

            context.setTexture(3, heightMap.getHeightTexture());
            waterShader.setUniform(WaterShader.Uniforms.HEIGHT_MAP, 3);
            waterShader.setUniform(WaterShader.Uniforms.WORLD_SIZE, (float) heightMap.getMetersPerWorld());

            float depthScale = switch (terrain) {
                case NATIVE -> NATIVE_DEPTH_SCALE;
                case VIKING -> VIKING_DEPTH_SCALE;
            };
            float minAlpha = switch (terrain) {
                case NATIVE -> NATIVE_MIN_ALPHA;
                case VIKING -> VIKING_MIN_ALPHA;
            };
            float maxAlpha = switch (terrain) {
                case NATIVE -> NATIVE_MAX_ALPHA;
                case VIKING -> VIKING_MAX_ALPHA;
            };

            waterShader.setUniform(WaterShader.Uniforms.DEPTH_SCALE, depthScale);
            waterShader.setUniform(WaterShader.Uniforms.MIN_ALPHA, minAlpha);
            waterShader.setUniform(WaterShader.Uniforms.MAX_ALPHA, maxAlpha);

            for (int i = 0; i < WAVE_COUNT; i++) {
                waterShader.setUniform(WaterShader.Uniforms.WAVE_DIR_LENGTH + "[" + i + "]", WAVE_DIRS_X[i],
                        WAVE_DIRS_Y[i], waveLengths[i], 0f);
                waterShader.setUniform(WaterShader.Uniforms.WAVE_AMP_STEEP + "[" + i + "]", waveAmplitudes[i],
                        waveSteepness[i], 0f, 0f);
            }
            waterShader.setUniform(WaterShader.Uniforms.WAVE_TIME, waveTime * waveSpeed);

            // Render Sky Water (Infinite Plane). u_waterHeight = 0 because Z is baked in.
            waterShader.setUniform(WaterShader.Uniforms.WATER_HEIGHT, heightMap.getSeaLevelMeters());
            skyWaterVao.bind();
            sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);
            skyWaterVao.unbind();

            // Render Instanced Water Patches. u_waterHeight = seaLevel.
            if (!visiblePatches.isEmpty()) {
                oceanInstanceBuffer.clear();
                inlandInstanceBuffer.clear();
                int oceanCount = 0;
                int inlandCount = 0;
                float patchSize = heightMap.getMetersPerPatch();
                int patchesPerWorld = heightMap.getPatchesPerWorld();

                for (LandscapeLeaf leaf : visiblePatches) {
                    int px = leaf.getPatchX();
                    int py = leaf.getPatchY();
                    if (heightMap.isBelowSeaLevel(px, py)) {
                        float worldX = px * patchSize;
                        float worldY = py * patchSize;
                        if (oceanPatches.get(py * patchesPerWorld + px)) {
                            oceanInstanceBuffer = addInstance(oceanInstanceBuffer, worldX, worldY, 1.0f);
                            oceanCount++;
                        } else {
                            inlandInstanceBuffer = addInstance(inlandInstanceBuffer, worldX, worldY, 0.0f);
                            inlandCount++;
                        }
                    }
                }

                if (oceanCount > 0) {
                    waterShader.setUniform(WaterShader.Uniforms.MIN_ALPHA, minAlpha);
                    oceanInstanceVBO = uploadAndDraw(oceanCount, oceanInstanceBuffer, oceanInstanceVBO);
                }

                if (inlandCount > 0) {
                    waterShader.setUniform(WaterShader.Uniforms.MIN_ALPHA, maxAlpha);
                    inlandInstanceVBO = uploadAndDraw(inlandCount, inlandInstanceBuffer, inlandInstanceVBO);
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
        waveTime += dt;

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

        // Move the second layer in a different direction (e.g., 90 degrees offset)
        // and slightly slower to create interference patterns.
        float flowDirection2 = flowDirection + (float) Math.toRadians(90f);
        float dx2 = (float) Math.cos(flowDirection2) * flowSpeed * 0.7f * dt;
        float dy2 = (float) Math.sin(flowDirection2) * flowSpeed * 0.7f * dt;

        scrollOffset1[0] += dx2;
        scrollOffset1[1] += dy2;
    }

    /**
     * Appends instance offsets to the buffer, resizing the buffer if necessary.
     */
    private @NonNull FloatBuffer addInstance(@NonNull FloatBuffer buffer, float x, float y, float z) {
        if (buffer.remaining() < 3) {
            int newCapacity = buffer.capacity() * 2;
            FloatBuffer newBuffer = BufferUtils.createFloatBuffer(newCapacity);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
        buffer.put(x);
        buffer.put(y);
        buffer.put(z);
        return buffer;
    }

    /**
     * Uploads instance offset data and renders the instanced patches.
     */
    private @NonNull FloatVBO uploadAndDraw(int count, @NonNull FloatBuffer buffer, @NonNull FloatVBO vbo) {
        buffer.flip();

        int requiredBytes = count * 3 * Float.BYTES;
        if (vbo.capacity() < requiredBytes) {
            vbo.close();
            //noinspection resource
            vbo = new FloatVBO(GL15.GL_STREAM_DRAW, Math.max(vbo.capacity() * 2, requiredBytes));
        }

        vbo.makeCurrent();
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, buffer);

        patchMesh.bind();

        // Setup instance attribute (Location 4: in_InstanceOffset)
        int offsetLoc = 4;
        GL20.glEnableVertexAttribArray(offsetLoc);
        GL20.glVertexAttribPointer(offsetLoc, 3, GL11.GL_FLOAT, false, 0, 0);
        GL33.glVertexAttribDivisor(offsetLoc, 1);

        patchMesh.drawInstanced(count);

        // Cleanup
        GL33.glVertexAttribDivisor(offsetLoc, 0);
        GL20.glDisableVertexAttribArray(offsetLoc);

        patchMesh.unbind();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return vbo;
    }

    @Override
    public void close() {
        skyWaterVao.close();
        patchMesh.delete();
        oceanInstanceVBO.close();
        inlandInstanceVBO.close();
        waterShader.close();
    }
}
