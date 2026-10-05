package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Marks where the editable map ends: a see-through wall all the way round on the outermost cells an edit can change,
 * from the ground up to a little above the sea or the land, with a bright band along its top. The cells outside it
 * stay on the sea floor whatever is done to them, and past those the world starts over from the far side.
 */
final class EdgeOverlay implements AutoCloseable {
    /** Meters the wall stands above the sea, or above the land where that is higher. */
    private static final float WALL_HEIGHT = 6f;
    /** Meters of bright band along the top of the wall. */
    private static final float BAND_HEIGHT = 1f;
    private static final float WALL_ALPHA = .3f;
    private static final float BAND_ALPHA = .9f;
    private static final float[] COLOR = {1f, .8f, .15f};
    // Position (3) and opacity (1) per vertex; two quads, wall and band, of two triangles per cell along each side.
    private static final int FLOATS_PER_VERTEX = 4;
    private static final int VERTICES_PER_CELL = 12;

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + ShaderProgram.GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec3 in_Position;
            layout(location = 1) in float in_Alpha;

            out float v_alpha;
            out float v_fogDist;

            void main() {
                vec4 viewPosition = u_viewMatrix * vec4(in_Position, 1.0);
                gl_Position = u_projectionMatrix * viewPosition;
                v_alpha = in_Alpha;
                v_fogDist = length(viewPosition.xyz);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ + ShaderProgram.GLOBAL_STATE_BLOCK + FogShader.FOG_FUNCTION + """
            uniform vec3 u_Color;

            in float v_alpha;
            in float v_fogDist;

            layout(location = 0) out vec4 out_FragColor;

            void main() {
                // Premultiplied, so fading into the fog fades all of it.
                out_FragColor = vec4(u_Color * v_alpha, v_alpha) * calculateFogFactor(v_fogDist, gl_FragCoord.xy);
            }
            """;

    private static final class WallShader extends ShaderProgram {
        WallShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER);
            link();
        }
    }

    private final @NonNull TerrainEditor editor;
    private final float sea_level;
    private final int size;
    private final @NonNull WallShader shader = new WallShader();
    private final @NonNull VertexArray vao = new VertexArray();
    private final @NonNull FloatVBO vertices;
    private final @NonNull FloatBuffer buffer;
    private final int vertex_count;

    private boolean visible;
    private boolean closed;

    EdgeOverlay(@NonNull TerrainEditor editor, float sea_level) {
        this.editor = editor;
        this.sea_level = sea_level;
        this.size = editor.getSize();
        // Each side runs between the corners of the editable square, from cell 1 to the cell before the last.
        vertex_count = 4 * (size - 3) * VERTICES_PER_CELL;
        buffer = BufferUtils.createFloatBuffer(vertex_count * FLOATS_PER_VERTEX);
        vertices = new FloatVBO(GL15.GL_STREAM_DRAW, vertex_count * FLOATS_PER_VERTEX);
        vao.bind();
        vertices.makeCurrent();
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        vao.unbind();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    /** Draws the wall, from a delegate's render3D, over the ground and under the sea drawn after it. */
    void render(@NonNull RenderContext context, @NonNull CameraState state) {
        if (!visible || closed)
            return;
        // The water reflection is drawn from a camera mirrored below the sea; the wall has no place in it.
        if (state.getCurrentZ() < sea_level)
            return;
        fill();
        try (ScopedState _ = shader.use(); ScopedState _ = context.withBlendMode(
                BlendMode.PREMULTIPLIED); ScopedState _ = context.withDepthMode(
                        DepthMode.READ_ONLY); ScopedState _ = context.withCullMode(CullMode.NONE)) {
            shader.setUniform("u_Color", COLOR[0], COLOR[1], COLOR[2]);
            // The mask buffer is not ours to write.
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            vertices.put(buffer);
            vao.bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertex_count);
            vao.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffers = stack.mallocInt(2);
                buffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();
                GL20.glDrawBuffers(buffers);
            }
        }
    }

    /** Lays the wall along the four sides, following the ground as it is now. */
    private void fill() {
        buffer.clear();
        int first = 1;
        int last = size - 2;
        for (int i = first; i < last; i++) {
            side(first, i, first, i + 1);
            side(last, i, last, i + 1);
            side(i, first, i + 1, first);
            side(i, last, i + 1, last);
        }
        buffer.flip();
    }

    /** The wall and its band between two neighbouring cells. */
    private void side(int ax, int ay, int bx, int by) {
        float x0 = ax * HeightMap.METERS_PER_UNIT_GRID;
        float y0 = ay * HeightMap.METERS_PER_UNIT_GRID;
        float x1 = bx * HeightMap.METERS_PER_UNIT_GRID;
        float y1 = by * HeightMap.METERS_PER_UNIT_GRID;
        float ground0 = editor.getHeight(ax, ay);
        float ground1 = editor.getHeight(bx, by);
        float top0 = Math.max(ground0, sea_level) + WALL_HEIGHT;
        float top1 = Math.max(ground1, sea_level) + WALL_HEIGHT;
        float band0 = top0 - BAND_HEIGHT;
        float band1 = top1 - BAND_HEIGHT;
        // Clear at the foot, firmer towards the band.
        quad(x0, y0, x1, y1, ground0, ground1, 0f, band0, band1, WALL_ALPHA);
        quad(x0, y0, x1, y1, band0, band1, BAND_ALPHA, top0, top1, BAND_ALPHA);
    }

    private void quad(float x0, float y0, float x1, float y1, float low0, float low1, float low_alpha, float high0,
            float high1, float high_alpha) {
        vertex(x0, y0, low0, low_alpha);
        vertex(x1, y1, low1, low_alpha);
        vertex(x1, y1, high1, high_alpha);
        vertex(x0, y0, low0, low_alpha);
        vertex(x1, y1, high1, high_alpha);
        vertex(x0, y0, high0, high_alpha);
    }

    private void vertex(float x, float y, float z, float alpha) {
        buffer.put(x).put(y).put(z).put(alpha);
    }

    @Override
    public void close() {
        // The editor keeps drawing while the screen fades out, so later calls must do nothing.
        closed = true;
        shader.close();
        vao.close();
        vertices.close();
    }
}
