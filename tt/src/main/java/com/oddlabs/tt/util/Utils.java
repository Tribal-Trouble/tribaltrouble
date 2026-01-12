package com.oddlabs.tt.util;

import com.oddlabs.tt.global.Globals;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public final class Utils {

	public static @NonNull String getBundleString(@NonNull ResourceBundle bundle, @NonNull String key, Object... object_array) {
		return MessageFormat.format(bundle.getString(key), object_array);
	}

	public static @NonNull Path getInstallDir() {
		return Path.of(System.getProperty("user.dir"));
	}

	public static @NonNull FloatBuffer toBuffer(float @NonNull [] floats) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(floats.length);
		buffer.put(floats);
		buffer.rewind();
		return buffer;
	}

	public static @NonNull ShortBuffer toBuffer(short @NonNull [] shorts) {
		ShortBuffer buffer = BufferUtils.createShortBuffer(shorts.length);
		buffer.put(shorts);
		buffer.rewind();
		return buffer;
	}

    public static ByteBuffer ioResourceToByteBuffer(@NonNull URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        }
    }

	public static void saveAsBMP(@NonNull String filename, @NonNull ByteBuffer pixel_data, int width, int height) {
		long before = System.nanoTime();
		int pad = 4 - (width*3)%4;
		if (pad == 4)
			pad = 0;
		int size = (width*3 + pad)*height + 54;
		ByteBuffer buffer = ByteBuffer.allocate(size);

		//write BMP header
		buffer.put((byte)0x42);							 // signature, must be 4D42 hex
		buffer.put((byte)0x4D);							 // ...
		buffer.put((byte)(size & 0x000000ff));		// size of BMP file in bytes
		buffer.put((byte)((size & 0x0000ff00)>>8));   // ...
		buffer.put((byte)((size & 0x00ff0000)>>16));  // ...
		buffer.put((byte)((size & 0xff000000)>>24));  // ...
		buffer.put((byte)0);								// reserved, must be zero
		buffer.put((byte)0);								// reserved, must be zero
		buffer.put((byte)0);								// reserved, must be zero
		buffer.put((byte)0);								// reserved, must be zero
		buffer.put((byte)54);							   // offset to start of image data in bytes
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)40);							   // size of BITMAPINFOHEADER structure, must be 40
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)(width & 0x000000ff));	   // image width in pixels
		buffer.put((byte)((width & 0x0000ff00)>>8));  // ...
		buffer.put((byte)((width & 0x00ff0000)>>16)); // ...
		buffer.put((byte)((width & 0xff000000)>>24)); // ...
		buffer.put((byte)(height & 0x000000ff));	  // image width in pixels
		buffer.put((byte)((height & 0x0000ff00)>>8)); // ...
		buffer.put((byte)((height & 0x00ff0000)>>16));// ...
		buffer.put((byte)((height & 0xff000000)>>24));// ...
		buffer.put((byte)1);								// number of planes in the image, must be 1
		buffer.put((byte)0);								// ...
		buffer.put((byte)24);		   // number of bits per pixel (1, 4, 8, or 24)
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// compression type (0=none, 1=RLE-8, 2=RLE-4)
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)((size - 54) & 0x000000ff));		// size of image data in bytes (including padding)
		buffer.put((byte)(((size - 54) & 0x0000ff00)>>8));   // ...
		buffer.put((byte)(((size - 54) & 0x00ff0000)>>16));  // ...
		buffer.put((byte)(((size - 54) & 0xff000000)>>24));  // ...
		buffer.put((byte)0);								// horizontal resolution in pixels per meter (unreliable)
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// vertical resolution in pixels per meter (unreliable)
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// number of colors in image, or zero
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// number of important colors, or zero
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...
		buffer.put((byte)0);								// ...

		pixel_data.rewind();
		IntBuffer int_pixel_data = pixel_data.asIntBuffer();
		//write BMP image data
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				int pixel = int_pixel_data.get(y*width + x);
				byte r = (byte)((pixel >> 24) & 0xff);
				byte g = (byte)((pixel >> 16) & 0xff);
				byte b = (byte)((pixel >> 8) & 0xff);
				buffer.put(b);
				buffer.put(g);
				buffer.put(r);
			}
			for (int i = 0; i < pad; i++) {
				buffer.put((byte)0);
			}

		}
		buffer.rewind();
		Path image_file = Path.of(filename);
		try (OutputStream fout = Files.newOutputStream(image_file)) {
			fout.write(buffer.array());
		} catch (IOException e) {
			e.printStackTrace();
		}
		long after = System.nanoTime();
		IO.println("File " + filename + " saved in " + TimeUnit.NANOSECONDS.toMillis(after - before) + " milliseconds");
	}

	public static void saveAsTGA(String filename, @NonNull ByteBuffer pixel_data, int width, int height) {
		long before = System.nanoTime();
		try (FileOutputStream fout = new FileOutputStream(filename + ".tga")) {

			//write TGA header
			fout.write(0); //ID length, 0 because no image id field
			fout.write(0); //no color map
			fout.write(2); //image type (24 bit RGB, uncompressed)
			fout.write(0); //color map origin, ignore because no color map
			fout.write(0); //color map origin, ignore because no color map
			fout.write(0); //color map origin, ignore because no color map
			fout.write(0); //color map length, ignore because no color map
			fout.write(0); //color map entry size, ignore because no color map
			fout.write(0); //x origin
			fout.write(0); //x origin
			fout.write(0); //x origin
			fout.write(0); //y origin
			short s = (short)width;
			fout.write((byte)(s & 0x00ff));	  //image width low byte
			fout.write((byte)((s & 0xff00)>>8)); //image width high byte
			s = (short)height;
			fout.write((byte)(s & 0x00ff));	  //image height low byte
			fout.write((byte)((s & 0xff00)>>8)); //image height high byte
			fout.write(32); //bpp
			fout.write(0); //description bits

			pixel_data.rewind();
			//write TGA image data
			fout.getChannel().write(pixel_data);

			fout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long after = System.nanoTime();
		IO.println("File " + filename + " saved in " + TimeUnit.NANOSECONDS.toMillis(after - before) + " milliseconds");
	}

	public static int numTextureSplits(int size) {
		return (size >> Globals.MAX_TEXTURE_POWER) + Globals.TEXTURE_SPLITS[size & (Globals.MAX_TEXTURE_SIZE - 1)];
	}

	public static int toTextureSize(int size) {
		return (size & ~(Globals.MAX_TEXTURE_SIZE - 1)) + Globals.TEXTURE_SIZES[size & (Globals.MAX_TEXTURE_SIZE - 1)];
	}

	public static int roundToTextureSize(int size) {
		assert size <= Globals.MAX_TEXTURE_SIZE;
		int tex_size = 1;
		while (tex_size < size)
			tex_size <<= 1;
		return tex_size;
	}

	public static int bestTextureSize(int size) {
		if (size >= Globals.MAX_TEXTURE_SIZE)
			return Globals.MAX_TEXTURE_SIZE;
		return Globals.BEST_SIZES[size];
	}

	public static float invsqrt(float x) {
		float xhalf = 0.5f * x;
		int i = Float.floatToRawIntBits(x);
		i = 0x5f375a86 - (i >> 1);
		x = Float.intBitsToFloat(i);
		x *= (1.5f - xhalf * x * x); // This line may be duplicated for more accuracy.
		return x;
	}

    private Utils() {
    }
}