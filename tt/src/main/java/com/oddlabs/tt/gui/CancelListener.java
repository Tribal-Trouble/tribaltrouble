package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.MouseClickListener;

public final class CancelListener implements MouseClickListener {
	private final Form form;

	public CancelListener(Form form) {
		this.form = form;
	}

    @Override
	public void mouseClicked(int button, int x, int y, int clicks) {
		form.cancel();
	}
}
