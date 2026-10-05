package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.render.shader.FogShader;
import com.oddlabs.tt.render.shader.ShaderProgram;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.render.state.ScopedState;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Another player's camera in a shared session, drawn where it is and turned the way it looks: an old film camera with
 * two reels on top and a lens in front, in the player's colour, and a faint cone of view ahead of the lens.
 *
 * <p>The model is built here from boxes and prisms, a metre or two long, with flat shaded faces. Further away it is
 * drawn larger, so a camera far off still shows.
 */
final class CameraModel implements AutoCloseable {
    /** Meters the model is scaled to near by, and the share of the distance it grows by further away. */
    private static final float NEAR_SCALE = 1.2f;
    private static final float DISTANCE_SCALE = .018f;
    private static final float VIEW_ALPHA = .16f;
    private static final int SEGMENTS = 12;
    // Position (3), normal (3) and how much of the player's colour it takes (1).
    private static final int FLOATS_PER_VERTEX = 7;

    private static final String VERTEX_SHADER = """
            #version 410 core
            """ + ShaderProgram.GLOBAL_STATE_BLOCK + """
            layout(location = 0) in vec3 in_Position;
            layout(location = 1) in vec3 in_Normal;
            layout(location = 2) in float in_Tint;

            uniform mat4 u_Model;

            out vec3 v_normal;
            out float v_tint;
            out float v_fogDist;

            void main() {
                vec4 viewPosition = u_viewMatrix * (u_Model * vec4(in_Position, 1.0));
                gl_Position = u_projectionMatrix * viewPosition;
                v_normal = mat3(u_Model) * in_Normal;
                v_tint = in_Tint;
                v_fogDist = length(viewPosition.xyz);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 410 core
            """ + ShaderProgram.GLOBAL_STATE_BLOCK + FogShader.FOG_FUNCTION + """
            uniform vec3 u_Color;
            uniform float u_Alpha;
            uniform float u_Lit;

            in vec3 v_normal;
            in float v_tint;
            in float v_fogDist;

            layout(location = 0) out vec4 out_FragColor;

            void main() {
                vec3 base = mix(vec3(0.12, 0.12, 0.14), u_Color, v_tint);
                float light = 1.0;
                if (u_Lit > 0.5) {
                    vec3 n = normalize(v_normal);
                    light = 0.4 + 0.6 * max(dot(n, normalize(vec3(0.35, 0.25, 0.9))), 0.0)
                            + 0.2 * max(dot(n, normalize(vec3(-0.6, -0.4, 0.2))), 0.0);
                }
                // Premultiplied, so fading into the fog fades all of it.
                out_FragColor = vec4(base * light * u_Alpha, u_Alpha) * calculateFogFactor(v_fogDist, gl_FragCoord.xy);
            }
            """;

    private static final class ModelShader extends ShaderProgram {
        ModelShader() {
            super(VERTEX_SHADER, FRAGMENT_SHADER);
            link();
        }
    }

    private final @NonNull ModelShader shader = new ModelShader();
    private final @NonNull VertexArray vao = new VertexArray();
    private final @NonNull FloatVBO vertices;
    // The camera's solid parts, then its cone of view.
    private final int solid_count;
    private final int view_count;
    private final @NonNull Matrix4f model = new Matrix4f();
    private boolean closed;

    CameraModel() {
        List<Float> data = new ArrayList<>();
        Mesh mesh = new Mesh(data);
        // Forward is +x, left +y and up +z; the lens is at the origin's front.
        mesh.box(-1.1f, .45f, -.42f, .42f, -.5f, .4f, 1f);
        // The viewfinder on the side, and a handle on top between the reels.
        mesh.box(-.95f, -.35f, .42f, .58f, -.05f, .22f, 0f);
        mesh.box(-.3f, -.1f, -.08f, .08f, .4f, .62f, 0f);
        mesh.frustum(.45f, .3f, 1.1f, .42f, 0f);
        // Two reels standing on the body.
        mesh.reel(-.62f, .9f, .48f, .14f, .85f);
        mesh.reel(.12f, .82f, .38f, .14f, .85f);
        int solid_floats = data.size();
        // The cone of view ahead of the lens.
        float length = 6f;
        float half_width = 2.4f;
        float half_height = 1.5f;
        Vector3f apex = new Vector3f(1.1f, 0f, 0f);
        Vector3f[] corners = {new Vector3f(1.1f + length, half_width, half_height), new Vector3f(1.1f + length,
                -half_width, half_height), new Vector3f(1.1f + length, -half_width, -half_height), new Vector3f(
                        1.1f + length, half_width, -half_height)};
        for (int i = 0; i < 4; i++)
            mesh.triangle(apex, corners[i], corners[(i + 1) % 4], 1f);
        solid_count = solid_floats / FLOATS_PER_VERTEX;
        view_count = (data.size() - solid_floats) / FLOATS_PER_VERTEX;
        float[] array = new float[data.size()];
        for (int i = 0; i < array.length; i++)
            array[i] = data.get(i);
        vertices = new FloatVBO(GL15.GL_STATIC_DRAW, array);
        vao.bind();
        vertices.makeCurrent();
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 1, GL11.GL_FLOAT, false, stride, 6L * Float.BYTES);
        vao.unbind();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    /** Builds flat shaded triangles into a list of vertex floats. */
    private record Mesh(@NonNull List<Float> data) {
        void triangle(@NonNull Vector3f a, @NonNull Vector3f b, @NonNull Vector3f c, float tint) {
            Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
            for (Vector3f v : new Vector3f[]{a, b, c}) {
                data.add(v.x);
                data.add(v.y);
                data.add(v.z);
                data.add(normal.x);
                data.add(normal.y);
                data.add(normal.z);
                data.add(tint);
            }
        }

        void quad(@NonNull Vector3f a, @NonNull Vector3f b, @NonNull Vector3f c, @NonNull Vector3f d, float tint) {
            triangle(a, b, c, tint);
            triangle(a, c, d, tint);
        }

        void box(float x0, float x1, float y0, float y1, float z0, float z1, float tint) {
            Vector3f[] v = new Vector3f[8];
            for (int i = 0; i < 8; i++)
                v[i] = new Vector3f((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1);
            quad(v[0], v[2], v[3], v[1], tint); // bottom
            quad(v[4], v[5], v[7], v[6], tint); // top
            quad(v[0], v[1], v[5], v[4], tint); // right
            quad(v[2], v[6], v[7], v[3], tint); // left
            quad(v[0], v[4], v[6], v[2], tint); // back
            quad(v[1], v[3], v[7], v[5], tint); // front
        }

        /** A prism round the x axis from x0 with one radius to x1 with another, closed at the far end. */
        void frustum(float x0, float r0, float x1, float r1, float tint) {
            for (int i = 0; i < SEGMENTS; i++) {
                double a = 2 * Math.PI * i / SEGMENTS;
                double b = 2 * Math.PI * (i + 1) / SEGMENTS;
                Vector3f p0 = new Vector3f(x0, r0 * (float) Math.cos(a), r0 * (float) Math.sin(a));
                Vector3f p1 = new Vector3f(x0, r0 * (float) Math.cos(b), r0 * (float) Math.sin(b));
                Vector3f q0 = new Vector3f(x1, r1 * (float) Math.cos(a), r1 * (float) Math.sin(a));
                Vector3f q1 = new Vector3f(x1, r1 * (float) Math.cos(b), r1 * (float) Math.sin(b));
                quad(p0, p1, q1, q0, tint);
                // The glass, a little lighter than the barrel.
                triangle(new Vector3f(x1, 0f, 0f), q0, q1, .25f);
            }
        }

        /** A reel round the y axis, its middle at (x, z). */
        void reel(float x, float z, float radius, float half_width, float tint) {
            for (int i = 0; i < SEGMENTS; i++) {
                double a = 2 * Math.PI * i / SEGMENTS;
                double b = 2 * Math.PI * (i + 1) / SEGMENTS;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                float cb = (float) Math.cos(b), sb = (float) Math.sin(b);
                Vector3f l0 = new Vector3f(x + radius * ca, half_width, z + radius * sa);
                Vector3f l1 = new Vector3f(x + radius * cb, half_width, z + radius * sb);
                Vector3f r0 = new Vector3f(x + radius * ca, -half_width, z + radius * sa);
                Vector3f r1 = new Vector3f(x + radius * cb, -half_width, z + radius * sb);
                quad(r0, r1, l1, l0, tint * .7f);
                triangle(new Vector3f(x, half_width, z), l0, l1, tint);
                triangle(new Vector3f(x, -half_width, z), r1, r0, tint);
            }
        }
    }

    /** Starts drawing cameras; close the batch when done. */
    @NonNull
    Batch begin(@NonNull RenderContext context) {
        return new Batch(context);
    }

    final class Batch implements AutoCloseable {
        private final @NonNull RenderContext context;
        private final @NonNull ScopedState shader_state;

        private Batch(@NonNull RenderContext context) {
            this.context = context;
            shader_state = shader.use();
            // The mask buffer is not ours to write.
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            vao.bind();
        }

        /**
         * Draws a camera.
         *
         * @param eye_distance how far it is from the viewer, in meters, to grow it by
         */
        void draw(float x, float y, float z, float horiz_angle, float vert_angle, @NonNull Vector4fc color,
                float eye_distance) {
            if (closed)
                return;
            float scale = NEAR_SCALE + eye_distance * DISTANCE_SCALE;
            model.translation(x, y, z).rotateZ(horiz_angle).rotateY(-vert_angle).scale(scale);
            shader.setUniform("u_Model", model);
            shader.setUniform("u_Color", color.x(), color.y(), color.z());
            try (ScopedState _ = context.withBlendMode(BlendMode.PREMULTIPLIED); ScopedState _ = context.withDepthMode(
                    DepthMode.READ_WRITE); ScopedState _ = context.withCullMode(CullMode.NONE)) {
                shader.setUniform("u_Alpha", 1f);
                shader.setUniform("u_Lit", 1f);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, solid_count);
            }
            try (ScopedState _ = context.withBlendMode(BlendMode.PREMULTIPLIED); ScopedState _ = context.withDepthMode(
                    DepthMode.READ_ONLY); ScopedState _ = context.withCullMode(CullMode.NONE)) {
                shader.setUniform("u_Alpha", VIEW_ALPHA);
                shader.setUniform("u_Lit", 0f);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, solid_count, view_count);
            }
        }

        @Override
        public void close() {
            vao.unbind();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffers = stack.mallocInt(2);
                buffers.put(GL30.GL_COLOR_ATTACHMENT0).put(GL30.GL_COLOR_ATTACHMENT1).flip();
                GL20.glDrawBuffers(buffers);
            }
            shader_state.close();
        }
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
