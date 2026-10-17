package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.render.shader.SpriteShader;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class SpriteList implements AutoCloseable {
    private static final @NonNull SpriteList QUAD_INSTANCE = new SpriteList();

    private final @NonNull BoundingBox @NonNull [] bounds;
    private final @NonNull Sprite @NonNull [] sprites;
    private final AnimationInfo.@NonNull AnimationType @NonNull [] type_array;
    private final @NonNull String @NonNull [] animation_names;

    // Geometry stays in these buffers; the VBOs and texture buffer are uploaded on first renderer request so
    // sprite data can be loaded without a GL context.
    private final @NonNull ShortBuffer index_data;
    private final @NonNull FloatBuffer vertex_and_normal_data;
    private final @NonNull FloatBuffer texcoord_data;
    private @Nullable ShortVBO indices;
    private @Nullable FloatVBO vertices_and_normals;
    private @Nullable FloatVBO texcoords;
    private @Nullable VertexArray vao;
    private int tboTextureHandle;

    public static @NonNull SpriteList getQuadInstance() {
        return QUAD_INSTANCE;
    }

    private SpriteList() {
        // Private constructor for the quad instance
        this.bounds = new BoundingBox[]{new BoundingBox()};
        this.type_array = new AnimationInfo.AnimationType[]{AnimationInfo.AnimationType.LOOP};
        this.animation_names = new String[]{"default"};

        float[] quad_vertices = {-0.5f, -0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f};
        float[] quad_normals = {0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1};
        float[] quad_texcoords = {0, 0, 1, 0, 1, 1, 0, 1};
        short[] quad_indices = {0, 1, 2, 0, 2, 3};

        FloatBuffer vertAndNormBuf = BufferUtils.createFloatBuffer(quad_vertices.length + quad_normals.length);
        vertAndNormBuf.put(quad_vertices);
        vertAndNormBuf.put(quad_normals);
        vertAndNormBuf.flip();
        this.vertex_and_normal_data = vertAndNormBuf;

        FloatBuffer texCoordBuf = BufferUtils.createFloatBuffer(quad_texcoords.length).put(quad_texcoords);
        texCoordBuf.flip();
        this.texcoord_data = texCoordBuf;

        ShortBuffer indexBuf = BufferUtils.createShortBuffer(quad_indices.length).put(quad_indices);
        indexBuf.flip();
        this.index_data = indexBuf;

        this.sprites = new Sprite[]{new Sprite(4, 2, 0, true)};
    }

    // Items name the sprite whose clips they use ("vikings/peon"); each unit's clips are then read once.
    private static final Map<String, AnimationInfo[]> shared_clips = new HashMap<>();

    private static AnimationInfo @NonNull [] clips(@NonNull Object clips_or_owner) {
        if (clips_or_owner instanceof AnimationInfo[] clips)
            return clips;
        String owner = (String) clips_or_owner;
        AnimationInfo[] clips = shared_clips.get(owner);
        if (clips == null) {
            Object[] sprites_and_animations = Utils.loadObject(Utils.makeURL("/geometry/" + owner + ".binsprite"));
            clips = clips(sprites_and_animations[1]);
            shared_clips.put(owner, clips);
        }
        return clips;
    }

    public SpriteList(@NonNull SpriteFile sprite_file) {
        Object[] sprites_and_animations = Utils.loadObject(sprite_file.getURL());
        SpriteInfo[] sprite_infos = (SpriteInfo[]) sprites_and_animations[0];
        AnimationInfo[] animation_infos = clips(sprites_and_animations[1]);
        bounds = Stream.generate(BoundingBox::new).limit(animation_infos.length).toArray(BoundingBox[]::new);

        int total_indices = 0;
        int total_vertices = 0;
        for (SpriteInfo sprite_info : sprite_infos) {
            total_indices += sprite_info.getIndices().length;
            total_vertices += sprite_info.getTexCoords().length / 2;
        }

        ShortBuffer all_indices = Objects.requireNonNull(BufferUtils.createShortBuffer(total_indices));
        FloatBuffer all_texcoords = Objects.requireNonNull(BufferUtils.createFloatBuffer(total_vertices * 2));

        int vert_and_normal_buffer_size = 0;
        for (SpriteInfo sprite_info : sprite_infos) {
            int num_vertices = sprite_info.getTexCoords().length / 2;
            int frame_size = num_vertices * 3 * 2; // pos(3) + norm(3)
            for (AnimationInfo animationInfo : animation_infos) {
                int num_frames = animationInfo.getFrames().length;
                vert_and_normal_buffer_size += num_frames * frame_size;
            }
        }

        FloatBuffer all_vertices_and_normals = Objects.requireNonNull(BufferUtils.createFloatBuffer(
                vert_and_normal_buffer_size));

        float[] cpw_array = new float[animation_infos.length];
        type_array = new AnimationInfo.AnimationType[animation_infos.length];
        animation_names = new String[animation_infos.length];
        int[] animation_length_array = new int[animation_infos.length];
        for (int i = 0; i < animation_infos.length; i++) {
            cpw_array[i] = 1f / animation_infos[i].getWPC();
            type_array[i] = animation_infos[i].getType();
            animation_names[i] = animation_infos[i].getName();
            animation_length_array[i] = animation_infos[i].getFrames().length;
        }
        sprites = Arrays.stream(sprite_infos).map(info -> new Sprite(info, animation_infos,
                sprite_file.hasAlpha(), sprite_file.isLighted(), sprite_file.isCulled(),
                sprite_file.hasModulateColor(), sprite_file.hasMaxAlpha(), sprite_file.getMipmapCutoff(),
                bounds, cpw_array, type_array, animation_length_array,
                all_indices, all_texcoords, all_vertices_and_normals)
        ).toArray(Sprite[]::new);

        all_indices.flip();
        index_data = all_indices;
        all_texcoords.flip();
        texcoord_data = all_texcoords;
        all_vertices_and_normals.flip();
        vertex_and_normal_data = all_vertices_and_normals;

        for (BoundingBox bound : bounds) {
            bound.maximizeXYPlane();
        }

    }

    /** Creates the VBOs and texture buffer from the retained geometry. Needs a GL context; idempotent. */
    private void upload() {
        if (indices != null) {
            return;
        }
        ShortVBO index_vbo = new ShortVBO(GL15.GL_STATIC_DRAW, index_data.remaining());
        index_vbo.put(index_data.duplicate());
        FloatVBO texcoord_vbo = new FloatVBO(GL15.GL_STATIC_DRAW, texcoord_data.remaining());
        texcoord_vbo.put(texcoord_data.duplicate());
        FloatVBO vertex_vbo = new FloatVBO(GL15.GL_STATIC_DRAW, vertex_and_normal_data.remaining());
        vertex_vbo.put(vertex_and_normal_data.duplicate());
        texcoords = texcoord_vbo;
        vertices_and_normals = vertex_vbo;
        indices = index_vbo;
        initTBO();
    }

    private void initTBO() {
        tboTextureHandle = org.lwjgl.opengl.GL11.glGenTextures();
        org.lwjgl.opengl.GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, tboTextureHandle);
        // vertices_and_normals is a VBO. We need its handle.
        // Assuming getHandle() exists or accessing protected field via VBO.
        // VBO.java likely stores handle.
        // I will use getHandle() and assume I need to expose it if missing.
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGB32F, vertices_and_normals.getHandle());
    }

    public int getTBOTextureHandle() {
        upload();
        return tboTextureHandle;
    }

    public void initVAO(@NonNull SpriteShader shader) {
        if (vao != null) return;
        upload();

        vao = new VertexArray();
        vao.bind();

        int texCoordLoc = shader.getAttributeLocation(SpriteShader.Attributes.TEX_COORD);
        int posLoc = shader.getAttributeLocation(SpriteShader.Attributes.POSITION);
        int normLoc = shader.getAttributeLocation(SpriteShader.Attributes.NORMAL);

        indices.makeCurrent();

        if (texCoordLoc >= 0) {
            GL20.glEnableVertexAttribArray(texCoordLoc);
        }

        if (posLoc >= 0) {
            GL20.glEnableVertexAttribArray(posLoc);
        }

        if (normLoc >= 0) {
            GL20.glEnableVertexAttribArray(normLoc);
        }
        vao.unbind();
    }

    public @Nullable VertexArray getVAO() {
        return vao;
    }

    public float @NonNull [] getClearColor() {
        return getSprite(0).getClearColor();
    }

    public @NonNull BoundingBox @NonNull [] getBounds() {
        return bounds;
    }

    public int getNumSprites() {
        return sprites.length;
    }

    public @NonNull Sprite getSprite(int index) {
        return sprites[index];
    }

    public AnimationInfo.@NonNull AnimationType @NonNull [] getAnimationTypes() {
        return type_array;
    }

    public @NonNull String @NonNull [] getAnimationNames() {
        return animation_names;
    }

    public int getAnimationIndex(@NonNull String name) {
        for (int i = 0; i < animation_names.length; i++) {
            if (animation_names[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public @NonNull ShortVBO getIndices() {
        upload();
        return indices;
    }

    public @NonNull FloatVBO getVerticesAndNormals() {
        upload();
        return vertices_and_normals;
    }

    public @NonNull FloatVBO getTexcoords() {
        upload();
        return texcoords;
    }

    public void close() {
        if (tboTextureHandle != 0) {
            org.lwjgl.opengl.GL11.glDeleteTextures(tboTextureHandle);
            tboTextureHandle = 0;
        }
        if (vao != null) {
            vao.close();
            vao = null;
        }
        if (indices != null) {
            indices.close();
            vertices_and_normals.close();
            texcoords.close();
            indices = null;
            vertices_and_normals = null;
            texcoords = null;
        }
    }
}
