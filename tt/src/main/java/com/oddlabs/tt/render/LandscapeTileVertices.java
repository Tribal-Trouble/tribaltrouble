package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTileIndices;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.vbo.FloatVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

final class LandscapeTileVertices {
	private final @NonNull FloatVBO patch_vertex_buffer;
	private final @NonNull FloatVBO patch_normal_buffer;
	private final @NonNull FloatVBO patch_texcoord_buffer;
	private final FloatBuffer edit_buffer;
	private final int patch_size;
	private final int elements_per_patch;
	private final int num_patches;
	private final HeightMap heightmap;

	private final static Vector3f v1 = new Vector3f();
	private final static Vector3f v2 = new Vector3f();
	private final static Vector3f normal = new Vector3f();

	public LandscapeTileVertices(HeightMap heightmap, int patch_exp, int num_patches) {
		this.heightmap = heightmap;
		this.num_patches = num_patches;
		this.patch_size = LandscapeTileIndices.getPatchSize(patch_exp);
		this.elements_per_patch = patch_size * patch_size * 3; // 3 floats per vertex (x, y, z)
		int floats_per_texcoord = patch_size * patch_size * 2; // 2 floats per texcoord (u, v)
		int floats_per_normal = patch_size * patch_size * 3; // 3 floats per normal (x, y, z)

		this.edit_buffer = BufferUtils.createFloatBuffer(elements_per_patch);

		int total_vertex_buffer_size = elements_per_patch * num_patches * num_patches;
		int total_texcoord_buffer_size = floats_per_texcoord * num_patches * num_patches;
		int total_normal_buffer_size = floats_per_normal * num_patches * num_patches;

		FloatBuffer vertices = BufferUtils.createFloatBuffer(total_vertex_buffer_size);
		FloatBuffer normals = BufferUtils.createFloatBuffer(total_normal_buffer_size);
		FloatBuffer texcoords = BufferUtils.createFloatBuffer(total_texcoord_buffer_size);

		for (int patch_y = 0; patch_y < num_patches; patch_y++) {
			for (int patch_x = 0; patch_x < num_patches; patch_x++) {
				fillVertexData(vertices, normals, texcoords, patch_x, patch_y);
			}
		}
		assert !vertices.hasRemaining();
		assert !normals.hasRemaining();
		assert !texcoords.hasRemaining();

		vertices.rewind();
		normals.rewind();
		texcoords.rewind();

		patch_vertex_buffer = new FloatVBO(ARBBufferObject.GL_DYNAMIC_DRAW_ARB, vertices.remaining());
		patch_vertex_buffer.put(vertices);

		patch_normal_buffer = new FloatVBO(ARBBufferObject.GL_DYNAMIC_DRAW_ARB, normals.remaining());
		patch_normal_buffer.put(normals);

		patch_texcoord_buffer = new FloatVBO(ARBBufferObject.GL_DYNAMIC_DRAW_ARB, texcoords.remaining());
		patch_texcoord_buffer.put(texcoords);
	}

	public void reload(int patch_x, int patch_y) {
		edit_buffer.clear();
		// Need to re-calculate normals and texcoords for the reloaded patch as well
		FloatBuffer temp_normals = BufferUtils.createFloatBuffer(patch_size * patch_size * 3);
		FloatBuffer temp_texcoords = BufferUtils.createFloatBuffer(patch_size * patch_size * 2);
		fillVertexData(edit_buffer, temp_normals, temp_texcoords, patch_x, patch_y);
		edit_buffer.flip();
		temp_normals.flip();
		temp_texcoords.flip();

		patch_vertex_buffer.putSubData(getVertexIndex(patch_x, patch_y), edit_buffer);
		patch_normal_buffer.putSubData(getNormalIndex(patch_x, patch_y), temp_normals);
		patch_texcoord_buffer.putSubData(getTexCoordIndex(patch_x, patch_y), temp_texcoords);
	}

	private int getVertexIndex(int patch_x, int patch_y) {
		return (patch_x + patch_y * num_patches) * elements_per_patch;
	}

	private int getNormalIndex(int patch_x, int patch_y) {
		return (patch_x + patch_y * num_patches) * patch_size * patch_size * 3;
	}

	private int getTexCoordIndex(int patch_x, int patch_y) {
		return (patch_x + patch_y * num_patches) * patch_size * patch_size * 2;
	}

	public void bind(int patch_x, int patch_y) {
		int vertex_position = getVertexIndex(patch_x, patch_y);
		int normal_position = getNormalIndex(patch_x, patch_y);
		int texcoord_position = getTexCoordIndex(patch_x, patch_y);

		patch_vertex_buffer.vertexPointer(3, 0, vertex_position);
		patch_normal_buffer.normalPointer(0, normal_position);
		patch_texcoord_buffer.texCoordPointer(2, 0, texcoord_position);
	}

	private void fillVertexData(@NonNull FloatBuffer vertex_array, @NonNull FloatBuffer normal_array, @NonNull FloatBuffer texcoord_array, int grid_origin_x, int grid_origin_y) {
		grid_origin_x *= patch_size - 1;
		grid_origin_y *= patch_size - 1;
		int world_border_mask = ~(patch_size - 2);
		for (int y = 0; y < patch_size; y++) {
			for (int x = 0; x < patch_size; x++) {
				int y_coord = grid_origin_y + y;
				int x_coord = grid_origin_x + x;
				boolean is_y_border = y_coord == 0 || y_coord == heightmap.getGridUnitsPerWorld();
				boolean is_x_border = x_coord == 0 || x_coord == heightmap.getGridUnitsPerWorld();
				if (is_y_border)
					x_coord = x_coord & world_border_mask;
				if (is_x_border)
					y_coord = y_coord & world_border_mask;
				float yf = y_coord * HeightMap.METERS_PER_UNIT_GRID;
				float xf = x_coord * HeightMap.METERS_PER_UNIT_GRID;
				float zf = heightmap.getWrappedHeight(x_coord, y_coord);
				vertex_array.put(xf);
				vertex_array.put(yf);
				vertex_array.put(zf);

				// Calculate normal
				Vector3f calculated_normal = calculateNormal(x_coord, y_coord);
				normal_array.put(calculated_normal.x);
				normal_array.put(calculated_normal.y);
				normal_array.put(calculated_normal.z);

				// Calculate texture coordinates
				float tex_u = xf * Globals.LANDSCAPE_TEXTURE_SCALE;
				float tex_v = yf * Globals.LANDSCAPE_TEXTURE_SCALE;
				texcoord_array.put(tex_u);
				texcoord_array.put(tex_v);
			}
		}
	}

	private @NonNull Vector3f calculateNormal(int x, int y) {
		float hL = heightmap.getWrappedHeight(x - 1, y);
		float hR = heightmap.getWrappedHeight(x + 1, y);
		float hD = heightmap.getWrappedHeight(x, y - 1);
		float hU = heightmap.getWrappedHeight(x, y + 1);

		v1.set(HeightMap.METERS_PER_UNIT_GRID * 2, 0, hR - hL);
		v2.set(0, HeightMap.METERS_PER_UNIT_GRID * 2, hU - hD);
		Vector3f.cross(v1, v2, normal);
		normal.normalise();
		return normal;
	}
}
