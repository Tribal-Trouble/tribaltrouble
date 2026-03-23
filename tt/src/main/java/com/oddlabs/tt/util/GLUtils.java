package com.oddlabs.tt.util;

import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class GLUtils {
    private static final Logger logger = Logger.getLogger(GLUtils.class.getSimpleName());
	public static final String SCREENSHOT_DEFAULT = "screenshot";

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
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer int_buf = stack.mallocInt(16);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, int_buf);
            GL11.glReadBuffer(GL11.GL_BACK);
            int width = int_buf.get(2) - int_buf.get(0);
            int height = int_buf.get(3) - int_buf.get(1);
            GLImage pixel_data = new GLIntImage(width, height, GL11.GL_RGBA);
            GL11.glReadPixels(int_buf.get(0), int_buf.get(1), int_buf.get(2), int_buf.get(3), pixel_data.getGLFormat(), pixel_data.getGLType(), pixel_data.getPixels());
            com.oddlabs.util.Utils.flip(pixel_data.getPixels(), width*4, height);
            pixel_data.saveAsBMP(filename);
        }
		return filename;
	}

	public static void saveTexture(int mipmap_level, @NonNull String filename) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer int_buf = stack.mallocInt(16);
            GL11.glGetTexLevelParameteriv(GL11.GL_TEXTURE_2D, mipmap_level, GL11.GL_TEXTURE_WIDTH, int_buf);
            int width = int_buf.get(0);
            GL11.glGetTexLevelParameteriv(GL11.GL_TEXTURE_2D, mipmap_level, GL11.GL_TEXTURE_HEIGHT, int_buf);
            int height = int_buf.get(0);
            GLImage pixel_data = new GLIntImage(width, height, GL11.GL_RGBA);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, mipmap_level, pixel_data.getGLFormat(), pixel_data.getGLType(), pixel_data.getPixels());
    //		swizzleColors(pixel_data.getPixels());
            com.oddlabs.util.Utils.flip(pixel_data.getPixels(), width*4, height);
            pixel_data.saveAsPNG(filename);
        }
	}

    /**
     * Checks for OpenGL errors and logs them.
     * @param message A descriptive message for the context of the OpenGL call.
     * @return A list of error codes found.
     */
    public static @NonNull List<Integer> checkGLError(@NonNull String message) {
        List<Integer> errors = new ArrayList<>();
        int error;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            logger.log(Level.WARNING, "OpenGL Error (" + message + "): " + errorToString(error), new Throwable("stacktrace"));
            errors.add(error);
        }
        return errors;
    }

    /**
     * Checks for OpenGL errors and throws an OpenGLException if any are found.
     * @param message A descriptive message for the context.
     * @throws OpenGLException if any OpenGL errors were detected.
     */
    public static void checkAndThrow(@NonNull String message) {
        List<Integer> errors = checkGLError(message);
        if (!errors.isEmpty()) {
            throw new OpenGLException("OpenGL failure (" + message + "): " + errorToString(errors.getFirst()));
        }
    }

    /**
     * Converts an OpenGL error code to a descriptive string.
     * @param error The OpenGL error code.
     * @return A string representation of the error.
     */
    public static @NonNull String errorToString(int error) {
        return switch (error) {
            case GL11.GL_NO_ERROR -> "GL_NO_ERROR";
            case GL11.GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL11.GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL11.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL11.GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
            case GL11.GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
            case GL11.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            default -> "Unknown OpenGL Error: " + error + " (0x" + Integer.toHexString(error) + ")";
        };
    }

    private GLUtils() {
    }
}
