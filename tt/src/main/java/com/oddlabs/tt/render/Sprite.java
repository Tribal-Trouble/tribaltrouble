package com.oddlabs.tt.render;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.procedural.GeneratorRespond;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Supplier;

final class Sprite {
	private static final int TEXTURE_NORMAL = 0;
	private static final int TEXTURE_TEAM = 1;
	private static final FloatBuffer decal_color = BufferUtils.createFloatBuffer(4);
    private static final String GENERATOR_STRING = "Generator:";

	public static int global_size = 0;
	private static final FloatBuffer white_color;

	private final Texture[] @NonNull [] textures;
	private final int num_triangles;
	private final int num_vertices;
	private final float[] clear_color;
	private final int @NonNull [] buffer_indices;
	private final boolean alpha;
	private final boolean lighted;
	private final boolean culled;
	private final boolean modulate_color;
	private final float[] cpw_array;
	private final int[] animation_length_array;
	private final int[] type_array;
	private final Texture respond_texture;
	private final int indices_offset;
	private final int texcoords_offset;

	static {
		white_color = BufferUtils.createFloatBuffer(4).put(Color.argb4f(0xFF_FF_FF_FF));
		white_color.rewind();
	}

	public Sprite(@NonNull SpriteInfo sprite_info, AnimationInfo @NonNull [] animations, boolean alpha, boolean lighted, boolean culled, boolean modulate_color, boolean max_alpha, int mipmap_cutoff, BoundingBox[] bounds, float[] cpw_array, int[] type_array, int[] animation_length_array, @NonNull ShortBuffer all_indices, @NonNull FloatBuffer all_texcoords, @NonNull FloatBuffer all_vertices_and_normals) {
		this.culled = culled;
		this.alpha = alpha;
		this.lighted = lighted;
		this.modulate_color = modulate_color;
		this.cpw_array = cpw_array;
		this.type_array = type_array;
		this.animation_length_array = animation_length_array;

		short[] tmp_indices = sprite_info.getIndices();
		float[] tmp_texcoords = sprite_info.getTexCoords();
		num_vertices = tmp_texcoords.length/2;
		num_triangles = tmp_indices.length/3;

		indices_offset = all_indices.position();
		texcoords_offset = all_texcoords.position();

		all_indices.put(tmp_indices);
		all_texcoords.put(tmp_texcoords);

		// Expand animations
		float[][][] tmp_vertices = new float[animations.length][][];
		float[][][] tmp_normals = new float[animations.length][][];
		expandAnimation(animations, tmp_vertices, tmp_normals, sprite_info.getVertices(), sprite_info.getNormals(), sprite_info.getSkinNames(), sprite_info.getSkinWeights(), bounds);

		clear_color = sprite_info.getClearColor();

		int frame_size = num_vertices*3*2;
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
		textures = new Texture[texture_names.length][2];
		for (int i = 0; i < texture_names.length; i++) {
			textures[i][TEXTURE_NORMAL] = getTextureForName(texture_names[i][0], color_format, mipmap_cutoff, max_alpha);
            textures[i][TEXTURE_TEAM] = texture_names[i][TEXTURE_TEAM] != null
                    ? getTextureForName(texture_names[i][1], Globals.COMPRESSED_RGB_FORMAT, mipmap_cutoff, max_alpha)
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

	static void setupDecalColor(float @NonNull [] color) {
		decal_color.put(color).rewind();
		GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, decal_color);
	}

	private void expandAnimation(AnimationInfo @NonNull [] animations, float[][][] tmp_vertices, float[][][] tmp_normals, float[] initial_pose_vertices, float[] initial_pose_normals, byte[][] skin_names, float[][] skin_weights, BoundingBox[] bounding_boxes) {
		int num_bones = animations[0].getFrames()[0].length/12;
		Matrix4f[] frame_bones = new Matrix4f[num_bones];
		for (int bone = 0; bone < frame_bones.length; bone++) {
            frame_bones[bone] = new Matrix4f();
        }
		Vector4f v = new Vector4f();
		Vector4f n = new Vector4f();
		Vector4f temp = new Vector4f();
		FloatBuffer matrix_buffer = FloatBuffer.allocate(16);
		matrix_buffer.put(15, 1f);
		for (int anim = 0; anim < animations.length; anim++) {
			BoundingBox bounding_box = bounding_boxes[anim];
			int num_frames = animations[anim].getFrames().length;
			tmp_vertices[anim] = new float[num_frames][num_vertices*3];
			tmp_normals[anim] = new float[num_frames][num_vertices*3];
			for (int frame = 0; frame < num_frames; frame++) {
				float[] frame_animation = animations[anim].getFrames()[frame];
				for (int bone = 0; bone < num_bones; bone++) {
					matrix_buffer.clear();
					matrix_buffer.put(frame_animation, bone*12, 12);
					matrix_buffer.rewind();
					frame_bones[bone].setTransposed(matrix_buffer);
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
					float x = initial_pose_vertices[vertex*3 + 0];
					float y = initial_pose_vertices[vertex*3 + 1];
					float z = initial_pose_vertices[vertex*3 + 2];
					float nx = initial_pose_normals[vertex*3 + 0];
					float ny = initial_pose_normals[vertex*3 + 1];
					float nz = initial_pose_normals[vertex*3 + 2];
					float result_x = 0, result_y = 0, result_z = 0, result_nx = 0, result_ny = 0, result_nz = 0;
					v.set(x, y, z, 1);
					n.set(nx, ny, nz, 0);
					byte[] vertex_skin_names = skin_names[vertex];
					float[] vertex_skin_weights = skin_weights[vertex];
					for (int bone = 0; bone < vertex_skin_names.length; bone++) {
						float weight = vertex_skin_weights[bone];
						Matrix4f bone_matrix = frame_bones[vertex_skin_names[bone]];
						bone_matrix.transform(v, temp);
						result_x += temp.x*weight;
						result_y += temp.y*weight;
						result_z += temp.z*weight;
						// Assume matrix is only translation and scaling
						bone_matrix.transform(n, temp);
						result_nx += temp.x*weight;
						result_ny += temp.y*weight;
						result_nz += temp.z*weight;
					}
					// Use Math.sqrt here for efficiency. Only used for normals (not gamestate affecting) anyway.
					float vec_len_inv = 1f/(float)Math.sqrt(result_nx*result_nx + result_ny*result_ny + result_nz*result_nz);
					result_nx *= vec_len_inv;
					result_ny *= vec_len_inv;
					result_nz *= vec_len_inv;
					frame_normals[vertex*3 + 0] = result_nx;
					frame_normals[vertex*3 + 1] = result_ny;
					frame_normals[vertex*3 + 2] = result_nz;

//result_x = x; result_y = y; result_z = z;
					if (result_x < bmin_x)
						bmin_x = result_x;
					else if (result_x > bmax_x)
						bmax_x = result_x;
					if (result_y < bmin_y)
						bmin_y = result_y;
					else if (result_y > bmax_y)
						bmax_y = result_y;
					if (result_z < bmin_z)
						bmin_z = result_z;
					else if (result_z > bmax_z)
						bmax_z = result_z;
					frame_vertices[vertex*3 + 0] = result_x;
					frame_vertices[vertex*3 + 1] = result_y;
					frame_vertices[vertex*3 + 2] = result_z;
				}
				bounding_box.checkBounds(bmin_x, bmax_x, bmin_y, bmax_y, bmin_z, bmax_z);
			}
		}
	}

	private static Texture getTextureForName(@NonNull String texture_name, int color_format, int mipmap_cutoff, boolean max_alpha) {
		if (texture_name.startsWith(GENERATOR_STRING)) {
			String generator_class_name = texture_name.substring(GENERATOR_STRING.length());
			try {
				Class<?> generator_class = Class.forName(generator_class_name);
				Supplier<Texture[]> descriptor = (Supplier<Texture[]>) generator_class.getDeclaredConstructor().newInstance();
				return Resources.findResource(descriptor)[0];
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		} else {
			return Resources.findResource(new TextureFile("/textures/models/" + texture_name, color_format, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT, mipmap_cutoff, 100000, 0.1f, max_alpha));
		}
	}

	public void setup(int tex_index, boolean respond, @NonNull SpriteList sprite_list) {
		setupWithColor(white_color, tex_index, respond, modulate_color, sprite_list);
	}

	public void setupWithColor(@NonNull FloatBuffer color, int tex_index, boolean respond, boolean modulate_color, @NonNull SpriteList sprite_list) {
		doSetup(color, tex_index, respond, modulate_color, sprite_list);
	}

	private void doSetup(@NonNull FloatBuffer color, int tex_index, boolean respond, boolean modulate_color, @NonNull SpriteList sprite_list) {
		int gl_flags = setupBasic(sprite_list);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[tex_index][TEXTURE_NORMAL].getHandle());
		if (modulate_color) {
			GL11.glAlphaFunc(GL11.GL_GREATER, 0f);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
		} else if (Globals.draw_light && lighted) {
			gl_flags = gl_flags | GLState.NORMAL_ARRAY;
			GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, color);
		}
		GL11.glColor4f(color.get(0), color.get(1), color.get(2), color.get(3));
		if (!modulate_color && (hasTeamDecal() || respond)) {
			gl_flags = gl_flags | GLState.TEXCOORD1_ARRAY;
			setupTeamDecal();
			if (respond) {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, respond_texture.getHandle());
			} else {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[tex_index][TEXTURE_TEAM].getHandle());
			}
			GLState.clientActiveTexture(GL13.GL_TEXTURE1);
			sprite_list.getTexcoords().texCoordPointer(2, 0, texcoords_offset);
			GLState.clientActiveTexture(GL13.GL_TEXTURE0);
		}
		GLStateStack.switchState(gl_flags);
	}

	private int setupBasic(@NonNull SpriteList sprite_list) {
		int gl_flags = GLState.VERTEX_ARRAY | GLState.TEXCOORD0_ARRAY;
		if (!culled) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
		if (alpha) {
			GL11.glEnable(GL11.GL_ALPHA_TEST);
			GL11.glAlphaFunc(GL11.GL_GREATER, .3f);
		}
		sprite_list.getTexcoords().texCoordPointer(2, 0, texcoords_offset);
		return gl_flags;
	}

	public boolean hasTeamDecal() {
		return textures[0][TEXTURE_TEAM] != null;
	}

	public int getNumTextures() {
		return textures.length;
	}

	public static void setupTeamDecal() {
		GLState.activeTexture(GL13.GL_TEXTURE1);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_BLEND);
	}

	public static void resetTeamDecal() {
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_DECAL);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GLState.activeTexture(GL13.GL_TEXTURE0);
	}

	public void reset(boolean respond, boolean modulate_color) {
		if (!modulate_color && (hasTeamDecal() || respond)) {
			resetTeamDecal();
		}
		if (modulate_color) {
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
		} else if (Globals.draw_light && lighted) {
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
		}
		resetBasic();
	}

	private void resetBasic() {
		if (!culled) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
		if (alpha) {
			GL11.glDisable(GL11.GL_ALPHA_TEST);
		}
	}

	private int getFrameCapped(int animation, int frame) {
		int anim_length = animation_length_array[animation];
		if (type_array[animation] == AnimationInfo.ANIM_LOOP)
			return frame%anim_length;
		else
			return Math.min(frame, anim_length - 1);
	}

	public void render(int animation, float anim_ticks, @NonNull SpriteList sprite_list) {
		float anim_position = anim_ticks*cpw_array[animation];
		int frame_non_capped = (int)(anim_position*animation_length_array[animation]);
		int frame = getFrameCapped(animation, frame_non_capped);
		int frame_size = num_vertices*3;
		int frame_index = frame*2;
		int vertex_index = buffer_indices[animation] + frame_index*frame_size;
		int normal_index = vertex_index + frame_size;

		sprite_list.getVerticesAndNormals().normalPointer(0, normal_index);
		sprite_list.getVerticesAndNormals().vertexPointer(3, 0, vertex_index);
		sprite_list.getIndices().drawElements(GL11.GL_TRIANGLES, num_triangles*3, indices_offset);
	}

	public void renderModel(int tex_index, @NonNull SpriteList sprite_list) {
		int gl_flags = setupBasic(sprite_list);
		GLStateStack.switchState(gl_flags);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures[tex_index][TEXTURE_NORMAL].getHandle());
		render(0, 0, sprite_list); // Render 1st frame of 1st animation to low detail texture
		resetBasic();
	}

	public float[] getClearColor() {
		return clear_color;
	}
}
