package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.render.UIRenderer;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface LoadCallback {
	UIRenderer load(@NonNull GUIRoot gui_root);
}
