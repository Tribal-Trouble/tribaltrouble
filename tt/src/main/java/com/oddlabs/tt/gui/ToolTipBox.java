package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public final class ToolTipBox extends TextField {
	static final float MAX_DELAY_SECONDS = 1.5f;
		
	private @NonNull IconQuad @Nullable [] icons;
	
	public ToolTipBox() {
		super(Skin.getSkin().getEditFont(), 1000);
	}
	
	@Override
			protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
					throw new RuntimeException("ToolTipBox.renderGeometry should not be called directly.");	}

	public final void append(@NonNull IconQuad @Nullable ... icons) {
		this.icons = icons;
	}

	@Override
	public void clear() {
		super.clear();
		icons = null;
	}
	
	public void render(int center_x, int top_y) {
		if (getText().isEmpty())
			return;
		ToolTipBoxInfo box = Skin.getSkin().getToolTipInfo();
		int text_width = getFont().getWidth(getText());
		int box_width = text_width + box.getLeftOffset() + box.getRightOffset();
		int box_height = box.getBox().getHeight();
		if (icons != null) {
			int i;
			for (i = 0; i < icons.length; i++) {
				box_width += icons[i].getWidth()/3;
			}
			box_width += icons[i - 1].getWidth()*2/3;
		}
		float x = Math.clamp(center_x - box_width/2f, 0, LocalInput.getViewWidth() - box_width);
		float y = Math.clamp(top_y - box_height, 0, LocalInput.getViewHeight() - box_height);

		box.getBox().render(x, y, box_width, ModeIconQuads.Mode.NORMAL);

		               TextLineRenderer.render(getFont(), getText(), x + box.getLeftOffset(), y + box.getBottomOffset(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Color.WHITE_INT);
		               GL11.glColor4f(1f, 1f, 1f, 1f); // Reset color after rendering
		               if (icons != null) {			float render_x = box_width - box.getRightOffset() - icons[icons.length - 1].getWidth();
            for (IconQuad icon : icons) {
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, icon.getTexture().getHandle());
				GL11.glBegin(GL11.GL_QUADS);
				icon.render(x + render_x, y + (box_height - icon.getHeight())/2f);
				GL11.glEnd();
                render_x -= icon.getWidth()/3f;
            }
		}
	}
}
