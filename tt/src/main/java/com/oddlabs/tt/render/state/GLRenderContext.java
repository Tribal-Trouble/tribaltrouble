package com.oddlabs.tt.render.state;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.oddlabs.tt.util.GLUtils.checkAndThrow;
import static com.oddlabs.tt.util.GLUtils.checkGLError;

/**
 * RenderContext implementation for LWJGL OpenGL bindings.
 * Manages OpenGL state transitions and provides shadowing to minimize redundant GL calls.
 */
public final class GLRenderContext implements RenderContext {
    private static final Logger logger = Logger.getLogger(GLRenderContext.class.getName());
    private static final ScopedState NO_OP = () -> {
    };

    private enum GLState {
        UNKNOWN,
        FALSE,
        TRUE;

        static GLState from(boolean value) {
            return value ? TRUE : FALSE;
        }

        boolean isTrue() {
            return this == TRUE;
        }
    }

    private @NonNull BlendMode currentBlend = BlendMode.NONE;
    private @NonNull DepthMode currentDepth = DepthMode.NONE;
    private @NonNull CullMode currentCull = CullMode.NONE;
    private int currentDepthFunc = -1;
    private @NonNull GLState sampleAlphaToCoverageEnabled = GLState.UNKNOWN;
    private @NonNull GLState depthTestEnabled = GLState.UNKNOWN;
    private @NonNull GLState depthMaskEnabled = GLState.UNKNOWN;
    private @NonNull GLState blendEnabled = GLState.UNKNOWN;
    private @NonNull GLState cullFaceEnabled = GLState.UNKNOWN;
    private @NonNull GLState framebufferSrgbEnabled = GLState.UNKNOWN;

    private @NonNull GLState maskR = GLState.UNKNOWN;
    private @NonNull GLState maskG = GLState.UNKNOWN;
    private @NonNull GLState maskB = GLState.UNKNOWN;
    private @NonNull GLState maskA = GLState.UNKNOWN;

    private int activeTextureUnit = -1;
    private final int[] boundTextures = new int[32];
    private final int[] boundTargets = new int[32];
    private final int[] boundBuffers = new int[2]; // 0: ARRAY_BUFFER, 1: ELEMENT_ARRAY_BUFFER

    private int globalUbo = 0;
    private static final int GLOBAL_UBO_BINDING = 0;

    private record FboCacheEntry(boolean hasAttachment0, boolean hasAttachment1) {
    }

    private final Map<@NonNull Integer, @NonNull FboCacheEntry> fboCache = new HashMap<>();

    public GLRenderContext() {
        Arrays.fill(boundTextures, -1);
        Arrays.fill(boundTargets, -1);
        Arrays.fill(boundBuffers, -1);
    }

    @Override
    public void init() {
        checkGLError("RenderContext.init entry");
        if (globalUbo == 0) {
            this.globalUbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, globalUbo);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, 1024, GL15.GL_DYNAMIC_DRAW); // Pre-allocate 1KB
            GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, GLOBAL_UBO_BINDING, globalUbo);
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
        }
        // Synchronize FBO state
        currentFBO = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        checkGLError("RenderContext.init complete");
    }

    @Override
    public void updateGlobalState(java.nio.@NonNull ByteBuffer data) {
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, globalUbo);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }


    // Resets shadow state, forcing next set* call to talk to GL
    public void reset() {
        fboCache.clear();
        currentBlend = BlendMode.NONE;
        currentDepth = DepthMode.NONE;
        currentCull = CullMode.NONE;
        currentDepthFunc = -1;

        maskR = GLState.UNKNOWN;
        maskG = GLState.UNKNOWN;
        maskB = GLState.UNKNOWN;
        maskA = GLState.UNKNOWN;

        activeTextureUnit = -1;
        for (int i = 0; i < boundTextures.length; i++) {
            setActiveTexture(i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
            boundTextures[i] = -1;
            boundTargets[i] = -1;
        }
        activeTextureUnit = -1;
        currentVAO = -1;
        // Don't reset currentFBO here as we want to maintain the binding across reset()
        // unless it's genuinely unknown.
        if (currentFBO == -1) {
            currentFBO = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        }

        currentBlendSrc = GL11.GL_ONE;
        currentBlendDst = GL11.GL_ZERO;
        currentBlendEquation = GL14.GL_FUNC_ADD;
        Arrays.fill(boundTextures, -1);
        Arrays.fill(boundTargets, -1);
        Arrays.fill(boundBuffers, -1);

        blendEnabled = GLState.UNKNOWN;
        depthTestEnabled = GLState.UNKNOWN;
        depthMaskEnabled = GLState.UNKNOWN;
        cullFaceEnabled = GLState.UNKNOWN;
        currentCullFaceMode = -1;
        sampleAlphaToCoverageEnabled = GLState.UNKNOWN;
        framebufferSrgbEnabled = GLState.UNKNOWN;
        maskState = GLState.UNKNOWN;

        viewX = -1;
        viewY = -1;
        // Keep viewW and viewH as they are, so getters return valid (if potentially stale) values.
        // Shadow state update will still be triggered by viewX/viewY being -1.
    }

    @Override
    public void applyDefaults() {
        reset();

        GL11.glFrontFace(GL11.GL_CCW);

        // Culling
        setCullMode(CullMode.BACK); // Implies Enable CULL_FACE, CULL_BACK

        // Color Mask
        setColorMask(true, true, true, true);

        // Pixel Store (Packing/Unpacking)
        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, 0);

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, 0);

        // Depth
        setDepthMode(DepthMode.READ_WRITE); // Enable Test + Mask True
        setDepthFunc(GL11.GL_LEQUAL);

        // Multisample
        if (Renderer.getRenderer().getSettings().view_samples > 0) {
            GL13.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            GL13.glDisable(GL13.GL_MULTISAMPLE);
        }

        // Blend
        setBlendMode(BlendMode.ALPHA);

        // Framebuffer sRGB
        setFramebufferSrgb(true);

        // Draw Buffers
        setDrawBuffers(true);

        // Clear State
        clearColor(0f, 0f, 0f, 0f);
        GL11.glClearDepth(1.0);
        clear(true, false);
    }

    @Override
    public void validate() {
        checkAndThrow("State Validation Pre-Check");

        // Verify Depth Func
        int glDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        if (currentDepthFunc != -1 && glDepthFunc != currentDepthFunc) {
            logger.severe("Depth Func Mismatch: Tracked=" + currentDepthFunc + ", GL=" + glDepthFunc);
        }

        // Verify Draw Buffers
        int drawBuffer0 = GL11.glGetInteger(GL30.GL_DRAW_BUFFER0);
        if (maskState == GLState.TRUE) {
            if (drawBuffer0 != GL30.GL_COLOR_ATTACHMENT0) {
                logger.severe("Draw Buffer 0 Mismatch: Expected ATTACHMENT0, GL=" + drawBuffer0);
            }
        } else if (maskState == GLState.FALSE) {
            if (currentFBO == 0) {
                if (drawBuffer0 != GL11.GL_BACK_LEFT && drawBuffer0 != GL11.GL_BACK) {
                    logger.severe("Draw Buffer 0 Mismatch: Expected BACK_LEFT or BACK, GL=" + drawBuffer0);
                }
            } else {
                if (drawBuffer0 != GL30.GL_COLOR_ATTACHMENT0) {
                    logger.severe("Draw Buffer 0 Mismatch: Expected ATTACHMENT0, GL=" + drawBuffer0);
                }
            }
        }

        checkAndThrow("State Validation Post-Check");
    }

    @Override
    public void setActiveTexture(int unit) {
        if (unit < 0 || unit >= boundTextures.length) {
            throw new IllegalArgumentException("Texture unit " + unit + " is out of bounds (max " + boundTextures.length
                    + ").");
        }
        if (activeTextureUnit != unit) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            activeTextureUnit = unit;
        }
    }

    @Override
    public void setTexture(int unit, int textureHandle) {
        setTexture(unit, textureHandle, GL11.GL_TEXTURE_2D);
    }

    @Override
    public void setTexture(int unit, int textureHandle, int target) {
        if (unit < 0 || unit >= boundTextures.length) {
            throw new IllegalArgumentException("Texture unit " + unit + " is out of bounds (max " + boundTextures.length
                    + ").");
        }

        if (boundTextures[unit] != textureHandle || boundTargets[unit] != target) {
            setActiveTexture(unit);
            GL11.glBindTexture(target, textureHandle);
            boundTextures[unit] = textureHandle;
            boundTargets[unit] = target;
            checkAndThrow("glBindTexture");
        }
    }

    @Override
    public void setBlendMode(@NonNull BlendMode mode) {
        if (this.currentBlend == mode) return;
        this.currentBlend = mode;
        mode.apply(this);
    }

    @Override
    public void setDepthMode(@NonNull DepthMode mode) {
        if (this.currentDepth == mode) return;
        this.currentDepth = mode;
        mode.apply(this);
    }

    @Override
    public void setCullMode(@NonNull CullMode mode) {
        if (this.currentCull == mode) return;
        this.currentCull = mode;
        mode.apply(this);
    }

    @Override
    public void setBlend(boolean enabled) {
        GLState state = GLState.from(enabled);
        if (blendEnabled == state) return;
        if (enabled) {
            GL11.glEnable(GL11.GL_BLEND);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        blendEnabled = state;
    }

    @Override
    public void setDepthTest(boolean enabled) {
        GLState state = GLState.from(enabled);
        if (depthTestEnabled == state) return;
        if (enabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        depthTestEnabled = state;
    }

    @Override
    public void setDepthMask(boolean enabled) {
        GLState state = GLState.from(enabled);
        if (depthMaskEnabled == state) return;
        GL11.glDepthMask(enabled);
        depthMaskEnabled = state;
    }

    @Override
    public void setCullFace(boolean enabled) {
        GLState state = GLState.from(enabled);
        if (cullFaceEnabled == state) return;
        if (enabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        cullFaceEnabled = state;
    }

    private int currentCullFaceMode = GL11.GL_BACK;

    @Override
    public void setCullFaceMode(int mode) {
        if (currentCullFaceMode == mode) return;
        GL11.glCullFace(mode);
        currentCullFaceMode = mode;
    }

    @Override
    public void setSampleAlphaToCoverage(boolean enabled) {
        GLState state = GLState.from(enabled);
        if (sampleAlphaToCoverageEnabled == state) return;
        if (enabled) {
            GL11.glEnable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
        } else {
            GL11.glDisable(GL13.GL_SAMPLE_ALPHA_TO_COVERAGE);
        }
        sampleAlphaToCoverageEnabled = state;
    }

    @Override
    public void setDepthFunc(int func) {
        if (currentDepthFunc == func) return;
        if (func != -1) {
            GL11.glDepthFunc(func);
        }
        currentDepthFunc = func;
    }

    @Override
    public void setColorMask(boolean r, boolean g, boolean b, boolean a) {
        GLState sr = GLState.from(r), sg = GLState.from(g), sb = GLState.from(b), sa = GLState.from(a);
        if (maskR == sr && maskG == sg && maskB == sb && maskA == sa) return;
        GL11.glColorMask(r, g, b, a);
        maskR = sr;
        maskG = sg;
        maskB = sb;
        maskA = sa;
    }

    private int currentBlendSrc = -1;
    private int currentBlendDst = -1;
    private int currentBlendEquation = GL14.GL_FUNC_ADD;

    @Override
    public void setBlendFunc(int src, int dst) {
        if (currentBlendSrc == src && currentBlendDst == dst) return;
        GL11.glBlendFunc(src, dst);
        currentBlendSrc = src;
        currentBlendDst = dst;
    }

    @Override
    public void setBlendEquation(int equation) {
        if (currentBlendEquation == equation) return;
        GL14.glBlendEquation(equation);
        currentBlendEquation = equation;
    }

    @Override
    public void resetBlendFunc() {
        if (currentBlendSrc != -1 && currentBlendDst != -1) {
            GL11.glBlendFunc(currentBlendSrc, currentBlendDst);
        }
    }

    private int viewX = -1;
    private int viewY = -1;
    private int viewW = SerializableDisplayMode.MIN_WIDTH;
    private int viewH = SerializableDisplayMode.MIN_HEIGHT;

    @Override
    public void setViewport(int x, int y, int w, int h) {
        if (viewX == x && viewY == y && viewW == w && viewH == h) return;
        GL11.glViewport(x, y, w, h);
        checkGLError("glViewport()");
        viewX = x;
        viewY = y;
        viewW = w;
        viewH = h;
    }

    @Override
    public int getViewportWidth() {
        return viewW;
    }

    @Override
    public int getViewportHeight() {
        return viewH;
    }

    private int currentVAO = -1;

    @Override
    public void bindVertexArray(int vao) {
        if (currentVAO == vao) return;
        GL30.glBindVertexArray(vao);
        currentVAO = vao;
        // Invalidate ELEMENT_ARRAY_BUFFER shadow as it's part of VAO state
        boundBuffers[1] = -1;
    }

    private int currentFBO = -1;

    @Override
    public void bindFramebuffer(int target, int framebuffer) {
        // Only return early if we are binding GL_FRAMEBUFFER and both targets are already correct.
        // If we previously only bound GL_DRAW_FRAMEBUFFER, a bind to GL_FRAMEBUFFER still needs to update GL_READ_FRAMEBUFFER.
        if (target == GL30.GL_FRAMEBUFFER && currentFBO == framebuffer) return;

        GL30.glBindFramebuffer(target, framebuffer);
        if (target == GL30.GL_FRAMEBUFFER || target == GL30.GL_DRAW_FRAMEBUFFER) {
            currentFBO = framebuffer;
            // Draw buffer state is per-FBO, so we must invalidate our shadow
            maskState = GLState.UNKNOWN;
        }
    }

    @Override
    public void bindBuffer(int target, int buffer) {
        int index = (target == GL15.GL_ARRAY_BUFFER) ? 0 : (target == GL15.GL_ELEMENT_ARRAY_BUFFER) ? 1 : -1;
        if (index != -1) {
            if (boundBuffers[index] == buffer) return;
            boundBuffers[index] = buffer;
        }
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void invalidateTexture(int handle) {
        for (int i = 0; i < boundTextures.length; i++) {
            if (boundTextures[i] == handle) {
                boundTextures[i] = -1;
            }
        }
    }

    @Override
    public void invalidateBuffer(int handle) {
        for (int i = 0; i < boundBuffers.length; i++) {
            if (boundBuffers[i] == handle) {
                boundBuffers[i] = -1;
            }
        }
    }

    @Override
    public void invalidateVertexArray(int handle) {
        if (currentVAO == handle) {
            currentVAO = -1;
            // ELEMENT_ARRAY_BUFFER is also part of VAO state
            boundBuffers[1] = -1;
        }
    }

    @Override
    public void invalidateFramebuffer(int handle) {
        fboCache.remove(handle);
        if (currentFBO == handle) {
            currentFBO = -1;
            maskState = GLState.UNKNOWN;
        }
    }

    @Override
    public void clearColor(float r, float g, float b, float a) {
        GL11.glClearColor(r, g, b, a);
    }

    @Override
    public void clear(boolean color, boolean depth) {
        int mask = 0;
        if (color) mask |= GL11.GL_COLOR_BUFFER_BIT;
        if (depth) mask |= GL11.GL_DEPTH_BUFFER_BIT;
        if (mask != 0) GL11.glClear(mask);
    }

    private GLState maskState = GLState.UNKNOWN;

    @Override
    public void setDrawBuffers(boolean mask) {
        GLState newState = GLState.from(mask);
        // We probe GL_DRAW_FRAMEBUFFER_BINDING to ensure we are truly in sync.
        int actualFBO = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        // If state is already cached and FBO hasn't changed behind our back, return.
        if (maskState == newState && actualFBO == currentFBO) return;

        currentFBO = actualFBO;

        if (currentFBO == 0) {
            GL11.glDrawBuffer(GL11.GL_BACK);
            maskState = GLState.FALSE;
            return;
        }

        FboCacheEntry entry = fboCache.get(currentFBO);
        if (entry == null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                stack.push();
                IntBuffer params = stack.mallocInt(1);

                // Probing for color attachments. We initialize params to GL_NONE to be safe.
                params.put(0, GL11.GL_NONE);
                GL30.glGetFramebufferAttachmentParameteriv(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                        GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, params);
                boolean hasAttachment0 = params.get(0) != GL11.GL_NONE;

                params.put(0, GL11.GL_NONE);
                GL30.glGetFramebufferAttachmentParameteriv(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1,
                        GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, params);
                boolean hasAttachment1 = params.get(0) != GL11.GL_NONE;
                stack.pop();

                entry = new FboCacheEntry(hasAttachment0, hasAttachment1);
                fboCache.put(currentFBO, entry);
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buffers = mask && entry.hasAttachment0 && entry.hasAttachment1
                    ? stack.ints(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1)
                    : stack.ints(entry.hasAttachment0 ? GL30.GL_COLOR_ATTACHMENT0 : GL11.GL_NONE);

            // Clear any existing errors before the critical call to isolate the failure
            GL11.glGetError();
            GL20.glDrawBuffers(buffers);
            checkAndThrow("glDrawBuffers(" + mask + ") FBO=" + currentFBO + " (att0=" + entry.hasAttachment0
                    + ", att1=" + entry.hasAttachment1 + ")");
        }
        maskState = newState;
    }

    @Override
    public void setDrawBuffers(int @NonNull [] attachments) {
        currentFBO = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        if (currentFBO == 0) {
            GL11.glDrawBuffer(GL11.GL_BACK);
            maskState = GLState.FALSE;
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL20.glDrawBuffers(stack.ints(attachments));
            checkAndThrow("glDrawBuffers(int[])");
        }
        // Sync maskState if it matches one of our known configurations
        // Treating GL_NONE as a form of disabled masking for simple FBOs
        maskState = attachments.length == 2 && attachments[0] == GL30.GL_COLOR_ATTACHMENT0 && attachments[1]
                == GL30.GL_COLOR_ATTACHMENT1
                ? GLState.TRUE
                : attachments.length == 1 && attachments[0] == GL30.GL_COLOR_ATTACHMENT0
                        ? GLState.FALSE
                : attachments.length == 1 && attachments[0] == GL11.GL_NONE
                        ? GLState.FALSE
                : GLState.UNKNOWN;
    }

    @Override
    public void setFramebufferSrgb(boolean enabled) {
        GLState state = GLState.from(enabled);
        if (framebufferSrgbEnabled == state) return;
        if (enabled) {
            GL11.glEnable(GL30.GL_FRAMEBUFFER_SRGB);
        } else {
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
        }
        framebufferSrgbEnabled = state;
    }

    // Scoped State Implementations

    @Override
    public @NonNull ScopedState withBlendMode(@NonNull BlendMode mode) {
        BlendMode previous = this.currentBlend;
        setBlendMode(mode);
        return () -> setBlendMode(previous);
    }

    @Override
    public @NonNull ScopedState withDepthMode(@NonNull DepthMode mode) {
        DepthMode previous = this.currentDepth;
        setDepthMode(mode);
        return () -> setDepthMode(previous);
    }

    @Override
    public @NonNull ScopedState withCullMode(@NonNull CullMode mode) {
        CullMode previous = this.currentCull;
        setCullMode(mode);
        return () -> setCullMode(previous);
    }

    @Override
    public @NonNull ScopedState withSampleAlphaToCoverage(boolean enabled) {
        GLState prevState = sampleAlphaToCoverageEnabled;
        setSampleAlphaToCoverage(enabled);
        return () -> {
            if (prevState != GLState.UNKNOWN) {
                setSampleAlphaToCoverage(prevState.isTrue());
            } else {
                sampleAlphaToCoverageEnabled = GLState.UNKNOWN;
                setSampleAlphaToCoverage(false); // Default to disabled on restoration from unknown
            }
        };
    }

    @Override
    public @NonNull ScopedState withColorMask(boolean r, boolean g, boolean b, boolean a) {
        GLState pr = maskR;
        GLState pg = maskG;
        GLState pb = maskB;
        GLState pa = maskA;
        setColorMask(r, g, b, a);
        return () -> {
            if (pr != GLState.UNKNOWN && pg != GLState.UNKNOWN && pb != GLState.UNKNOWN && pa != GLState.UNKNOWN) {
                setColorMask(pr.isTrue(), pg.isTrue(), pb.isTrue(), pa.isTrue());
            } else {
                maskR = GLState.UNKNOWN;
                maskG = GLState.UNKNOWN;
                maskB = GLState.UNKNOWN;
                maskA = GLState.UNKNOWN;
                setColorMask(true, true, true, true); // Safe default
            }
        };
    }

    @Override
    public @NonNull ScopedState withDepthFunc(int func) {
        if (currentDepthFunc == func) return NO_OP;
        int previous = currentDepthFunc;
        setDepthFunc(func);
        return () -> {
            if (previous != -1) setDepthFunc(previous);
            else {
                currentDepthFunc = -1;
                setDepthFunc(GL11.GL_LEQUAL); // Force restoration to safe default
            }
        };
    }

    @Override
    public @NonNull ScopedState withDrawBuffers(boolean mask) {
        GLState previousState = maskState;
        int previousFBO = currentFBO;
        setDrawBuffers(mask);
        return () -> {
            if (currentFBO != previousFBO) {
                // FBO changed, restoration might be invalid for current FBO
                maskState = GLState.UNKNOWN;
                return;
            }
            if (previousState != GLState.UNKNOWN) {
                setDrawBuffers(previousState.isTrue());
            } else {
                // If it was unknown, we force it back to UNKNOWN and ensure the next call actually updates GL
                maskState = GLState.UNKNOWN;
                setDrawBuffers(true); // Force sync to a known default
            }
        };
    }

    @Override
    public @NonNull ScopedState withFramebufferSrgb(boolean enabled) {
        GLState previousState = framebufferSrgbEnabled;
        setFramebufferSrgb(enabled);
        return () -> {
            if (previousState != GLState.UNKNOWN) {
                setFramebufferSrgb(previousState.isTrue());
            } else {
                framebufferSrgbEnabled = GLState.UNKNOWN;
                setFramebufferSrgb(true); // Safe linear default for the project
            }
        };
    }
}
