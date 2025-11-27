package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.util.Quad;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;

public class CampaignDialogForm extends Form {
	private static final int WIDTH = 300;

	private final Runnable runnable;
	private final boolean cancel;

	private HorizButton ok_button;

	public CampaignDialogForm(@NonNull CharSequence header, @NonNull CharSequence text, Quad image, @NonNull Origin align) {
		this(header, text, image, align, null);
	}

	public CampaignDialogForm(@NonNull CharSequence header, @NonNull CharSequence text, Quad image, @NonNull Origin align, Runnable runnable) {
		this(header, text, image, align, runnable, false);
	}

	public CampaignDialogForm(@NonNull CharSequence header, @NonNull CharSequence text, Quad image, @NonNull Origin align, Runnable runnable, boolean cancel) {
		this.runnable = runnable;
		this.cancel = cancel;
		buildForm(header, text, image, align, cancel);
		ok_button.addMouseClickListener((int _, int _, int _, int _) -> {
                    remove();
                    run();
                });
	}

	protected void run() {
		if (runnable != null)
			runnable.run();
	}

	@Override
	protected final void doCancel() {
		if (!cancel)
			run();
	}

	private void buildForm(@NonNull CharSequence header, @NonNull CharSequence text, @Nullable Quad image, @NonNull Origin align, boolean cancel) {
		GUIIcon gui_icon = null;
		if (image != null) {
			gui_icon = new GUIIcon(image);
			addChild(gui_icon);
		}
		Label header_label = new Label(header, Skin.getSkin().getHeadlineFont());
		addChild(header_label);
		LabelBox label_box = new LabelBox(text, Skin.getSkin().getEditFont(), WIDTH);
		addChild(label_box);
		ok_button = new OKButton(80);
		addChild(ok_button);

		if (gui_icon != null) {
			gui_icon.place();
			if (align == Origin.AT_START) {
				label_box.place(gui_icon, RIGHT_MID);
			} else {
				label_box.place(gui_icon, LEFT_MID);
			}
		} else {
			label_box.place();
		}
		header_label.place(label_box, TOP_LEFT);
		ok_button.place(Origin.AT_END);
		if (cancel) {
			HorizButton cancel_button = new CancelButton(80);
			addChild(cancel_button);
			cancel_button.place(ok_button, RIGHT_MID);
			cancel_button.addMouseClickListener((int _, int _, int _, int _) -> this.cancel());
		}

		compileCanvas();
		centerPos();
	}

	@Override
	public final void setFocus() {
		ok_button.setFocus();
	}
}
