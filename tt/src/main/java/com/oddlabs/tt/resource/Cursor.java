package com.oddlabs.tt.resource;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.render.NativeCursor;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.util.Image;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.net.URL;

public final class Cursor {

    private final @NonNull NativeCursor native_cursor;

    private final int offset_x;
    private final int offset_y; // This is hotspot_y_from_top
    private final @NonNull IconQuad cursor;
    private final int height;

    private boolean render_gl_cursor;

    public void setActive() {
        render_gl_cursor = !native_cursor.setActive();
    }

    public void render(float x, float y) {
        if (render_gl_cursor || LocalEventQueue.getQueue().getDeterministic().isPlayback()) {
            // x and y are the desired hotspot coordinates in the GUI's Y-up system
            // offset_x is hotspot_x_from_left
            // offset_y is hotspot_y_from_top
            
            // Quad.render expects bottom-left coordinates
            float draw_x = x - offset_x;
            // Corrected: Calculate bottom-left Y based on hotspot_y_from_top and image height
            float draw_y = y - (height - offset_y); 
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, cursor.getTexture().getHandle());
			GL11.glBegin(GL11.GL_QUADS);
			cursor.render(draw_x, draw_y);
			GL11.glEnd();
        }
    }

    public Cursor(@NonNull URL url_16_1, int offset_x_16_1, int offset_y_16_1,
                  @NonNull URL url_32_1, int offset_x_32_1, int offset_y_32_1,
                  @NonNull URL url_32_8, int offset_x_32_8, int offset_y_32_8) {
        this.offset_x = offset_x_32_8;
        this.offset_y = offset_y_32_8; // This is hotspot_y_from_top
        Image image = Image.read(url_32_8);
        int width = image.getWidth();
        this.height = image.getHeight();
        GLIntImage img_32_8 = new GLIntImage(width, height, image.getPixels(), GL11.GL_RGBA);

        Image image_16_1 = Image.read(url_16_1);
        GLIntImage img_16_1 = new GLIntImage(image_16_1.getWidth(), image_16_1.getHeight(), image_16_1.getPixels(), GL11.GL_RGBA);
        Image image_32_1 = Image.read(url_32_1);
        GLIntImage img_32_1 = new GLIntImage(image_32_1.getWidth(), image_32_1.getHeight(), image_32_1.getPixels(), GL11.GL_RGBA);

        native_cursor = new NativeCursor(img_16_1, offset_x_16_1, offset_y_16_1,
                img_32_1, offset_x_32_1, offset_y_32_1,
                img_32_8, offset_x_32_8, offset_y_32_8);

        var texture = new Texture(new GLImage[]{img_32_8},
                GL11.GL_RGBA,
                GL11.GL_NEAREST,
                GL11.GL_NEAREST,
                GL11.GL_REPEAT,
                GL11.GL_REPEAT);
        cursor = new IconQuad(0, 0, 1, 1, width, height, texture);
    }
}
