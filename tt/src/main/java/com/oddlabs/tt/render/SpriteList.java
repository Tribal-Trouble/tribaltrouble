package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.ARBBufferObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public final class SpriteList {
	private final BoundingBox @NonNull [] bounds;
	private final Sprite @NonNull [] sprites;
	private final int @NonNull [] type_array;

	private final @NonNull ShortVBO indices;
	private final @NonNull FloatVBO vertices_and_normals;
	private final @NonNull FloatVBO texcoords;

	public SpriteList(@NonNull SpriteFile sprite_file) {
		Object[] sprites_and_animations = Utils.loadObject(sprite_file.getURL());
		SpriteInfo[] sprite_infos = (SpriteInfo[])sprites_and_animations[0];
		AnimationInfo[] animation_infos = (AnimationInfo[])sprites_and_animations[1];
		bounds = new BoundingBox[animation_infos.length];
		for (int i = 0; i < bounds.length; i++) {
            bounds[i] = new BoundingBox();
        }

		int total_indices = 0;
		int total_vertices = 0;
		for (SpriteInfo sprite_info : sprite_infos) {
			total_indices += sprite_info.getIndices().length;
			total_vertices += sprite_info.getTexCoords().length / 2;
		}

		ShortBuffer all_indices = ByteBuffer.allocateDirect(total_indices * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
		FloatBuffer all_texcoords = ByteBuffer.allocateDirect(total_vertices * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		int vert_and_normal_buffer_size = 0;
		for (int i = 0; i < sprite_infos.length; i++) {
			SpriteInfo sprite_info = sprite_infos[i];
			int num_vertices = sprite_info.getTexCoords().length / 2;
			int frame_size = num_vertices * 3 * 2;
			for (int j = 0; j < animation_infos.length; j++) {
				int num_frames = animation_infos[j].getFrames().length;
				vert_and_normal_buffer_size += num_frames * frame_size;
			}
		}

		FloatBuffer all_vertices_and_normals = ByteBuffer.allocateDirect(vert_and_normal_buffer_size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		sprites = new Sprite[sprite_infos.length];
		float[] cpw_array = new float[animation_infos.length];
		type_array = new int[animation_infos.length];
		int[] animation_length_array = new int[animation_infos.length];
		for (int i = 0; i < animation_infos.length; i++) {
			cpw_array[i] = 1f/animation_infos[i].getWPC();
			type_array[i] = animation_infos[i].getType();
			animation_length_array[i] = animation_infos[i].getFrames().length;
		}
		for (int i = 0; i < sprites.length; i++) {
            sprites[i] = new Sprite(sprite_infos[i], animation_infos, sprite_file.hasAlpha(), sprite_file.isLighted(), sprite_file.isCulled(), sprite_file.hasModulateColor(), sprite_file.hasMaxAlpha(), sprite_file.getMipmapCutoff(), bounds, cpw_array, type_array, animation_length_array, all_indices, all_texcoords, all_vertices_and_normals);
        }

		all_indices.flip();
		indices = new ShortVBO(ARBBufferObject.GL_STATIC_DRAW_ARB, all_indices.remaining());
		indices.put(all_indices);

		all_texcoords.flip();
		texcoords = new FloatVBO(ARBBufferObject.GL_STATIC_DRAW_ARB, all_texcoords.remaining());
		texcoords.put(all_texcoords);

		all_vertices_and_normals.flip();
		vertices_and_normals = new FloatVBO(ARBBufferObject.GL_STATIC_DRAW_ARB, all_vertices_and_normals.remaining());
		vertices_and_normals.put(all_vertices_and_normals);

            for (BoundingBox bound : bounds) {
                bound.maximizeXYPlane();
            }
	}

	public void setup(int tex_index, boolean respond) {
		getSprite(0).setup(tex_index, respond, this);
	}

	public void reset(int index, boolean respond, boolean modulate_tex1) {
		getSprite(index).reset(respond, modulate_tex1);
	}

	public void render(int index, int animation, float anim_ticks) {
		getSprite(index).render(animation, anim_ticks, this);
	}

	public float[] getClearColor() {
		return getSprite(0).getClearColor();
	}

	public void renderModel(int tex_index) {
		getSprite(0).renderModel(tex_index, this);
	}

	public BoundingBox @NonNull [] getBounds() {
		return bounds;
	}

	public int getNumSprites() {
		return sprites.length;
	}

	Sprite getSprite(int index) {
		return sprites[index];
	}

	public int[] getAnimationTypes() {
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
}
