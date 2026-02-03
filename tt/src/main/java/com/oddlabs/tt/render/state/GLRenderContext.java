package com.oddlabs.tt.render.state;

import com.oddlabs.tt.global.Settings;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.util.Arrays;
import java.util.logging.Logger;

public final class GLRenderContext implements RenderContext {
    private static final Logger logger = Logger.getLogger(GLRenderContext.class.getName());
    private static final ScopedState NO_OP = () -> {};

    private @NonNull BlendMode currentBlend = BlendMode.NONE;
    private @NonNull DepthMode currentDepth = DepthMode.NONE;
    private @NonNull CullMode currentCull = CullMode.NONE;
    private int currentDepthFunc = GL11.GL_LEQUAL;
    private boolean scissorEnabled = false;
    
    private boolean maskR = true, maskG = true, maskB = true, maskA = true;
    
    private int activeTextureUnit = -1;
    private final int[] boundTextures = new int[8];
    
    private int globalUbo = 0;
    private static final int GLOBAL_UBO_BINDING = 0;

    public GLRenderContext() {
        Arrays.fill(boundTextures, -1);
    }

    @Override
    public void init() {
        if (globalUbo != 0) return;
        this.globalUbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, globalUbo);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, 1024, GL15.GL_DYNAMIC_DRAW); // Pre-allocate 1KB
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, GLOBAL_UBO_BINDING, globalUbo);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    @Override
    public void updateGlobalState(java.nio.@NonNull ByteBuffer data) {
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, globalUbo);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, data);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }


    // Resets shadow state, forcing next set* call to talk to GL
    public void reset() {
        currentBlend = BlendMode.NONE;
        currentDepth = DepthMode.NONE;
        currentCull = CullMode.NONE;
        currentDepthFunc = -1;
        scissorEnabled = false; // We can't know for sure, but usually we start disabled
        GL11.glDisable(GL11.GL_SCISSOR_TEST); // Ensure consistent start
        
        maskR = maskG = maskB = maskA = true;
        
        activeTextureUnit = -1;
        Arrays.fill(boundTextures, -1);
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
        if (Settings.getSettings().view_samples > 0) {
            GL13.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            GL13.glDisable(GL13.GL_MULTISAMPLE);
        }

        // Blend
        setBlendMode(BlendMode.ALPHA);
        
        // Clear State
        clearColor(0f, 0f, 0f, 0f);
        GL11.glClearDepth(1.0);
        clear(true, false);
    }
    
    @Override
    public void validate() {
        // ... verify blend, depth test, depth mask, cull face ...
        
        // Verify Depth Func
        int glDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        if (glDepthFunc != currentDepthFunc) {
            // If tracked is -1 (unknown), sync it instead of crashing
            if (currentDepthFunc == -1) {
                currentDepthFunc = glDepthFunc;
            } else {
                logger.severe("Depth Func Mismatch: Tracked=" + currentDepthFunc + ", GL=" + glDepthFunc);
                // Try to recover
                currentDepthFunc = glDepthFunc;
            }
        }

        // ... verify textures ...
    }

    @Override
    public void setActiveTexture(int unit) {
        if (activeTextureUnit != unit) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            activeTextureUnit = unit;
        }
    }

    @Override
    public void setTexture(int unit, int textureHandle) {
        if (unit < 0 || unit >= boundTextures.length) return;
        
        setActiveTexture(unit);
        
        if (boundTextures[unit] != textureHandle) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureHandle);
            boundTextures[unit] = textureHandle;
        }
    }

    @Override
    public void setBlendMode(@NonNull BlendMode mode) {
        if (this.currentBlend == mode) return;
        this.currentBlend = mode;
        mode.apply();
    }

    @Override
    public void setDepthMode(@NonNull DepthMode mode) {
        if (this.currentDepth == mode) return;
        this.currentDepth = mode;
        mode.apply();
    }

    @Override
    public void setCullMode(@NonNull CullMode mode) {
        if (this.currentCull == mode) return;
        this.currentCull = mode;
        mode.apply();
    }

    @Override
    public void setDepthFunc(int func) {
        if (currentDepthFunc == func) return;
        GL11.glDepthFunc(func);
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            // Log error instead of crashing to allow recovery
            logger.severe("glDepthFunc produced error: " + error + " (0x" + Integer.toHexString(error) + ")");
            logger.severe("Invalid depth func value: " + func);
            
            // Do NOT update currentDepthFunc if the call failed, GL state didn't change.
            return;
        }
        currentDepthFunc = func;
    }

    @Override
    public void setColorMask(boolean r, boolean g, boolean b, boolean a) {
        if (maskR == r && maskG == g && maskB == b && maskA == a) return;
        GL11.glColorMask(r, g, b, a);
        maskR = r;
        maskG = g;
        maskB = b;
        maskA = a;
    }

    @Override
    public void setBlendFunc(int src, int dst) {
        GL11.glBlendFunc(src, dst);
        this.currentBlend = BlendMode.CUSTOM; // Invalidate shadow state
    }

    @Override
    public void setScissor(int x, int y, int w, int h) {
        if (!scissorEnabled) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            scissorEnabled = true;
        }
        GL11.glScissor(x, y, w, h);
    }

    @Override
    public void clearScissor() {
        if (scissorEnabled) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            scissorEnabled = false;
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

    // Scoped State Implementations

    @Override
    public @NonNull ScopedState withBlendMode(@NonNull BlendMode mode) {
        BlendMode previous = this.currentBlend;
        setBlendMode(mode);
        return () -> setBlendMode(previous != null ? previous : BlendMode.NONE);
    }

    @Override
    public @NonNull ScopedState withDepthMode(@NonNull DepthMode mode) {
        DepthMode previous = this.currentDepth;
        setDepthMode(mode);
        return () -> setDepthMode(previous != null ? previous : DepthMode.READ_WRITE);
    }

    @Override
    public @NonNull ScopedState withCullMode(@NonNull CullMode mode) {
        if (this.currentCull == mode) return NO_OP;
        CullMode previous = this.currentCull;
        setCullMode(mode);
        return () -> setCullMode(previous);
    }

    @Override
    public @NonNull ScopedState withColorMask(boolean r, boolean g, boolean b, boolean a) {
        if (maskR == r && maskG == g && maskB == b && maskA == a) return NO_OP;
        boolean pr = maskR, pg = maskG, pb = maskB, pa = maskA;
        setColorMask(r, g, b, a);
        return () -> setColorMask(pr, pg, pb, pa);
    }

    @Override
    public @NonNull ScopedState withDepthFunc(int func) {
        if (currentDepthFunc == func) return NO_OP;
        int previous = currentDepthFunc;
        setDepthFunc(func);
        return () -> setDepthFunc(previous);
    }
}
