package com.oddlabs.tt.gui;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import com.oddlabs.tt.render.Display;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;

public final strictfp class GUIImage extends GUIObject {
	private final float u1;
	private final float v1;
	private final float u2;
	private final float v2;
	private final Texture texture;

    private int program = -1;
    private int var_tex = 0;
    private int var_pos = 0;
    private int var_size = 0;
    private int var_resolution = 0;
    private int vao = 0;

    /**
     * Creates a GUIImage with specified texture coordinates.
     *
     * @param width The desired width of the image to display in game.
     * @param height The desired height of the image to display in game.
     * @param u1 The left texture coordinate (0.0 to 1.0).
     * @param v1 The top texture coordinate (0.0 to 1.0).
     * @param u2 The right texture coordinate (0.0 to 1.0).
     * @param v2 The bottom texture coordinate (0.0 to 1.0).
     * @param texture_name The name of the texture resource to load from disk.
     * Should not include an extension
     */
    public GUIImage(int width, int height, float u1, float v1, float u2, float v2, String texture_name, boolean is_button) {
        this(width, height, u1, v1, u2, v2, (Texture) Resources.findResource(new TextureFile(texture_name, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT)), is_button);
    }

    /**
     * Creates a GUIImage with specified texture coordinates and allows for
     * mouse clicked events (aka focusing).
     *
     * @param width The desired width of the image to display in game.
     * @param height The desired height of the image to display in game.
     * @param u1 The left texture coordinate (0.0 to 1.0).
     * @param v1 The top texture coordinate (0.0 to 1.0
     * @param u2 The right texture coordinate (0.0 to 1.0).
     * @param v2 The bottom texture coordinate (0.0 to 1.0
     * @param texture The texture to use for this image.
     * @param is_button If true, the image can be focused and clicked on.
     */
    public GUIImage(int width, int height, float u1, float v1, float u2, float v2, Texture texture, boolean is_button) {
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.texture = texture;
        if (width < 0 || height < 0) {
            setDim(texture.getWidth(), texture.getHeight());
        } else {
            setDim(width, height);
        }
        setCanFocus(is_button);
    }

	public GUIImage(int width, int height, float u1, float v1, float u2, float v2, String texture_name) {
		this(width, height, u1, v1, u2, v2, (Texture)Resources.findResource(new TextureFile(texture_name, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT)));
	}

	public GUIImage(int width, int height, float u1, float v1, float u2, float v2, Texture texture) {
		this.u1 = u1;
		this.v1 = v1;
		this.u2 = u2;
		this.v2 = v2;
		this.texture = texture;
		if (width <= 0 || height <= 0)
			setDim(texture.getWidth(), texture.getHeight());
		else
			setDim(width, height);
		setCanFocus(false);
	}

    private void initProgram() {
        String vertexSrc =
            "#version 330 core\n" +
            "layout(location = 0) in vec4 v_vertex;\n" +
            "uniform vec2 u_resolution;\n" +
            "uniform vec2 u_pos;\n" +
            "uniform vec2 u_size;\n" +
            "out vec2 f_uv;\n" +
            "void main() {\n" +
            "    vec2 pos = 2.0 * vec2(u_pos.x / u_resolution.x, u_pos.y / u_resolution.y) - vec2(1.0, 1.0);\n" +
            "    vec2 size = 2.0 * (vec2(u_size.x / u_resolution.x, u_size.y / u_resolution.y));\n" +
            "    gl_Position.xy = pos + vec2(v_vertex.z * size.x, v_vertex.w * size.y);\n" +
            "    gl_Position.z = 0.0;\n" +
            "    gl_Position.w = 1.0;\n" +
            "    f_uv = v_vertex.xy;\n" +
            "}";

        String fragmentSrc = 
            "#version 330 core\n" +
            "uniform sampler2D u_tex;\n" +
            "in vec2 f_uv;\n" +
            "out vec4 color;\n" +
            "void main() {\n" +
            "    color = texture(u_tex, f_uv);\n" +
            "}";

        int vertexShader = GL33.glCreateShader(GL33.GL_VERTEX_SHADER);
        GL33.glShaderSource(vertexShader, vertexSrc);
        GL33.glCompileShader(vertexShader);
        checkCompileStatus(vertexShader);

        int fragmentShader = GL33.glCreateShader(GL33.GL_FRAGMENT_SHADER);
        GL33.glShaderSource(fragmentShader, fragmentSrc);
        GL33.glCompileShader(fragmentShader);
        checkCompileStatus(fragmentShader);

        program = GL33.glCreateProgram();
        GL33.glAttachShader(program, vertexShader);
        GL33.glAttachShader(program, fragmentShader);
        GL33.glLinkProgram(program);
        checkLinkStatus(program);

        GL33.glDeleteShader(vertexShader);
        GL33.glDeleteShader(fragmentShader);

        GL33.glUseProgram(program);
        var_tex = GL33.glGetUniformLocation(program, "u_tex");
        var_size = GL33.glGetUniformLocation(program, "u_size");
        var_pos = GL33.glGetUniformLocation(program, "u_pos");
        var_resolution = GL33.glGetUniformLocation(program, "u_resolution");
        vao = createVAO();
    }
    
    private int createVAO() {
        float[] vertex = {
            u1, v1, 0.0f, 0.0f,
            u1, v2, 0.0f, 1.0f,
            u2, v1, 1.0f, 0.0f,
            u2, v2, 1.0f, 1.0f
        };

        int vao = GL33.glGenVertexArrays();
        int vbo = GL33.glGenBuffers();

        GL33.glBindVertexArray(vao);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, vertex, GL33.GL_STATIC_DRAW);

        GL33.glVertexAttribPointer(0, 4, GL33.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL33.glEnableVertexAttribArray(0);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        GL33.glBindVertexArray(0);

        return vao;
    }

    private void checkCompileStatus(int shader) {
        if (GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS) == GL33.GL_FALSE) {
            System.out.println("Shader error: " + GL33.glGetShaderInfoLog(shader));
        }
    }

    private void checkLinkStatus(int prog) {
        if (GL33.glGetProgrami(prog, GL33.GL_LINK_STATUS) == GL33.GL_FALSE) {
            System.out.println("Shader program link error: " + GL33.glGetProgramInfoLog(prog));
        }
    }

    public final void renderGeometry() {
        if (program == -1) {
            initProgram();
        }

        GL33.glUseProgram(program);
        GL33.glDisable(GL33.GL_CULL_FACE);
        GL33.glEnable(GL33.GL_BLEND);
        GL33.glDisable(GL33.GL_DEPTH_TEST);
        GL33.glEnable(GL33.GL_TEXTURE_2D);
        GL33.glActiveTexture(GL33.GL_TEXTURE0);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, texture.getHandle());
        GL33.glUniform1i(var_tex, 0);
        GL33.glUniform2fv(var_size, new float[]{getWidth(), getHeight()});
        GL33.glUniform2fv(var_resolution, new float[]{Display.getWidth(), Display.getHeight()});
        GL33.glUniform2fv(var_pos, new float[]{getX(), getY()});
        GL33.glBindVertexArray(vao);
        GL33.glDrawArrays(GL33.GL_TRIANGLE_STRIP, 0, 4);
	}

    @Override
    protected int getCursorIndex() {
        if(isHovered()) {
            return GUIRoot.CURSOR_TARGET;
        }
        return GUIRoot.CURSOR_NORMAL;
    }
}

