package com.oddlabs.tt.font;

import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.util.FontInfo;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class Font {
	private final @Nullable Quad @NonNull [] key_array;
	private final @NonNull Texture texture;
	private final int x_border;
	private final int y_border;
	private final int height;

	public Font(@NonNull FontInfo font_info) {
		this.key_array = font_info.getKeyMap();
		TextureFile file = new TextureFile(font_info.getTextureName(),
										   GL11.GL_RGBA,
										   GL11.GL_LINEAR,
										   GL11.GL_LINEAR,
										   GL11.GL_CLAMP,
										   GL11.GL_CLAMP);
		this.texture = Resources.findResource(file);
		this.x_border = font_info.getBorderX();
		this.y_border = font_info.getBorderY();
		this.height = font_info.getHeight();
	}

	public @Nullable Quad getQuad(int codepoint) {
		return codepoint < key_array.length ? key_array[codepoint] : null;
	}

	public void setup() {
		GL11.glEnd();
		setupQuads();
	}

	public void setupQuads() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
		GL11.glBegin(GL11.GL_QUADS);
	}

	public void resetQuads() {
		GL11.glEnd();
	}

	public void reset() {
		resetQuads();
		Skin.getSkin().bindTexture();
		GL11.glBegin(GL11.GL_QUADS);
	}

	public int getXBorder() {
		return x_border;
	}

	public int getYBorder() {
		return y_border;
	}

	public int getHeight() {
		return height;
	}

	public @NonNull Texture getTexture() {
		return texture;
	}

    // TODO: not unicode-safe
	public char getWidestChar(@NonNull CharSequence text) {
		assert !text.isEmpty() : "Empty CharSequence";

		int widest = 0;
		char widest_char = ' ';
		for (int i = 0; i < text.length(); i++) {
			Quad quad = getQuad(text.charAt(i));
			if (quad != null) {
				int width = quad.getWidth() - x_border;
				if (widest < width) {
					widest = width;
					widest_char = text.charAt(i);
				}
			}
		}
		return widest_char;
	}

	public int getWidth(@NonNull CharSequence text) {
		if (text.isEmpty())
			return 0;

        int width = text.codePoints().reduce(0, (current, codePoint) -> {
            var quad = getQuad(codePoint);
            return current + (null != quad ? quad.getWidth() - x_border : 0);
        });
        return width + x_border;
	}
}
