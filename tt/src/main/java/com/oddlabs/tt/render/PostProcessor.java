package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.shader.PostProcessShader;
import com.oddlabs.tt.render.state.BlendMode;
import com.oddlabs.tt.render.state.CullMode;
import com.oddlabs.tt.render.state.DepthMode;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Manages the full-screen post-processing pipeline.
 * Handles rendering the scene to an FBO and applying effects via PostProcessShader.
 */
public final class PostProcessor implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(PostProcessor.class.getSimpleName());

    private final @NonNull PostProcessShader shader;
    private final @NonNull VertexArray vao;
    private final @NonNull FloatVBO quadVBO;
    private final @NonNull FBO sceneFBO;
    private final @NonNull FBO depthCopyFBO;
    private int currentWidth;
    private int currentHeight;

    public PostProcessor(int width, int height) {
        this.currentWidth = width;
        this.currentHeight = height;
        this.shader = new PostProcessShader();
        this.sceneFBO = FBO.createSceneFBO(width, height);

        // Depth Copy FBO (for Soft Particles)
        this.depthCopyFBO = new FBO(width, height);
        this.depthCopyFBO.bind();
        Texture depthCopy = new Texture(width, height, GL30.GL_DEPTH_COMPONENT24, GL11.GL_NEAREST, GL11.GL_NEAREST,
                GL12.GL_CLAMP_TO_EDGE);
        this.depthCopyFBO.attachTexture(GL30.GL_DEPTH_ATTACHMENT, depthCopy);
        // This FBO has no color attachment
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        this.depthCopyFBO.checkStatus();
        this.depthCopyFBO.unbind();

        // Setup Full-Screen Quad
        this.vao = new VertexArray();
        this.vao.bind();

        try (var stack = MemoryStack.stackPush()) {
            this.quadVBO = new FloatVBO(GL15.GL_STATIC_DRAW, stack.floats(
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
                    -1.0f, 1.0f,
                    1.0f, 1.0f
            ));
        }

        int posLoc = shader.getAttributeLocation(PostProcessShader.Attributes.POSITION);
        if (posLoc >= 0) {
            GL20.glEnableVertexAttribArray(posLoc);
            quadVBO.vertexAttribPointer(posLoc, 2, 0, 0);
        }

        this.vao.unbind();
    }

    public boolean resize(int width, int height) {
        if (this.currentWidth == width && this.currentHeight == height) return false;
        this.currentWidth = width;
        this.currentHeight = height;
        sceneFBO.resize(width, height);

        depthCopyFBO.resize(width, height);
        depthCopyFBO.bind();
        // Since resize() in FBO.java doesn't handle custom depth-only FBOs cleanly yet,
        // we'll manually ensure it's still color-less.
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        depthCopyFBO.unbind();

        return true;
    }

    public void copyDepthBuffer() {
        sceneFBO.blitDepthTo(depthCopyFBO);
    }

    public @NonNull Texture getDepthCopyTexture() {
        return Objects.requireNonNull(depthCopyFBO.getDepthTexture());
    }

    public void bindSceneFBO(@NonNull RenderContext context) {
        sceneFBO.bind(context);
    }

    public void unbindSceneFBO(@NonNull RenderContext context) {
        sceneFBO.unbind(context);
    }

    public void renderComposite(@NonNull RenderContext context, @NonNull Consumer<
            @NonNull RenderContext> guiRenderCallback) {
        // 1. Render GUI into the Scene FBO (on top of the 3D scene)
        bindSceneFBO(context);

        // Ensure blending is enabled for the GUI pass.
        // Buffer 0 (Color): GL_ONE, GL_ONE_MINUS_SRC_ALPHA (Premultiplied Linear)
        // Buffer 1 (Mask): Wipes unit color proportionally and uses MAX for the marker alpha.
        try (var _ = context.withBlendMode(BlendMode.CUSTOM)) {
            context.setBlend(true);
            GL40.glBlendFunci(0, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // Mask RGB: Wipe background unit color as GUI becomes opaque
            // Mask Alpha: Use MAX to prevent marker (0.5) from accumulating to 1.0
            GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_ADD, GL14.GL_MAX);
            GL40.glBlendFunci(1, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            guiRenderCallback.accept(context);

            // Explicitly reset per-buffer state to prevent leaking into next pass/frame
            GL40.glBlendEquationSeparatei(1, GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            try (var stack = MemoryStack.stackPush()) {
                GL20.glDrawBuffers(stack.ints(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1));
            }
        }

        unbindSceneFBO(context);

        // 2. Composite the FBO to the screen with Post-Processing (CVD, High Contrast, Team Stencil)
        // Render to the default framebuffer (screen)
        context.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        context.setViewport(0, 0, currentWidth, currentHeight);
        context.setDrawBuffers(false); // Ensure only back buffer is active for FBO 0
        context.clear(true, true);

        try (var _ = shader.use(); var _ = context.withBlendMode(BlendMode.NONE); var _ = context.withDepthMode(
                DepthMode.NONE); var _ = context.withCullMode(CullMode.NONE)) {

            Settings settings = Renderer.getRenderer().getSettings();
            shader.setSubroutines(settings.cvd_mode, settings.high_contrast);

            shader.setUniform(PostProcessShader.Uniforms.CVD_INTENSITY, settings.cvd_intensity);
            shader.setUniform(PostProcessShader.Uniforms.CONTRAST_INTENSITY, settings.contrast_intensity);
            shader.setUniform(PostProcessShader.Uniforms.INVERT_COLORS, settings.invert_colours);
            shader.setUniform(PostProcessShader.Uniforms.CONTRAST_BRIGHTNESS, settings.contrast_brightness);
            shader.setUniform(PostProcessShader.Uniforms.CONTRAST_CLARITY, settings.contrast_clarity);
            shader.setUniform(PostProcessShader.Uniforms.TEAM_STENCIL, settings.team_stencil);
            shader.setUniform(PostProcessShader.Uniforms.SCENE_TEXTURE, 0);
            shader.setUniform(PostProcessShader.Uniforms.MASK_TEXTURE, 1);

            context.setTexture(0, sceneFBO.getColorTexture());
            context.setTexture(1, sceneFBO.getMaskTexture());

            vao.bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
            vao.unbind();
        }

        // Unbind textures to prevent feedback loops in next frame
        context.setTexture(0, 0);
        context.setTexture(1, 0);
        context.setTexture(2, 0);
    }

    @Override
    public void close() {
        shader.close();
        sceneFBO.close();
        depthCopyFBO.close();
        vao.close();
        quadVBO.close();
    }
}
