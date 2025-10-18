package com.oddlabs.tt.gui;

import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.CryptUtils;

public class PasswordLine extends EditLine {
	private final static StringBuffer star_string = new StringBuffer();

	private String password_digest;

	public PasswordLine(int width, int max_chars) {
		super(width, max_chars);
	}

        @Override
	protected final void renderText(TextLineRenderer text_renderer, int x, int y, int offset_x, float clip_left, float clip_right, float clip_bottom, float clip_top, int render_index) {
		star_string.delete(0, star_string.length());
		for (int i = 0; i < getText().length(); i++) {
            star_string.append('*');
        }

		text_renderer.render(x, y, offset_x, clip_left, clip_right, clip_bottom, clip_top, star_string, render_index);
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
	
	public final String getPasswordDigest() {
		return password_digest;
	}

	public final void setPasswordDigest(String password_digest) {
		this.password_digest = password_digest;
	}
}
