package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.util.DXTImage;
import com.oddlabs.util.Image;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Defines the properties and loading parameters for a texture resource.
 * This class specifies how a texture should be loaded and configured in OpenGL.
 */
public final class TextureFile extends File<Texture> {
	/** The internal format of the texture, e.g., GL_RGBA or a compressed format. */
	private final int internal_format;
	/** The minification filter, used when the texture is scaled down. */
	private final int min_filter;
	/** The magnification filter, used when the texture is scaled up. */
	private final int mag_filter;
	/** The wrap mode for the S (U) texture coordinate. */
	private final int wrap_s;
	/** The wrap mode for the T (V) texture coordinate. */
	private final int wrap_t;
	/** The base mipmap level for fadeout effects. */
	private final int base_fadeout_level;
	/** The maximum mipmap level to generate and use. */
	private final int max_mipmap_level;
	/** The factor by which mipmap levels fade out. */
	private final float fadeout_factor;
	/** If true, alpha values are maximized during processing. */
	private final boolean max_alpha;
	/** If true, the texture is a DXT compressed image. */
	private final boolean is_dxt;

	public TextureFile(String location) {
		this(location, Globals.COMPRESSED_RGBA_FORMAT);
	}

	public TextureFile(String location, int internal_format) {
		this(location, internal_format, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
	}

	public TextureFile(String location, int internal_format, int min_filter, int mag_filter, int wrap_s, int wrap_t) {
		this(location, internal_format, min_filter, mag_filter, wrap_s, wrap_t, Globals.NO_MIPMAP_CUTOFF, 10000, 1.0f);
	}

	public TextureFile(String location, int internal_format, int min_filter, int mag_filter, int wrap_s, int wrap_t, int max_mipmap_level, int base_fadeout_level, float fadeout_factor) {
		this(location, internal_format, min_filter, mag_filter, wrap_s, wrap_t, max_mipmap_level, base_fadeout_level, fadeout_factor, false);
	}

	public TextureFile(String location, int internal_format, int min_filter, int mag_filter, int wrap_s, int wrap_t, int max_mipmap_level, int base_fadeout_level, float fadeout_factor, boolean max_alpha) {
		super(locateTexture(location));
		this.is_dxt = locateDXT(location) != null;
		this.internal_format = internal_format;
		this.min_filter = min_filter;
		this.mag_filter = mag_filter;
		this.wrap_s = wrap_s;
		this.wrap_t = wrap_t;
		this.base_fadeout_level = base_fadeout_level;
		this.max_mipmap_level = max_mipmap_level;
		this.fadeout_factor = fadeout_factor;
		this.max_alpha = max_alpha;
	}

	private static @Nullable URI locate(@NonNull String location_with_ext) {
		URL url_classpath = Utils.class.getResource(location_with_ext);
		if (url_classpath != null) try {
            return url_classpath.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        Path file = com.oddlabs.tt.util.Utils.getInstallDir().resolve(location_with_ext);
        return Files.isRegularFile(file) && Files.isReadable(file) ? file.toUri() : null;
	}

	private static @Nullable URI locateDXT(String location) {
		return locate(location + ".dds");
	}

	private static @NonNull URI locateTexture(String location) {
		URI url = locateDXT(location);
		if (url != null)
			return url;

		url = locate(location + ".image");
		if (url != null)
			return url;

		throw new IllegalArgumentException(location);
	}

	public boolean isDXTImage() {
		return is_dxt;
	}

	public @NonNull DXTImage getDXTImage() {
		try {
			return DXTImage.read(getURL());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public @NonNull GLImage getImage() {
		Image image = Image.read(getURL());
		return new GLIntImage(image.getWidth(), image.getHeight(), image.getPixels(), GL11.GL_RGBA);
	}

	@Override
	public @NonNull Texture get() {
		return new Texture(this);
	}

	@Override
	public boolean equals(@Nullable Object o) {
        return o instanceof TextureFile other &&
                internal_format == other.internal_format &&
                min_filter == other.min_filter && mag_filter == other.mag_filter &&
                max_mipmap_level == other.max_mipmap_level &&
                wrap_s == other.wrap_s && wrap_t == other.wrap_t &&
                base_fadeout_level == other.base_fadeout_level && fadeout_factor == other.fadeout_factor &&
                super.equals(o);
	}

	public int getInternalFormat() {
		if (is_dxt) {
			return switch (getDXTImage().getFourCC()) {
				case DXTImage.FOURCC_DXT1 -> EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
				case DXTImage.FOURCC_DXT5 -> EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
				default -> throw new IllegalStateException("Unsupported DXT format: " + Integer.toHexString(getDXTImage().getFourCC()));
			};
		}
		return internal_format;
	}

	public int getMinFilter() {
		return min_filter;
	}

	public int getMagFilter() {
		return mag_filter;
	}

	public int getWrapS() {
		return wrap_s;
	}

	public int getWrapT() {
		return wrap_t;
	}

	public int getBaseFadeoutLevel() {
		return base_fadeout_level;
	}

	public int getMaxMipmapLevel() {
		return max_mipmap_level;
	}

	public float getFadeoutFactor() {
		return fadeout_factor;
	}

	public boolean hasMaxAlpha() {
		return max_alpha;
	}
}
