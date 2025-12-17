package com.oddlabs.tt.render;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.shader.LandscapeShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public final class PatchMesh {
    private static final int PATCH_SIZE = HeightMap.GRID_UNITS_PER_PATCH; // 16
    private static final int VERTEX_COUNT = (PATCH_SIZE + 1) * (PATCH_SIZE + 1);
    private static final int INDEX_COUNT = PATCH_SIZE * PATCH_SIZE * 6;

    private final FloatVBO vbo;
    private final ShortVBO ibo;

    public PatchMesh() {
        FloatBuffer vertices = BufferUtils.createFloatBuffer(VERTEX_COUNT * 2); // x, y
        for (int y = 0; y <= PATCH_SIZE; y++) {
            for (int x = 0; x <= PATCH_SIZE; x++) {
                vertices.put(x * HeightMap.METERS_PER_UNIT_GRID);
                vertices.put(y * HeightMap.METERS_PER_UNIT_GRID);
            }
        }
        vertices.flip();
        vbo = new FloatVBO(GL15.GL_STATIC_DRAW, vertices);

        ShortBuffer indices = BufferUtils.createShortBuffer(INDEX_COUNT);
        for (int y = 0; y < PATCH_SIZE; y++) {
            for (int x = 0; x < PATCH_SIZE; x++) {
                short v0 = (short) (x + y * (PATCH_SIZE + 1));
                short v1 = (short) (x + 1 + y * (PATCH_SIZE + 1));
                short v2 = (short) (x + (y + 1) * (PATCH_SIZE + 1));
                short v3 = (short) (x + 1 + (y + 1) * (PATCH_SIZE + 1));

                indices.put(v0).put(v2).put(v1);
                indices.put(v1).put(v2).put(v3);
            }
        }
        indices.flip();
        ibo = new ShortVBO(GL15.GL_STATIC_DRAW, indices);
    }

    public void bind(LandscapeShader shader) {
        int posLoc = shader.getAttributeLocation(LandscapeShader.Attributes.POSITION);
        if (posLoc != -1) {
            vbo.vertexAttribPointer(posLoc, 2, 0, 0);
            GL20.glEnableVertexAttribArray(posLoc);
        }
        ibo.makeCurrent();
    }

    public void draw() {
        ibo.drawElements(org.lwjgl.opengl.GL11.GL_TRIANGLES, INDEX_COUNT, 0);
    }

    public void unbind(LandscapeShader shader) {
        int posLoc = shader.getAttributeLocation(LandscapeShader.Attributes.POSITION);
        if (posLoc != -1) {
            GL20.glDisableVertexAttribArray(posLoc);
        }
        // VBO.releaseAll() handled by caller or frame start usually
    }
    
    public void delete() {
        vbo.close();
        ibo.close();
    }
}
