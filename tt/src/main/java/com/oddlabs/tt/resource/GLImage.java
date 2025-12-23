package com.oddlabs.tt.resource;

import com.oddlabs.procedural.Channel;
import com.oddlabs.procedural.Layer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Image;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;

public abstract class GLImage {
	private final int width;
	private final int height;
	private final int type;
	private final int format;
	private final @NonNull ByteBuffer pixel_data;

	public final int getWidth() {
		return width;
	}

	public final int getHeight() {
		return height;
	}

	public final int getGLType() {
		return type;
	}

	public abstract int getPixel(int x, int y);

	public abstract void putPixel(int x, int y, int pixel);

	public GLImage(int width, int height, ByteBuffer pixel_data, int format) {
		//assert Utils.isPowerOf2(width): "Width must be power of 2";
		//assert Utils.isPowerOf2(height): "Height must be power of 2";
		this.width = width;
		this.height = height;
		this.pixel_data = pixel_data;
		this.format =  format;
		this.type = determineType(format);
	}

	private int determineType(int format) {
        return switch (format) {
            case GL11.GL_RGBA, GL12.GL_BGRA, GL13.GL_COMPRESSED_RGBA -> {
                // assert width * height * 4 == pixel_data.remaining();
                yield GL11.GL_UNSIGNED_BYTE;
            }
            case GL11.GL_LUMINANCE, GL13.GL_COMPRESSED_LUMINANCE, GL11.GL_ALPHA, GL13.GL_COMPRESSED_ALPHA, GL11.GL_RED -> {
                // assert width * height == pixel_data.remaining();
                yield GL11.GL_UNSIGNED_BYTE;
            }
            default -> throw new IllegalArgumentException("Invalid format: " + format);
        };
	}

	public final int getGLFormat() {
		return format;
	}

	public abstract GLImage createImage(int width, int height, int format);
	public abstract GLImage createFromLayer(@NonNull Layer layer, int format);

	public final @NonNull GLImage @NonNull [] createMipMaps() {
		return buildMipMaps(10000, 1.0f, false, false);
	}

	public final @NonNull GLImage @NonNull [] buildMipMaps(int base_fadeout_level, float fadeout_factor, boolean wrapping, boolean max_alpha) {
		int max = Math.max(height, width);
		int max_level = (int)(Math.log(max)/Math.log(2));
		GLImage[] result = new GLImage[max_level + 1];
		result[0] = this;

		for (int i = 1; i < result.length; i++) {
			int current_width = Math.max(width >> i, 1);
			int current_height = Math.max(height >> i, 1);

			result[i] = createImage(current_width, current_height, format);

			GLImage prev = result[i - 1];
			int width_div = prev.getWidth() / current_width;
			int height_div = prev.getHeight() / current_height;

			for (int y = 0; y < current_height; y++) {
				for (int x = 0; x < current_width; x++) {
					result[i].putPixel(x, y, averagePixel(prev, x * width_div, y * height_div, height_div, width_div, base_fadeout_level, fadeout_factor, i, max_alpha));
				}
			}
		}
		return result;
	}

	private static void applyFadeout(@NonNull GLImage image, float factor) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = image.getPixel(x, y);
				int a = (pixel >>> 24);
				int r = (pixel >>> 16) & 0xff;
				int g = (pixel >>> 8) & 0xff;
				int b = pixel & 0xff;

				a = (int) (a * factor);
				r = (int) (r * factor);
				g = (int) (g * factor);
				b = (int) (b * factor);

				image.putPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
			}
		}
	}

	/**
	 * Applies a progressive alpha fade-out effect to a series of mipmaps.
	 * This is used to smoothly fade objects out of view at a distance.
	 *
	 * @param mipmaps           An array of GLImages representing the mipmap levels.
	 * @param base_fadeout_level The mipmap level at which to start the fade-out.
	 * @param fadeout_factor    The factor by which to reduce the alpha at each successive level.
	 * @param start_x           The starting X coordinate of the area to modify.
	 * @param start_y           The starting Y coordinate of the area to modify.
	 * @param width             The width of the area to modify.
	 * @param height            The height of the area to modify.
	 * @param max_alpha         If true, only pixels with full alpha (255) are faded.
	 */
	public static void updateMipMapsArea(GLImage @NonNull [] mipmaps, int base_fadeout_level, float fadeout_factor, int start_x, int start_y, int width, int height, boolean max_alpha) {
		for (int i = 1; i < mipmaps.length; i++) {
			int height_div = mipmaps[i - 1].getHeight()/mipmaps[i].getHeight();
			int width_div = mipmaps[i - 1].getWidth()/mipmaps[i].getWidth();
			start_x /= width_div;
			start_y /= height_div;
			width = (int)Math.ceil((float)width/width_div);
			height = (int)Math.ceil((float)height/height_div);
			for (int y = start_y; y < start_y + height; y++) {
                for (int x = start_x; x < start_x + width; x++) {
                    mipmaps[i].putPixel(x, y, averagePixel(mipmaps[i - 1], width_div*x, height_div*y, height_div, width_div, base_fadeout_level, fadeout_factor, i, max_alpha));
                }
            }
		}
	}

	public static void blendMipMapsArea(GLImage @NonNull [] dest_mipmaps, GLImage @NonNull [] source_mipmaps, int base_fadeout_level, float fadeout_factor, int start_x, int start_y, int width, int height) {
		int mip_map_level = 0;
		while (source_mipmaps[0].getWidth() != dest_mipmaps[mip_map_level].getWidth() && source_mipmaps[0].getHeight() != dest_mipmaps[mip_map_level].getHeight())
			mip_map_level++;
		for (int i = 1; i < dest_mipmaps.length; i++) {
			int height_div = dest_mipmaps[i - 1].getHeight()/dest_mipmaps[i].getHeight();
			int width_div = dest_mipmaps[i - 1].getWidth()/dest_mipmaps[i].getWidth();
			start_x /= width_div;
			start_y /= height_div;
			width = (int)Math.ceil((float)width/width_div);
			height = (int)Math.ceil((float)height/height_div);
			if (i >= base_fadeout_level) {
				if (i >= mip_map_level)
					dest_mipmaps[i].drawImageBlended(source_mipmaps[i - mip_map_level], start_x, start_y, start_x, start_y, width, height, 1.0f - fadeout_factor);
				fadeout_factor *= fadeout_factor;
			}
		}
	}

	/**
	 * Calculates the average pixel value from a block of pixels in a source image,
	 * used for generating mipmap levels. It can apply a fadeout effect and uses
	 * the maximum alpha value from the source block instead of averaging if specified.
	 *
	 * @param last_img           The source image (previous mipmap level).
	 * @param x                  The top-left x-coordinate of the block in the source image.
	 * @param y                  The top-left y-coordinate of the block in the source image.
	 * @param height_div         The height of the pixel block to average.
	 * @param width_div          The width of the pixel block to average.
	 * @param base_fadeout_level The mipmap level at which to begin applying fadeout.
	 * @param fadeout_factor     The factor by which to reduce color components for fading.
	 * @param current_level      The current mipmap level being generated.
	 * @param max_alpha          If true, the resulting alpha is the maximum from the source block;
	 *                           otherwise, it's the average.
	 * @return The calculated 32-bit ARGB pixel value.
	 */
	private static int averagePixel(@NonNull GLImage last_img, int x, int y, int height_div, int width_div, int base_fadeout_level, float fadeout_factor, int current_level, boolean max_alpha) {
		float inv_num_averaged = 1f/(height_div * width_div);
		int a_acc = 0;
		int r_acc = 0;
		int g_acc = 0;
		int b_acc = 0;
		for (int offset_y = 0; offset_y < height_div; offset_y++)
			for (int offset_x = 0; offset_x < width_div; offset_x++) {
				int pixel = last_img.getPixel(x + offset_x, y + offset_y);

				// Unpack ARGB
				int a = (pixel >>> 24);
				int r = (pixel >>> 16) & 0xff;
				int g = (pixel >>> 8) & 0xff;
				int b = pixel & 0xff;

				r_acc += r;
				g_acc += g;
				b_acc += b;

				if (max_alpha) {
					a_acc = Math.max(a_acc, a);
				} else {
					a_acc += a;
				}
			}
		if (current_level >= base_fadeout_level) {
			a_acc = (int)(a_acc*fadeout_factor);
			r_acc = (int)(r_acc*fadeout_factor);
			g_acc = (int)(g_acc*fadeout_factor);
			b_acc = (int)(b_acc*fadeout_factor);
		}

		if (!max_alpha)
			a_acc = (int)(a_acc*inv_num_averaged);

		r_acc = (int)(r_acc*inv_num_averaged);
		g_acc = (int)(g_acc*inv_num_averaged);
		b_acc = (int)(b_acc*inv_num_averaged);

		return (a_acc << 24) | (r_acc << 16) | (g_acc << 8) | b_acc;
	}

    /**
	 * Calculates the average pixel value from a block of pixels in a source image.
	 * This alternative implementation averages all channels, including alpha, and then
     * applies a simple threshold to the averaged alpha if {@code max_alpha} is true.
	 *
	 * @param last_img           The source image (previous mipmap level).
	 * @param x                  The top-left x-coordinate of the block in the source image.
	 * @param y                  The top-left y-coordinate of the block in the source image.
	 * @param height_div         The height of the pixel block to average.
	 * @param width_div          The width of the pixel block to average.
	 * @param base_fadeout_level The mipmap level at which to begin applying fadeout.
	 * @param fadeout_factor     The factor by which to reduce color components for fading.
	 * @param current_level      The current mipmap level being generated.
	 * @param max_alpha          If true, sets alpha to 255 if the average is >= 128.
	 * @return The calculated 32-bit ARGB pixel value.
	 */

	private static int averagePixelThreshold(@NonNull GLImage last_img, int x, int y, int height_div, int width_div, int base_fadeout_level, float fadeout_factor, int current_level, boolean max_alpha) {
		float inv_num_averaged = 1f/(height_div * width_div);
        int col1 = 0; // Alpha (MSB)
        int col2 = 0; // Blue
        int col3 = 0; // Green
        int col4 = 0; // Red (LSB)
		for (int offset_y = 0; offset_y < height_div; offset_y++) {
            for (int offset_x = 0; offset_x < width_div; offset_x++) {
                int pixel = last_img.getPixel(x + offset_x, y + offset_y);
                col1 += (pixel >>> 24);
                col2 += (pixel >>> 16) & 0xff;
                col3 += (pixel >>> 8) & 0xff;
                col4 += pixel & 0xff;
            }
        }
		if (current_level >= base_fadeout_level) {
			col1 = (int)(col1*fadeout_factor);
			col2 = (int)(col2*fadeout_factor);
			col3 = (int)(col3*fadeout_factor);
			col4 = (int)(col4*fadeout_factor);
		}
		col1 = (int)(col1*inv_num_averaged);
		col2 = (int)(col2*inv_num_averaged);
		col3 = (int)(col3*inv_num_averaged);
		col4 = (int)(col4*inv_num_averaged);
		if (max_alpha) {
			if (col1 >= 128)
				col1 = 255;
		}
		return (col1 << 24) + (col2 << 16) + (col3 << 8) + col4;
	}

    /** Clear the entire image to the specified color. */
	public final void clearAll(int color) {
        clear(0, 0, width, height, color);
	}

    /** Clear a specified rectangle of the image to the specified color. */
	public final void clear(int x, int y, int width, int height, int color) {
		int yy = y;
		for (; yy < y + height; yy++) {
			for (int xx = x; xx < x + width; xx++) {
                putPixel(yy, xx, color);
            }
		}
	}

	public final void drawImageBlended(@NonNull GLImage img, int dx, int dy, int sx, int sy, int w, int h, float alpha_factor) {
		int spixel;
		int dpixel;
		int sr;
		int sg;
		int sb;
		int sa;
		int sa_inverse;
		int dr;
		int dg;
		int db;
		int da;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				spixel = img.getPixel(x + sx, y + sy);
				sa = (spixel >>> 24);
				if (alpha_factor != 1f) {
//					System.out.println("sa " + sa + " af " + alpha_factor + " round " + Math.round(sa * alpha_factor) + " r and " + (Math.round(sa * alpha_factor) & 0xff));
					sa = Math.round(sa * alpha_factor) & 0xff;
//					System.out.println("sa after: " + sa);
				}
				if (sa == 0) {
					continue;
				}
				if (sa == 255) {
					putPixel(x + dx, y + dy, spixel);
					continue;
				}
				sa_inverse = 255 - sa;
				dpixel = getPixel(x + dx, y + dy);
				sr = spixel >>> 16 & 0xff; sg = spixel >>> 8 & 0xff; sb = spixel & 0xff;
				dr = dpixel >>> 16 & 0xff; dg = dpixel >>> 8 & 0xff; db = dpixel & 0xff; da = dpixel >>> 24;
				putPixel(x + dx, y + dy, (((sa * sa + da * sa_inverse)/255) << 24) + (((sr * sa + dr * sa_inverse)/255) << 16) + ((sg * sa + dg * sa_inverse)/255 << 8) + ((sb * sa + db * sa_inverse)/255));
//				System.out.println("result dp " + Integer.toHexString(pixels[x+dy_loop]) + " sp " + Integer.toHexString(spixel) + " dp " + Integer.toHexString(dpixel) + " sa " + Integer.toHexString(sa) + " sa_inv " + Integer.toHexString(sa_inverse) + " sr " + Integer.toHexString(sr) + " sg " + Integer.toHexString(sg) + " sb " + Integer.toHexString(sb) + " dr " + Integer.toHexString(dr) + " dg " + Integer.toHexString(dg)  + " db " + Integer.toHexString(db) + " da " + Integer.toHexString(da));
			}
		}
	}

	protected abstract int getPixelSize();

	public final ByteBuffer getPixels() {
		return pixel_data;
	}

	public final void drawImage(@NonNull GLImage img, int dx, int dy, int sx, int sy, int w, int h) {
		int pixel_size = getPixelSize();
		assert pixel_size == img.getPixelSize();
		ByteBuffer pixels = getPixels();
		ByteBuffer other_pixels = img.getPixels();
		int byte_width = w*pixel_size;
		for (int i = 0; i < h; i++) {
			int other_pos = ((sy+i)*img.getWidth() + sx)*pixel_size;
			int pos = ((dy+i)*getWidth() + dx)*pixel_size;
			other_pixels.position(other_pos);
			other_pixels.limit(other_pos + byte_width);
			pixels.position(pos);
//System.out.println("pos = " + pos + " | byte_width = " + byte_width + " | pixels.capacity() = "+ pixels.capacity());
			pixels.limit(pos + byte_width);
			pixels.put(other_pixels);
//			System.arraycopy(img.getPixelArray(), (sy+i)*img.getWidth() + sx, getPixelArray(), (dy+i)*getWidth() + dx, w);
			pixels.clear();
			other_pixels.clear();
		}
	}

	public @NonNull Layer toLayer() {
		int width = getWidth();
		int height = getHeight();
		Channel r = new Channel(width, height);
		Channel g = new Channel(width, height);
		Channel b = new Channel(width, height);
		Channel a = new Channel(width, height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
                int pixel = getPixel(x, y);
                // Unpack ABGR
				a.putPixel(x, y, (pixel>>>24)/255f);
				b.putPixel(x, y, ((pixel>>16) & 0xff)/255f);
				g.putPixel(x, y, ((pixel>>8) & 0xff)/255f);
				r.putPixel(x, y, (pixel & 0xff)/255f);
			}
		}
		return new Layer(r, g, b, a);
	}

	public final void saveAsPNG(String filename) {
		toLayer().saveAsPNG(filename);
	}

	public final void saveAsBMP(@NonNull String filename) {
		Utils.saveAsBMP(filename, getPixels(), getWidth(), getHeight());
	}

	public final void saveAsImage(String filename) {
		new Image(getWidth(), getHeight(), getPixels()).write(filename);
	}
}
