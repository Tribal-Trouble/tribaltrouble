package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public class Label extends TextField implements Comparable<Label> {
	public static final int ALIGN_LEFT		= 0;
	public static final int ALIGN_CENTER	= 1;
	public static final int ALIGN_RIGHT		= 2;
	public static final float[] DEFAULT_COLOR = Color.argb4f(0xFFFFFFFF);
	public static final float[] DISABLED_COLOR = Color.argb4f(0xB2B2B2B2);

	private final int align;
	private final @NonNull TextLineRenderer text_renderer;

	private float[] color = DEFAULT_COLOR;

	public Label(@NonNull CharSequence text, @NonNull Font font) {
		this(text, font, font.getWidth(text), ALIGN_LEFT);
	}

	public Label(@NonNull CharSequence text, @NonNull Font font, int width) {
		this(text, font, width, ALIGN_LEFT);
	}

	public Label(@NonNull CharSequence text, @NonNull Font font, int width, int align) {
		super(text, font, Integer.MAX_VALUE);
		this.align = align;
		text_renderer = new TextLineRenderer(font);
		setDim(width, font.getHeight());
	}

	public final void setColor(float[] color) {
		this.color = color;
	}

	@Override
	protected final void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
		// Radeon 9200 doesn't like glColor between Begin/End if not followed by a glVertex
		GL11.glEnd();
		if (isDisabled()) {
			GL11.glColor4f(DISABLED_COLOR[0], DISABLED_COLOR[1], DISABLED_COLOR[2], DISABLED_COLOR[3]);
		} else {
			GL11.glColor4f(color[0], color[1], color[2], color[3]);
		}
		GL11.glBegin(GL11.GL_QUADS);
            switch (align) {
                case ALIGN_LEFT:
                    text_renderer.renderCropped(0, 0, clip_left, clip_right, clip_bottom, clip_top, getText());
                    break;
                case ALIGN_CENTER:
                    text_renderer.render(0, 0, (getWidth() - getFont().getWidth(getText()))/2, clip_left, clip_right, clip_bottom, clip_top, getText(), -1);
                    break;
                case ALIGN_RIGHT:
                    text_renderer.render(0, 0, getWidth() - getFont().getWidth(getText()), clip_left, clip_right, clip_bottom, clip_top, getText(), -1);
                    break;
                default:
                    break;
            }
		GL11.glEnd();
		GL11.glColor3f(1f, 1f, 1f);
		GL11.glBegin(GL11.GL_QUADS);
	}

	@Override
	public int compareTo(@NonNull Label o) {
		return getText().toString().compareToIgnoreCase(o.getText().toString());
	}
}
