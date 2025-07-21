package com.oddlabs.tt.resource;

import java.net.URL;

import org.lwjgl.opengl.GL11;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.util.Image;
import com.oddlabs.util.Quad;

public final strictfp class Cursor {
	private final Texture texture;

	private final int offset_x;
	private final int offset_y;
	private final Quad cursor;

	public final void setActive() {

    }
	
	public final void render(float x, float y) {
        cursor.setTexture(texture.getHandle());
		cursor.render(x - offset_x, y - offset_y);
	}

	public Cursor(URL url_16_1, int offset_x_16_1, int offset_y_16_1,
				  URL url_32_1, int offset_x_32_1, int offset_y_32_1,
				  URL url_32_8, int offset_x_32_8, int offset_y_32_8) {
		this.offset_x = offset_x_32_8;
		this.offset_y = offset_y_32_8;
		Image image = Image.read(url_32_8);
		int width = image.getWidth();
		int height = image.getHeight();
		GLIntImage img_32_8 = new GLIntImage(width, height, image.getPixels(), GL11.GL_RGBA);

		Image image_16_1 = Image.read(url_16_1);
		GLIntImage img_16_1 = new GLIntImage(image_16_1.getWidth(), image_16_1.getHeight(), image_16_1.getPixels(), GL11.GL_RGBA);
		Image image_32_1 = Image.read(url_32_1);
		GLIntImage img_32_1 = new GLIntImage(image_32_1.getWidth(), image_32_1.getHeight(), image_32_1.getPixels(), GL11.GL_RGBA);
		
		texture = new Texture(new GLImage[]{img_32_8},
								GL11.GL_RGBA,
								GL11.GL_NEAREST,
								GL11.GL_NEAREST,
								GL11.GL_REPEAT,
								GL11.GL_REPEAT);
		cursor = new Quad(0, 0, 1, 1, 32, 32);
	}
}
