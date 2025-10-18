package com.oddlabs.tt.form;

import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Quad;

public final class InGameCampaignDialogForm extends CampaignDialogForm {
	private final WorldViewer viewer;

	public InGameCampaignDialogForm(WorldViewer viewer, CharSequence header, CharSequence text, Quad image, int align) {
		this(viewer, header, text, image, align, null);
	}

	public InGameCampaignDialogForm(WorldViewer viewer, CharSequence header, CharSequence text, Quad image, int align, Runnable runnable) {
		this(viewer, header, text, image, align, runnable, false);
	}

	public InGameCampaignDialogForm(WorldViewer viewer, CharSequence header, CharSequence text, Quad image, int align, Runnable runnable, boolean cancel) {
		super(header, text, image, align, runnable, cancel);
		this.viewer = viewer;
		viewer.setPaused(true);
	}

    @Override
	protected void run() {
		viewer.setPaused(false);
		super.run();
	}
}
