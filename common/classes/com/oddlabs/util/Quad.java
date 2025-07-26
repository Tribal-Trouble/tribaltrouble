package com.oddlabs.util;

import com.oddlabs.util.TrafoState;

import java.io.*;
import org.lwjgl.opengl.*;

public strictfp class Quad implements Serializable {
	private final static long serialVersionUID = 1;

	private final float u1;
	private final float v1;
	private final float u2;
	private final float v2;
	private final int width;
	private final int height;
    private int texture = 0;

    private static int tex_program = -1;
    private static int tex_var_tex = 0;
    private static int tex_var_clr = 0;
    private static int tex_var_pos = 0;
    private static int tex_var_size = 0;
    private static int tex_var_uv = 0;
    private static int tex_var_resolution = 0;
    
    private static int clr_program = -1;
    private static int clr_var_clr = 0;
    private static int clr_var_pos = 0;
    private static int clr_var_size = 0;
    private static int clr_var_resolution = 0;

    private static int vao = 0;

    public static void initShaders() {
        String vertexSrcTex =
            "#version 330 core\n" +
            "layout(location = 0) in vec4 v_vertex;\n" +
            "uniform vec2 u_resolution;\n" +
            "uniform vec2 u_pos;\n" +
            "uniform vec2 u_size;\n" +
            "uniform vec4 u_uv;\n" +
            "out vec2 f_uv;\n" +
            "void main() {\n" +
            "    vec2 pos = 2.0 * vec2(u_pos.x / u_resolution.x, u_pos.y / u_resolution.y) - vec2(1.0, 1.0);\n" +
            "    vec2 size = 2.0 * (vec2(u_size.x / u_resolution.x, u_size.y / u_resolution.y));\n" +
            "    gl_Position.xy = pos + vec2(v_vertex.x * size.x, v_vertex.y * size.y);\n" +
            "    gl_Position.z = 0.0;\n" +
            "    gl_Position.w = 1.0;\n" +
            "    if (v_vertex.x < 0.5) f_uv.x = u_uv.x; else f_uv.x = u_uv.z;\n" +
            "    if (v_vertex.y < 0.5) f_uv.y = u_uv.y; else f_uv.y = u_uv.w;\n" +
            "}";

        String fragmentSrcTex = 
            "#version 330 core\n" +
            "uniform sampler2D u_tex;\n" +
            "uniform vec4 u_color;\n" +
            "in vec2 f_uv;\n" +
            "out vec4 color;\n" +
            "void main() {\n" +
            "    color = texture(u_tex, f_uv) * u_color;\n" +
            "}";

        int vertexShaderTex = GL33.glCreateShader(GL33.GL_VERTEX_SHADER);
        GL33.glShaderSource(vertexShaderTex, vertexSrcTex);
        GL33.glCompileShader(vertexShaderTex);
        checkCompileStatus(vertexShaderTex);

        int fragmentShaderTex = GL33.glCreateShader(GL33.GL_FRAGMENT_SHADER);
        GL33.glShaderSource(fragmentShaderTex, fragmentSrcTex);
        GL33.glCompileShader(fragmentShaderTex);
        checkCompileStatus(fragmentShaderTex);

        tex_program = GL33.glCreateProgram();
        GL33.glAttachShader(tex_program, vertexShaderTex);
        GL33.glAttachShader(tex_program, fragmentShaderTex);
        GL33.glLinkProgram(tex_program);
        checkLinkStatus(tex_program);

        GL33.glDeleteShader(vertexShaderTex);
        GL33.glDeleteShader(fragmentShaderTex);

        GL33.glUseProgram(tex_program);
        tex_var_tex = GL33.glGetUniformLocation(tex_program, "u_tex");
        tex_var_clr = GL33.glGetUniformLocation(tex_program, "u_color");
        tex_var_size = GL33.glGetUniformLocation(tex_program, "u_size");
        tex_var_uv = GL33.glGetUniformLocation(tex_program, "u_uv");
        tex_var_pos = GL33.glGetUniformLocation(tex_program, "u_pos");
        tex_var_resolution = GL33.glGetUniformLocation(tex_program, "u_resolution");

        String vertexSrcClr =
            "#version 330 core\n" +
            "layout(location = 0) in vec4 v_vertex;\n" +
            "uniform vec2 u_resolution;\n" +
            "uniform vec2 u_pos;\n" +
            "uniform vec2 u_size;\n" +
            "void main() {\n" +
            "    vec2 pos = 2.0 * vec2(u_pos.x / u_resolution.x, u_pos.y / u_resolution.y) - vec2(1.0, 1.0);\n" +
            "    vec2 size = 2.0 * (vec2(u_size.x / u_resolution.x, u_size.y / u_resolution.y));\n" +
            "    gl_Position.xy = pos + vec2(v_vertex.x * size.x, v_vertex.y * size.y);\n" +
            "    gl_Position.z = 0.0;\n" +
            "    gl_Position.w = 1.0;\n" +
            "}";

        String fragmentSrcClr = 
            "#version 330 core\n" +
            "uniform vec4 u_color;\n" +
            "out vec4 color;\n" +
            "void main() {\n" +
            "    color = u_color;\n" +
            "}";

        int vertexShaderClr = GL33.glCreateShader(GL33.GL_VERTEX_SHADER);
        GL33.glShaderSource(vertexShaderClr, vertexSrcClr);
        GL33.glCompileShader(vertexShaderClr);
        checkCompileStatus(vertexShaderClr);

        int fragmentShaderClr = GL33.glCreateShader(GL33.GL_FRAGMENT_SHADER);
        GL33.glShaderSource(fragmentShaderClr, fragmentSrcClr);
        GL33.glCompileShader(fragmentShaderClr);
        checkCompileStatus(fragmentShaderClr);

        clr_program = GL33.glCreateProgram();
        GL33.glAttachShader(clr_program, vertexShaderClr);
        GL33.glAttachShader(clr_program, fragmentShaderClr);
        GL33.glLinkProgram(clr_program);
        checkLinkStatus(clr_program);

        GL33.glDeleteShader(vertexShaderClr);
        GL33.glDeleteShader(fragmentShaderClr);

        GL33.glUseProgram(clr_program);
        clr_var_clr = GL33.glGetUniformLocation(clr_program, "u_color");
        clr_var_size = GL33.glGetUniformLocation(clr_program, "u_size");
        clr_var_pos = GL33.glGetUniformLocation(clr_program, "u_pos");
        clr_var_resolution = GL33.glGetUniformLocation(clr_program, "u_resolution");

        vao = createVAO();
    }
    
    private static int createVAO() {
        float[] vertex = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        };

        int vao = GL33.glGenVertexArrays();
        int vbo = GL33.glGenBuffers();

        GL33.glBindVertexArray(vao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, vertex, GL33.GL_STATIC_DRAW);

        GL33.glVertexAttribPointer(0, 2, GL33.GL_FLOAT, false, 2 * Float.BYTES, 0);
        GL33.glEnableVertexAttribArray(0);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        GL33.glBindVertexArray(0);

        return vao;
    }

    private static void checkCompileStatus(int shader) {
        if (GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            System.out.println("Shader error: " + GL33.glGetShaderInfoLog(shader));
        }
    }

    private static void checkLinkStatus(int prog) {
        if (GL33.glGetProgrami(prog, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            System.out.println("Shader program link error: " + GL33.glGetProgramInfoLog(prog));
        }
    }
 
	public Quad(float u1, float v1, float u2, float v2, int width, int height) {
		this.u1 = u1;
		this.v1 = v1;
		this.u2 = u2;
		this.v2 = v2;
		this.width = width;
		this.height = height;
	}

	public void renderClipped(float x, float y, float clip_left, float clip_right, float clip_bottom, float clip_top) {
		float x1 = x;
		float x2 = x + width;
		float y1 = y;
		float y2 = y + height;
		float cleft_amount = StrictMath.max(0, clip_left - x1);
		float cright_amount = StrictMath.max(0, x2 - clip_right);
		float cbottom_amount = StrictMath.max(0, clip_bottom - y1);
		float ctop_amount = StrictMath.max(0, y2 - clip_top);
		if (ctop_amount + cbottom_amount >= height || cright_amount + cleft_amount >= width)
			return;
		x1 += cleft_amount;
		x2 -= cright_amount;
		y1 += cbottom_amount;
		y2 -= ctop_amount;
		float tex_width_scale = (u2 - u1)/width;
		float tex_height_scale = (v2 - v1)/height;
		float clipped_u1 = u1 + cleft_amount*tex_width_scale;
		float clipped_u2 = u2 - cright_amount*tex_width_scale;
		float clipped_v1 = v1 + cbottom_amount*tex_height_scale;
		float clipped_v2 = v2 - ctop_amount*tex_height_scale;
		render(x1, y1, x2, y1, x2, y2, x1, y2, clipped_u1, clipped_u2, clipped_v1, clipped_v2);
	}

	public final void render(float x, float y) {
		render(x, y, width, height);
	}

	public void render(float x, float y, int width, int height) {
		render(x, y, x + width, y, x + width, y + height, x, y + height);
	}

	public void render(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		render(x1, y1, x2, y2, x3, y3, x4, y4, u1, u2, v1, v2);
	}

    public void setTexture(int tex) {
        texture = tex;
    }

	public void render(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
			float u1, float u2, float v1, float v2) {

        if (tex_program == -1) {
            initShaders();
        }

        Vector4f vec0 = new Vector4f(x1, y1, 0f, 1f);
        Vector4f vec1 = new Vector4f(x3, y3, 0f, 1f);

        Matrix4f.transform(TrafoState.matrix, vec0, vec0);
        Matrix4f.transform(TrafoState.matrix, vec1, vec1);

        x1 = vec0.x;
        y1 = vec0.y;
        x3 = vec1.x;
        y3 = vec1.y;

        if (texture != 0) {
            GL33.glUseProgram(tex_program);
            GL33.glEnable(GL33.GL_BLEND);
            GL33.glDisable(GL33.GL_DEPTH_TEST);
            GL33.glEnable(GL33.GL_TEXTURE_2D);
            GL33.glBlendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);
            GL33.glActiveTexture(GL33.GL_TEXTURE0);
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, texture);
            GL33.glUniform1i(tex_var_tex, 0);
            GL33.glUniform4fv(tex_var_clr, TrafoState.color);
            GL33.glUniform2fv(tex_var_size, new float[]{x3 - x1, y3 - y1});
            GL33.glUniform2fv(tex_var_resolution, new float[]{TrafoState.getWidth(), TrafoState.getHeight()});
            GL33.glUniform2fv(tex_var_pos, new float[]{x1, y1});
            GL33.glUniform4fv(tex_var_uv, new float[]{u1, v1, u2, v2});
            GL33.glBindVertexArray(vao);
            GL33.glDrawArrays(GL33.GL_TRIANGLE_STRIP, 0, 4);
        } else {
            GL33.glUseProgram(clr_program);
            GL33.glEnable(GL33.GL_BLEND);
            GL33.glDisable(GL33.GL_DEPTH_TEST);
            GL33.glEnable(GL33.GL_TEXTURE_2D);
            GL33.glBlendFunc(GL33.GL_SRC_ALPHA, GL33.GL_ONE_MINUS_SRC_ALPHA);
            GL33.glUniform4fv(clr_var_clr, TrafoState.color);
            GL33.glUniform2fv(clr_var_size, new float[]{x3 - x1, y3 - y1});
            GL33.glUniform2fv(clr_var_resolution, new float[]{TrafoState.getWidth(), TrafoState.getHeight()});
            GL33.glUniform2fv(clr_var_pos, new float[]{x1, y1});
            GL33.glBindVertexArray(vao);
            GL33.glDrawArrays(GL33.GL_TRIANGLE_STRIP, 0, 4);
        }
	}

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	public final String toString() {
		return "u1 = " + u1 + " | v1 = " + v1 + " | u2 = " + u2 + " | v2 = " + v2 + " | width = " + width + " | height = " + height;
	}
}
