package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.util.Color;
import com.oddlabs.util.CryptUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** provides password entry substituting the password characters with asterisks */
public class PasswordLine extends EditLine {
	private @Nullable String password_digest;

	public PasswordLine(int width, int max_chars) {
		super(width, max_chars);
	}

	@Override
	protected @NonNull CharSequence getDisplayText() {
        if (getText().isEmpty()) {
            return "";
        }
		if (isActive()) {
			return "*".repeat(getText().length());
		} else {
			int asteriskWidth = getRenderedWidth("*");
			if (asteriskWidth == 0) return "";
			int numAsterisks = max_text_width / asteriskWidth;
			return "*".repeat(numAsterisks);
		}
    }

	@Override
	protected void renderText(@NonNull GUIRenderer renderer, @NonNull Box box, int offset_x, int render_index) {
		var displayText = getDisplayText();
		TextLineRenderer.render(renderer, getFont(), displayText, box.getLeftOffset() + offset_x, box.getBottomOffset(), box.getLeftOffset(), getWidth() - box.getRightOffset(), Color.WHITE_INT);
		if (render_index != -1) {
			int cursorX = getRenderedWidth(displayText.subSequence(0, render_index));
			Index.renderIndex(renderer, box.getLeftOffset() + offset_x + cursorX, box.getBottomOffset(), getFont());
		}
	}
	
	@Override
	protected final boolean insert(int index, char key) {
		boolean result = super.insert(index, key);
		updatePassword();
		return result;
		
	}

	@Override
	protected final void delete(int index) {
		super.delete(index);
		updatePassword();
	}

	private void updatePassword() {
		password_digest = CryptUtils.digest(getText().toString());
	}
	
	public final @Nullable String getPasswordDigest() {
		return password_digest;
	}

	public final void setPasswordDigest(@Nullable String password_digest) {
		this.password_digest = password_digest;
	}
}
