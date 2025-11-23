package com.oddlabs.procedural;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a multi-channel image, typically RGBA, where each channel is a 2D grid of floating-point values.
 * This class is used for procedural image generation and manipulation.
 */
public final class Layer {
    private static final Logger logger = Logger.getLogger(Layer.class.getSimpleName());
	private static final float GAMMA_EXPONENT = 2.2f;
	private static final float INV_GAMMA_EXPONENT = 1f/GAMMA_EXPONENT;

	private int width;
	private int height;
	public Channel r;
	public Channel g;
	public Channel b;
	public @Nullable Channel a;

	/**
	 * Constructs a new, opaque white Layer with the specified dimensions.
	 *
	 * @param width  the width of the layer in pixels.
	 * @param height the height of the layer in pixels.
	 * @throws IllegalArgumentException if width or height are not positive.
	 */
	public Layer(int width, int height) throws IllegalArgumentException {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Width and height must be positive.");
		}
		this.width = width;
		this.height = height;
		Channel empty = new Channel(width, height);
		empty.fill(1f);
		this.r = empty;
		this.g = empty.copy();
		this.b = empty.copy();
		this.a = null;
	}

	/**
	 * Constructs a new Layer from the given R, G, B, and optional A channels.
	 *
	 * @param r the red channel.
	 * @param g the green channel.
	 * @param b the blue channel.
	 * @param a the alpha channel (optional, can be null).
	 * @throws NullPointerException if R, G, or B channels are null.
	 * @throws IllegalArgumentException if channels have mismatching dimensions.
	 */
	public Layer(@NonNull Channel r, @NonNull Channel g, @NonNull Channel b, @Nullable Channel a)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(r, "Red channel cannot be null.");
		Objects.requireNonNull(g, "Green channel cannot be null.");
		Objects.requireNonNull(b, "Blue channel cannot be null.");

		this.width = r.getWidth();
		this.height = r.getHeight();
		if (g.getWidth() != width || b.getWidth() != width || g.getHeight() != height || b.getHeight() != height
				|| (a != null && (a.getWidth() != width || a.getHeight() != height))) {
			throw new IllegalArgumentException("All channels must have the same dimensions.");
		}
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	/**
	 * Constructs a new opaque Layer from the given R, G, and B channels.
	 *
	 * @param r the red channel.
	 * @param g the green channel.
	 * @param b the blue channel.
	 * @throws NullPointerException if R, G, or B channels are null.
	 * @throws IllegalArgumentException if channels have mismatching dimensions.
	 */
	public Layer(@NonNull Channel r, @NonNull Channel g, @NonNull Channel b)
			throws NullPointerException, IllegalArgumentException {
		this(r, g, b, null);
	}

	/**
	 * Constructs a new Layer from an existing RGB Layer and a new alpha channel.
	 *
	 * @param rgb the source RGB layer.
	 * @param a the new alpha channel.
	 * @throws NullPointerException if the rgb layer or alpha channel is null.
	 * @throws IllegalArgumentException if the alpha channel's dimensions do not match the layer's.
	 */
	public Layer(@NonNull Layer rgb, @NonNull Channel a)
			throws NullPointerException, IllegalArgumentException {
		this(rgb.r, rgb.g, rgb.b, a);
	}

	public void loadFromBytes(byte[] data) {
		float inv_255 = 1f/255f;
		int index = 0;
		for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                int br = Byte.toUnsignedInt(data[index++]);
                int bg = Byte.toUnsignedInt(data[index++]);
                int bb = Byte.toUnsignedInt(data[index++]);
                int ba = Byte.toUnsignedInt(data[index++]);
                float fr = br*inv_255;
                float fg = bg*inv_255;
                float fb = bb*inv_255;
                float fa = ba*inv_255;
                r.putPixel(x, y, fr);
                g.putPixel(x, y, fg);
                b.putPixel(x, y, fb);
                if (a != null)
                    a.putPixel(x, y, fa);
            }
        }
	}

	public byte[] convertToBytes() {
		byte[] byte_pixel_data = new byte[getWidth()*getHeight()*4];
		for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                int ri = ((int)(r.getPixel(x, y)*255 + .5f)) & 0xff;
                int gi = ((int)(g.getPixel(x, y)*255 + .5f)) & 0xff;
                int bi = ((int)(b.getPixel(x, y)*255 + .5f)) & 0xff;
                int ai;
                if (a != null) {
                    ai = ((int)(a.getPixel(x, y)*255 + .5f)) & 0xff;
                } else {
                    ai = 255;
                }
                int index = y*getWidth() + x;
                byte_pixel_data[index*4] = (byte)ri;
                byte_pixel_data[index*4 + 1] = (byte)gi;
                byte_pixel_data[index*4 + 2] = (byte)bi;
                byte_pixel_data[index*4 + 3] = (byte)ai;
            }
        }
		return byte_pixel_data;
	}

	private @NonNull BufferedImage convertToImage() {
		byte[] byte_pixel_data = convertToBytes();
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		image.getRaster().setDataElements(0, 0, getWidth(), getHeight(), byte_pixel_data);
		return image;
	}

	public void saveAsPNG(String filename) {
        Path file = Path.of(  filename + ".png");
        try {
		    saveAsPNG(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed writing image to " + file, e);
        }
	}

	public void saveAsPNG(@NonNull Path file) throws IOException {
		BufferedImage image = convertToImage();
		try (OutputStream outputStream = Files.newOutputStream(file)) {
            ImageIO.write(image, "PNG", outputStream);
        }
	}

	public void addAlpha() {
		a = new Channel(width, height);
	}

	public void addAlpha(Channel alpha) {
		a = alpha;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Channel getR() {
		return r;
	}

	public Channel getG() {
		return g;
	}

	public Channel getB() {
		return b;
	}

	public @Nullable Channel getA() {
		return a;
	}

	/**
	 * Sets the RGB values of a pixel at the specified coordinates.
	 *
	 * @param x the x-coordinate of the pixel.
	 * @param y the y-coordinate of the pixel.
	 * @param r the new red value.
	 * @param g the new green value.
	 * @param b the new blue value.
	 * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
	 */
	public void putPixel(int x, int y, float r, float g, float b) throws IndexOutOfBoundsException {
		this.r.putPixel(x, y, r);
		this.g.putPixel(x, y, g);
		this.b.putPixel(x, y, b);
	}

	/**
	 * Sets the RGBA values of a pixel at the specified coordinates.
	 *
	 * @param x the x-coordinate of the pixel.
	 * @param y the y-coordinate of the pixel.
	 * @param r the new red value.
	 * @param g the new green value.
	 * @param b the new blue value.
	 * @param a the new alpha value.
	 * @throws IndexOutOfBoundsException if the coordinates are out of bounds.
	 */
	public void putPixel(int x, int y, float r, float g, float b, float a) throws IndexOutOfBoundsException {
		this.r.putPixel(x, y, r);
		this.g.putPixel(x, y, g);
		this.b.putPixel(x, y, b);
		if (this.a != null) {
			this.a.putPixel(x, y, a);
		}
	}

	public void putPixelWrap(int x, int y, float r, float g, float b) {
		this.r.putPixelWrap(x, y, r);
		this.g.putPixelWrap(x, y, g);
		this.b.putPixelWrap(x, y, b);
	}

	public void putPixelWrap(int x, int y, float r, float g, float b, float a) {
		this.r.putPixelWrap(x, y, r);
		this.g.putPixelWrap(x, y, g);
		this.b.putPixelWrap(x, y, b);
		if (this.a != null) {
			this.a.putPixelWrap(x, y, a);
		}
	}

	public void putPixelClip(int x, int y, float r, float g, float b) {
		this.r.putPixelClip(x, y, r);
		this.g.putPixelClip(x, y, g);
		this.b.putPixelClip(x, y, b);
	}

	public void putPixelClip(int x, int y, float r, float g, float b, float a) {
		this.r.putPixelClip(x, y, r);
		this.g.putPixelClip(x, y, g);
		this.b.putPixelClip(x, y, b);
		if (this.a != null) {
			this.a.putPixelClip(x, y, a);
		}
	}

	public void fill(float value) {
		r.fill(value);
		g.fill(value);
		b.fill(value);
	}

	public void fill(float r, float g, float b) {
		this.r.fill(r);
		this.g.fill(g);
		this.b.fill(b);
	}

	public void fill(float r, float g, float b, float a) {
		this.r.fill(r);
		this.g.fill(g);
		this.b.fill(b);
		if (this.a != null) {
			this.a.fill(a);
		}
	}

	public float findMin() {
		float min_r = r.findMin();
		float min_g = g.findMin();
		float min_b = b.findMin();
		return Math.min(min_r, Math.min(min_g, min_b));
	}

	public float findMax() {
		float max_r = r.findMax();
		float max_g = g.findMax();
		float max_b = b.findMax();
		return Math.max(max_r, Math.max(max_g, max_b));
	}

	public @NonNull Layer copy() {
		if (a != null) {
			return new Layer(r.copy(), g.copy(), b.copy(), a.copy());
		} else {
			return new Layer(r.copy(), g.copy(), b.copy());
		}
	}

	public @NonNull Layer dynamicRange() {
		float min_r = r.findMin();
		float min_g = g.findMin();
		float min_b = b.findMin();
		float max_r = r.findMax();
		float max_g = g.findMax();
		float max_b = b.findMax();
		float min = Math.min(min_r, Math.min(min_g, min_b));
		float max = Math.max(max_r, Math.max(max_g, max_b));
		min_r = Tools.interpolateLinear(0, 1, (min_r - min)/(max - min));
		min_g = Tools.interpolateLinear(0, 1, (min_g - min)/(max - min));
		min_b = Tools.interpolateLinear(0, 1, (min_b - min)/(max - min));
		max_r = Tools.interpolateLinear(0, 1, (max_r - min)/(max - min));
		max_g = Tools.interpolateLinear(0, 1, (max_g - min)/(max - min));
		max_b = Tools.interpolateLinear(0, 1, (max_b - min)/(max - min));
		r.dynamicRange(min_r, max_r);
		g.dynamicRange(min_g, max_g);
		b.dynamicRange(min_b, max_b);
		return this;
	}

	public @NonNull Layer dynamicRange(float new_min, float new_max) {
		float min_r = r.findMin();
		float min_g = g.findMin();
		float min_b = b.findMin();
		float max_r = r.findMax();
		float max_g = g.findMax();
		float max_b = b.findMax();
		float min = Math.min(min_r, Math.min(min_g, min_b));
		float max = Math.max(max_r, Math.max(max_g, max_b));
		min_r = Tools.interpolateLinear(new_min, new_max, (min_r - min)/(max - min));
		min_g = Tools.interpolateLinear(new_min, new_max, (min_g - min)/(max - min));
		min_b = Tools.interpolateLinear(new_min, new_max, (min_b - min)/(max - min));
		max_r = Tools.interpolateLinear(new_min, new_max, (max_r - min)/(max - min));
		max_g = Tools.interpolateLinear(new_min, new_max, (max_g - min)/(max - min));
		max_b = Tools.interpolateLinear(new_min, new_max, (max_b - min)/(max - min));
		r.dynamicRange(min_r, max_r);
		g.dynamicRange(min_g, max_g);
		b.dynamicRange(min_b, max_b);
		return this;
	}

	public @NonNull Layer dynamicRange(float min, float max, float new_min, float new_max) {
		r.dynamicRange(min, max, new_min, new_max);
		g.dynamicRange(min, max, new_min, new_max);
		b.dynamicRange(min, max, new_min, new_max);
		return this;
	}

	public @NonNull Layer clip() {
		r.clip();
		g.clip();
		b.clip();
		return this;
	}

	public @NonNull Layer crop(int x_lo, int y_lo, int x_hi, int y_hi) {
		r = r.crop(x_lo, y_lo, x_hi, y_hi);
		g = g.crop(x_lo, y_lo, x_hi, y_hi);
		b = b.crop(x_lo, y_lo, x_hi, y_hi);
		if (a != null) {
			a = a.crop(x_lo, y_lo, x_hi, y_hi);
		}
		width = r.getWidth();
		height = r.getHeight();
		return this;
	}

	public @NonNull Layer cropWrap(int x_lo, int y_lo, int x_hi, int y_hi) {
		r = r.cropWrap(x_lo, y_lo, x_hi, y_hi);
		g = g.cropWrap(x_lo, y_lo, x_hi, y_hi);
		b = b.cropWrap(x_lo, y_lo, x_hi, y_hi);
		if (a != null) {
			a = a.cropWrap(x_lo, y_lo, x_hi, y_hi);
		}
		width = r.getWidth();
		height = r.getHeight();
		return this;
	}

	public @NonNull Layer tile(int new_width, int new_height) {
		r = r.tile(new_width, new_height);
		g = g.tile(new_width, new_height);
		b = b.tile(new_width, new_height);
		if (a != null) {
			a = a.tile(new_width, new_height);
		}
		width = r.getWidth();
		height = r.getHeight();
		return this;
	}

	public @NonNull Layer tileDouble() {
		r = r.tileDouble();
		g = g.tileDouble();
		b = b.tileDouble();
		if (a != null) {
			a = a.tileDouble();
		}
		width = width<<1;
		height = height<<1;
		return this;
	}

	public @NonNull Layer offset(int x_offset, int y_offset) {
		r = r.offset(x_offset, y_offset);
		g = g.offset(x_offset, y_offset);
		b = b.offset(x_offset, y_offset);
		if (a != null) {
			a = a.offset(x_offset, y_offset);
		}
		return this;
	}

	public @NonNull Layer brightness(float brightness) {
		r.brightness(brightness);
		g.brightness(brightness);
		b.brightness(brightness);
		return this;
	}

	public @NonNull Layer brightness(float r, float g, float b) {
		this.r.brightness(r);
		this.g.brightness(g);
		this.b.brightness(b);
		return this;
	}

	public @NonNull Layer multiply(float factor) {
		r.multiply(factor);
		g.multiply(factor);
		b.multiply(factor);
		return this;
	}

	public @NonNull Layer multiply(float r, float g, float b) {
		this.r.multiply(r);
		this.g.multiply(g);
		this.b.multiply(b);
		return this;
	}

	public @NonNull Layer multiply(float r, float g, float b, float a) {
		this.r.multiply(r);
		this.g.multiply(g);
		this.b.multiply(b);
		if (this.a != null) {
			this.a.multiply(a);
		}
		return this;
	}

	public @NonNull Layer add(float add) {
		r.add(add);
		g.add(add);
		b.add(add);
		return this;
	}

	public @NonNull Layer add(float r, float g, float b) {
		this.r.add(r);
		this.g.add(g);
		this.b.add(b);
		return this;
	}

	public @NonNull Layer addClip(float r, float g, float b) {
		this.r.addClip(r);
		this.g.addClip(g);
		this.b.addClip(b);
		return this;
	}

	public @NonNull Layer addClip(float r, float g, float b, float a) {
		this.r.addClip(r);
		this.g.addClip(g);
		this.b.addClip(b);
		if (this.a != null) {
			this.a.addClip(a);
		}
		return this;
	}

	public @NonNull Layer contrast(float contrast) {
		r.contrast(contrast);
		g.contrast(contrast);
		b.contrast(contrast);
		return this;
	}

	public @NonNull Layer contrast(float r, float g, float b) {
		this.r.contrast(r);
		this.g.contrast(g);
		this.b.contrast(b);
		return this;
	}

	public @NonNull Layer gamma(float gamma) {
		r.gamma(gamma);
		g.gamma(gamma);
		b.gamma(gamma);
		return this;
	}

	public @NonNull Layer gamma(float r, float g, float b) {
		this.r.gamma(r);
		this.g.gamma(g);
		this.b.gamma(b);
		return this;
	}

	public @NonNull Layer gamma2() {
		r.gamma2();
		g.gamma2();
		b.gamma2();
		return this;
	}

	public @NonNull Layer gamma4() {
		r.gamma4();
		g.gamma4();
		b.gamma4();
		return this;
	}

	public @NonNull Layer gamma8() {
		r.gamma8();
		g.gamma8();
		b.gamma8();
		return this;
	}

	public @NonNull Layer invert() {
		r.invert();
		g.invert();
		b.invert();
		return this;
	}

	public @NonNull Layer threshold(float start, float end) {
		r.threshold(start, end);
g.threshold(start, end);
		b.threshold(start, end);
		return this;
	}

	public @NonNull Layer scaleHalf() {
		r = r.scaleHalf();
		g = g.scaleHalf();
		b = b.scaleHalf();
		if (a != null) {
			a = a.scaleHalf();
		}
		width = r.getWidth();
		height = r.getHeight();
		return this;
	}

	public @NonNull Layer scale(int new_width, int new_height) {
		r = r.scale(new_width, new_height);
		g = g.scale(new_width, new_height);
		b = b.scale(new_width, new_height);
		if (a != null) {
			a = a.scale(new_width, new_height);
		}
		width = new_width;
		height = new_height;
		return this;
	}

	public @NonNull Layer scaleCubic(int new_width, int new_height) {
		r = r.scaleCubic(new_width, new_height);
		g = g.scaleCubic(new_width, new_height);
		b = b.scaleCubic(new_width, new_height);
		if (a != null) {
			a = a.scaleCubic(new_width, new_height);
		}
		width = new_width;
		height = new_height;
		return this;
	}

	public @NonNull Layer scaleFast(int new_width, int new_height) {
		r = r.scaleFast(new_width, new_height);
		g = g.scaleFast(new_width, new_height);
		b = b.scaleFast(new_width, new_height);
		if (a != null) {
			a = a.scaleFast(new_width, new_height);
		}
		width = new_width;
		height = new_height;
		return this;
	}

	public @NonNull Layer rotate(int degrees) {
		r = r.rotate(degrees);
		g = g.rotate(degrees);
		b = b.rotate(degrees);
		if (a != null) {
			a = a.rotate(degrees);
		}
		width = r.getWidth();
		height = r.getHeight();
		return this;
	}

	public @NonNull Layer shear(float offset) {
		r = r.shear(offset);
		g = g.shear(offset);
		b = b.shear(offset);
		if (a != null) {
			a = a.shear(offset);
		}
		return this;
	}

	public @NonNull Layer flipH() {
		r = r.flipH();
		g = g.flipH();
		b = b.flipH();
		if (a != null) {
			a = a.flipH();
		}
		return this;
	}

	public @NonNull Layer flipV() {
		r = r.flipV();
		g = g.flipV();
		b = b.flipV();
		if (a != null) {
			a = a.flipV();
		}
		return this;
	}

	public @NonNull Layer smooth(int radius) {
		r.smooth(radius);
		g.smooth(radius);
		b.smooth(radius);
		return this;
	}

	public @NonNull Layer sharpen(int radius) {
		r.sharpen(radius);
		g.sharpen(radius);
		b.sharpen(radius);

		return this;
	}

	public @NonNull Layer convolution(float[] @NonNull [] filter, float divisor, float offset) {
		r.convolution(filter, divisor, offset);
		g.convolution(filter, divisor, offset);
		b.convolution(filter, divisor, offset);
		return this;
	}

	public @NonNull Layer grow(float r, float g, float b, int radius) {
		this.r.grow(r, radius);
		this.g.grow(g, radius);
		this.b.grow(b, radius);
		return this;
	}

	public @NonNull Layer bump(@NonNull Channel bumpmap, float lx, float ly, float shadow, float light_r, float light_g, float light_b, float ambient_r, float ambient_g, float ambient_b) {
		assert bumpmap.getWidth() == width && bumpmap.getHeight() == height: "bumpmap size does not match layer size";
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float nx = bumpmap.getPixelWrap(x + 1, y) - bumpmap.getPixelWrap(x - 1, y);
				float ny = bumpmap.getPixelWrap(x, y + 1) - bumpmap.getPixelWrap(x, y - 1);
				float brightness = nx*lx + ny*ly;
				if (brightness >= 0) {
					putPixelClip(x, y, (r.getPixel(x, y) + brightness*light_r)*(bumpmap.getPixel(x, y)*shadow + 1 - shadow),
						(g.getPixel(x, y) + brightness*light_g)*(bumpmap.getPixel(x, y)*shadow + 1 - shadow),
						(b.getPixel(x, y) + brightness*light_b)*(bumpmap.getPixel(x, y)*shadow + 1 - shadow));
				} else {
					putPixelClip(x, y, (r.getPixel(x, y) + brightness*(1 - ambient_r))*(bumpmap.getPixel(x, y)*shadow + 1 - shadow),
						(g.getPixel(x, y) + brightness*(1 - ambient_g))*(bumpmap.getPixel(x, y)*shadow + 1 - shadow),
						(b.getPixel(x, y) + brightness*(1 - ambient_b))*(bumpmap.getPixel(x, y)*shadow + 1 - shadow));
				}
			}
		}
		return this;
	}

	public @NonNull Layer bumpFast(@NonNull Channel bumpmap, float lx, float light, float ambient) {
		assert bumpmap.getWidth() == width && bumpmap.getHeight() == height: "bumpmap size does not match layer size";
		ambient = 1f - ambient;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float brightness = lx*(bumpmap.getPixelWrap(x + 1, y) - bumpmap.getPixelWrap(x - 1, y));
				if (brightness >= 0) {
					brightness = brightness*light;
					putPixel(x, y, r.getPixel(x, y) + brightness,
						g.getPixel(x, y) + brightness,
						b.getPixel(x, y) + brightness);
				} else {
					brightness = brightness*ambient;
					putPixel(x, y, r.getPixel(x, y) + brightness,
						g.getPixel(x, y) + brightness,
						b.getPixel(x, y) + brightness);
				}
			}
		}
		return this;
	}

	public @NonNull Layer bumpSpecular(@NonNull Channel bumpmap, float lx, float ly, float lz, float shadow, float light_r, float light_g, float light_b, int specular) {
		assert bumpmap.getWidth() == width && bumpmap.getHeight() == height: "bumpmap size does not match layer size";
		float lnorm = (float)Math.sqrt(lx*lx + ly*ly + lz*lz);
		float nz = 4*(1f/Math.min(width, height));
		float nzlz = nz*lz;
		float nz2 = nz*nz;
		int power = 2<<specular;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float nx = bumpmap.getPixelWrap(x + 1, y) - bumpmap.getPixelWrap(x - 1, y);
				float ny = bumpmap.getPixelWrap(x, y + 1) - bumpmap.getPixelWrap(x, y - 1);
				float brightness = nx*lx + ny*ly;
				float costheta = (brightness + nzlz)/((float)Math.sqrt(nx*nx + ny*ny + nz2)*lnorm); // can use math here, not game state affecting
				float highlight;
				if (costheta > 0) {
					highlight = (float)Math.pow(costheta, power); // can use math here, not game state affecting
				} else {
					highlight = 0;
				}
				putPixelClip(x, y,
					(r.getPixel(x, y) + highlight*light_r)*(bumpmap.getPixel(x, y)*shadow + 1 - shadow),
					(g.getPixel(x, y) + highlight*light_g)*(bumpmap.getPixel(x, y)*shadow + 1 - shadow),
					(b.getPixel(x, y) + highlight*light_b)*(bumpmap.getPixel(x, y)*shadow + 1 - shadow));
			}
		}
		return this;
	}

	public @NonNull Layer toHSV() {
		float min = 0;
		float max = 0;
		float delta = 0;
		float h_val = 0;
		float s_val = 0;
		float v_val = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float r_val = r.getPixel(x, y);
				float g_val = g.getPixel(x, y);
				float b_val = b.getPixel(x, y);
				min = Math.min(r_val, Math.min(g_val, b_val));
				max = Math.max(r_val, Math.max(g_val, b_val));

				v_val = max;
				delta = max - min;

				if (max != 0) {
					s_val = delta/max;
				} else {
					s_val = 0;
				}

				if (max == r_val) {
					h_val = (g_val - b_val)/delta;
				}
				if (max == g_val) {
					h_val = 2 + (b_val - r_val)/delta;
				}
				if (max == b_val) {
					h_val = 4 + (r_val - g_val)/delta;
				}

				h_val /= 6;
				if (h_val < 0) {
					h_val += 1;
				}

				r.putPixel(x, y, h_val);
				g.putPixel(x, y, s_val);
				b.putPixel(x, y, v_val);
			}
		}
		return this;
	}

	public @NonNull Layer toRGB() {
		int i;
		float f;
		float p;
		float q;
		float t;
		float r_val = 0;
		float g_val = 0;
		float b_val = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				float h_val = r.getPixel(x, y);
				float s_val = g.getPixel(x, y);
				float v_val = b.getPixel(x, y);

				if (s_val == 0) {
					r.putPixel(x, y, v_val);
					g.putPixel(x, y, v_val);
					b.putPixel(x, y, v_val);
					continue;
				}

				h_val *= 6;
				i = (int)h_val;
				f = h_val - i;
				p = v_val*(1 - s_val);
				q = v_val*(1 - s_val*f);
				t = v_val*(1 - s_val*(1 - f));

				switch (i) {
					case 0:
						r_val = v_val;
						g_val = t;
						b_val = p;
						break;
					case 1:
						r_val = q;
						g_val = v_val;
						b_val = p;
						break;
					case 2:
						r_val = p;
						g_val = v_val;
						b_val = t;
						break;
					case 3:
						r_val = p;
						g_val = q;
						b_val = v_val;
						break;
					case 4:
						r_val = t;
						g_val = p;
						b_val = v_val;
						break;
					case 5:
						r_val = v_val;
						g_val = p;
						b_val = q;
						break;
					case 6:
						r_val = v_val;
						g_val = p;
						b_val = q;
						break;
					default:
						assert false: "hsv to rgb error";
				}

				r.putPixel(x, y, r_val);
				g.putPixel(x, y, g_val);
				b.putPixel(x, y, b_val);
			}
		}
		return this;
	}

	public @NonNull Layer saturation(float saturation) {
		toHSV();
		float s_val;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				s_val = g.getPixel(x, y) * saturation;
				if (s_val < 0) {
					s_val = 0;
				} else {
					if (s_val > 1) {
						s_val = 1;
					}
				}
				g.putPixel(x, y, s_val);
			}
		}
		return toRGB();
	}

	public @NonNull Layer hue(float hue) {
		toHSV();
		float h_val;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				h_val = r.getPixel(x, y) + hue;
				if (h_val < 0) {
					h_val += 1;
				} else {
					if (h_val > 1) {
						h_val -= 1;
					}
				}
				r.putPixel(x, y, h_val);
			}
		}
		return toRGB();
	}

	public @NonNull Layer hueRotation(float min, float max, float new_min, float new_max) {
		toHSV();
		r.dynamicRange(min, max, new_min, new_max);
		return toRGB();
	}

	public @NonNull Layer lineart() {
		r.lineart();
		g.lineart();
		b.lineart();
		return this;
	}

	public @NonNull Layer place(@NonNull Layer sprite, int x_offset, int y_offset) {
		r.place(sprite.r, x_offset, y_offset);
		g.place(sprite.g, x_offset, y_offset);
		b.place(sprite.b, x_offset, y_offset);
		if (a != null && sprite.a != null)
			a.place(sprite.a, x_offset, y_offset);
		return this;
	}

	public @NonNull Layer abs() {
		r.abs();
		g.abs();
		b.abs();
		return this;
	}

	public @NonNull Layer layerBlend(@NonNull Layer layer, float alpha) {
		r.channelBlend(layer.r, alpha);
		g.channelBlend(layer.g, alpha);
		b.channelBlend(layer.b, alpha);
		return this;
	}

	public @NonNull Layer layerBlend(@NonNull Layer rgb, Channel a) {
		return layerBlend(new Layer(rgb.r, rgb.g, rgb.b, a));
	}

	public @NonNull Layer layerBlend(@NonNull Layer layer) {
		assert layer.a != null : "cannot blend RGB only layer";

		if (a == null) {
			r.channelBlend(layer.r, layer.a);
			g.channelBlend(layer.g, layer.a);
			b.channelBlend(layer.b, layer.a);
		} else {
			float alpha;
			float alpha_inv;
			float r1;
			float g1;
			float b1;
			float a2;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (a.getPixel(x, y) == 0) {
						putPixel(x, y, layer.r.getPixel(x, y), layer.g.getPixel(x, y), layer.b.getPixel(x, y), layer.a.getPixel(x, y));
					} else if (layer.a.getPixel(x, y) == 0) {
					} else {
						alpha = 1f - (1f - a.getPixel(x, y))*(1f - layer.a.getPixel(x, y));
						alpha_inv = 1f/alpha;
						r1 = r.getPixel(x, y);
						g1 = g.getPixel(x, y);
						b1 = b.getPixel(x, y);
						a2 = layer.a.getPixel(x, y);
						r.putPixel(x, y, r1 - r1*a2*alpha_inv + layer.r.getPixel(x, y)*a2*alpha_inv);
						g.putPixel(x, y, g1 - g1*a2*alpha_inv + layer.g.getPixel(x, y)*a2*alpha_inv);
						b.putPixel(x, y, b1 - b1*a2*alpha_inv + layer.b.getPixel(x, y)*a2*alpha_inv);
						a.putPixel(x, y, alpha);
					}
				}
			}
		}
		return this;
	}

	public @NonNull Layer layerAdd(@NonNull Layer layer) {
		r.channelAdd(layer.r);
		g.channelAdd(layer.g);
		b.channelAdd(layer.b);
		return this;
	}

	public @NonNull Layer layerSubtract(@NonNull Layer layer) {
		r.channelSubtract(layer.r);
		g.channelSubtract(layer.g);
		b.channelSubtract(layer.b);
		return this;
	}

	public @NonNull Layer layerAverage(@NonNull Layer layer) {
		r.channelAverage(layer.r);
		g.channelAverage(layer.g);
		b.channelAverage(layer.b);
		return this;
	}

	public @NonNull Layer layerMultiply(@NonNull Layer layer) {
		r.channelMultiply(layer.r);
		g.channelMultiply(layer.g);
		b.channelMultiply(layer.b);
		return this;
	}

	public @NonNull Layer layerDifference(@NonNull Layer layer) {
		r.channelDifference(layer.r);
		g.channelDifference(layer.g);
		b.channelDifference(layer.b);
		return this;
	}

	public @NonNull Layer layerDarkest(@NonNull Layer layer) {
		r.channelDarkest(layer.r);
		g.channelDarkest(layer.g);
		b.channelDarkest(layer.b);
		return this;
	}

	public @NonNull Layer layerBrightest(@NonNull Layer layer) {
		r.channelBrightest(layer.r);
g.channelBrightest(layer.g);
		b.channelBrightest(layer.b);
		return this;
	}

}
