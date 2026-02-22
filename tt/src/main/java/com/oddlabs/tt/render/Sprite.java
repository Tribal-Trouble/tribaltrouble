package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.procedural.GeneratorRespond;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.vbo.VertexArray;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Supplier;

public final class Sprite {
    static final int TEXTURE_NORMAL = 0;
    static final int TEXTURE_TEAM = 1;
    static final int TEXTURE_BUMP = 2;
    private static final String GENERATOR_STRING = "Generator:";

    final Texture@NonNull [] @NonNull [] textures;
    private final int num_triangles;
    private final int num_vertices;
    private final float @Nullable [] clear_color;
    private final int @NonNull [] buffer_indices;
    final boolean alpha;
    final boolean lighted;
    final boolean culled;
    final boolean modulate_color;
    private final float @Nullable [] cpw_array;
    private final int @Nullable [] animation_length_array;
    private final AnimationInfo.AnimationType @NonNull [] type_array;
    final @Nullable Texture respond_texture;
    final int indices_offset;
    final int texcoords_offset;

    /** Dummy constructor for creating a simple quad sprite. */
    Sprite(int num_vertices, int num_triangles, int indices_offset, boolean modulate_color) {
        this.textures = new Texture[0][0];
        this.num_vertices = num_vertices;
        this.num_triangles = num_triangles;
        this.indices_offset = indices_offset;
        this.texcoords_offset = 0;
        this.clear_color = null;
        this.buffer_indices = null;
        this.alpha = false;
        this.lighted = false;
        this.culled = true;
        this.modulate_color = modulate_color;
        this.cpw_array = null;
        this.animation_length_array = null;
        this.type_array = null;
        this.respond_texture = null;
    }

    public Sprite(@NonNull SpriteInfo sprite_info, AnimationInfo @NonNull [] animations, boolean alpha, boolean lighted, boolean culled, boolean modulate_color, boolean max_alpha, int mipmap_cutoff, BoundingBox[] bounds, float[] cpw_array, AnimationInfo.AnimationType @NonNull [] type_array, int[] animation_length_array, @NonNull ShortBuffer all_indices, @NonNull FloatBuffer all_texcoords, @NonNull FloatBuffer all_vertices_and_normals) {
        this.culled = culled;
        this.alpha = alpha;
        this.lighted = lighted;
        this.modulate_color = modulate_color;
        this.cpw_array = cpw_array;
        this.type_array = type_array;
        this.animation_length_array = animation_length_array;

        short[] tmp_indices = sprite_info.getIndices();
        float[] tmp_texcoords = sprite_info.getTexCoords();
        num_vertices = tmp_texcoords.length / 2;
        num_triangles = tmp_indices.length / 3;

        indices_offset = all_indices.position();
        texcoords_offset = all_texcoords.position();

        all_indices.put(tmp_indices);
        all_texcoords.put(tmp_texcoords);

        float[][][] tmp_vertices = new float[animations.length][][];
        float[][][] tmp_normals = new float[animations.length][][];
        expandAnimation(animations, tmp_vertices, tmp_normals, sprite_info.getVertices(), sprite_info.getNormals(), sprite_info.getSkinNames(), sprite_info.getSkinWeights(), bounds);

        clear_color = sprite_info.getClearColor();

        buffer_indices = new int[tmp_vertices.length];
        for (int j = 0; j < tmp_vertices.length; j++) {
            buffer_indices[j] = all_vertices_and_normals.position();
            for (int i = 0; i < tmp_vertices[j].length; i++) {
                all_vertices_and_normals.put(tmp_vertices[j][i]);
                all_vertices_and_normals.put(tmp_normals[j][i]);
            }
        }

        int color_format = alpha ? Globals.COMPRESSED_RGBA_FORMAT : Globals.COMPRESSED_RGB_FORMAT;

        String[][] texture_names = sprite_info.getTextures();
        textures = new Texture[texture_names.length][3];
        for (int i = 0; i < texture_names.length; i++) {
            Texture[] diffuseAndBump = getTextureForName(texture_names[i][0], color_format, mipmap_cutoff, max_alpha);
            textures[i][TEXTURE_NORMAL] = diffuseAndBump[0];
            if (diffuseAndBump.length > 1) {
                textures[i][TEXTURE_BUMP] = diffuseAndBump[1];
            }
            
            textures[i][TEXTURE_TEAM] = texture_names[i][TEXTURE_TEAM] != null
                    ? getTextureForName(texture_names[i][1], Globals.COMPRESSED_RGB_FORMAT, mipmap_cutoff, max_alpha)[0]
                    : null;
        }
        this.respond_texture = Resources.findResource(new GeneratorRespond())[0];
    }

    public boolean modulateColor() {
        return modulate_color;
    }

    public int getTriangleCount() {
        return num_triangles;
    }

    private void expandAnimation(AnimationInfo @NonNull [] animations, float[][][] tmp_vertices, float[][][] tmp_normals, float[] initial_pose_vertices, float[] initial_pose_normals, byte[][] skin_names, float[][] skin_weights, BoundingBox[] bounding_boxes) {
        int num_bones = animations[0].getFrames()[0].length / 12;
        Matrix4f[] frame_bones = new Matrix4f[num_bones];
        for (int bone = 0; bone < frame_bones.length; bone++) {
            frame_bones[bone] = new Matrix4f();
        }
        Vector4f v = new Vector4f();
        Vector4f n = new Vector4f();
        Vector4f temp = new Vector4f();
        
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            FloatBuffer matrix_buffer = stack.mallocFloat(16);
            matrix_buffer.put(15, 1f);
            for (int anim = 0; anim < animations.length; anim++) {
                BoundingBox bounding_box = bounding_boxes[anim];
                int num_frames = animations[anim].getFrames().length;
                tmp_vertices[anim] = new float[num_frames][num_vertices * 3];
                tmp_normals[anim] = new float[num_frames][num_vertices * 3];
                for (int frame = 0; frame < num_frames; frame++) {
                    float[] frame_animation = animations[anim].getFrames()[frame];
                    for (int bone = 0; bone < num_bones; bone++) {
                        int offset = bone * 12;
                        frame_bones[bone].set(
                            frame_animation[offset + 0], frame_animation[offset + 4], frame_animation[offset + 8], 0.0f,
                            frame_animation[offset + 1], frame_animation[offset + 5], frame_animation[offset + 9], 0.0f,
                            frame_animation[offset + 2], frame_animation[offset + 6], frame_animation[offset + 10], 0.0f,
                            frame_animation[offset + 3], frame_animation[offset + 7], frame_animation[offset + 11], 1.0f
                        );
                    }
                    float[] frame_normals = tmp_normals[anim][frame];
                    float[] frame_vertices = tmp_vertices[anim][frame];
                    float bmax_x = Float.NEGATIVE_INFINITY;
                    float bmax_y = Float.NEGATIVE_INFINITY;
                    float bmax_z = Float.NEGATIVE_INFINITY;
                    float bmin_x = Float.POSITIVE_INFINITY;
                    float bmin_y = Float.POSITIVE_INFINITY;
                    float bmin_z = Float.POSITIVE_INFINITY;
                    for (int vertex = 0; vertex < num_vertices; vertex++) {
                        float x = initial_pose_vertices[vertex * 3 + 0];
                        float y = initial_pose_vertices[vertex * 3 + 1];
                        float z = initial_pose_vertices[vertex * 3 + 2];
                        float nx = initial_pose_normals[vertex * 3 + 0];
                        float ny = initial_pose_normals[vertex * 3 + 1];
                        float nz = initial_pose_normals[vertex * 3 + 2];
                        float result_x = 0, result_y = 0, result_z = 0, result_nx = 0, result_ny = 0, result_nz = 0;
                        v.set(x, y, z, 1);
                        n.set(nx, ny, nz, 0);
                        byte[] vertex_skin_names = skin_names[vertex];
                        float[] vertex_skin_weights = skin_weights[vertex];
                        for (int bone = 0; bone < vertex_skin_names.length; bone++) {
                            float weight = vertex_skin_weights[bone];
                            Matrix4f bone_matrix = frame_bones[vertex_skin_names[bone]];
                            bone_matrix.transform(v, temp);
                            result_x += temp.x * weight;
                            result_y += temp.y * weight;
                            result_z += temp.z * weight;
                            bone_matrix.transform(n, temp);
                            result_nx += temp.x * weight;
                            result_ny += temp.y * weight;
                            result_nz += temp.z * weight;
                        }
                        float vec_len_inv = 1f / (float) Math.sqrt(result_nx * result_nx + result_ny * result_ny + result_nz * result_nz);
                            result_nx *= vec_len_inv;
                            result_ny *= vec_len_inv;
                            result_nz *= vec_len_inv;
                        frame_normals[vertex * 3 + 0] = result_nx;
                        frame_normals[vertex * 3 + 1] = result_ny;
                        frame_normals[vertex * 3 + 2] = result_nz;

                        if (result_x < bmin_x) bmin_x = result_x;
                        else if (result_x > bmax_x) bmax_x = result_x;
                        if (result_y < bmin_y) bmin_y = result_y;
                        else if (result_y > bmax_y) bmax_y = result_y;
                        if (result_z < bmin_z) bmin_z = result_z;
                        else if (result_z > bmax_z) bmax_z = result_z;
                        frame_vertices[vertex * 3 + 0] = result_x;
                        frame_vertices[vertex * 3 + 1] = result_y;
                        frame_vertices[vertex * 3 + 2] = result_z;
                    }
                    bounding_box.checkBounds(bmin_x, bmax_x, bmin_y, bmax_y, bmin_z, bmax_z);
                }
            }
        }
    }

    private static Texture @NonNull [] getTextureForName(@NonNull String texture_name, int color_format, int mipmap_cutoff, boolean max_alpha) {
        if (texture_name.startsWith(GENERATOR_STRING)) {
            String generator_class_name = texture_name.substring(GENERATOR_STRING.length());
            try {
                Class<?> generator_class = Class.forName(generator_class_name);
                Supplier<Texture[]> descriptor = (Supplier<Texture[]>) generator_class.getDeclaredConstructor().newInstance();
                return Resources.findResource(descriptor);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            int wrapMode = GL11.GL_REPEAT;
            String lowerName = texture_name.toLowerCase();
            if (lowerName.contains("leaf") || lowerName.contains("plant") || lowerName.contains("crown") || lowerName.contains("branch") || lowerName.contains("foliage") || lowerName.contains("bush")) {
                wrapMode = GL12.GL_CLAMP_TO_EDGE;
            }
            return new Texture[] { Resources.findResource(new TextureFile("/textures/models/" + texture_name, color_format, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, wrapMode, wrapMode, mipmap_cutoff, 100000, 0.1f, max_alpha)) };
        }
    }

    public boolean hasTeamDecal() {
        return textures.length > 0 && textures[0].length > TEXTURE_TEAM && textures[0][TEXTURE_TEAM] != null;
    }
    
    public boolean hasBumpMap(int tex_index) {
        return textures.length > tex_index && textures[tex_index].length > TEXTURE_BUMP && textures[tex_index][TEXTURE_BUMP] != null;
    }

    public int getNumTextures() {
        return textures.length;
    }

    public void setupShaderUniforms(@NonNull RenderContext context, @NonNull SpriteShader shader, int tex_index, boolean respond) {
        context.setTexture(0, textures[tex_index][TEXTURE_NORMAL]);
        shader.setUniform(SpriteShader.Uniforms.TEXTURE_0, 0);

        boolean useLighting = Globals.draw_light && lighted;
        shader.setUniform(SpriteShader.Uniforms.ENABLE_LIGHTING, useLighting);
        shader.setUniform(SpriteShader.Uniforms.REPLACE_MODE, !useLighting && !modulate_color);

        if (modulate_color) {
            shader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, true);
            shader.setUniform(SpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
            shader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.0f);
        } else {
            shader.setUniform(SpriteShader.Uniforms.MODULATE_COLOR, false);
            shader.setUniform(SpriteShader.Uniforms.ALPHA_TEST_VALUE, 0.3f);
            if (hasTeamDecal() || respond) {
                shader.setUniform(SpriteShader.Uniforms.ENABLE_TEAM_COLOR, true);
                context.setTexture(1, respond ? respond_texture : textures[tex_index][TEXTURE_TEAM]);
                shader.setUniform(SpriteShader.Uniforms.TEXTURE_1, 1);
            } else {
                shader.setUniform(SpriteShader.Uniforms.ENABLE_TEAM_COLOR, false);
            }
        }
        
        if (hasBumpMap(tex_index)) {
            shader.setUniform(SpriteShader.Uniforms.ENABLE_NORMAL_MAP, true);
            context.setTexture(2, textures[tex_index][TEXTURE_BUMP]);
            shader.setUniform(SpriteShader.Uniforms.NORMAL_MAP, 2);
        } else {
            shader.setUniform(SpriteShader.Uniforms.ENABLE_NORMAL_MAP, false);
        }
    }

    public void renderShader(@NonNull SpriteShader shader, int animation, float anim_ticks, @NonNull SpriteList sprite_list) {
        VertexArray vao = sprite_list.getVAO();
        if (vao == null) {
            sprite_list.initVAO(shader);
            vao = sprite_list.getVAO();
        }

        int texCoordLoc = shader.getAttributeLocation(SpriteShader.Attributes.TEX_COORD);
        int posLoc = shader.getAttributeLocation(SpriteShader.Attributes.POSITION);
        int normLoc = shader.getAttributeLocation(SpriteShader.Attributes.NORMAL);

        vao.bind();

        try {
            if (texCoordLoc >= 0) {
                sprite_list.getTexcoords().vertexAttribPointer(texCoordLoc, 2, 0, texcoords_offset * 4L);
            }

            int vertex_index = getVertexOffset(animation, anim_ticks);
            int normal_index = getNormalOffset(vertex_index);

            if (posLoc >= 0) {
                sprite_list.getVerticesAndNormals().vertexAttribPointer(posLoc, 3, 0, vertex_index * 4L);
            }

            if (normLoc >= 0) {
                sprite_list.getVerticesAndNormals().vertexAttribPointer(normLoc, 3, 0, normal_index * 4L);
            }

            sprite_list.getIndices().drawElements(GL11.GL_TRIANGLES, num_triangles * 3, indices_offset);
        } finally {
            vao.unbind();
        }
    }

    public int getNumVertices() {
        return num_vertices;
    }

    public record FrameState(int pos1, int norm1, int pos2, int norm2, float tween) {}

    public @NonNull FrameState getAnimationState(int animation, float anim_ticks) {
        if (cpw_array == null) {
            // Static mesh (quad or non-animated)
            int offset = buffer_indices != null && buffer_indices.length > animation ? buffer_indices[animation] / 3 : 0;
            // For quad, layout is [Pos][Norm]. N=4.
            // Vertices 12 floats (4*3). Normals 12 floats.
            // PosOffset = 0. NormOffset = 4.
            return new FrameState(offset, offset + num_vertices, offset, offset + num_vertices, 0f);
        }

        float anim_position = anim_ticks * cpw_array[animation];
        int len = animation_length_array[animation];
        float exactFrame = anim_position * len;
        
        int frame1 = (int) exactFrame;
        int frame2 = frame1 + 1;
        float tween = exactFrame - frame1;
        
        if (type_array[animation] == AnimationInfo.AnimationType.LOOP) {
            frame1 %= len;
            frame2 %= len;
        } else {
            frame1 = Math.min(frame1, len - 1);
            frame2 = Math.min(frame2, len - 1);
        }
        
        // Calculate offsets in Texels (floats / 3)
        // Layout: [Pos (3N)] [Norm (3N)] per frame.
        // Frame stride: 6N floats.
        // Base: buffer_indices[animation] (floats)
        
        int baseTexelOffset = buffer_indices[animation] / 3;
        int frameStrideTexels = num_vertices * 2; // 6N floats / 3 = 2N texels
        
        int pos1 = baseTexelOffset + frame1 * frameStrideTexels;
        int norm1 = pos1 + num_vertices;
        
        int pos2 = baseTexelOffset + frame2 * frameStrideTexels;
        int norm2 = pos2 + num_vertices;
        
        return new FrameState(pos1, norm1, pos2, norm2, tween);
    }

    public int getVertexOffset(int animation, float anim_ticks) {
        if (cpw_array == null) return 0; // For quad instance
        float anim_position = anim_ticks * cpw_array[animation];
        int frame_non_capped = (int) (anim_position * animation_length_array[animation]);
        int frame = getFrameCapped(animation, frame_non_capped);
        int frame_size = num_vertices * 3;
        int frame_index = frame * 2;
        return buffer_indices[animation] + frame_index * frame_size;
    }

    public int getNormalOffset(int vertex_offset) {
        return vertex_offset + num_vertices * 3;
    }

    private int getFrameCapped(int animation, int frame) {
        int anim_length = animation_length_array[animation];
        if (type_array[animation] == AnimationInfo.AnimationType.LOOP)
            return frame % anim_length;
        else
            return Math.min(frame, anim_length - 1);
    }

    public float[] getClearColor() {
        return clear_color;
    }
}
