package com.oddlabs.tt.vbo;

import com.oddlabs.tt.render.shader.ShaderProgram;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

public final class QuadVBO {
    private final FloatVBO vbo;

    public QuadVBO() {
        // Full screen quad in NDC: -1 to 1
        float[] vertices = {
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
             1f,  1f, 1f, 1f,
            -1f,  1f, 0f, 1f
        };
        vbo = new FloatVBO(GL15.GL_STATIC_DRAW, vertices);
    }

    public void render(ShaderProgram shader) {
        // Assume 'a_position' is 0 (vec2) and 'a_texCoord0' is 1 (vec2) or combined.
        // Wait, standard ShaderProgram attributes might differ.
        // Let's assume vertex layout: pos(2), uv(2)
        // But shader usually expects a_position as vec3.
        // Let's pass 4 floats per vertex: x, y, u, v.
        
        // We will manually bind attributes if needed, or rely on convention.
        // Position: stride 16 (4 floats), offset 0.
        // TexCoord: stride 16 (4 floats), offset 8 (2 floats).
        
        int posLoc = shader.getAttributeLocation("a_position");
        int texLoc = shader.getAttributeLocation("a_texCoord0"); // Standard name

        if (posLoc != -1) {
            vbo.vertexAttribPointer(posLoc, 2, 16, 0); // 2 components for position
            GL20.glEnableVertexAttribArray(posLoc);
        }
        if (texLoc != -1) {
            vbo.vertexAttribPointer(texLoc, 2, 16, 8); // 2 components for uv
            GL20.glEnableVertexAttribArray(texLoc);
        }

        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);

        if (posLoc != -1) GL20.glDisableVertexAttribArray(posLoc);
        if (texLoc != -1) GL20.glDisableVertexAttribArray(texLoc);
    }
    
    public void delete() {
        vbo.close();
    }
}
