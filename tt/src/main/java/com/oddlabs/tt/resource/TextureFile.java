package com.oddlabs.tt.resource;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.util.DXTImage;
import com.oddlabs.util.Image;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TextureFile extends File<Texture> {
	private final int internal_format;
	private final int min_filter;
	private final int mag_filter;
	private final int wrap_s;
	private final int wrap_t;
	private final int base_fadeout_level;
	private final int max_mipmap_level;
	private final float fadeout_factor;
	private final boolean max_alpha;
	private final boolean is_dxt;

	public TextureFile(String location) {
		this(location, Globals.COMPRESSED_RGBA_FORMAT);
	}

	public TextureFile(String location, int internal_format) {
		this(location, internal_format, GL11.GL_LINEAR_MIPMAP_NEAREST, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
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

	private static @Nullable URL locate(@NonNull String location_with_ext) {
		URL url_classpath = Utils.class.getResource(location_with_ext);
		if (url_classpath != null)
			return url_classpath;
		try {
			Path file =  com.oddlabs.tt.util.Utils.getInstallDir().resolve(location_with_ext);
			if (Files.isRegularFile(file) && Files.isReadable(file)) {
				return file.toUri().toURL();
            }
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private static @Nullable URL locateDXT(String location) {
		return locate(location + ".dxtn");
	}

	private static @NonNull URL locateTexture(String location) {
		URL url = locateDXT(location);
		if (url != null)
			return url;

/*		String location_jpg = location + ".jpg";
		URL url_jpg = Utils.class.getResource(location_jpg);
		if (url_jpg != null)
			return url_jpg;*/

		url = locate(location + ".image");
		if (url != null)
			return url;

		throw new RuntimeException(location);
	}

	private BufferedImage readFile(@NonNull URL url) {
		try {
			return ImageIO.read(url);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isDXTImage() {
		return is_dxt;
	}

	public @NonNull DXTImage getDXTImage() {
		try {
			return DXTImage.read(getURL());
		} catch (IOException e) {
			throw new RuntimeException(e);
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
	public boolean equals(Object o) {
		if (!(o instanceof TextureFile))
			return false;
		TextureFile other = (TextureFile)o;
		if (internal_format != other.internal_format || min_filter != other.min_filter || mag_filter != other.mag_filter ||
			max_mipmap_level != other.max_mipmap_level ||
			wrap_s != other.wrap_s || wrap_t != other.wrap_t || base_fadeout_level != other.base_fadeout_level || fadeout_factor != other.fadeout_factor)
			return false;
		return super.equals(o);
	}

	public int getInternalFormat() {
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
