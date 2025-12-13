package com.oddlabs.tt.scenery;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.GeneratorClouds;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.shader.SeaBottomShader;
import com.oddlabs.tt.render.shader.SkyShader;
import com.oddlabs.tt.resource.DistanceFogInfo;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.util.Stitcher;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.tt.vbo.VertexArrays;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public final class Sky {

    private static final float[] SKYDOME_SPEED_OUTER = {0.2f, 0f};
    private static final float[] SKYDOME_SPEED_INNER = {0.4f, 0f};
    private static final float SKYDOME_HEIGHT = 0f;
    private static final int SKYDOME_GRADIENT_LENGTH = 20;
    private static final int SKYDOME_DEFAULT_COLOR = 8;

    private static final float[][] SKYDOME_INITCOLOR = {
            /* Native */ Color.rgb3f(0xE5_F2_FF),
            /* Viking */ Color.rgb3f(0xFF_E5_A6)
    };

    private static final float[][] SKYDOME_GRADIENT = {
            /* Native */ Color.rgb3f(0xBF_D2_F2),
            /* Viking */ Color.rgb3f(0x99_99_D9)
    };

    private static final float[][] tex_env_color = new float[][]{
            /* Native */ Color.argb4f(0xFF_F2_F8_FF),
            /* Viking */ Color.argb4f(0xFF_FF_F2_CC)
    };

    private static final float SKYDOME_OUTER_UTILING = 8f;
    private static final float SKYDOME_OUTER_VTILING = 8f;
    private static final float SKYDOME_INNER_UTILING = 8f;
    private static final float SKYDOME_INNER_VTILING = 8f;

    private static final int NUM_WATER_RINGS = 6;

    private static final float START_ANGLE = -(float) Math.PI / 4f;

    private final FloatBuffer color;
    private final ShortVBO @NonNull [] strip_indices;
    private final @NonNull ShortVBO fan_indices;
    private final @NonNull FloatVBO water_vertices;
    private final @NonNull FloatVBO bottom_vertices;
    private final @NonNull ShortVBO water_indices;
    private FloatVBO sky_vertices;
    private FloatVBO sky_normals;
    private FloatVBO sky_tex0;
    private FloatVBO sky_tex1;
    private FloatVBO sky_colors;

    private final @NonNull Texture @NonNull [] clouds;
    private final int subdiv_axis;
    private final int subdiv_height;
    private final Landscape.@NonNull TerrainType terrain;

    private final @NonNull SkyShader skyShader;
    private final @NonNull SeaBottomShader seaBottomShader;
    private final @NonNull Texture detail;
    private final @NonNull VertexArray skyVAO;
    private final @NonNull VertexArray seaBottomVAO;

    // Cloud animation state
    private final float[] innerOffset = new float[2];
    private final float[] outerOffset = new float[2];
    
    // Inner layer state
    private float innerDirection = 0f;
    private float innerSpeed = SKYDOME_SPEED_INNER[0] * 0.01f;
    private float targetInnerDirection = innerDirection;
    private float targetInnerSpeed = innerSpeed;
    private float innerTimeSinceChange = 0f;
    private float innerChangeInterval = 20f;

    // Outer layer state
    private float outerDirection = 0f;
    private float outerSpeed = SKYDOME_SPEED_OUTER[0] * 0.01f;
    private float targetOuterDirection = outerDirection;
    private float targetOuterSpeed = outerSpeed;
    private float outerTimeSinceChange = 0f;
    private float outerChangeInterval = 25f;

    // Cloud density state
    private float innerCloudDensity = 0f;
    private float targetInnerCloudDensity = 0f;
    private float outerCloudDensity = 0f;
    private float targetOuterCloudDensity = 0f;
    private float densityTimeSinceChange = 0f;
    private float densityChangeInterval = 60f;

    private float lastTime = 0f;
    private final java.util.Random random = new java.util.Random();

    public Sky(@NonNull LandscapeRenderer renderer, Landscape.@NonNull TerrainType terrain, @NonNull SkyShader skyShader, @NonNull SeaBottomShader seaBottomShader, @NonNull Texture detail) {
        this(renderer, terrain, (float) (renderer.getHeightMap().getMetersPerWorld() * Math.sqrt(2) / 2), 6000f, 20, 20, SKYDOME_OUTER_UTILING, SKYDOME_OUTER_VTILING, SKYDOME_INNER_UTILING, SKYDOME_INNER_VTILING, renderer.getHeightMap().getMetersPerWorld() / 2, renderer.getHeightMap().getMetersPerWorld() / 2, SKYDOME_HEIGHT, skyShader, seaBottomShader, detail);
    }

    public void render(@NonNull CameraState state, @NonNull MatrixStack modelView, @NonNull MatrixStack projection) {
        try (var _ = skyShader.use()) {
            skyShader.setUniformMatrix4(SkyShader.Uniforms.PROJECTION_MATRIX, false, projection.current());
            skyShader.setUniformMatrix4(SkyShader.Uniforms.MODEL_VIEW_MATRIX, false, modelView.current());
            skyShader.setUniform(SkyShader.Uniforms.SKY_COLOR, color.get(0), color.get(1), color.get(2), color.get(3));

            FogInfo fog = state.getFog();
            if (fog.isEnabled()) {
                float[] fogColor = fog.getFogColor();
                skyShader.setUniform(SkyShader.Uniforms.FOG_COLOR, fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
                skyShader.setUniform(SkyShader.Uniforms.FOG_FADE_START, 0.0f);
                skyShader.setUniform(SkyShader.Uniforms.FOG_FADE_END, 0.1f);
                skyShader.setUniform(SkyShader.Uniforms.CAMERA_HEIGHT, state.getCurrentZ());

                if (fog instanceof DistanceFogInfo distanceFog) {
                    skyShader.setUniform(SkyShader.Uniforms.FOG_HEIGHT_FACTOR, distanceFog.getHeightFactor());
                } else {
                    skyShader.setUniform(SkyShader.Uniforms.FOG_HEIGHT_FACTOR, 0.0f);
                }
            } else {
                skyShader.setUniform(SkyShader.Uniforms.FOG_FADE_START, 1.0f);
                skyShader.setUniform(SkyShader.Uniforms.FOG_FADE_END, 1.0f);
                // Ensure fog color is set even if disabled, to prevent black artifacts if shader uses it
                skyShader.setUniform(SkyShader.Uniforms.FOG_COLOR, 0f, 0f, 0f, 0f); 
            }

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, clouds[GeneratorClouds.INNER].getHandle());
            skyShader.setUniform(SkyShader.Uniforms.TEXTURE_0, 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, clouds[GeneratorClouds.OUTER].getHandle());
            skyShader.setUniform(SkyShader.Uniforms.TEXTURE_1, 1);

            float currentTime = LocalEventQueue.getQueue().getTime();
            float dt = currentTime - lastTime;
            if (dt < 0 || dt > 1.0f) dt = 0.016f;
            lastTime = currentTime;

            // Update Inner Layer
            innerTimeSinceChange += dt;
            if (innerTimeSinceChange > innerChangeInterval) {
                innerTimeSinceChange = 0f;
                // Randomize interval (15-45s)
                innerChangeInterval = 30f + (float) random.nextGaussian() * 10f; 
                
                // Change direction by +/- ~10 degrees
                float dirChange = (float) random.nextGaussian() * 10f;
                targetInnerDirection += (float) Math.toRadians(dirChange);

                // Change speed by +/- ~10%
                float speedChange = innerSpeed * (float) random.nextGaussian() * 0.1f;
                targetInnerSpeed = Math.clamp(targetInnerSpeed + speedChange, 0.002f, 0.008f);
            }
            innerDirection += (targetInnerDirection - innerDirection) * dt * 0.2f;
            innerSpeed += (targetInnerSpeed - innerSpeed) * dt * 0.2f;
            
            innerOffset[0] += (float) Math.cos(innerDirection) * innerSpeed * dt;
            innerOffset[1] += (float) Math.sin(innerDirection) * innerSpeed * dt;

            // Update Outer Layer
            outerTimeSinceChange += dt;
            if (outerTimeSinceChange > outerChangeInterval) {
                outerTimeSinceChange = 0f;
                outerChangeInterval = 40f + (float) random.nextGaussian() * 15f; 

                float dirChange = (float) random.nextGaussian() * 8f; // Slower changes for outer layer
                targetOuterDirection += (float) Math.toRadians(dirChange);

                float speedChange = outerSpeed * (float) random.nextGaussian() * 0.1f;
                targetOuterSpeed = Math.clamp(targetOuterSpeed + speedChange, 0.001f, 0.004f);
            }
            outerDirection += (targetOuterDirection - outerDirection) * dt * 0.1f;
            outerSpeed += (targetOuterSpeed - outerSpeed) * dt * 0.1f;

            outerOffset[0] += (float) Math.cos(outerDirection) * outerSpeed * dt;
            outerOffset[1] += (float) Math.sin(outerDirection) * outerSpeed * dt;

            // Update Cloud Density
            densityTimeSinceChange += dt;
            if (densityTimeSinceChange > densityChangeInterval) {
                densityTimeSinceChange = 0f;
                // Change every 1-2 minutes
                densityChangeInterval = 60f + random.nextFloat() * 60f;
                
                // Target density +/- 0.2 (normal distribution, subtle)
                float innerChange = (float) random.nextGaussian() * 0.1f;
                targetInnerCloudDensity = Math.clamp(innerChange, -0.2f, 0.2f);

                // Outer layer varies independently but similarly
                float outerChange = (float) random.nextGaussian() * 0.1f;
                targetOuterCloudDensity = Math.clamp(outerChange, -0.2f, 0.2f);
            }
            // Very slow interpolation
            innerCloudDensity += (targetInnerCloudDensity - innerCloudDensity) * dt * 0.05f;
            outerCloudDensity += (targetOuterCloudDensity - outerCloudDensity) * dt * 0.05f;

            skyShader.setUniform(SkyShader.Uniforms.INNER_OFFSET, innerOffset[0], innerOffset[1]);
            skyShader.setUniform(SkyShader.Uniforms.OUTER_OFFSET, outerOffset[0], outerOffset[1]);
            skyShader.setUniform(SkyShader.Uniforms.INNER_CLOUD_DENSITY, innerCloudDensity);
            skyShader.setUniform(SkyShader.Uniforms.OUTER_CLOUD_DENSITY, outerCloudDensity);

            if (VertexArrays.isSupported()) {
                skyVAO.bind();
            } else {
                setupSkyAttributes();
            }

            for (ShortVBO strip_indice : strip_indices) {
                strip_indice.drawElements(GL11.GL_TRIANGLE_STRIP, subdiv_axis * 2 + 2, 0);
            }
            fan_indices.drawElements(GL11.GL_TRIANGLE_FAN, subdiv_axis + 2, 0);

            if (VertexArrays.isSupported()) {
                skyVAO.unbind();
            } else {
                int posLoc = skyShader.getAttributeLocation(SkyShader.Attributes.POSITION);
                int normLoc = skyShader.getAttributeLocation(SkyShader.Attributes.NORMAL);
                int tex0Loc = skyShader.getAttributeLocation(SkyShader.Attributes.TEX_COORD_0);
                int tex1Loc = skyShader.getAttributeLocation(SkyShader.Attributes.TEX_COORD_1);
                int colLoc = skyShader.getAttributeLocation(SkyShader.Attributes.COLOR);

                GL20.glDisableVertexAttribArray(posLoc);
                GL20.glDisableVertexAttribArray(normLoc);
                GL20.glDisableVertexAttribArray(tex0Loc);
                GL20.glDisableVertexAttribArray(tex1Loc);
                GL20.glDisableVertexAttribArray(colLoc);
            }
        } finally {
            com.oddlabs.tt.vbo.VBO.releaseIndexVBO();
        }
    }

    public void renderSeaBottom(@NonNull CameraState state, @NonNull MatrixStack modelView, @NonNull MatrixStack projection) {
        try (var _ = seaBottomShader.use();
             var _ = state.getFog().setup(seaBottomShader, state.getCurrentZ())) {
            seaBottomShader.setUniformMatrix4(SeaBottomShader.Uniforms.PROJECTION_MATRIX, false, projection.current());
            seaBottomShader.setUniformMatrix4(SeaBottomShader.Uniforms.MODEL_VIEW_MATRIX, false, modelView.current());

            float[] seaColor = Globals.SEA_BOTTOM_COLOR[terrain.ordinal()];
            seaBottomShader.setUniform(SeaBottomShader.Uniforms.BASE_COLOR, seaColor[0], seaColor[1], seaColor[2], 1.0f);

            if (Globals.draw_detail) {
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, detail.getHandle());
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.TEXTURE_1, 1);
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.DETAIL_SCALE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE);
            } else {
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.DETAIL_SCALE, 0f);
            }

            if (VertexArrays.isSupported()) {
                seaBottomVAO.bind();
            } else {
                setupSeaBottomAttributes();
            }

            water_indices.drawElements(GL11.GL_TRIANGLES, water_indices.capacity(), 0);

            if (VertexArrays.isSupported()) {
                seaBottomVAO.unbind();
            } else {
                int bottomPosLoc = seaBottomShader.getAttributeLocation(SeaBottomShader.Attributes.POSITION);
                GL20.glDisableVertexAttribArray(bottomPosLoc);
            }

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        } finally {
            com.oddlabs.tt.vbo.VBO.releaseIndexVBO();
        }
    }

    private void setupSkyAttributes() {
        int posLoc = skyShader.getAttributeLocation(SkyShader.Attributes.POSITION);
        int normLoc = skyShader.getAttributeLocation(SkyShader.Attributes.NORMAL);
        int tex0Loc = skyShader.getAttributeLocation(SkyShader.Attributes.TEX_COORD_0);
        int tex1Loc = skyShader.getAttributeLocation(SkyShader.Attributes.TEX_COORD_1);
        int colLoc = skyShader.getAttributeLocation(SkyShader.Attributes.COLOR);

        sky_vertices.vertexAttribPointer(posLoc, 3, 0, 0);
        GL20.glEnableVertexAttribArray(posLoc);
        sky_normals.vertexAttribPointer(normLoc, 3, 0, 0);
        GL20.glEnableVertexAttribArray(normLoc);
        sky_tex0.vertexAttribPointer(tex0Loc, 2, 0, 0);
        GL20.glEnableVertexAttribArray(tex0Loc);
        sky_tex1.vertexAttribPointer(tex1Loc, 2, 0, 0);
        GL20.glEnableVertexAttribArray(tex1Loc);
        sky_colors.vertexAttribPointer(colLoc, 3, 0, 0);
        GL20.glEnableVertexAttribArray(colLoc);
    }

    private void setupSeaBottomAttributes() {
        int bottomPosLoc = seaBottomShader.getAttributeLocation(SeaBottomShader.Attributes.POSITION);
        bottom_vertices.vertexAttribPointer(bottomPosLoc, 3, 0, 0);
        GL20.glEnableVertexAttribArray(bottomPosLoc);
    }

    private Sky(@NonNull LandscapeRenderer landscape_renderer, Landscape.@NonNull TerrainType terrain, float inner_radius, float radius, int subdiv_axis, int subdiv_height, float outer_utile, float outer_vtile, float inner_utile, float inner_vtile, float origin_x, float origin_y, float origin_z, @NonNull SkyShader skyShader, @NonNull SeaBottomShader seaBottomShader, @NonNull Texture detail) {
        this.terrain = terrain;
        this.skyShader = skyShader;
        this.seaBottomShader = seaBottomShader;
        this.detail = detail;
        this.subdiv_axis = subdiv_axis;
        this.subdiv_height = subdiv_height;
        this.color = BufferUtils.createFloatBuffer(4).put(tex_env_color[terrain.ordinal()]);
        color.rewind();
        TextureGenerator clouds_desc = new GeneratorClouds(terrain);
        clouds = Resources.findResource(clouds_desc);
        makeSkyVertices(radius, outer_utile, outer_vtile, inner_utile, inner_vtile, origin_x, origin_y, origin_z);
        strip_indices = makeSkyStripIndices();
        fan_indices = makeSkyFanIndices();

        List<SkyStitchVertex[]> vertices_stitch_list = new ArrayList<>();
        List<ShortBuffer> stitch_indices_list = new ArrayList<>();
        int num_vertices = 0;
        int num_indices = 0;
        SkyStitchVertex[] previous_vertices = makeLandscapeVertices(landscape_renderer.getHeightMap());
        vertices_stitch_list.add(previous_vertices);
        num_vertices += previous_vertices.length;
        for (int i = 0; i < NUM_WATER_RINGS; i++) {
            float radius_factor = (float) (i + 1) / NUM_WATER_RINGS;
            float ring_radius = inner_radius + (float) Math.pow(radius - inner_radius, radius_factor);
            SkyStitchVertex[] ring_vertices = makeDomeVertices(landscape_renderer.getHeightMap(), i + 1, num_vertices, ring_radius, origin_x, origin_y);
            vertices_stitch_list.add(ring_vertices);
            num_vertices += ring_vertices.length;
            SkyStitchVertex[] stitch_vertices = new SkyStitchVertex[ring_vertices.length + previous_vertices.length];
            System.arraycopy(previous_vertices, 0, stitch_vertices, 0, previous_vertices.length);
            System.arraycopy(ring_vertices, 0, stitch_vertices, previous_vertices.length, ring_vertices.length);
            ShortBuffer stitch_indices = Stitcher.stitch(stitch_vertices);
            stitch_indices_list.add(stitch_indices);
            num_indices += stitch_indices.remaining();
            previous_vertices = ring_vertices;
        }
        SkyStitchVertex[] all_vertices = new SkyStitchVertex[num_vertices];
        int index = 0;
        for (SkyStitchVertex[] vertices : vertices_stitch_list) {
            System.arraycopy(vertices, 0, all_vertices, index, vertices.length);
            index += vertices.length;
        }
        assert index == all_vertices.length;
        ShortBuffer all_indices = BufferUtils.createShortBuffer(num_indices);
        for (ShortBuffer indices : stitch_indices_list) {
            all_indices.put(indices);
        }
        assert !all_indices.hasRemaining();
        all_indices.flip();
        water_indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices);
        water_vertices = toVBO(all_vertices, landscape_renderer.getHeightMap().getSeaLevelMeters());
        bottom_vertices = toVBO(all_vertices, 0);

        // Setup VAOs
        this.skyVAO = VertexArrays.create();
        this.skyVAO.bind();
        if (VertexArrays.isSupported()) {
            setupSkyAttributes();
        }
        this.skyVAO.unbind();

        this.seaBottomVAO = VertexArrays.create();
        this.seaBottomVAO.bind();
        if (VertexArrays.isSupported()) {
            setupSeaBottomAttributes();
        }
        this.seaBottomVAO.unbind();
    }

    private static @NonNull FloatVBO toVBO(SkyStitchVertex @NonNull [] vertices, float height) {
        FloatBuffer vertex_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(vertices.length * 3));
        for (int i = 0; i < vertices.length; i++) {
            SkyStitchVertex vertex = vertices[i];
            assert vertex.getIndex() == i : vertex.getIndex() + " " + i;
            float x = vertex.x;
            float y = vertex.y;
            float z = (height * (NUM_WATER_RINGS - vertex.getSide())) / NUM_WATER_RINGS;
            vertex_buffer.put(x).put(y).put(z);
        }
        assert !vertex_buffer.hasRemaining();
        vertex_buffer.flip();
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertex_buffer);
    }

    public @NonNull FloatVBO getWaterVertices() {
        return water_vertices;
    }

    public @NonNull ShortVBO getWaterIndices() {
        return water_indices;
    }

    private void makeSkyVertices(float radius, float outer_utile, float outer_vtile, float inner_utile, float inner_vtile, float origin_x, float origin_y, float origin_z) {
        float r;
        float x, y, z;
        float height_coeff;
        float dome_height = radius;
        float h_angle_inc = ((float) java.lang.Math.PI / 2) / (subdiv_height - 1);
        float a_angle_inc = (float) java.lang.Math.PI * 2 / subdiv_axis;
        float offset_angle = a_angle_inc / 2f;
        int num_vertices = subdiv_axis * (subdiv_height - 1) + 1;
        float[] vertices = new float[num_vertices * 3];
        float[] normals = new float[num_vertices * 3];
        float[] tex0 = new float[num_vertices * 2];
        float[] tex1 = new float[num_vertices * 2];
        float[] colors = new float[num_vertices * 3];

        float[] skydome_default_color = new float[]{
                (float) Math.pow(SKYDOME_GRADIENT[terrain.ordinal()][0], SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(SKYDOME_GRADIENT[terrain.ordinal()][1], SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(SKYDOME_GRADIENT[terrain.ordinal()][2], SKYDOME_DEFAULT_COLOR)
        };
        float[][] skydome_gradient = new float[SKYDOME_GRADIENT_LENGTH][3];
        skydome_gradient[0] = SKYDOME_INITCOLOR[terrain.ordinal()];

        float alpha;
        float [] gradient = SKYDOME_GRADIENT[terrain.ordinal()];
        for (int i = 1; i < SKYDOME_GRADIENT_LENGTH; i++) {
            alpha = (float) i / (SKYDOME_GRADIENT_LENGTH - 1);
            skydome_gradient[i] = new float[]{
                    alpha * skydome_default_color[0] + (1f - alpha) * skydome_gradient[i - 1][0] * gradient[0],
                    alpha * skydome_default_color[1] + (1f - alpha) * skydome_gradient[i - 1][1] * gradient[1],
                    alpha * skydome_default_color[2] + (1f - alpha) * skydome_gradient[i - 1][2] * gradient[2]
            };
        }

        for (int i = 0; i < subdiv_height - 1; i++) {
            z = (float) java.lang.Math.sin(h_angle_inc * i) * radius;
            r = (float) java.lang.Math.cos(h_angle_inc * i) * radius;

            height_coeff = Math.abs(z) < 250f ? dome_height / 250f : dome_height / z;

            for (int j = 0; j < subdiv_axis; j++) {
                x = (float) java.lang.Math.cos(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;
                y = (float) java.lang.Math.sin(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;
                putArray(i < SKYDOME_GRADIENT_LENGTH ? skydome_gradient[i] : skydome_default_color, i * subdiv_axis + j, colors);

                float[] position = {x + origin_x, y + origin_y, z + origin_z};
                putArray(position, i * subdiv_axis + j, vertices);

                float inv_len = 1.0f / (float) Math.sqrt(x * x + y * y + z * z);
                putArray(new float[]{x * inv_len, y * inv_len, z * inv_len}, i * subdiv_axis + j, normals);

                putArray(new float[]{x * height_coeff / (radius * outer_utile) + 0.5f, y * height_coeff / (radius * outer_vtile) + 0.5f}, i * subdiv_axis + j, tex0);
                putArray(new float[]{x * height_coeff / (radius * inner_utile) + 0.5f, y * height_coeff / (radius * inner_vtile) + 0.5f}, i * subdiv_axis + j, tex1);
            }
        }
        int last_index = subdiv_axis * (subdiv_height - 1);
        putArray(subdiv_height - 1 < SKYDOME_GRADIENT_LENGTH ? skydome_gradient[subdiv_height - 1] : skydome_default_color, last_index, colors);

        putArray(new float[]{origin_x, origin_y, radius + origin_z}, last_index, vertices);
        putArray(new float[]{0, 0, 1}, last_index, normals); // Zenith normal points straight up
        putArray(new float[]{0.5f, 0.5f}, last_index, tex0);
        putArray(new float[]{0.5f, 0.5f}, last_index, tex1);

        sky_vertices = new FloatVBO(GL15.GL_STATIC_DRAW, vertices);
        sky_normals = new FloatVBO(GL15.GL_STATIC_DRAW, normals);
        sky_tex0 = new FloatVBO(GL15.GL_STATIC_DRAW, tex0);
        sky_tex1 = new FloatVBO(GL15.GL_STATIC_DRAW, tex1);
        sky_colors = new FloatVBO(GL15.GL_STATIC_DRAW, colors);
    }

    private void putArray(float @NonNull [] src, int offset, float @NonNull [] dest) {
        System.arraycopy(src, 0, dest, offset * src.length, src.length);
    }

    private ShortVBO @NonNull [] makeSkyStripIndices() {
        ShortVBO[] strip_indices = new ShortVBO[subdiv_height - 2];
        for (int i = 0; i < strip_indices.length; i++) {
            int size = subdiv_axis * 2 + 2;
            ShortBuffer temp = Objects.requireNonNull(BufferUtils.createShortBuffer(size));
            for (int j = 0; j < subdiv_axis; j++) {
                temp.put(j * 2, (short) (i * subdiv_axis + j));
                temp.put(j * 2 + 1, (short) ((i + 1) * subdiv_axis + j));
            }
            temp.put(subdiv_axis * 2, (short) (i * subdiv_axis));
            temp.put(subdiv_axis * 2 + 1, (short) ((i + 1) * subdiv_axis));
            strip_indices[i] = new ShortVBO(GL15.GL_STATIC_DRAW, size);
            temp.rewind();
            strip_indices[i].put(temp);
        }
        return strip_indices;
    }

    private @NonNull ShortVBO makeSkyFanIndices() {
        int size = subdiv_axis + 2;
        ShortBuffer temp = Objects.requireNonNull(BufferUtils.createShortBuffer(size));
        temp.put(0, (short) (sky_vertices.capacity() / 3 - 1));
        for (int i = 0; i < subdiv_axis; i++) {
            temp.put(i + 1, (short) ((subdiv_height - 1) * subdiv_axis - i - 1));
        }
        temp.put(subdiv_axis + 1, (short) ((subdiv_height - 1) * subdiv_axis - 1));

        ShortVBO fan_indices = new ShortVBO(GL15.GL_STATIC_DRAW, size);
        temp.rewind();
        fan_indices.put(temp);
        return fan_indices;
    }

    private @NonNull SkyStitchVertex @NonNull [] makeDomeVertices(@NonNull HeightMap heightmap, int ring_id, int index_offset, float radius, float origin_x, float origin_y) {
        float a_angle_inc = (float) Math.PI * 2 / subdiv_axis;
        return IntStream.range(0, subdiv_axis)
                .mapToObj(i -> {
                    int index = i + index_offset;
                    return new SkyStitchVertex(heightmap, index, ring_id,
                            (float) java.lang.Math.cos(START_ANGLE + a_angle_inc * i) * radius + origin_x,
                            (float) java.lang.Math.sin(START_ANGLE + a_angle_inc * i) * radius + origin_y);
                }).toArray(SkyStitchVertex[]::new);
    }

    private @NonNull SkyStitchVertex @NonNull [] makeLandscapeVertices(@NonNull HeightMap heightmap) {
        int size = 4 * heightmap.getPatchesPerWorld();
        SkyStitchVertex[] result = new SkyStitchVertex[size];

        for (int i = 0; i < heightmap.getPatchesPerWorld(); i++) {
            int index = i;
            result[index] = new SkyStitchVertex(heightmap, index, 0, 0, heightmap.getMetersPerPatch() * i);
            index = i + heightmap.getPatchesPerWorld();
            result[index] = new SkyStitchVertex(heightmap, index, 0,
                    heightmap.getMetersPerPatch() * i, heightmap.getMetersPerWorld());
            index = i + heightmap.getPatchesPerWorld() * 2;
            result[index] = new SkyStitchVertex(heightmap, index, 0,
                    heightmap.getMetersPerWorld(),
                    heightmap.getMetersPerWorld() - heightmap.getMetersPerPatch() * i);
            index = i + heightmap.getPatchesPerWorld() * 3;
            result[index] = new SkyStitchVertex(heightmap, index, 0,
                    heightmap.getMetersPerWorld() - heightmap.getMetersPerPatch() * i,
                    0);
        }
        return result;
    }

    private static class SkyStitchVertex extends Stitcher.Vertex<SkyStitchVertex> {

        private final float x;
        private final float y;
        private final float theta;
        private final @NonNull HeightMap heightmap;

        private SkyStitchVertex(@NonNull HeightMap heightmap, int index, int side, float x, float y) {
            super(index, side);
            this.heightmap = heightmap;
            this.x = x;
            this.y = y;
            float half_world_size = heightmap.getMetersPerWorld() * .5f;
            this.theta = (float) Math.atan2(y - half_world_size, x - half_world_size);
        }

        @Override
        public final int compareTo(@NonNull SkyStitchVertex o) {
            return -Float.compare(theta, o.theta);
        }

        @Override
        public int hashCode() {
            return Float.hashCode(theta);
        }

        @Override
        public final boolean equals(Object o) {
            return o instanceof SkyStitchVertex other && other.heightmap == heightmap && other.theta == theta;
        }

        @Override
        public final @NonNull String toString() {
            float half_world_size = heightmap.getMetersPerWorld() * .5f;
            float x0 = x - half_world_size;
            float y0 = y - half_world_size;
            float inv_len = (float) (1 / Math.sqrt(x0 * x0 + y0 * y0));
            x0 *= inv_len;
            y0 *= inv_len;
            return x + " " + y + "\t\t" + x0 + " " + y0 + " " + theta / Math.PI + " " + super.toString();
        }
    }
}