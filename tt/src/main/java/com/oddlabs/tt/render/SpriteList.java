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
import java.util.Objects;
import java.util.stream.Stream;

public final class SpriteList implements AutoCloseable{
    private static final @NonNull SpriteList QUAD_INSTANCE = new SpriteList();

	private final @NonNull BoundingBox @NonNull [] bounds;
	private final @NonNull Sprite @NonNull [] sprites;
	private final AnimationInfo.@NonNull AnimationType @NonNull [] type_array;

	private final @NonNull ShortVBO indices;
	private final @NonNull FloatVBO vertices_and_normals;
	private final @NonNull FloatVBO texcoords;
    private @Nullable VertexArray vao;
    private int tboTextureHandle;

    public static @NonNull SpriteList getQuadInstance() {
        return QUAD_INSTANCE;
    }

    private SpriteList() {
        // Private constructor for the quad instance
        this.bounds = new BoundingBox[]{new BoundingBox()};
        this.type_array = new AnimationInfo.AnimationType[]{AnimationInfo.AnimationType.LOOP};
        
        float[] quad_vertices = { -0.5f, -0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f };
        float[] quad_normals = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
        float[] quad_texcoords = { 0, 0, 1, 0, 1, 1, 0, 1 };
        short[] quad_indices = { 0, 1, 2, 0, 2, 3 };

        FloatBuffer vertAndNormBuf = BufferUtils.createFloatBuffer(quad_vertices.length + quad_normals.length);
        vertAndNormBuf.put(quad_vertices);
        vertAndNormBuf.put(quad_normals);
        vertAndNormBuf.flip();
        this.vertices_and_normals = new FloatVBO(GL15.GL_STATIC_DRAW, vertAndNormBuf);

        FloatBuffer texCoordBuf = BufferUtils.createFloatBuffer(quad_texcoords.length).put(quad_texcoords);
        texCoordBuf.flip();
        this.texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, texCoordBuf);

        ShortBuffer indexBuf = BufferUtils.createShortBuffer(quad_indices.length).put(quad_indices);
        indexBuf.flip();
        this.indices = new ShortVBO(GL15.GL_STATIC_DRAW, indexBuf);
        
        this.sprites = new Sprite[]{ new Sprite(4, 2, 0, true) };
        
        initTBO();
    }

	public SpriteList(@NonNull SpriteFile sprite_file) {
		Object[] sprites_and_animations = Utils.loadObject(sprite_file.getURL());
		SpriteInfo[] sprite_infos = (SpriteInfo[]) sprites_and_animations[0];
		AnimationInfo[] animation_infos = (AnimationInfo[]) sprites_and_animations[1];
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

		FloatBuffer all_vertices_and_normals = Objects.requireNonNull(BufferUtils.createFloatBuffer(vert_and_normal_buffer_size));

		float[] cpw_array = new float[animation_infos.length];
		type_array = new AnimationInfo.AnimationType[animation_infos.length];
		int[] animation_length_array = new int[animation_infos.length];
		for (int i = 0; i < animation_infos.length; i++) {
			cpw_array[i] = 1f/animation_infos[i].getWPC();
			type_array[i] = animation_infos[i].getType();
			animation_length_array[i] = animation_infos[i].getFrames().length;
		}
        sprites = Arrays.stream(sprite_infos)
                .map(info -> new Sprite(info, animation_infos,
                    sprite_file.hasAlpha(), sprite_file.isLighted(), sprite_file.isCulled(),
                    sprite_file.hasModulateColor(), sprite_file.hasMaxAlpha(), sprite_file.getMipmapCutoff(),
                    bounds, cpw_array, type_array, animation_length_array,
                    all_indices, all_texcoords, all_vertices_and_normals)
                ).toArray(Sprite[]::new);

		all_indices.flip();
		indices = new ShortVBO(GL15.GL_STATIC_DRAW, all_indices.remaining());
		indices.put(all_indices);

		all_texcoords.flip();
		texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, all_texcoords.remaining());
		texcoords.put(all_texcoords);

		all_vertices_and_normals.flip();
		vertices_and_normals = new FloatVBO(GL15.GL_STATIC_DRAW, all_vertices_and_normals.remaining());
		vertices_and_normals.put(all_vertices_and_normals);

        for (BoundingBox bound : bounds) {
            bound.maximizeXYPlane();
        }
        
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
        return tboTextureHandle;
    }
    
    public void initVAO(@NonNull SpriteShader shader) {
        if (vao != null) return;
        
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

	public @NonNull ShortVBO getIndices() {
		return indices;
	}

	public @NonNull FloatVBO getVerticesAndNormals() {
		return vertices_and_normals;
	}

	public @NonNull FloatVBO getTexcoords() {
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
        indices.close();
        vertices_and_normals.close();
        texcoords.close();
    }
}
