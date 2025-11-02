package com.oddlabs.tt.util;

import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.util.Image;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class GLUtils {
	public final static String SCREENSHOT_DEFAULT = "screenshot";
	
	private final static @NonNull ByteBuffer byte_buf = BufferUtils.createByteBuffer(16);
	private final static IntBuffer int_buf = BufferUtils.createIntBuffer(16);
	private final static FloatBuffer plane = BufferUtils.createFloatBuffer(Float.BYTES);

	public static @NonNull GLIntImage loadAsGLImage(@NonNull String location) {
		Image img = null;
		img = Image.read(com.oddlabs.util.Utils.makeURL(location));
		GLIntImage glimage = new GLIntImage(img.getWidth(), img.getHeight(), img.getPixels(), GL11.GL_RGBA);
		return glimage;
	}

	public static boolean getGLBoolean(int gl_enum) {
		GL11.glGetBoolean(gl_enum, byte_buf);
		return byte_buf.get(0) == (byte)1;
	}

	public static int getGLInteger(int gl_enum) {
		GL11.glGetInteger(gl_enum, int_buf);
		return int_buf.get(0);
	}

	public static void setupTexGen(float scale_x, float scale_y, float offset_x, float offset_y) {
		plane.put(0, scale_x);
		plane.put(1, 0f);
		plane.put(2, 0f);
		plane.put(3, offset_x*scale_x);
		GL11.glTexGen(GL11.GL_S, GL11.GL_OBJECT_PLANE, plane);
		plane.put(0, 0f);
		plane.put(1, scale_y);
		plane.put(2, 0f);
		plane.put(3, offset_y*scale_y);
		GL11.glTexGen(GL11.GL_T, GL11.GL_OBJECT_PLANE, plane);
	}

	public static @NonNull String takeScreenshot(@NonNull String filename) {
		if (filename.isEmpty()) {
			int i = 0;
			File file;
			do {
				filename = SCREENSHOT_DEFAULT + "000000";
				String number = ""+i;
				filename = System.getProperty("user.home") + File.separator + filename.substring(0, filename.length() - number.length()) + number + ".bmp";
				file = new File(filename);
				i++;
			} while (file.exists());
		}
		GL11.glGetInteger(GL11.GL_VIEWPORT, int_buf);
		GL11.glReadBuffer(GL11.GL_FRONT);
		int width = int_buf.get(2) - int_buf.get(0);
		int height = int_buf.get(3) - int_buf.get(1);
		GLImage pixel_data = new GLIntImage(width, height, GL11.GL_RGBA);
		GL11.glReadPixels(int_buf.get(0), int_buf.get(1), int_buf.get(2), int_buf.get(3), pixel_data.getGLFormat(), pixel_data.getGLType(), pixel_data.getPixels());
		GL11.glReadBuffer(GL11.GL_BACK);
		com.oddlabs.util.Utils.flip(pixel_data.getPixels(), width*4, height);
		pixel_data.saveAsBMP(filename);
		System.gc();
		return filename;
	}

/*	private static void swizzleColors(ByteBuffer pixels) {
		for (int i = 0; i < pixels.remaining()/4; i++) {
			byte b1 = pixels.get(i*4);
			byte b2 = pixels.get(i*4 + 1);
			byte b3 = pixels.get(i*4 + 2);
			byte b4 = pixels.get(i*4 + 3);
			pixels.put(i*4, b4);
			pixels.put(i*4 + 1, b1);
			pixels.put(i*4 + 2, b2);
			pixels.put(i*4 + 3, b3);
		}
	}
	*/
	public static void saveTexture(int mipmap_level, String filename) {
		GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mipmap_level, GL11.GL_TEXTURE_WIDTH, int_buf);
		int width = int_buf.get(0);
		GL11.glGetTexLevelParameter(GL11.GL_TEXTURE_2D, mipmap_level, GL11.GL_TEXTURE_HEIGHT, int_buf);
		int height = int_buf.get(0);
		GLImage pixel_data = new GLIntImage(width, height, GL11.GL_RGBA);
		GL11.glGetTexImage(GL11.GL_TEXTURE_2D, mipmap_level, pixel_data.getGLFormat(), pixel_data.getGLType(), pixel_data.getPixels());
//		swizzleColors(pixel_data.getPixels());
		com.oddlabs.util.Utils.flip(pixel_data.getPixels(), width*4, height);
		pixel_data.saveAsPNG(filename);
		System.gc();
	}

    private GLUtils() {
    }
}
