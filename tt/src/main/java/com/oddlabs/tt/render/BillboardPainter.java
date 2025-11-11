package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

public abstract class BillboardPainter {
	private final static FloatBuffer matrix_buf = BufferUtils.createFloatBuffer(16);
	private final static DoubleBuffer plane_buf = BufferUtils.createDoubleBuffer(4);

	private static void initClipPlane(int clip_enum, int face_index, int vertex_index1, int vertex_index2, short @NonNull [] indices, float @NonNull [] face_tex_coords, float handedness) {
		float u1 = getElement(face_index, vertex_index1, 0, 2, indices, face_tex_coords);
		float v1 = getElement(face_index, vertex_index1, 1, 2, indices, face_tex_coords);
		float u2 = getElement(face_index, vertex_index2, 0, 2, indices, face_tex_coords);
		float v2 = getElement(face_index, vertex_index2, 1, 2, indices, face_tex_coords);
		Vector3f vec1 = new Vector3f(0f, 0f, 1f);
		Vector3f vec2 = new Vector3f(u2 - u1, v2 - v1, 0);
		Vector3f vec3 = new Vector3f();
		vec1.cross(vec2, vec3);
		vec3.mul(handedness);
		vec3.normalize(vec3);
		vec1.set(u1, v1, 0f);
		float d = -vec3.dot(vec1);
		plane_buf.put(0, vec3.x).put(1, vec3.y).put(2, vec3.z).put(3, d);
		GL11.glClipPlane(clip_enum, plane_buf);
	}

	public static void finish() {
		GL11.glDisable(GL11.GL_CLIP_PLANE0);
		GL11.glDisable(GL11.GL_CLIP_PLANE1);
		GL11.glDisable(GL11.GL_CLIP_PLANE2);
	}

	public static void init() {
		GL11.glEnable(GL11.GL_CLIP_PLANE0);
		GL11.glEnable(GL11.GL_CLIP_PLANE1);
		GL11.glEnable(GL11.GL_CLIP_PLANE2);
	}

	private static float getElement(int face_index, int vertex_index, int element_index, int vertex_size, short @NonNull [] indices, float @NonNull [] vertices) {
		int vertices_index = indices[face_index*3 + vertex_index];
		return vertices[vertices_index*vertex_size + element_index];
	}

	public static void loadFaceMatrixAndClipPlanes(int face_index, short @NonNull [] indices, float @NonNull [] face_vertices, float @NonNull [] face_tex_coords) {
		// Find object space to texture space matrix, mapping vectors in object space to vectors in texture space
		Vector3f v1 = new Vector3f(getElement(face_index, 0, 0, 3, indices, face_vertices),
								   getElement(face_index, 0, 1, 3, indices, face_vertices),
								   getElement(face_index, 0, 2, 3, indices, face_vertices));
		Vector3f v2 = new Vector3f(getElement(face_index, 1, 0, 3, indices, face_vertices),
								   getElement(face_index, 1, 1, 3, indices, face_vertices),
								   getElement(face_index, 1, 2, 3, indices, face_vertices));
		Vector3f v3 = new Vector3f(getElement(face_index, 2, 0, 3, indices, face_vertices),
								   getElement(face_index, 2, 1, 3, indices, face_vertices),
								   getElement(face_index, 2, 2, 3, indices, face_vertices));
		Vector2f w1 = new Vector2f(getElement(face_index, 0, 0, 2, indices, face_tex_coords),
								   getElement(face_index, 0, 1, 2, indices, face_tex_coords));
		Vector2f w2 = new Vector2f(getElement(face_index, 1, 0, 2, indices, face_tex_coords),
								   getElement(face_index, 1, 1, 2, indices, face_tex_coords));
		Vector2f w3 = new Vector2f(getElement(face_index, 2, 0, 2, indices, face_tex_coords),
								   getElement(face_index, 2, 1, 2, indices, face_tex_coords));

		float x1 = v2.x - v1.x;
		float x2 = v3.x - v1.x;
		float y1 = v2.y - v1.y;
		float y2 = v3.y - v1.y;
		float z1 = v2.z - v1.z;
		float z2 = v3.z - v1.z;

		float s1 = w2.x - w1.x;
		float s2 = w3.x - w1.x;
		float t1 = w2.y - w1.y;
		float t2 = w3.y - w1.y;

		float r = 1.0f/(s1 * t2 - s2 * t1);
		Vector3f tan1 = new Vector3f((t2 * x1 - t1 * x2) * r, (t2 * y1 - t1 * y2) * r, (t2 * z1 - t1 * z2) * r);
		Vector3f tan2 = new Vector3f((s1 * x2 - s2 * x1) * r, (s1 * y2 - s2 * y1) * r, (s1 * z2 - s2 * z1) * r);

		Vector3f v1v2 = new Vector3f();
		Vector3f v1v3 = new Vector3f();
		Vector2f w1w2 = new Vector2f();
		v2.sub(v1, v1v2);
		v3.sub(v1, v1v3);
		w2.sub(w1, w1w2);
		Vector3f n = new Vector3f();
		v1v2.cross(v1v3, n);
		n.normalize(n);
		Vector3f tangent3 = new Vector3f(n);
		float dot = n.dot(tan1);
		tangent3.mul(dot);
		tan1.sub(tangent3, tangent3);
		tangent3.normalize(tangent3);
		Vector3f cross = new Vector3f();
		n.cross(tan1, cross);
		float handedness = cross.dot(tan2) < 0f ? -1f : 1f;
		n.cross(tangent3, cross);
		cross.mul(handedness);

		Matrix4f obj_to_tex_matrix = new Matrix4f(
                tangent3.x, tangent3.y, tangent3.z, 0,
		        cross.x, cross.y, cross.z, 0,
		        n.x, n.y, n.z, 0,
                0, 0, 0,1);

		Matrix4f result = new Matrix4f();
		Matrix4f result1 = new Matrix4f();
		Matrix4f result2 = new Matrix4f();
		// Find the texture translation
		Matrix4f tex_translation = new Matrix4f();
		tex_translation.translate(-w1.x, -w1.y, 0f);
		// Find the object space translation
		Matrix4f vert_translation = new Matrix4f();
		vert_translation.translate(v1.x, v1.y, v1.z);
		// Find the relative scaling between object space and texture space
		Matrix4f scaling = new Matrix4f();
		float scale_factor = v1v2.length()/w1w2.length();
		scaling.scale(scale_factor, scale_factor, scale_factor);
		// Combine matrices resulting in the matrix converting points in texture space to points in object space
		vert_translation.mul(obj_to_tex_matrix, result1);
		result1.mul(scaling, result2);
		result2.mul(tex_translation, result);
		//Invert the matrix to get the matrix from points in object space to points in texture space
		result.invert();
		result.get(matrix_buf);
		GL11.glLoadIdentity();
		initClipPlane(GL11.GL_CLIP_PLANE0, face_index, 0, 1, indices, face_tex_coords, handedness);
		initClipPlane(GL11.GL_CLIP_PLANE1, face_index, 1, 2, indices, face_tex_coords, handedness);
		initClipPlane(GL11.GL_CLIP_PLANE2, face_index, 2, 0, indices, face_tex_coords, handedness);
		GL11.glLoadMatrix(matrix_buf);
	}

    private BillboardPainter() {
    }
}
