package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class InGameCampaignDialogForm extends CampaignDialogForm {
	private final @NonNull WorldViewer viewer;

	public InGameCampaignDialogForm(@NonNull WorldViewer viewer, @NonNull CharSequence header, @NonNull CharSequence text, @NonNull IconQuad image, @NonNull Origin align) {
		this(viewer, header, text, image, align, null);
	}

	public InGameCampaignDialogForm(@NonNull WorldViewer viewer, @NonNull CharSequence header, @NonNull CharSequence text, @NonNull IconQuad image, @NonNull Origin align, @Nullable Runnable runnable) {
		this(viewer, header, text, image, align, runnable, false);
	}

	public InGameCampaignDialogForm(@NonNull WorldViewer viewer, @NonNull CharSequence header, @NonNull CharSequence text, @NonNull IconQuad image, @NonNull Origin align, @Nullable Runnable runnable, boolean cancel) {
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
