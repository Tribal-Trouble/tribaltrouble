package com.oddlabs.tt.font;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.util.FontInfo;
import com.oddlabs.util.HashTable;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class Font {
	private final @NonNull HashTable<@NonNull Quad> key_map;
	private final @NonNull Texture texture;
	private final int x_border;
	private final int y_border;
	private final int height;
    private final int max_ascension;
    private final int max_descension;

	public Font(@NonNull FontInfo font_info) {
		this.key_map = font_info.getKeyMap();
		TextureFile file = new TextureFile(font_info.getTextureName(),
										   GL11.GL_RGBA,
										   GL11.GL_LINEAR,
										   GL11.GL_LINEAR,
                                           GL12.GL_CLAMP_TO_EDGE,
                                           GL12.GL_CLAMP_TO_EDGE);
		this.texture = Resources.findResource(file);
		this.x_border = font_info.getBorderX();
		this.y_border = font_info.getBorderY();
		this.height = font_info.getHeight();
        this.max_ascension = font_info.getMaxAscension();
        this.max_descension = font_info.getMaxDescension();
	}

	public @Nullable Quad getQuad(int codepoint) {
		return key_map.get(codepoint);
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

    public int getMaxAscension() {
        return max_ascension;
    }

    public int getMaxDescension() {
        return max_descension;
    }

	public @NonNull Texture getTexture() {
		return texture;
	}

	public int getWidestCodepoint(@NonNull CharSequence text) {
		assert !text.isEmpty() : "Empty CharSequence";

		return text.codePoints().reduce(text.charAt(0), (current, codePoint) -> {
			var widest = getQuad(current);
			var quad = getQuad(codePoint);

			return null != quad && quad.getWidth() > widest.getWidth() ? codePoint : current;
		});
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
