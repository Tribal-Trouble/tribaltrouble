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
import com.oddlabs.util.Color;
import org.joml.Vector4f;
import org.joml.Vector4fc;
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
import java.util.Random;
import java.util.stream.IntStream;

import static com.oddlabs.tt.procedural.Landscape.NATIVE_SEA_BOTTOM_COLOR;
import static com.oddlabs.tt.procedural.Landscape.VIKING_SEA_BOTTOM_COLOR;

public final class Sky implements AutoCloseable {
    private static final float[] SKYDOME_SPEED_OUTER = {0.2f, 0f};
    private static final float[] SKYDOME_SPEED_INNER = {0.4f, 0f};
    private static final float SKYDOME_HEIGHT = 0f;
    private static final int SKYDOME_GRADIENT_LENGTH = 20;
    private static final int SKYDOME_DEFAULT_COLOR = 8;

    private static final Vector4fc[] SKYDOME_INITCOLOR = {
            /* Native */ Color.argb4v(0xFF_E5_F2_FF),
            /* Viking */ Color.argb4v(0xFF_FF_E5_A6)
    };

    private static final Vector4fc[] SKYDOME_GRADIENT = {
            /* Native */ Color.argb4v(0xFF_BF_D2_F2),
            /* Viking */ Color.argb4v(0xFF_99_99_D9)
    };

    private static final Vector4fc[] tex_env_color = {
            /* Native */ Color.argb4v(0xFF_F2_F8_FF),
            /* Viking */ Color.argb4v(0xFF_FF_F2_CC)
    };

    private static final float SKYDOME_OUTER_UTILING = 8f;
    private static final float SKYDOME_OUTER_VTILING = 8f;
    private static final float SKYDOME_INNER_UTILING = 8f;
    private static final float SKYDOME_INNER_VTILING = 8f;

    private static final int NUM_WATER_RINGS = 6;

    private static final float START_ANGLE = -(float) Math.PI / 4f;

    private final @NonNull FloatBuffer color;
    private final ShortVBO @NonNull [] strip_indices;
    private final @NonNull ShortVBO fan_indices;
    private final @NonNull FloatVBO water_vertices;
    private final @NonNull FloatVBO bottom_vertices;
    private final @NonNull ShortVBO water_indices;
    private final @NonNull FloatVBO sky_vbo;

    private final @NonNull Texture @NonNull [] clouds;
    private final int subdiv_axis;
    private final int subdiv_height;
    private final Landscape.@NonNull TerrainType terrain;

    private final SkyShader skyShader = new SkyShader();
    private final SeaBottomShader seaBottomShader = new SeaBottomShader();
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
    private final Random random = new Random();

    public Sky(@NonNull LandscapeRenderer renderer, Landscape.@NonNull TerrainType terrain, @NonNull Texture detail) {
        this(renderer, terrain, (float) (renderer.getHeightMap().getMetersPerWorld() * Math.sqrt(2) / 2), 6000f, 20, 20, SKYDOME_OUTER_UTILING, SKYDOME_OUTER_VTILING, SKYDOME_INNER_UTILING, SKYDOME_INNER_VTILING, renderer.getHeightMap().getMetersPerWorld() / 2, renderer.getHeightMap().getMetersPerWorld() / 2, SKYDOME_HEIGHT, detail);
    }

    public void render(@NonNull CameraState state, @NonNull MatrixStack modelView, @NonNull MatrixStack projection) {
        try (var _ = skyShader.use()) {
            skyShader.setUniformMatrix4(SkyShader.Uniforms.PROJECTION_MATRIX, false, projection.current());
            skyShader.setUniformMatrix4(SkyShader.Uniforms.MODEL_VIEW_MATRIX, false, modelView.current());
            skyShader.setUniform(SkyShader.Uniforms.SKY_COLOR, color.get(0), color.get(1), color.get(2), color.get(3));

            FogInfo fog = state.getFog();
            if (fog.isEnabled()) {
                var fogColor = fog.getColor();
                skyShader.setUniform(SkyShader.Uniforms.FOG_COLOR, fogColor.x(), fogColor.y(), fogColor.z(), fogColor.w());
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
                skyShader.setUniform(SkyShader.Uniforms.FOG_COLOR, 0f, 0f, 0f, 0f); 
            }

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, clouds[GeneratorClouds.INNER].getHandle());
            skyShader.setUniform(SkyShader.Uniforms.TEXTURE_0, 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, clouds[GeneratorClouds.OUTER].getHandle());
            skyShader.setUniform(SkyShader.Uniforms.TEXTURE_1, 1);

            updateAnimation();

            skyShader.setUniform(SkyShader.Uniforms.INNER_OFFSET, innerOffset[0], innerOffset[1]);
            skyShader.setUniform(SkyShader.Uniforms.OUTER_OFFSET, outerOffset[0], outerOffset[1]);
            skyShader.setUniform(SkyShader.Uniforms.INNER_CLOUD_DENSITY, innerCloudDensity);
            skyShader.setUniform(SkyShader.Uniforms.OUTER_CLOUD_DENSITY, outerCloudDensity);

            skyVAO.bind();

            for (ShortVBO strip_indice : strip_indices) {
                strip_indice.drawElements(GL11.GL_TRIANGLE_STRIP, subdiv_axis * 2 + 2, 0);
            }
            fan_indices.drawElements(GL11.GL_TRIANGLE_FAN, subdiv_axis + 2, 0);

            skyVAO.unbind();
        } finally {
            com.oddlabs.tt.vbo.VBO.releaseIndexVBO();
        }
    }

    private void updateAnimation() {
        float currentTime = LocalEventQueue.getQueue().getTime();
        float dt = currentTime - lastTime;
        if (dt < 0 || dt > 1.0f) dt = 0.016f;
        lastTime = currentTime;

        innerTimeSinceChange += dt;
        if (innerTimeSinceChange > innerChangeInterval) {
            innerTimeSinceChange = 0f;
            innerChangeInterval = 30f + (float) random.nextGaussian() * 10f; 
            float dirChange = (float) random.nextGaussian() * 10f;
            targetInnerDirection += (float) Math.toRadians(dirChange);
            float speedChange = innerSpeed * (float) random.nextGaussian() * 0.1f;
            targetInnerSpeed = Math.clamp(targetInnerSpeed + speedChange, 0.002f, 0.008f);
        }
        innerDirection += (targetInnerDirection - innerDirection) * dt * 0.2f;
        innerSpeed += (targetInnerSpeed - innerSpeed) * dt * 0.2f;
        
        innerOffset[0] += (float) Math.cos(innerDirection) * innerSpeed * dt;
        innerOffset[1] += (float) Math.sin(innerDirection) * innerSpeed * dt;

        outerTimeSinceChange += dt;
        if (outerTimeSinceChange > outerChangeInterval) {
            outerTimeSinceChange = 0f;
            outerChangeInterval = 40f + (float) random.nextGaussian() * 15f; 
            float dirChange = (float) random.nextGaussian() * 8f;
            targetOuterDirection += (float) Math.toRadians(dirChange);
            float speedChange = outerSpeed * (float) random.nextGaussian() * 0.1f;
            targetOuterSpeed = Math.clamp(targetOuterSpeed + speedChange, 0.001f, 0.004f);
        }
        outerDirection += (targetOuterDirection - outerDirection) * dt * 0.1f;
        outerSpeed += (targetOuterSpeed - outerSpeed) * dt * 0.1f;

        outerOffset[0] += (float) Math.cos(outerDirection) * outerSpeed * dt;
        outerOffset[1] += (float) Math.sin(outerDirection) * outerSpeed * dt;

        densityTimeSinceChange += dt;
        if (densityTimeSinceChange > densityChangeInterval) {
            densityTimeSinceChange = 0f;
            densityChangeInterval = 60f + random.nextFloat() * 60f;
            float innerChange = (float) random.nextGaussian() * 0.1f;
            targetInnerCloudDensity = Math.clamp(innerChange, -0.2f, 0.2f);
            float outerChange = (float) random.nextGaussian() * 0.1f;
            targetOuterCloudDensity = Math.clamp(outerChange, -0.2f, 0.2f);
        }
        innerCloudDensity += (targetInnerCloudDensity - innerCloudDensity) * dt * 0.05f;
        outerCloudDensity += (targetOuterCloudDensity - outerCloudDensity) * dt * 0.05f;
    }

    public void renderSeaBottom(@NonNull CameraState state, @NonNull MatrixStack modelView, @NonNull MatrixStack projection) {
        try (var _ = seaBottomShader.use();
             var _ = state.getFog().setup(seaBottomShader, state.getCurrentZ())) {
            seaBottomShader.setUniformMatrix4(SeaBottomShader.Uniforms.PROJECTION_MATRIX, false, projection.current());
            seaBottomShader.setUniformMatrix4(SeaBottomShader.Uniforms.MODEL_VIEW_MATRIX, false, modelView.current());

            var seaColor = switch (terrain) {
                case NATIVE -> NATIVE_SEA_BOTTOM_COLOR;
                case VIKING -> VIKING_SEA_BOTTOM_COLOR;
            };
            seaBottomShader.setUniform(SeaBottomShader.Uniforms.BASE_COLOR, seaColor.x(), seaColor.y(), seaColor.z(), seaColor.w());

            if (Globals.draw_detail) {
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, detail.getHandle());
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.TEXTURE_1, 1);
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.DETAIL_SCALE, Globals.LANDSCAPE_DETAIL_REPEAT_RATE);
            } else {
                seaBottomShader.setUniform(SeaBottomShader.Uniforms.DETAIL_SCALE, 0f);
            }

            seaBottomVAO.bind();
            water_indices.drawElements(GL11.GL_TRIANGLES, water_indices.capacity(), 0);
            seaBottomVAO.unbind();

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        } finally {
            com.oddlabs.tt.vbo.VBO.releaseIndexVBO();
        }
    }

    private Sky(@NonNull LandscapeRenderer landscape_renderer, Landscape.@NonNull TerrainType terrain, float inner_radius, float radius, int subdiv_axis, int subdiv_height, float outer_utile, float outer_vtile, float inner_utile, float inner_vtile, float origin_x, float origin_y, float origin_z, @NonNull Texture detail) {
        this.terrain = terrain;
        this.detail = detail;
        this.subdiv_axis = subdiv_axis;
        this.subdiv_height = subdiv_height;
        this.color = tex_env_color[terrain.ordinal()].get(BufferUtils.createFloatBuffer(4)).rewind();
        TextureGenerator clouds_desc = new GeneratorClouds(terrain);
        clouds = Resources.findResource(clouds_desc);
        
        // Create interleaved VBO for the sky
        int num_vertices_sky = subdiv_axis * (subdiv_height - 1) + 1;
        int stride = (3 + 3 + 2 + 2 + 3) * Float.BYTES; // pos, norm, uv0, uv1, color
        FloatBuffer skyBuffer = BufferUtils.createFloatBuffer(num_vertices_sky * (stride / Float.BYTES));
        makeSkyVertices(radius, outer_utile, outer_vtile, inner_utile, inner_vtile, origin_x, origin_y, origin_z, skyBuffer);
        skyBuffer.flip();
        sky_vbo = new FloatVBO(GL15.GL_STATIC_DRAW, skyBuffer);

        strip_indices = makeSkyStripIndices();
        fan_indices = makeSkyFanIndices();

        // --- Sea bottom and water stitching logic (remains complex) ---
        List<SkyStitchVertex[]> vertices_stitch_list = new ArrayList<>();
        List<ShortBuffer> stitch_indices_list = new ArrayList<>();
        int num_vertices_water = 0;
        int num_indices = 0;
        SkyStitchVertex[] previous_vertices = makeLandscapeVertices(landscape_renderer.getHeightMap());
        vertices_stitch_list.add(previous_vertices);
        num_vertices_water += previous_vertices.length;
        for (int i = 0; i < NUM_WATER_RINGS; i++) {
            float radius_factor = (float) (i + 1) / NUM_WATER_RINGS;
            float ring_radius = inner_radius + (float) Math.pow(radius - inner_radius, radius_factor);
            SkyStitchVertex[] ring_vertices = makeDomeVertices(landscape_renderer.getHeightMap(), i + 1, num_vertices_water, ring_radius, origin_x, origin_y);
            vertices_stitch_list.add(ring_vertices);
            num_vertices_water += ring_vertices.length;
            SkyStitchVertex[] stitch_vertices = new SkyStitchVertex[ring_vertices.length + previous_vertices.length];
            System.arraycopy(previous_vertices, 0, stitch_vertices, 0, previous_vertices.length);
            System.arraycopy(ring_vertices, 0, stitch_vertices, previous_vertices.length, ring_vertices.length);
            ShortBuffer stitch_indices = Stitcher.stitch(stitch_vertices);
            stitch_indices_list.add(stitch_indices);
            num_indices += stitch_indices.remaining();
            previous_vertices = ring_vertices;
        }
        SkyStitchVertex[] all_vertices = new SkyStitchVertex[num_vertices_water];
        int index = 0;
        for (SkyStitchVertex[] vertices : vertices_stitch_list) {
            System.arraycopy(vertices, 0, all_vertices, index, vertices.length);
            index += vertices.length;
        }
        ShortBuffer all_indices = BufferUtils.createShortBuffer(num_indices);
        for (ShortBuffer indices : stitch_indices_list) {
            all_indices.put(indices);
        }
        all_indices.flip();
        water_indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices);
        water_vertices = toVBO(all_vertices, landscape_renderer.getHeightMap().getSeaLevelMeters());
        bottom_vertices = toVBO(all_vertices, 0);

        this.skyVAO = new VertexArray();
        skyVAO.bind();
        sky_vbo.makeCurrent();
        int posLoc = skyShader.getAttributeLocation(SkyShader.Attributes.POSITION);
        GL20.glEnableVertexAttribArray(posLoc);
        GL20.glVertexAttribPointer(posLoc, 3, GL11.GL_FLOAT, false, stride, 0);
        int normLoc = skyShader.getAttributeLocation(SkyShader.Attributes.NORMAL);
        GL20.glEnableVertexAttribArray(normLoc);
        GL20.glVertexAttribPointer(normLoc, 3, GL11.GL_FLOAT, false, stride, 3 * Float.BYTES);
        int tex0Loc = skyShader.getAttributeLocation(SkyShader.Attributes.TEX_COORD_0);
        GL20.glEnableVertexAttribArray(tex0Loc);
        GL20.glVertexAttribPointer(tex0Loc, 2, GL11.GL_FLOAT, false, stride, 6 * Float.BYTES);
        int tex1Loc = skyShader.getAttributeLocation(SkyShader.Attributes.TEX_COORD_1);
        GL20.glEnableVertexAttribArray(tex1Loc);
        GL20.glVertexAttribPointer(tex1Loc, 2, GL11.GL_FLOAT, false, stride, 8 * Float.BYTES);
        int colLoc = skyShader.getAttributeLocation(SkyShader.Attributes.COLOR);
        GL20.glEnableVertexAttribArray(colLoc);
        GL20.glVertexAttribPointer(colLoc, 3, GL11.GL_FLOAT, false, stride, 10 * Float.BYTES);
        skyVAO.unbind();

        this.seaBottomVAO = new VertexArray();
        seaBottomVAO.bind();
        bottom_vertices.makeCurrent();
        int bottomPosLoc = seaBottomShader.getAttributeLocation(SeaBottomShader.Attributes.POSITION);
        GL20.glEnableVertexAttribArray(bottomPosLoc);
        GL20.glVertexAttribPointer(bottomPosLoc, 3, GL11.GL_FLOAT, false, 0, 0);
        seaBottomVAO.unbind();
    }

    private static @NonNull FloatVBO toVBO(SkyStitchVertex @NonNull [] vertices, float height) {
        FloatBuffer vertex_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(vertices.length * 3));
        for (SkyStitchVertex vertex : vertices) {
            float x = vertex.x;
            float y = vertex.y;
            float z = (height * (NUM_WATER_RINGS - vertex.getSide())) / NUM_WATER_RINGS;
            vertex_buffer.put(x).put(y).put(z);
        }
        vertex_buffer.flip();
        return new FloatVBO(GL15.GL_STATIC_DRAW, vertex_buffer);
    }

    public @NonNull FloatVBO getWaterVertices() {
        return water_vertices;
    }

    public @NonNull ShortVBO getWaterIndices() {
        return water_indices;
    }

    private void makeSkyVertices(float radius, float outer_utile, float outer_vtile, float inner_utile, float inner_vtile, float origin_x, float origin_y, float origin_z, @NonNull FloatBuffer buffer) {
        float r;
        float x, y, z;
        float height_coeff;
        float dome_height = radius;
        float h_angle_inc = ((float) java.lang.Math.PI / 2) / (subdiv_height - 1);
        float a_angle_inc = (float) java.lang.Math.PI * 2 / subdiv_axis;
        float offset_angle = a_angle_inc / 2f;
        
        Vector4f skydome_default_color = new Vector4f(
                (float) Math.pow(SKYDOME_GRADIENT[terrain.ordinal()].x(), SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(SKYDOME_GRADIENT[terrain.ordinal()].y(), SKYDOME_DEFAULT_COLOR),
                (float) Math.pow(SKYDOME_GRADIENT[terrain.ordinal()].z(), SKYDOME_DEFAULT_COLOR),
                1.0f
        );
        Vector4f[] skydome_gradient = new Vector4f[SKYDOME_GRADIENT_LENGTH];
        skydome_gradient[0] = new Vector4f(SKYDOME_INITCOLOR[terrain.ordinal()]);

        float alpha;
        Vector4fc gradient = SKYDOME_GRADIENT[terrain.ordinal()];
        for (int i = 1; i < SKYDOME_GRADIENT_LENGTH; i++) {
            alpha = (float) i / (SKYDOME_GRADIENT_LENGTH - 1);
            skydome_gradient[i] = new Vector4f(
                    alpha * skydome_default_color.x() + (1f - alpha) * skydome_gradient[i - 1].x() * gradient.x(),
                    alpha * skydome_default_color.y() + (1f - alpha) * skydome_gradient[i - 1].y() * gradient.y(),
                    alpha * skydome_default_color.z() + (1f - alpha) * skydome_gradient[i - 1].z() * gradient.z(),
                    1.0f
            );
        }

        for (int i = 0; i < subdiv_height - 1; i++) {
            z = (float) java.lang.Math.sin(h_angle_inc * i) * radius;
            r = (float) java.lang.Math.cos(h_angle_inc * i) * radius;
            height_coeff = Math.abs(z) < 250f ? dome_height / 250f : dome_height / z;

            for (int j = 0; j < subdiv_axis; j++) {
                x = (float) java.lang.Math.cos(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;
                y = (float) java.lang.Math.sin(START_ANGLE + a_angle_inc * j + offset_angle * i) * r;
                
                buffer.put(x + origin_x).put(y + origin_y).put(z + origin_z); // Position
                float inv_len = 1.0f / (float) Math.sqrt(x * x + y * y + z * z);
                buffer.put(x * inv_len).put(y * inv_len).put(z * inv_len); // Normal
                buffer.put(x * height_coeff / (radius * outer_utile) + 0.5f).put(y * height_coeff / (radius * outer_vtile) + 0.5f); // TexCoord0
                buffer.put(x * height_coeff / (radius * inner_utile) + 0.5f).put(y * height_coeff / (radius * inner_vtile) + 0.5f); // TexCoord1
                Vector4fc color = i < SKYDOME_GRADIENT_LENGTH ? skydome_gradient[i] : skydome_default_color;
                buffer.put(color.x()).put(color.y()).put(color.z()); // Color
            }
        }
        int last_index = subdiv_axis * (subdiv_height - 1);
        buffer.put(origin_x).put(origin_y).put(radius + origin_z); // Position
        buffer.put(0).put(0).put(1); // Normal
        buffer.put(0.5f).put(0.5f); // TexCoord0
        buffer.put(0.5f).put(0.5f); // TexCoord1
        Vector4fc color = subdiv_height - 1 < SKYDOME_GRADIENT_LENGTH ? skydome_gradient[subdiv_height - 1] : skydome_default_color;
        buffer.put(color.x()).put(color.y()).put(color.z()); // Color
    }

    private @NonNull ShortVBO @NonNull [] makeSkyStripIndices() {
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
        temp.put(0, (short) (sky_vbo.capacity() / ( (3 + 3 + 2 + 2 + 3) * Float.BYTES) - 1));
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

    @Override
    public void close() {
        skyVAO.close();
        seaBottomVAO.close();
        skyShader.close();
        seaBottomShader.close();
        sky_vbo.close();
        water_vertices.close();
        bottom_vertices.close();
        water_indices.close();
        fan_indices.close();
        for (ShortVBO vbo : strip_indices) {
            vbo.close();
        }
    }
}
