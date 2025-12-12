package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.LandscapeTileIndices;
import com.oddlabs.tt.render.shader.LandscapeShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import com.oddlabs.tt.vbo.VertexArrays;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.Objects;

final class LandscapeTileVertices {
	private final @NonNull FloatVBO patch_vertex_buffer;
	private final @NonNull FloatVBO patch_normal_buffer;
	private final @NonNull FloatVBO patch_colormap_texcoord_buffer;
	private final @NonNull FloatVBO patch_lightmap_texcoord_buffer;
	private final @NonNull FloatBuffer edit_buffer;
	private final int patch_size;
	private final int elements_per_patch;
	private final int num_patches;
	private final HeightMap heightmap;
    private VertexArray[] mainVAOs;

	private static final Vector3f v1 = new Vector3f();
	private static final Vector3f v2 = new Vector3f();

	public LandscapeTileVertices(HeightMap heightmap, int patch_exp, int num_patches) {
		this.heightmap = heightmap;
		this.num_patches = num_patches;
		this.patch_size = LandscapeTileIndices.getPatchSize(patch_exp);
		this.elements_per_patch = patch_size * patch_size * 3; // 3 floats per vertex (x, y, z)
		int floats_per_texcoord = patch_size * patch_size * 2; // 2 floats per texcoord (u, v)
		int floats_per_normal = patch_size * patch_size * 3; // 3 floats per normal (x, y, z)

		this.edit_buffer = Objects.requireNonNull(BufferUtils.createFloatBuffer(elements_per_patch));

		int total_vertex_buffer_size = elements_per_patch * num_patches * num_patches;
		int total_texcoord_buffer_size = floats_per_texcoord * num_patches * num_patches;
		int total_normal_buffer_size = floats_per_normal * num_patches * num_patches;

		FloatBuffer vertices = Objects.requireNonNull(BufferUtils.createFloatBuffer(total_vertex_buffer_size));
		FloatBuffer normals = Objects.requireNonNull(BufferUtils.createFloatBuffer(total_normal_buffer_size));
		FloatBuffer colormap_texcoords = Objects.requireNonNull(BufferUtils.createFloatBuffer(total_texcoord_buffer_size));
		FloatBuffer lightmap_texcoords = Objects.requireNonNull(BufferUtils.createFloatBuffer(total_texcoord_buffer_size));

		for (int patch_y = 0; patch_y < num_patches; patch_y++) {
			for (int patch_x = 0; patch_x < num_patches; patch_x++) {
				fillVertexData(vertices, normals, colormap_texcoords, lightmap_texcoords, patch_x, patch_y);
			}
		}
		assert !vertices.hasRemaining();
		assert !normals.hasRemaining();
		assert !colormap_texcoords.hasRemaining();
		assert !lightmap_texcoords.hasRemaining();

		vertices.rewind();
		normals.rewind();
		colormap_texcoords.rewind();
		lightmap_texcoords.rewind();

		patch_vertex_buffer = new FloatVBO(GL15.GL_DYNAMIC_DRAW, vertices.remaining());
		patch_vertex_buffer.put(vertices);

		patch_normal_buffer = new FloatVBO(GL15.GL_DYNAMIC_DRAW, normals.remaining());
		patch_normal_buffer.put(normals);

		patch_colormap_texcoord_buffer = new FloatVBO(GL15.GL_DYNAMIC_DRAW, colormap_texcoords.remaining());
		patch_colormap_texcoord_buffer.put(colormap_texcoords);

		patch_lightmap_texcoord_buffer = new FloatVBO(GL15.GL_DYNAMIC_DRAW, lightmap_texcoords.remaining());
		patch_lightmap_texcoord_buffer.put(lightmap_texcoords);
	}

    public void init(LandscapeShader shader) {
        if (!VertexArrays.isSupported()) return;
        
        mainVAOs = new VertexArray[num_patches * num_patches];
        for (int y = 0; y < num_patches; y++) {
            for (int x = 0; x < num_patches; x++) {
                VertexArray vao = VertexArrays.create();
                vao.bind();
                bindAttributes(shader, x, y);
                vao.unbind();
                mainVAOs[x + y * num_patches] = vao;
            }
        }
    }

	public void reload(int patch_x, int patch_y) {
		edit_buffer.clear();
		// Need to re-calculate normals and texcoords for the reloaded patch as well
		FloatBuffer temp_normals = Objects.requireNonNull(BufferUtils.createFloatBuffer(patch_size * patch_size * 3));
		FloatBuffer temp_colormap_texcoords = Objects.requireNonNull(BufferUtils.createFloatBuffer(patch_size * patch_size * 2));
		FloatBuffer temp_lightmap_texcoords = Objects.requireNonNull(BufferUtils.createFloatBuffer(patch_size * patch_size * 2));
		fillVertexData(edit_buffer, temp_normals, temp_colormap_texcoords, temp_lightmap_texcoords, patch_x, patch_y);
		edit_buffer.flip();
		temp_normals.flip();
		temp_colormap_texcoords.flip();
		temp_lightmap_texcoords.flip();

		patch_vertex_buffer.putSubData(getVertexIndex(patch_x, patch_y), edit_buffer);
		patch_normal_buffer.putSubData(getNormalIndex(patch_x, patch_y), temp_normals);
		patch_colormap_texcoord_buffer.putSubData(getTexCoordIndex(patch_x, patch_y), temp_colormap_texcoords);
		patch_lightmap_texcoord_buffer.putSubData(getTexCoordIndex(patch_x, patch_y), temp_lightmap_texcoords);
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

    public void bind(int patch_x, int patch_y, LandscapeShader shader) {
        if (mainVAOs != null) {
            mainVAOs[patch_x + patch_y * num_patches].bind();
        } else {
            bindAttributes(shader, patch_x, patch_y);
        }
    }
    
    public void unbind(LandscapeShader shader) {
        if (mainVAOs != null) {
            VertexArrays.unbind();
        } else {
            unbindAttributes(shader);
        }
    }

    public void bindAttributes(@NonNull ShaderProgram shader, int patch_x, int patch_y) {
        int vertex_position = getVertexIndex(patch_x, patch_y);
        int normal_position = getNormalIndex(patch_x, patch_y);
        int texcoord_position = getTexCoordIndex(patch_x, patch_y);

        int posLoc = shader.getAttributeLocation(LandscapeShader.Attributes.POSITION);
        if (posLoc != -1) {
            patch_vertex_buffer.vertexAttribPointer(posLoc, 3, 0, (long) vertex_position * Float.BYTES);
            GL20.glEnableVertexAttribArray(posLoc);
        }

        int normLoc = shader.getAttributeLocation(LandscapeShader.Attributes.NORMAL);
        if (normLoc != -1) {
            patch_normal_buffer.vertexAttribPointer(normLoc, 3, 0, (long) normal_position * Float.BYTES);
            GL20.glEnableVertexAttribArray(normLoc);
        }

        int tex0Loc = shader.getAttributeLocation(LandscapeShader.Attributes.TEX_COORD_0);
        if (tex0Loc != -1) {
            patch_colormap_texcoord_buffer.vertexAttribPointer(tex0Loc, 2, 0, (long) texcoord_position * Float.BYTES);
            GL20.glEnableVertexAttribArray(tex0Loc);
        }
        
        int tex1Loc = shader.getAttributeLocation(LandscapeShader.Attributes.TEX_COORD_1);
        if (tex1Loc != -1) {
            patch_lightmap_texcoord_buffer.vertexAttribPointer(tex1Loc, 2, 0, (long) texcoord_position * Float.BYTES);
            GL20.glEnableVertexAttribArray(tex1Loc);
        }
    }
    
    public void unbindAttributes(@NonNull ShaderProgram shader) {
        int posLoc = shader.getAttributeLocation(LandscapeShader.Attributes.POSITION);
        if (posLoc != -1) GL20.glDisableVertexAttribArray(posLoc);
        int normLoc = shader.getAttributeLocation(LandscapeShader.Attributes.NORMAL);
        if (normLoc != -1) GL20.glDisableVertexAttribArray(normLoc);
        int tex0Loc = shader.getAttributeLocation(LandscapeShader.Attributes.TEX_COORD_0);
        if (tex0Loc != -1) GL20.glDisableVertexAttribArray(tex0Loc);
        int tex1Loc = shader.getAttributeLocation(LandscapeShader.Attributes.TEX_COORD_1);
        if (tex1Loc != -1) GL20.glDisableVertexAttribArray(tex1Loc);
    }

	private void fillVertexData(@NonNull FloatBuffer vertex_array, @NonNull FloatBuffer normal_array, @NonNull FloatBuffer colormap_texcoord_array, @NonNull FloatBuffer lightmap_texcoord_array, int grid_origin_x, int grid_origin_y) {
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

				// Calculate colormap texture coordinates
				colormap_texcoord_array.put(xf * Globals.LANDSCAPE_TEXTURE_SCALE);
				colormap_texcoord_array.put(yf * Globals.LANDSCAPE_TEXTURE_SCALE);

                // Calculate lightmap texture coordinates
                float lightmap_u = xf / heightmap.getMetersPerWorld();
                float lightmap_v = yf / heightmap.getMetersPerWorld();
                lightmap_texcoord_array.put(lightmap_u);
                lightmap_texcoord_array.put(lightmap_v);
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
        return v1.cross(v2, new Vector3f()).normalize();
	}
}
