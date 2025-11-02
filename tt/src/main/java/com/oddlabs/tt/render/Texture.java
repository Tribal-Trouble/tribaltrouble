package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.util.DXTImage;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GLContext;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class Texture extends NativeResource {
	private final static IntBuffer handle_buffer;
	private final static IntBuffer size_buffer;
	private final static FloatBuffer border_color_buffer;

	public static int global_size = 0;

	private final int texture_handle;
	private final int width;
	private final int height;

	private int size;

	static {
		handle_buffer = BufferUtils.createIntBuffer(1);
		size_buffer = BufferUtils.createIntBuffer(4);
		border_color_buffer = BufferUtils.createFloatBuffer(4);
	}

	private static int initTexture(int min_filter, int mag_filter, int wrap_s, int wrap_t, int max_mipmap_level) {
		GL11.glGenTextures(handle_buffer);
		int tex_handle = handle_buffer.get(0);
		assert tex_handle != 0;
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex_handle);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap_s);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap_t);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min_filter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag_filter);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, max_mipmap_level);
		border_color_buffer.put(0, 0f).put(1, 0f).put(2, 0f).put(3, 0f);
		GL11.glTexParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, border_color_buffer);

		if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
			float max_anisotropy = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, max_anisotropy);
		}

		return tex_handle;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setSize(int size) {
		global_size += size - this.size;
		this.size = size;
	}

	public Texture(@NonNull TextureFile texture_file) {
		this.texture_handle = initTexture(texture_file.getMinFilter(), texture_file.getMagFilter(), texture_file.getWrapS(), texture_file.getWrapT(), texture_file.getMaxMipmapLevel());
		if (texture_file.isDXTImage()) {
			DXTImage dxt_image = texture_file.getDXTImage();
			this.width = dxt_image.getWidth();
			this.height =  dxt_image.getHeight();
			uploadDXTTexture(dxt_image, texture_file);
		} else {
			GLImage img = texture_file.getImage();
			this.width = img.getWidth();
			this.height = img.getHeight();
			GLImage[] mipmaps;
			if (texture_file.getMinFilter() == GL11.GL_LINEAR_MIPMAP_LINEAR || texture_file.getMinFilter() == GL11.GL_LINEAR_MIPMAP_LINEAR) {
				mipmaps = img.buildMipMaps();
				GLImage.updateMipMapsArea(mipmaps, texture_file.getBaseFadeoutLevel(), texture_file.getFadeoutFactor(), 0, 0, width, height, texture_file.hasMaxAlpha());
			} else
				mipmaps = new GLImage[]{img};
			uploadTexture(mipmaps, texture_file.getInternalFormat(), texture_file.getMaxMipmapLevel());
		}
	}

	public Texture(int width, int height, int min_filter, int mag_filter, int wrap_s, int wrap_t, int max_mipmap_level) {
		texture_handle = initTexture(min_filter, mag_filter, wrap_s, wrap_t, max_mipmap_level);
		this.width = width;
		this.height = height;
	}

	public Texture(GLImage @NonNull [] mipmaps, int internal_format, int min_filter, int mag_filter, int wrap_s, int wrap_t) {
		this(mipmaps, internal_format, min_filter, mag_filter, wrap_s, wrap_t, Globals.NO_MIPMAP_CUTOFF);
	}

	public Texture(GLImage @NonNull [] mipmaps, int internal_format, int min_filter, int mag_filter, int wrap_s, int wrap_t, int max_mipmap_level) {
		this(mipmaps[0].getWidth(), mipmaps[0].getHeight(), min_filter, mag_filter, wrap_s, wrap_t, max_mipmap_level);
		uploadTexture(mipmaps, internal_format, max_mipmap_level);
	}

	private static int getDetailShift(int num_mipmaps) {
		return Math.min(num_mipmaps - 1, Globals.TEXTURE_MIP_SHIFT[Settings.getSettings().graphic_detail]);
	}

	private static int getMaxMipmapIndex(int num_mipmaps, int max_mipmap_level, int detail_shift) {
		return Math.min(num_mipmaps, max_mipmap_level + 1) - detail_shift;
	}

	private void uploadDXTTexture(@NonNull DXTImage dxt_image, @NonNull TextureFile texture_file) {
		int detail_shift = getDetailShift(dxt_image.getNumMipMaps());
		int max_index = getMaxMipmapIndex(dxt_image.getNumMipMaps(), texture_file.getMaxMipmapLevel(), detail_shift);
		int total_size = 0;
		for (int i = 0; i < max_index; i++) {
			int mipmap_level = i + detail_shift;
			dxt_image.position(mipmap_level);
			total_size += dxt_image.getMipMap().remaining();
			GLState.glCompressedTexImage2D(GL11.GL_TEXTURE_2D, i, dxt_image.getInternalFormat(), dxt_image.getWidth(mipmap_level), dxt_image.getHeight(mipmap_level), 0, dxt_image.getMipMap());
		}
		setSize(total_size);
/*for (int i = 0; i < max_index; i++) {
int mipmap_level = i + detail_shift;
GLUtils.saveTexture(i, new java.io.File(texture_file.getURL().getFile() + dxt_image.getWidth(mipmap_level) + "x" + dxt_image.getHeight(mipmap_level)).getName());
}*/
	}

	private void uploadTexture(GLImage @NonNull [] mipmaps, int internal_format, int max_mipmap_level) {
		assert mipmaps.length > 0;
		int detail_shift = getDetailShift(mipmaps.length);
		int max_index = getMaxMipmapIndex(mipmaps.length, max_mipmap_level, detail_shift);
		for (int i = 0; i < max_index; i++) {
			GLImage mipmap = mipmaps[i + detail_shift];
			assert Utils.isPowerOf2(mipmap.getWidth()) && Utils.isPowerOf2(mipmap.getHeight()): "Mipmap level " + i + " dimensions are not power of two";
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, i, internal_format, mipmap.getWidth(), mipmap.getHeight(), 0, mipmap.getGLFormat(), mipmap.getGLType(), mipmap.getPixels());
		}

		int total_size = 0;
		for (int i = 0; i < max_index; i++) {
			GLImage mipmap = mipmaps[i + detail_shift];
			int size = determineMipMapSize(i, internal_format, mipmap.getWidth(), mipmap.getHeight());
			total_size += size;
		}
		setSize(total_size);
	}

	private static int determineMipMapSize(int mipmap, int internal_format, int width, int height) {
		GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mipmap, GL13.GL_TEXTURE_COMPRESSED, size_buffer);
		boolean compressed = size_buffer.get(0) == GL11.GL_TRUE;

		if (compressed) {
			GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mipmap, GL13.GL_TEXTURE_COMPRESSED_IMAGE_SIZE, size_buffer);
			return size_buffer.get(0);
		} else {
            return switch (internal_format) {
                case GL13.GL_COMPRESSED_RGB, GL11.GL_RGB -> width * height * 3;
                case GL13.GL_COMPRESSED_RGBA, GL11.GL_RGBA -> width * height * 4;
                case GL11.GL_LUMINANCE, GL11.GL_ALPHA8, GL11.GL_ALPHA, GL13.GL_COMPRESSED_LUMINANCE,
                     GL13.GL_COMPRESSED_ALPHA -> width * height;
                default -> throw new RuntimeException("0x" + Integer.toHexString(internal_format));
            };
		}
	}

	public int getHandle() {
		return texture_handle;
	}

        @Override
	public void doDelete() {
		global_size -= size;
		handle_buffer.put(0, texture_handle);
		GL11.glDeleteTextures(handle_buffer);
	}
}
