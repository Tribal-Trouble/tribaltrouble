package com.oddlabs.converter;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.util.IndexListOptimizer;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.nio.ShortBuffer;
import java.util.Map;

public final class Optimizer {
	private static final float VERTEX_THRESHOLD = 0.000001f;

	private static boolean shortsEquals(int index1, int index2, int size, short[] array1, short[] array2) {
		for (int i = 0; i < size; i++) {
			if (array1[index1*size + i] != array2[index2*size + i]) {
				return false;
			}
		}
		return true;
	}

	private static boolean floatsEquals(int index1, int index2, int size, float[] array1, float[] array2) {
		for (int i = 0; i < size; i++) {
			if (Math.abs(array1[index1*size + i] - array2[index2*size + i]) > VERTEX_THRESHOLD)
				return false;
		}
		return true;
	}

	private static boolean bytesEquals(int index1, int index2, int size, byte[] array1, byte[] array2) {
		for (int i = 0; i < size; i++) {
			if (array1[index1*size + i] != array2[index2*size + i])
				return false;
		}
		return true;
	}

	private static void copyFloats(int index1, int index2, int size, float @NonNull [] array1, float @NonNull [] array2) {
        if (size >= 0) System.arraycopy(array1, index1 * size, array2, index2 * size, size);
	}

	private static void copyObjects(int index1, int index2, int size, Object @NonNull [] array1, Object @NonNull [] array2) {
        if (size >= 0) System.arraycopy(array1, index1 * size, array2, index2 * size, size);
	}

	private static boolean floatArrayEquals(int index1, int index2, float@NonNull [] @NonNull [] array1, float@NonNull [] @NonNull [] array2) {
		if (array1[index1].length != array2[index2].length) {
			return false;
		}
		for (int i = 0; i < array1[index1].length; i++) {
			if (Math.abs(array1[index1][i] - array2[index2][i]) > VERTEX_THRESHOLD) {
				return false;
			}
		}
		return true;
	}

	private static boolean byteArrayEquals(int index1, int index2, byte@NonNull [] @NonNull [] array1, byte@NonNull [] @NonNull [] array2) {
		if (array1[index1].length != array2[index2].length) {
			return false;
		}
		for (int i = 0; i < array1[index1].length; i++) {
			if (array1[index1][i] != array2[index2][i]) {
				return false;
			}
		}
		return true;
	}

	static @NonNull ModelInfo optimize(/*String tex_name, */int num_vertices, float @NonNull [] vertices, float @NonNull [] normals, float @NonNull [] colors, float @NonNull [] uvs, byte@NonNull [] @NonNull [] skin_names, float@NonNull [] @NonNull [] skin_weights) {
		short[] indices = new short[num_vertices];
		float[] r_vertices = new float[vertices.length];
		float[] r_colors = new float[colors.length];
		float[] r_normals = new float[normals.length];
		float[] r_uvs = new float[uvs.length];
		byte[][] r_skin_names = new byte[skin_names.length][];
		float[][] r_skin_weights = new float[skin_weights.length][];

		int index = 0;

		for (int i = 0; i < num_vertices; i++) {
			int j;
			for (j = 0; j < index; j++) {
				boolean equals = floatsEquals(i, j, 3, vertices, r_vertices) &&
								 floatsEquals(i, j, 3, normals, r_normals) &&
								 floatsEquals(i, j, 3, colors, r_colors) &&
								 floatsEquals(i, j, 2, uvs, r_uvs) &&
								 floatArrayEquals(i, j, skin_weights, r_skin_weights) &&
								 byteArrayEquals(i, j, skin_names, r_skin_names);
				if (equals)
					break;
			}
			if (j == index) {
				copyFloats(i, index, 3, vertices, r_vertices);
				copyFloats(i, index, 3, normals, r_normals);
				copyFloats(i, index, 3, colors, r_colors);
				copyFloats(i, index, 2, uvs, r_uvs);
				copyObjects(i, index, 1, skin_names, r_skin_names);
				copyObjects(i, index, 1, skin_weights, r_skin_weights);
				indices[i] = (short)index;
				index++;
			} else {
				indices[i] = (short)j;
			}
		}
		r_vertices = stripArray(index*3, r_vertices);
		r_normals = stripArray(index*3, r_normals);
		r_colors = stripArray(index*3, r_colors);
		r_uvs = stripArray(index*2, r_uvs);
		r_skin_names = stripArray(index, r_skin_names);
		r_skin_weights = stripArray(index, r_skin_weights);
		ShortBuffer index_buffer = ShortBuffer.wrap(indices);
		IndexListOptimizer.optimize(index_buffer);
//		System.out.println("resulting vertices = " + index);
		return new ModelInfo(/*tex_name,*/ indices, r_vertices, r_normals, r_colors, r_uvs, r_skin_names, r_skin_weights);
	}

	private static float[][] stripArray(int length, float@NonNull [] @NonNull [] array) {
		float[][] copy = new float[length][];
        System.arraycopy(array, 0, copy, 0, length);
		return copy;
	}

	private static byte[][] stripArray(int length, byte@NonNull [] @NonNull [] array) {
		byte[][] copy = new byte[length][];
        System.arraycopy(array, 0, copy, 0, length);
		return copy;
	}

	private static float[] stripArray(int length, float @NonNull [] array) {
		float[] copy = new float[length];
        System.arraycopy(array, 0, copy, 0, length);
		return copy;
	}

	static @NonNull SpriteInfo convertToSprite(String[][] textures, @NonNull ModelInfo model_info, float[] clear_color) {
		return new SpriteInfo(textures, model_info.indices, model_info.vertices, model_info.normals, model_info.texcoords, model_info.skin_names, model_info.skin_weights, clear_color);
	}

	public static @NonNull AnimationInfo convertToAnimation(/*float[] skeleton_vertices,*/ @NonNull Bone skeleton, @NonNull Map<String,float[]> initial_pose, Map<String,float[]> @NonNull [] anim_map, AnimationInfo.@NonNull AnimationType type, float wpc) {
		// animations format: [frames] [bones] [matrix]
		int num_frames = anim_map.length;
		float[][] frames = new float[num_frames][];
		for (int frame_index = 0; frame_index < num_frames; frame_index++) {
			float[] bones = new float[initial_pose.size()*12];
			frames[frame_index] = bones;
			normalizeSkeleton(/*new float[]{0, 0, 0}, skeleton_vertices,*/ bones, skeleton, initial_pose, anim_map[frame_index]);
		}
		return new AnimationInfo(frames, type, wpc);
	}

	private static void normalizeSkeleton(/*float[] parent_bone_vertex, float[] skeleton_vertices,*/ float @NonNull [] bones, @NonNull Bone current_bone, @NonNull Map<String,float[]> initial_pose_map, @NonNull Map<String,float[]> frame_map) {
		assert initial_pose_map.size() == bones.length/12;
		assert frame_map.size() == bones.length/12;
		String bone_name = current_bone.getName();
		float[] initial_pose_data = initial_pose_map.get(bone_name);
		float[] frame_data = frame_map.get(bone_name);
		Matrix4f absolute_initial_pose_matrix = new Matrix4f().set(initial_pose_data);
		Matrix4f absolute_frame_matrix = new Matrix4f().set(frame_data);

		Matrix4f inverted_absolute_initial_pose_matrix = new Matrix4f(absolute_initial_pose_matrix);
		inverted_absolute_initial_pose_matrix.invert();
		Matrix4f resulting_matrix = absolute_frame_matrix.mul(inverted_absolute_initial_pose_matrix, new Matrix4f());
		int offset = current_bone.getIndex()*12;

		// Store elements in column-major order, matching LWJGL2's storeTranspose behavior for the first 12 elements
		bones[offset++] = resulting_matrix.m00();
		bones[offset++] = resulting_matrix.m10();
		bones[offset++] = resulting_matrix.m20();
		bones[offset++] = resulting_matrix.m30(); // Should be 0.0f for affine transformations

		bones[offset++] = resulting_matrix.m01();
		bones[offset++] = resulting_matrix.m11();
		bones[offset++] = resulting_matrix.m21();
		bones[offset++] = resulting_matrix.m31(); // Should be 0.0f

		bones[offset++] = resulting_matrix.m02();
		bones[offset++] = resulting_matrix.m12();
		bones[offset++] = resulting_matrix.m22();
		bones[offset++] = resulting_matrix.m32(); // Should be 0.0f

		final float DELTA = .0001f;
		assert Math.abs(resulting_matrix.m03()) < DELTA : resulting_matrix.m03();
		assert Math.abs(resulting_matrix.m13()) < DELTA : resulting_matrix.m13();
		assert Math.abs(resulting_matrix.m23()) < DELTA : resulting_matrix.m23();
		assert Math.abs(resulting_matrix.m33() - 1f) < DELTA: resulting_matrix.m33();

/*Vector4f bone_point = new Vector4f();
Vector4f bone_point_transformed = new Vector4f();
bone_point.set(0, 0, 0, 1);
Matrix4f.transform(absolute_frame_matrix, bone_point, bone_point_transformed);
skeleton_vertices[current_bone.getIndex()*6 + 0] = parent_bone_vertex[0];
skeleton_vertices[current_bone.getIndex()*6 + 1] = parent_bone_vertex[1];
skeleton_vertices[current_bone.getIndex()*6 + 2] = parent_bone_vertex[2];
skeleton_vertices[current_bone.getIndex()*6 + 3] = bone_point_transformed.x;
skeleton_vertices[current_bone.getIndex()*6 + 4] = bone_point_transformed.y;
skeleton_vertices[current_bone.getIndex()*6 + 5] = bone_point_transformed.z;
System.out.println("bone_point_transformed.x = " + bone_point_transformed.x + " | bone_point_transformed.y = " + bone_point_transformed.y + " | bone_point_transformed.z = " + bone_point_transformed.z);
System.out.println(absolute_frame_matrix);*/
        for (Bone child_bone : current_bone.getChildren()) {
            normalizeSkeleton(/*new float[]{bone_point_transformed.x, bone_point_transformed.y, bone_point_transformed.z}, skeleton_vertices,*/ bones, child_bone, initial_pose_map, frame_map);
        }
	}
}
