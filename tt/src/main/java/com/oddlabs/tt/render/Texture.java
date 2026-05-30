package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.util.OpenGLException;
import com.oddlabs.util.DXTImage;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A 2D graphic image for drawing
 */
public final class Texture extends NativeResource<Texture.NativeTexture> {
    private static final Logger logger = Logger.getLogger(Texture.class.getSimpleName());

    /**
     * This class is not thread safe.
     */
    static final class NativeTexture extends NativeResource.NativeState {
        static final AtomicInteger global_size = new AtomicInteger();

        private int size;
        private final int texture_handle;

        NativeTexture(int texture_handle) {
            this.texture_handle = texture_handle;
        }

        @Override
        public void close() {
            global_size.addAndGet(-size);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer handle_buffer = stack.mallocInt(1);
                handle_buffer.put(0, texture_handle);
                GL11.glDeleteTextures(handle_buffer);
            }
        }
    }

    private final int width;
    private final int height;

    private static int initTexture(int min_filter, int mag_filter, int wrap_s, int wrap_t, int max_mipmap_level) {
        int tex_handle;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer handle_buffer = stack.mallocInt(1);
            GL11.glGenTextures(handle_buffer);
            tex_handle = handle_buffer.get(0);
            if (tex_handle == 0) {
                List<Integer> errors = GLUtils.checkGLError("glGenTextures");
                String detail = errors.isEmpty() ? "" : ": " + GLUtils.errorToString(errors.getFirst());
                String msg = "Failed to generate OpenGL texture handle" + detail;
                logger.severe(msg);
                throw new OpenGLException(msg);
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex_handle);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap_s);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap_t);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min_filter);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, max_mipmap_level);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag_filter);

            FloatBuffer border_color_buffer = stack.mallocFloat(4);
            border_color_buffer.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 0f);
            GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, border_color_buffer);

            if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                float max_anisotropy = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                        max_anisotropy);
            }
        }
        GLUtils.checkAndThrow("initTexture");
        return tex_handle;
    }

    public static int globalSize() {
        return NativeTexture.global_size.intValue();
    }

    public Texture(@NonNull TextureFile texture_file) {
        this(texture_file.isDXTImage() ? texture_file.getDXTImage() : null, !texture_file.isDXTImage() ? texture_file
                .getImage() : null, texture_file.getInternalFormat(), texture_file.getMinFilter(), texture_file
                        .getMagFilter(), texture_file.getWrapS(), texture_file.getWrapT(), texture_file
                                .getMaxMipmapLevel(), texture_file.getBaseFadeoutLevel(), texture_file
                                        .getFadeoutFactor(), texture_file.hasMaxAlpha());
    }

    private Texture(@Nullable DXTImage dxtImage, @Nullable GLImage image, int internalFormat, int minFilter,
            int magFilter, int wrapS, int wrapT, int maxMipmapLevel, int baseFadeoutLevel, float fadeoutFactor,
            boolean max_alpha) {
        this(dxtImage != null ? dxtImage.getWidth() : image.getWidth(), dxtImage != null ? dxtImage.getHeight() : image
                .getHeight(), minFilter, magFilter, wrapS, wrapT, maxMipmapLevel);

        int total_size;
        if (dxtImage != null) {
            total_size = uploadDXTTexture(dxtImage, internalFormat, maxMipmapLevel);
        } else {
            GLImage[] mipmaps;
            if (minFilter == GL11.GL_LINEAR_MIPMAP_LINEAR || minFilter == GL11.GL_NEAREST_MIPMAP_LINEAR) {
                boolean isWrapping = wrapS == GL11.GL_REPEAT || wrapT == GL11.GL_REPEAT;
                mipmaps = image.buildMipMaps(baseFadeoutLevel, fadeoutFactor, isWrapping, max_alpha);
            } else {
                mipmaps = new GLImage[]{image};
            }
            total_size = uploadTexture(mipmaps, internalFormat, maxMipmapLevel);
        }
        setSize(total_size);
    }

    public Texture(int width, int height, int internal_format) {
        this(width, height, internal_format, GL11.GL_LINEAR, GL11.GL_LINEAR, org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE);
    }

    public Texture(int width, int height, int internal_format, int min_filter, int mag_filter, int wrap) {
        this(width, height, min_filter, mag_filter, wrap, wrap, 1000);
        int format = GL11.GL_RGBA;
        int type = GL11.GL_UNSIGNED_BYTE;

        if (internal_format == GL30.GL_DEPTH_COMPONENT24) {
            format = GL11.GL_DEPTH_COMPONENT;
            type = GL11.GL_FLOAT;
        } else if (internal_format == GL30.GL_RGBA16F) {
            format = GL11.GL_RGBA;
            type = GL30.GL_HALF_FLOAT;
        } else if (internal_format == GL11.GL_RGB || internal_format == GL11.GL_RGB8) {
            format = GL11.GL_RGB;
        } else if (internal_format == GL11.GL_RED || internal_format == GL30.GL_R8) {
            format = GL11.GL_RED;
        }

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internal_format, width, height, 0, format, type,
                (java.nio.ByteBuffer) null);
        GLUtils.checkAndThrow("Texture storage allocation");
        int size = determineMipMapSize(0, internal_format, width, height);
        setSize(size);
    }

    public Texture(int width, int height, int min_filter, int mag_filter, int wrap_s, int wrap_t,
            int max_mipmap_level) throws IllegalArgumentException {
        super(new NativeTexture(initTexture(min_filter, mag_filter, wrap_s, wrap_t, max_mipmap_level)));
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive.");
        }
        this.width = width;
        this.height = height;
    }

    public Texture(@NonNull GLImage image, int internal_format, int min_filter, int mag_filter, int wrap_s,
            int wrap_t) throws IllegalArgumentException, NullPointerException {
        this(image.createMipMaps(), internal_format, min_filter, mag_filter, wrap_s, wrap_t, Globals.NO_MIPMAP_CUTOFF);
    }

    public Texture(@NonNull GLImage @NonNull [] mipmaps, int internal_format, int min_filter, int mag_filter,
            int wrap_s, int wrap_t) throws IllegalArgumentException, NullPointerException {
        this(mipmaps, internal_format, min_filter, mag_filter, wrap_s, wrap_t, Globals.NO_MIPMAP_CUTOFF);
    }

    public Texture(@NonNull GLImage @NonNull [] mipmaps, int internal_format, int min_filter, int mag_filter,
            int wrap_s, int wrap_t, int max_mipmap_level) throws IllegalArgumentException, NullPointerException {
        this(getCheckedMipmaps(
                mipmaps)[0].getWidth(), mipmaps[0].getHeight(), min_filter, mag_filter, wrap_s, wrap_t,
                max_mipmap_level);
        int total_size = uploadTexture(mipmaps, internal_format, max_mipmap_level);
        setSize(total_size);
    }

    private static @NonNull GLImage @NonNull [] getCheckedMipmaps(@NonNull GLImage @NonNull [] mipmaps) {
        Objects.requireNonNull(mipmaps, "Mipmaps array cannot be null.");
        if (mipmaps.length == 0) {
            throw new IllegalArgumentException("Mipmaps array cannot be empty.");
        }
        Objects.requireNonNull(mipmaps[0], "First mipmap cannot be null.");
        return mipmaps;
    }

    private static int getDetailShift(int num_mipmaps) {
        return Math.min(num_mipmaps - 1, Globals.TEXTURE_MIP_SHIFT[Settings.getSettings().graphic_detail]);
    }

    private static int getMaxMipmapIndex(int num_mipmaps, int max_mipmap_level, int detail_shift) {
        return Math.min(num_mipmaps, max_mipmap_level + 1) - detail_shift;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private void setSize(int size) {
        NativeTexture.global_size.addAndGet(size - state.size);
        state.size += size;
    }

    private int uploadDXTTexture(@NonNull DXTImage dxt_image, int internalFormat, int max_mipmap_level) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(
                    "Uploading DXT texture: handle=" + getHandle() + ", " + dxt_image.getWidth() + "x" + dxt_image
                            .getHeight() + ", internalFormat=0x" + Integer.toHexString(
                                    internalFormat) + ", mips=" + dxt_image.getNumMipMaps());
        }
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        int detail_shift = getDetailShift(dxt_image.getNumMipMaps());
        int max_index = getMaxMipmapIndex(dxt_image.getNumMipMaps(), max_mipmap_level, detail_shift);
        int total_size = 0;
        for (int i = 0; i < max_index; i++) {
            int mipmap_level = i + detail_shift;
            ByteBuffer mipData = dxt_image.getMipMap(mipmap_level);
            total_size += mipData.remaining();
            GL13.glCompressedTexImage2D(GL11.GL_TEXTURE_2D, i, internalFormat, dxt_image.getWidth(mipmap_level),
                    dxt_image.getHeight(mipmap_level), 0, mipData);
        }
        GLUtils.checkAndThrow("uploadDXTTexture");
        return total_size;
    }

    private int uploadTexture(@NonNull GLImage @NonNull [] mipmaps, int internal_format, int max_mipmap_level) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(
                    "Uploading standard texture: handle=" + getHandle() + ", " + mipmaps[0].getWidth() + "x"
                            + mipmaps[0].getHeight() + ", internal_format=0x" + Integer.toHexString(
                                    internal_format) + ", mips=" + mipmaps.length);
        }
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        assert mipmaps.length > 0;
        int detail_shift = getDetailShift(mipmaps.length);
        int max_index = getMaxMipmapIndex(mipmaps.length, max_mipmap_level, detail_shift);
        for (int i = 0; i < max_index; i++) {
            GLImage mipmap = mipmaps[i + detail_shift];
            assert Utils.isPowerOf2(mipmap.getWidth()) && Utils.isPowerOf2(
                    mipmap.getHeight()) : "Mipmap level " + i + " dimensions are not power of two";
            ByteBuffer originalPixels = mipmap.getPixels();

            ByteBuffer nativePixels = BufferUtils.createByteBuffer(originalPixels.capacity());
            originalPixels.rewind(); // Make sure we read from the start
            nativePixels.put(originalPixels);
            nativePixels.flip();

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, internal_format, mipmap.getWidth(), mipmap.getHeight(), 0,
                    mipmap.getGLFormat(), mipmap.getGLType(), nativePixels);
        }

        int total_size = 0;
        for (int i = 0; i < max_index; i++) {
            GLImage mipmap = mipmaps[i + detail_shift];
            int size = determineMipMapSize(i, internal_format, mipmap.getWidth(), mipmap.getHeight());
            total_size += size;
        }
        GLUtils.checkAndThrow("uploadTexture");
        return total_size;
    }

    private static int determineMipMapSize(int mipmap, int internal_format, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size_buffer = stack.mallocInt(4);
            GL11.glGetTexLevelParameteriv(GL11.GL_TEXTURE_2D, mipmap, GL13.GL_TEXTURE_COMPRESSED, size_buffer);
            boolean compressed = size_buffer.get(0) == GL11.GL_TRUE;

            if (compressed) {
                GL11.glGetTexLevelParameteriv(GL11.GL_TEXTURE_2D, mipmap, GL13.GL_TEXTURE_COMPRESSED_IMAGE_SIZE,
                        size_buffer);
                return size_buffer.get(0);
            } else {
                return switch (internal_format) {
                    case GL13.GL_COMPRESSED_RGB, GL11.GL_RGB, GL11.GL_RGB8 -> width * height * 3;
                    case GL13.GL_COMPRESSED_RGBA, GL11.GL_RGBA, GL11.GL_RGBA8 -> width * height * 4;
                    case GL11.GL_LUMINANCE, GL11.GL_ALPHA8, GL11.GL_ALPHA, GL13.GL_COMPRESSED_LUMINANCE,
                            GL13.GL_COMPRESSED_ALPHA, GL11.GL_RED, GL30.GL_R8 -> width * height;
                    case GL30.GL_R32F -> width * height * 4;
                    case GL30.GL_RGBA16F -> width * height * 8; // 4 channels * 2 bytes (half float)
                    case GL30.GL_DEPTH_COMPONENT24 -> width * height * 4; // Treated as 32-bit for alignment/estimation
                    default -> {
                        String msg = "Unknown internal format: 0x" + Integer.toHexString(internal_format);
                        logger.severe(msg);
                        throw new IllegalArgumentException(msg);
                    }
                };
            }
        }
    }

    public int getHandle() {
        return state.texture_handle;
    }
}
